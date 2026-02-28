# inbox-service

Microservice fuer Inbox-Nachrichten auf Basis von Java 21, Spring Boot 4.x und Maven.

## Lokal starten

Voraussetzungen:
- Java 21
- Maven Wrapper (`mvnw`)
- Laufende MariaDB

Benutzte Environment-Variablen:
- `INBOX_DB_HOST` (Default: `localhost`)
- `INBOX_DB_PORT` (Default: `3306`)
- `INBOX_DB_NAME` (Default: `inbox`)
- `INBOX_DB_USER` (Default: `root`)
- `INBOX_DB_PASSWORD` (Default: leer)
- `INBOXPORT` (Default: `8105`)
- `INBOX_INTERNAL_TOKEN` (Optional, Default: `change-me`)

Start:
```bash
./mvnw spring-boot:run
```

Unter Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

Die API laeuft standardmaessig auf:
- `http://localhost:8105/api/v1`

Swagger UI:
- `http://localhost:8105/api/v1/swagger-ui`

## Mit Docker starten

Beispiel-ENV fuer Docker Compose:
```bash
INBOX_DB_HOST=mariadb
INBOX_DB_PORT=3306
INBOX_DB_NAME=inbox
INBOX_DB_USER=root
INBOX_DB_PASSWORD=
INBOXPORT=8105
INBOX_INTERNAL_TOKEN=change-me
```

Build und Start aller Services (MariaDB + inbox-service):
```bash
docker compose up --build
```

Swagger UI:
- `http://localhost:8105/api/v1/swagger-ui`

Healthcheck:
- `http://localhost:8105/api/v1/actuator/health`

Stoppen:
```bash
docker compose down
```

Datenbankdaten entfernen:
```bash
docker compose down -v
```

## IntelliJ Run Configuration

Beispiel fuer Environment Variables:

`INBOX_DB_HOST=localhost;INBOX_DB_PORT=3306;INBOX_DB_NAME=inbox;INBOX_DB_USER=root;INBOX_DB_PASSWORD=;INBOXPORT=8105;INBOX_INTERNAL_TOKEN=change-me`

Hinweis: `Include system environment variables` kann aktiviert bleiben, da die Variablen service-spezifisch sind.

## Hinweise zur DB-Konfiguration (nur Development)

Die lokale Default-Konfiguration in `src/main/resources/application.properties` nutzt:
- URL: `jdbc:mariadb://${INBOX_DB_HOST:localhost}:${INBOX_DB_PORT:3306}/${INBOX_DB_NAME:inbox}`
- User: `${INBOX_DB_USER:root}`
- Passwort: `${INBOX_DB_PASSWORD:}`

Diese Konfiguration ist ausschliesslich fuer lokale Entwicklung gedacht und nicht fuer Produktion geeignet.
