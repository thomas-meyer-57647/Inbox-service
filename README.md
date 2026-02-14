# inbox-service

Microservice fuer Inbox-Nachrichten auf Basis von Java 21, Spring Boot 4.x und Maven.

## Lokal starten

Voraussetzungen:
- Java 21
- Maven Wrapper (`mvnw`)
- Laufende MariaDB auf `localhost:3306`
- Datenbank `inbox`

Start:
```bash
./mvnw spring-boot:run
```

Unter Windows (PowerShell):
```powershell
.\mvnw.cmd spring-boot:run
```

Die API laeuft standardmaessig auf:
- `http://localhost:8082/api/v1`

Swagger UI:
- `http://localhost:8082/api/v1/swagger-ui`

## Mit Docker starten

Build und Start aller Services (MariaDB + inbox-service):
```bash
docker compose up --build
```

Swagger UI:
- `http://localhost:8082/api/v1/swagger-ui`

Stoppen:
```bash
docker compose down
```

Datenbankdaten entfernen:
```bash
docker compose down -v
```

## Hinweise zur DB-Konfiguration (nur Development)

Die lokale Default-Konfiguration in `src/main/resources/application.properties` nutzt:
- URL: `jdbc:mariadb://localhost:3306/inbox`
- User: `root`
- Passwort: leer

Diese Konfiguration ist ausschliesslich fuer lokale Entwicklung gedacht und nicht fuer Produktion geeignet.
