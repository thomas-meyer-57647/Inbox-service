Set-Location E:\DevProjects\InboxService

@"
# AGENTS.md – Projektregeln für den Agent

## Ziel
Quellcode auf Pflichtenheft heben (MUSS-Kriterien zuerst). Am Ende: Build + Tests grün, Docker vorhanden, Swagger-Doku vollständig.

## Build & Test (muss grün sein)
- `mvn -q clean test`
- optional: `mvn -q clean verify`

## Arbeitsweise (token-sparsam)
- Kleine Schritte: max. 3–5 Dateien pro Änderung
- Max. 120 Zeilen Output pro Antwort
- Keine langen Analysen, nur: Dateien + Patch/Diff + Tests
- Wenn Infos fehlen: STOP und exakt benötigte Datei nennen (Pfad)

## Tests (Pflicht)
- Zu jedem Feature mindestens:
    - 1 positiver Test
    - 1 negativer Test
- Tests via MockMvc / spring-security-test, ggf. Testcontainers für DB/Flyway

## Security (Pflichtenheft)
- OAuth2 Resource Server (JWT Bearer)
- JWKS-Validierung (jwk-set-uri)
- Scopes: inbox.read, inbox.write, inbox.deliver
- Tenant aus JWT claim `tenant_id` ist Source of Truth
- Tenant-Mismatch => 403 + ErrorCode TENANT_MISMATCH

## Docker (Pflicht)
- Dockerfile + docker-compose (App + MariaDB)
- `docker compose up --build` muss starten

## Swagger/OpenAPI (Pflicht)
Für alle neu hinzugefügten/angepassten Controller/DTOs/Enums:
- Controller: @Tag, @Operation, @ApiResponses (400/401/403/404), @SecurityRequirement
- DTO/Enums: @Schema pro Feld (description + example)
  "@ | Set-Content -Encoding UTF8 AGENTS.md

## Pflicht: Tests wirklich ausführen
Nach JEDEM Task müssen diese Commands ausgeführt werden:
- mvn -q clean test
  Wenn ein Test fehlschlägt:
- Fix im selben Task nachliefern, erneut mvn -q clean test ausführen
  Der Agent darf diese Commands ausführen (und ich bestätige).