# Java Guice Template

A minimal project template using [Google Guice](https://github.com/google/guice) for dependency injection, built with Maven and Java 17.

## Project Structure

```
src/
├── main/java/com/example/
│   ├── app/
│   │   └── Application.java          # Entry point
│   ├── module/
│   │   └── AppModule.java            # Guice module (bindings)
│   └── service/
│       ├── GreetingService.java       # Service interface
│       └── impl/
│           └── GreetingServiceImpl.java  # Service implementation
└── test/java/com/example/
    └── service/
        └── GreetingServiceTest.java   # Unit tests
```

## Prerequisites

- Java 17+
- Maven 3.8+

## Build

```bash
mvn clean package
```

## Run

```bash
java -jar target/guice-template-1.0.0-SNAPSHOT.jar
```

## Test

```bash
mvn test
```

## Adding New Services

1. Define an interface in `com.example.service`.
2. Create an implementation in `com.example.service.impl` and annotate it with `@Singleton` (or your preferred scope).
3. Register the binding in `AppModule.configure()`:
   ```java
   bind(MyService.class).to(MyServiceImpl.class);
   ```
4. Inject wherever needed using `@Inject`:
   ```java
   @Inject
   public MyConsumer(MyService myService) {
       this.myService = myService;
   }
   ```
