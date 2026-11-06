# quarkus-getting-started

## Aufgabe 2.4: Aufgabenverwaltung

Aufbau: `TaskController` nimmt HTTP-Anfragen entgegen, `TaskService` liefert
Beispieldaten und `TaskDto` beschreibt die JSON-Daten.

| Methode | URL | Antwort |
| --- | --- | --- |
| GET | `/tasks` | 200: Liste mit einer Beispielaufgabe |
| GET | `/tasks/{id}` | 200: Beispielaufgabe mit ID 1; sonst 404 |
| POST | `/tasks` | 200: übergebene Aufgabe mit Beispiel-ID 2 und Status `offen` |
| PUT | `/tasks/{id}` | 200: übergebene Aufgabe mit der URL-ID; nur ID 1 ist vorhanden |
| DELETE | `/tasks/{id}` | 204 ohne Inhalt für ID 1; sonst 404 |

Wie im Auftrag erlaubt, sind die Antworten statisch. POST, PUT und DELETE
ändern die Beispieldaten nicht dauerhaft. Wiederholte POST-Anfragen erhalten
deshalb dieselbe Beispiel-ID 2. Eine Datenbank ist nicht eingebunden.

POST benötigt einen nicht leeren `title`. PUT benötigt zusätzlich den
`status` (`offen` oder `erledigt`). `description` ist optional.
Fehlende oder ungültige Pflichtwerte liefern 400.

Beispiel für POST `/tasks`:

```json
{
  "title": "Lernen",
  "description": "REST-API üben"
}
```

Unter Windows starten: `.\mvnw.cmd quarkus:dev`.
Danach: <http://localhost:8080/tasks>.
Tests ausführen: `.\mvnw.cmd test`.

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
