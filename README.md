# Aufgabenverwaltung (Modul 223, individuelles Projekt)

Webanwendung zum Verwalten von Aufgaben. Aufgaben können Projekten zugeordnet, mit
Labels versehen, mit einem Fälligkeitsdatum geplant und als erledigt markiert werden.
Benutzer melden sich mit einem JWT an; Admins dürfen zusätzlich Projekte und Labels löschen.

## Starten

Das Projekt benötigt **Java 25** und läuft darum im **Devcontainer**
(VS Code → `Ctrl+Shift+P` → *Reopen in Container*).

1. JWT-Schlüssel erzeugen (der private Schlüssel ist nicht im Repository):

   ```shell
   openssl genpkey -algorithm RSA -out src/main/resources/privateKey.pem
   openssl rsa -pubout -in src/main/resources/privateKey.pem -out src/main/resources/publicKey.pem
   ```

2. Im Terminal des Devcontainers starten:

   ```shell
   ./mvnw quarkus:dev
   ```

3. Öffnen (Port gemäss VS-Code-Tab *Ports*):
   - Frontend: <http://localhost:8080/>
   - Swagger UI: <http://localhost:8080/q/swagger-ui>

Die Daten liegen in einer lokalen H2-Datenbank unter `./data/` (nicht im Repository).
Zum Zurücksetzen den Ordner `data` löschen; beim nächsten Start werden die Testdaten neu geladen.

### Testbenutzer (nur im Dev-Profil)

| Benutzername | Passwort | Rolle |
|---|---|---|
| `admin` | `admin123` | Admin |
| `anna` | `anna1234` | User |

Neue Benutzer können sich im Frontend registrieren (Rolle *User*).

## Datenmodell (Aufgabe 3.3 / 3.4)

Vier Entitäten mit Beziehungen, siehe [docs/klassendiagramm.md](docs/klassendiagramm.md):

| Entität | Beschreibung | Beziehungen |
|---|---|---|
| `Task` | Aufgabe mit Titel, Beschreibung, Status (`offen`/`erledigt`), Fälligkeitsdatum | n:1 `Project`, n:m `Label`, n:1 `AppUser` (Ersteller) |
| `Project` | Gruppiert Aufgaben | 1:n `Task` |
| `Label` | Schlagwort mit Farbe, z.B. „Dringend“ | n:m `Task` (Join-Tabelle `task_label`) |
| `AppUser` | Benutzer mit BCrypt-gehashtem Passwort und Rolle | 1:n `Task` |

Testdaten (Aufgabe 3.5) lädt `DemoDataLoader` beim Start im Dev- und Test-Profil:
2 Benutzer, 3 Projekte, 4 Labels und 7 Aufgaben.

## REST-API

Alle Endpunkte ausser `/auth/*` und `/hello` brauchen den Header `Authorization: Bearer <token>`.

| Methode | URL | Beschreibung | Antwort |
|---|---|---|---|
| POST | `/auth/register` | Benutzer registrieren (`username`, `password`) | 200 Token; 409 Name vergeben |
| POST | `/auth/login` | Anmelden | 200 Token; 401 falsche Daten |
| GET | `/tasks` | Aufgaben, optional `?status=offen&projectId=1&labelId=2` | 200 Liste |
| GET | `/tasks/{id}` | Eine Aufgabe | 200; 404 |
| POST | `/tasks` | Aufgabe erstellen (Ersteller = angemeldeter Benutzer) | 200; 400 |
| PUT | `/tasks/{id}` | Aufgabe bearbeiten (Status Pflicht) | 200; 400; 404 |
| DELETE | `/tasks/{id}` | Aufgabe löschen | 204; 404 |
| GET | `/projects` | Projekte mit Anzahl (offener) Aufgaben | 200 Liste |
| GET | `/projects/{id}` | Ein Projekt | 200; 404 |
| POST / PUT | `/projects`, `/projects/{id}` | Projekt erstellen/bearbeiten | 200; 400; 409 |
| DELETE | `/projects/{id}` | Projekt löschen (**nur Admin**), Aufgaben bleiben erhalten | 204; 403; 404 |
| GET | `/labels` | Labels | 200 Liste |
| POST / PUT | `/labels`, `/labels/{id}` | Label erstellen/bearbeiten (`name`, `color` als `#rrggbb`) | 200; 400; 409 |
| DELETE | `/labels/{id}` | Label löschen (**nur Admin**) | 204; 403; 404 |

Beispiel `POST /tasks`:

```json
{
  "title": "Prüfung vorbereiten",
  "description": "Block 1 bis 8",
  "dueDate": "2026-12-01",
  "projectId": 1,
  "labelIds": [1, 2]
}
```

Fehler werden als JSON zurückgegeben, z.B. `{"message": "Projekt existiert nicht"}`.

## Architektur und Dependency Injection (Aufgabe 4.2)

Schichten: **Controller** (HTTP, DTO-Validierung) → **Service** (Fachlogik, Transaktionen,
DTO-Mapping) → **Repository** (Datenzugriff mit Panache) → **Datenbank**.

Dependency Injection übernimmt der CDI-Container von Quarkus (ArC):

- Services, Repositories und der `DtoMapper` sind mit `@ApplicationScoped` annotiert.
  Quarkus erstellt davon je **eine** Instanz und verwaltet ihren Lebenszyklus.
- Abhängigkeiten werden per **Feldinjektion** mit `@Inject` angefordert, z.B.
  `TaskController` → `TaskService` → `TaskRepository`, `ProjectRepository`, `LabelRepository`,
  `AppUserRepository`, `DtoMapper`. Keine Klasse erzeugt ihre Abhängigkeiten selbst mit `new`
  (Inversion of Control).
- Konfigurationswerte werden mit `@ConfigProperty` injiziert (z.B. der JWT-Issuer in
  `AuthService`, der Schalter für die Testdaten in `DemoDataLoader`).
- Das JWT des aktuellen Requests wird mit `@Inject JsonWebToken jwt` in den `TaskController`
  injiziert (request-bezogen, obwohl der Controller nur einmal existiert).

## Authentifizierung (Aufgabe 5.3)

- `POST /auth/login` prüft das Passwort gegen den BCrypt-Hash und stellt ein mit
  `privateKey.pem` signiertes JWT aus (Claims: `upn` = Benutzername, `groups` = Rolle,
  `iss`, `exp` = 8 Stunden).
- Quarkus prüft eingehende Tokens mit `publicKey.pem` (Signatur, Issuer, Ablauf).
- `@Authenticated` schützt Tasks, Projekte und Labels; `@RolesAllowed("Admin")` das Löschen
  von Projekten und Labels. Ohne Token → 401, ohne passende Rolle → 403.

## Frontend (Aufgabe 8.1)

HTML, CSS und JavaScript ohne Build-Schritt in `src/main/resources/META-INF/resources/`.
Quarkus liefert es direkt unter `/` aus, darum braucht es kein CORS.

- Anmelden und Registrieren; das Token wird im `localStorage` gespeichert
- Aufgaben anzeigen, nach Status, Projekt und Label filtern
- Aufgaben erstellen, bearbeiten, per Checkbox erledigen und löschen
- Projekte und Labels erstellen; Admins können sie löschen
- Überfällige Aufgaben werden rot markiert; Hell-/Dunkelmodus folgt dem System

## Tests (Aufgabe 6.3)

```shell
./mvnw test
```

- `TaskControllerTest`: Integrationstests Controller + Service + Datenbank (H2 in-memory)
  im Given-When-Then-Schema, inkl. End-to-End-Ablauf *erstellen → erledigen → löschen*
- `AuthAndProjectTest`: Login, Registrierung, Rollenprüfung (403 für User, 204 für Admin)
- Die Tests laufen mit einer eigenen In-Memory-Datenbank (`%test`-Profil) und verändern die
  Entwicklungsdaten nicht.

---

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/quarkus-getting-started-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
