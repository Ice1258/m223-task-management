package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
class TaskControllerTest {

    @Test
    void readsExampleTasks() {
        given().when().get("/tasks").then().statusCode(200)
                .body("$", hasSize(1)).body("[0].id", is(1));
        given().when().get("/tasks/1").then().statusCode(200)
                .body("title", is("REST-API umsetzen"));
        given().when().get("/tasks/99").then().statusCode(404);
    }

    @Test
    void createsAndUpdatesStaticResponses() {
        given().contentType("application/json")
                .body("{\"title\":\"Lernen\"}")
                .when().post("/tasks").then().statusCode(200)
                .body("id", is(2)).body("title", is("Lernen"))
                .body("status", is("offen"));
        given().contentType("application/json")
                .body("{\"id\":99,\"title\":\"Fertig\",\"status\":\"erledigt\"}")
                .when().put("/tasks/1").then().statusCode(200)
                .body("id", is(1)).body("status", is("erledigt"));
        given().contentType("application/json")
                .body("{\"title\":\"Fertig\",\"status\":\"erledigt\"}")
                .when().put("/tasks/99").then().statusCode(404);
    }

    @Test
    void rejectsInvalidInput() {
        given().contentType("application/json").body("{}")
                .when().post("/tasks").then().statusCode(400);
        given().contentType("application/json").body("null")
                .when().post("/tasks").then().statusCode(400);
        given().contentType("application/json")
                .body("{\"title\":\"Lernen\",\"status\":\"unbekannt\"}")
                .when().put("/tasks/1").then().statusCode(400);
    }

    @Test
    void deletesWithStaticResponse() {
        given().when().delete("/tasks/1").then().statusCode(204);
        given().when().delete("/tasks/99").then().statusCode(404);
        // Statische Beispieldaten bleiben bei dieser Aufgabe unverändert.
        given().when().get("/tasks/1").then().statusCode(200);
    }
}
