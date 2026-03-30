#!/usr/bin/env python3
"""
VIN API Data Extractor

Reads VIN numbers from an input CSV, authenticates via OAuth2 client credentials,
calls a GET API for each VIN, and writes the results to an output CSV.
"""

import argparse
import csv
import json
import logging
import os
import sys
import time
from datetime import datetime, timedelta, timezone
from pathlib import Path

import requests
from dotenv import load_dotenv

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


class OAuthTokenManager:
    """Manages OAuth2 client-credentials tokens with automatic refresh."""

    def __init__(self, token_url: str, client_id: str, client_secret: str, scope: str = ""):
        self.token_url = token_url
        self.client_id = client_id
        self.client_secret = client_secret
        self.scope = scope
        self._token: str | None = None
        self._expires_at: datetime | None = None

    def get_token(self) -> str:
        if self._token and self._expires_at and datetime.now(timezone.utc) < self._expires_at:
            return self._token

        logger.info("Requesting new OAuth token from %s", self.token_url)
        payload = {
            "grant_type": "client_credentials",
            "client_id": self.client_id,
            "client_secret": self.client_secret,
        }
        if self.scope:
            payload["scope"] = self.scope

        resp = requests.post(self.token_url, data=payload, timeout=30)
        resp.raise_for_status()
        data = resp.json()

        self._token = data["access_token"]
        expires_in = int(data.get("expires_in", 3600))
        # Refresh 60 seconds early to avoid edge-case expiry
        self._expires_at = datetime.now(timezone.utc) + timedelta(seconds=max(expires_in - 60, 0))
        logger.info("OAuth token acquired (expires in %d s)", expires_in)
        return self._token


def call_vin_api(
    vin: str,
    api_url_template: str,
    token_manager: OAuthTokenManager,
    max_retries: int = 3,
    backoff_base: float = 2.0,
) -> dict:
    """Call the VIN GET API with retry and exponential back-off."""
    url = api_url_template.format(vin=vin)
    headers = {"Authorization": f"Bearer {token_manager.get_token()}"}

    for attempt in range(1, max_retries + 1):
        try:
            logger.info("GET %s (attempt %d/%d)", url, attempt, max_retries)
            resp = requests.get(url, headers=headers, timeout=30)

            if resp.status_code == 401 and attempt < max_retries:
                logger.warning("Received 401 — refreshing token and retrying")
                token_manager._token = None
                headers["Authorization"] = f"Bearer {token_manager.get_token()}"
                continue

            resp.raise_for_status()
            return resp.json()

        except requests.RequestException as exc:
            if attempt == max_retries:
                logger.error("Failed after %d attempts for VIN %s: %s", max_retries, vin, exc)
                return {"error": str(exc)}
            wait = backoff_base ** attempt
            logger.warning("Attempt %d failed (%s) — retrying in %.1f s", attempt, exc, wait)
            time.sleep(wait)

    return {"error": "max retries exceeded"}


def flatten_dict(d: dict, parent_key: str = "", sep: str = "_") -> dict:
    """Flatten a nested dict into a single-level dict with composite keys."""
    items: list[tuple[str, object]] = []
    for k, v in d.items():
        new_key = f"{parent_key}{sep}{k}" if parent_key else k
        if isinstance(v, dict):
            items.extend(flatten_dict(v, new_key, sep).items())
        elif isinstance(v, list):
            items.append((new_key, json.dumps(v)))
        else:
            items.append((new_key, v))
    return dict(items)


def detect_vin_column(headers: list[str]) -> str:
    """Auto-detect which column holds the VIN value."""
    for h in headers:
        if h.strip().upper() == "VIN":
            return h
    for h in headers:
        if "VIN" in h.upper():
            return h
    raise ValueError(
        f"Could not auto-detect a VIN column. Headers found: {headers}. "
        "Use --vin-column to specify the column name explicitly."
    )


def process_csv(
    input_path: str,
    output_path: str,
    api_url_template: str,
    token_manager: OAuthTokenManager,
    vin_column: str | None = None,
    max_retries: int = 3,
    raw_json_column: bool = False,
) -> None:
    """Read input CSV, call API per row, write results to output CSV."""
    input_file = Path(input_path)
    if not input_file.exists():
        logger.error("Input file not found: %s", input_path)
        sys.exit(1)

    with open(input_file, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        input_headers = list(reader.fieldnames or [])
        if not input_headers:
            logger.error("Input CSV has no headers")
            sys.exit(1)

        vin_col = vin_column or detect_vin_column(input_headers)
        logger.info("Using VIN column: '%s'", vin_col)

        rows = list(reader)

    logger.info("Loaded %d records from %s", len(rows), input_path)

    results: list[dict] = []
    all_response_keys: list[str] = []

    for idx, row in enumerate(rows, start=1):
        vin = row.get(vin_col, "").strip()
        if not vin:
            logger.warning("Row %d: VIN column is empty — skipping", idx)
            result = {**row, "api_status": "skipped", "api_error": "empty VIN"}
            results.append(result)
            continue

        logger.info("Processing row %d/%d — VIN: %s", idx, len(rows), vin)
        response = call_vin_api(vin, api_url_template, token_manager, max_retries)

        if raw_json_column:
            result = {**row, "api_status": "success", "api_response_json": json.dumps(response)}
        else:
            flat = flatten_dict(response, parent_key="api")
            for key in flat:
                if key not in all_response_keys:
                    all_response_keys.append(key)
            error = response.get("error")
            result = {
                **row,
                "api_status": "error" if error else "success",
                **flat,
            }

        results.append(result)

    if raw_json_column:
        output_headers = input_headers + ["api_status", "api_response_json"]
    else:
        output_headers = input_headers + ["api_status"] + all_response_keys

    # Deduplicate while preserving order
    seen = set()
    unique_headers = []
    for h in output_headers:
        if h not in seen:
            seen.add(h)
            unique_headers.append(h)

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=unique_headers, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(results)

    logger.info("Results written to %s (%d rows)", output_path, len(results))


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="Extract vehicle data from a VIN API for each row in a CSV file.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python vin_api_extractor.py -i vins.csv -o results.csv
  python vin_api_extractor.py -i vins.csv -o results.csv --vin-column vehicle_vin
  python vin_api_extractor.py -i vins.csv -o results.csv --raw-json

Environment variables (or .env file):
  OAUTH_TOKEN_URL   - OAuth2 token endpoint
  OAUTH_CLIENT_ID   - OAuth2 client ID
  OAUTH_CLIENT_SECRET- OAuth2 client secret
  OAUTH_SCOPE       - (optional) OAuth2 scope
  VIN_API_URL       - API URL template with {vin} placeholder
        """,
    )
    parser.add_argument("-i", "--input", required=True, help="Path to the input CSV file containing VIN data")
    parser.add_argument("-o", "--output", required=True, help="Path for the output CSV file with API responses")
    parser.add_argument(
        "--vin-column",
        default=None,
        help="Name of the CSV column containing VIN numbers (auto-detected if omitted)",
    )
    parser.add_argument(
        "--api-url",
        default=os.getenv("VIN_API_URL", "https://api.example.com/vehicles/{vin}"),
        help="API URL template; use {vin} as placeholder (default: $VIN_API_URL or built-in demo URL)",
    )
    parser.add_argument(
        "--token-url",
        default=os.getenv("OAUTH_TOKEN_URL", ""),
        help="OAuth2 token endpoint (default: $OAUTH_TOKEN_URL)",
    )
    parser.add_argument(
        "--client-id",
        default=os.getenv("OAUTH_CLIENT_ID", ""),
        help="OAuth2 client ID (default: $OAUTH_CLIENT_ID)",
    )
    parser.add_argument(
        "--client-secret",
        default=os.getenv("OAUTH_CLIENT_SECRET", ""),
        help="OAuth2 client secret (default: $OAUTH_CLIENT_SECRET)",
    )
    parser.add_argument(
        "--scope",
        default=os.getenv("OAUTH_SCOPE", ""),
        help="OAuth2 scope (default: $OAUTH_SCOPE)",
    )
    parser.add_argument(
        "--max-retries",
        type=int,
        default=3,
        help="Maximum number of API call retries per VIN (default: 3)",
    )
    parser.add_argument(
        "--raw-json",
        action="store_true",
        default=False,
        help="Store the full API response as a single JSON column instead of flattening",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        default=False,
        help="Enable debug-level logging",
    )
    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    if not args.token_url:
        logger.error("OAuth token URL is required. Set OAUTH_TOKEN_URL or use --token-url.")
        sys.exit(1)
    if not args.client_id:
        logger.error("OAuth client ID is required. Set OAUTH_CLIENT_ID or use --client-id.")
        sys.exit(1)
    if not args.client_secret:
        logger.error("OAuth client secret is required. Set OAUTH_CLIENT_SECRET or use --client-secret.")
        sys.exit(1)

    token_manager = OAuthTokenManager(
        token_url=args.token_url,
        client_id=args.client_id,
        client_secret=args.client_secret,
        scope=args.scope,
    )

    process_csv(
        input_path=args.input,
        output_path=args.output,
        api_url_template=args.api_url,
        vin_column=args.vin_column,
        token_manager=token_manager,
        max_retries=args.max_retries,
        raw_json_column=args.raw_json,
    )


if __name__ == "__main__":
    main()
