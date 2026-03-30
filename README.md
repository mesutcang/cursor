# VIN API Data Extractor

A Python script that reads VIN (Vehicle Identification Number) records from a CSV file, authenticates via OAuth2, calls a GET API for each VIN, and writes the combined results to an output CSV.

## Features

- **OAuth2 client-credentials authentication** with automatic token caching and refresh
- **CSV input/output** — reads any CSV with a VIN column, appends API response fields to the output
- **Auto-detection** of the VIN column (or specify it explicitly)
- **Retry with exponential back-off** on transient API failures and 401 token expiry
- **Flattened JSON responses** — nested API responses are flattened into individual CSV columns
- **Raw JSON mode** — optionally store the full API response as a single JSON column
- **Configuration via environment variables** or CLI flags (supports `.env` files)

## Quick Start

### 1. Install dependencies

```bash
pip install -r requirements.txt
```

### 2. Configure credentials

Copy `.env.example` to `.env` and fill in your OAuth2 and API details:

```bash
cp .env.example .env
```

Edit `.env`:

```
OAUTH_TOKEN_URL=https://auth.example.com/oauth/token
OAUTH_CLIENT_ID=your_client_id
OAUTH_CLIENT_SECRET=your_client_secret
OAUTH_SCOPE=vehicle:read
VIN_API_URL=https://api.example.com/vehicles/{vin}
```

### 3. Prepare your input CSV

The CSV must contain a column with VIN numbers. The column name is auto-detected (looks for `VIN`), or you can specify it with `--vin-column`.

Example `input.csv`:

```
VIN,Year,Make
1HGBH41JXMN109186,2021,Honda
5YJSA1DG9DFP14705,2013,Tesla
```

### 4. Run the script

```bash
python vin_api_extractor.py -i input.csv -o output.csv
```

## Usage

```
python vin_api_extractor.py -i INPUT -o OUTPUT [OPTIONS]
```

| Flag | Description | Default |
|------|-------------|---------|
| `-i`, `--input` | Path to input CSV file (required) | — |
| `-o`, `--output` | Path to output CSV file (required) | — |
| `--vin-column` | Name of the VIN column in the CSV | Auto-detected |
| `--api-url` | API URL template with `{vin}` placeholder | `$VIN_API_URL` |
| `--token-url` | OAuth2 token endpoint | `$OAUTH_TOKEN_URL` |
| `--client-id` | OAuth2 client ID | `$OAUTH_CLIENT_ID` |
| `--client-secret` | OAuth2 client secret | `$OAUTH_CLIENT_SECRET` |
| `--scope` | OAuth2 scope | `$OAUTH_SCOPE` |
| `--max-retries` | Max API retries per VIN | `3` |
| `--raw-json` | Store full response as single JSON column | `false` |
| `--verbose` | Enable debug logging | `false` |

## Examples

Basic usage (credentials from `.env`):

```bash
python vin_api_extractor.py -i vins.csv -o results.csv
```

Specify VIN column and use raw JSON output:

```bash
python vin_api_extractor.py -i data.csv -o out.csv --vin-column vehicle_vin --raw-json
```

Pass credentials via CLI:

```bash
python vin_api_extractor.py \
  -i vins.csv -o results.csv \
  --token-url https://auth.example.com/oauth/token \
  --client-id my_id \
  --client-secret my_secret \
  --api-url "https://api.example.com/v2/vehicles/{vin}"
```

## Output

The output CSV contains all original input columns plus:

| Column | Description |
|--------|-------------|
| `api_status` | `success`, `error`, or `skipped` |
| `api_*` | Flattened API response fields (default mode) |
| `api_response_json` | Full JSON response (when `--raw-json` is used) |
