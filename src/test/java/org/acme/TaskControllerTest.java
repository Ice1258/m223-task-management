package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integrationstests: Controller + Service + Datenbank (H2 in-memory) im Zusammenspiel.
 * Aufbau nach Given-When-Then.
 */
@QuarkusTest
class TaskControllerTest {

    private String token;

    @BeforeEach
    void login() {
        token = TestAuth.userToken();
    }

    @Test
    void getTasks_withoutToken_returns401() {
        given()
        .when().get("/tasks")
        .then().statusCode(401);
    }

    @Test
    void getTasks_withToken_returnsDemoTasks() {
        given().auth().oauth2(token)
        .when().get("/tasks")
        .then().statusCode(200)
                .body("size()", greaterThanOrEqualTo(2))
                .body("title", hasItem("Einkaufen"));
    }

    @Test
    void getTasks_filteredByStatus_returnsOnlyThatStatus() {
        given().auth().oauth2(token).queryParam("status", "erledigt")
        .when().get("/tasks")
        .then().statusCode(200)
                .body("status", everyItem(is("erledigt")));
    }

    @Test
    void createTask_withValidData_storesTaskWithCreator() {
        // Given: eine neue Aufgabe ohne Status
        String body = """
                {"title":"Integrationstest schreiben","dueDate":"2030-01-15"}
                """;

        given().auth().oauth2(token).contentType("application/json").body(body)
        // When: sie erstellt wird
        .when().post("/tasks")
        // Then: bekommt sie eine ID, den Status "offen" und den angemeldeten Benutzer als Ersteller
        .then().statusCode(200)
                .body("id", notNullValue())
                .body("status", is("offen"))
                .body("dueDate", is("2030-01-15"))
                .body("createdBy", is("anna"));
    }

    @Test
    void createTask_withProjectAndLabels_linksThem() {
        int projectId = given().auth().oauth2(token).contentType("application/json")
                .body("{\"name\":\"Testprojekt Labels\"}")
                .when().post("/projects")
                .then().statusCode(200).extract().path("id");
        int labelId = given().auth().oauth2(token).contentType("application/json")
                .body("{\"name\":\"Testlabel\",\"color\":\"#123456\"}")
                .when().post("/labels")
                .then().statusCode(200).extract().path("id");

        given().auth().oauth2(token).contentType("application/json")
                .body("{\"title\":\"Mit Projekt\",\"projectId\":" + projectId + ",\"labelIds\":[" + labelId + "]}")
        .when().post("/tasks")
        .then().statusCode(200)
                .body("projectName", is("Testprojekt Labels"))
                .body("labels", hasSize(1))
                .body("labels[0].name", is("Testlabel"));
    }

    @Test
    void createTask_withoutTitle_returns400() {
        given().auth().oauth2(token).contentType("application/json").body("{\"title\":\"  \"}")
        .when().post("/tasks")
        .then().statusCode(400);
    }

    @Test
    void createTask_withUnknownProject_returns400() {
        given().auth().oauth2(token).contentType("application/json")
                .body("{\"title\":\"X\",\"projectId\":99999}")
        .when().post("/tasks")
        .then().statusCode(400)
                .body("message", is("Projekt existiert nicht"));
    }

    @Test
    void updateTask_withInvalidStatus_returns400() {
        int id = createTask("Status testen");

        given().auth().oauth2(token).contentType("application/json")
                .body("{\"title\":\"Status testen\",\"status\":\"fertig\"}")
        .when().put("/tasks/" + id)
        .then().statusCode(400);
    }

    @Test
    void updateTask_unknownId_returns404() {
        given().auth().oauth2(token).contentType("application/json")
                .body("{\"title\":\"Gibt es nicht\",\"status\":\"offen\"}")
        .when().put("/tasks/99999")
        .then().statusCode(404);
    }

    @Test
    void deleteTask_unknownId_returns404() {
        given().auth().oauth2(token)
        .when().delete("/tasks/99999")
        .then().statusCode(404);
    }

    /**
     * End-to-End-Ablauf: Aufgabe erstellen, als erledigt markieren, in der Liste pruefen, loeschen.
     */
    @Test
    void taskWorkflow_createCompleteAndDelete() {
        // Given: eine neu erstellte Aufgabe
        int id = createTask("E2E Aufgabe");

        // When: sie als erledigt markiert wird
        given().auth().oauth2(token).contentType("application/json")
                .body("{\"title\":\"E2E Aufgabe\",\"status\":\"erledigt\"}")
                .when().put("/tasks/" + id)
                .then().statusCode(200).body("status", is("erledigt"));

        // Then: erscheint sie in der Liste der erledigten Aufgaben
        given().auth().oauth2(token).queryParam("status", "erledigt")
                .when().get("/tasks")
                .then().statusCode(200).body("id", hasItem(id));

        // And: nach dem Loeschen ist sie nicht mehr vorhanden
        given().auth().oauth2(token).when().delete("/tasks/" + id).then().statusCode(204);
        given().auth().oauth2(token).when().get("/tasks/" + id).then().statusCode(404);
        given().auth().oauth2(token).when().get("/tasks")
                .then().statusCode(200).body("id", not(hasItem(id)));
    }

    private int createTask(String title) {
        return given().auth().oauth2(token).contentType("application/json")
                .body("{\"title\":\"" + title + "\"}")
                .when().post("/tasks")
                .then().statusCode(200)
                .extract().path("id");
    }
}
