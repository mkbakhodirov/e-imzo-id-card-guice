# E-IMZO ID Card Guice

Java/Guice/Jetty conversion of the PHP demo at:

```text
example.uz/php/demo/eimzoidcard
```

The app keeps the original static ID-card QR UI and replaces the PHP result pages with Java servlets.

## Requirements

- Docker Desktop is installed and running.
- Ports `80`, `8080`, and `8081` are free.
- `C:\e-imzo-server` exists and contains `Dockerfile`, `e-imzo-server.jar`, `keys`, and `test-config.properties`.

## Run With Docker

```powershell
docker compose up --build
```

Redis is available for local development at:

```text
host=127.0.0.1
port=6379
password=test
db=1
```

Open:

```text
http://localhost/demo/eimzoidcard
```

E-IMZO server health check:

```text
http://localhost:8080/ping
```

## Run Jetty Locally

This starts only the Java web app on port `8081`; the app still expects E-IMZO server at `http://127.0.0.1:8080`.

```powershell
.\mvnw.cmd jetty:run
```

Open:

```text
http://localhost:8081/demo/eimzoidcard
```

## Stop Docker

```powershell
docker compose down
```
