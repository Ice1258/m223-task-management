package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Integrationstests fuer Login/Registrierung und die Rollenpruefung bei Projekten.
 */
@QuarkusTest
class AuthAndProjectTest {

    @Test
    void login_withValidCredentials_returnsTokenAndRole() {
        given().contentType("application/json")
                .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
        .when().post("/auth/login")
        .then().statusCode(200)
                .body("token", notNullValue())
                .body("username", is("admin"))
                .body("role", is("Admin"));
    }

    @Test
    void login_withWrongPassword_returns401() {
        given().contentType("application/json")
                .body("{\"username\":\"admin\",\"password\":\"falsch123\"}")
        .when().post("/auth/login")
        .then().statusCode(401);
    }

    @Test
    void register_newUser_returnsUserRole_andDuplicateIsRejected() {
        String body = "{\"username\":\"neuer.user\",\"password\":\"geheim123\"}";

        given().contentType("application/json").body(body)
        .when().post("/auth/register")
        .then().statusCode(200)
                .body("role", is("User"))
                .body("token", notNullValue());

        given().contentType("application/json").body(body)
        .when().post("/auth/register")
        .then().statusCode(409);
    }

    @Test
    void register_withShortPassword_returns400() {
        given().contentType("application/json")
                .body("{\"username\":\"kurz\",\"password\":\"123\"}")
        .when().post("/auth/register")
        .then().statusCode(400);
    }

    @Test
    void createProject_withExistingName_returns409() {
        given().auth().oauth2(TestAuth.userToken()).contentType("application/json")
                .body("{\"name\":\"Haushalt\"}")
        .when().post("/projects")
        .then().statusCode(409);
    }

    @Test
    void deleteProject_asUser_returns403_asAdmin_returns204() {
        // Given: ein Projekt mit einer Aufgabe
        String userToken = TestAuth.userToken();
        int projectId = given().auth().oauth2(userToken).contentType("application/json")
                .body("{\"name\":\"Wird geloescht\"}")
                .when().post("/projects")
                .then().statusCode(200).extract().path("id");
        int taskId = given().auth().oauth2(userToken).contentType("application/json")
                .body("{\"title\":\"Bleibt bestehen\",\"projectId\":" + projectId + "}")
                .when().post("/tasks")
                .then().statusCode(200).extract().path("id");

        // When/Then: ein normaler User darf nicht loeschen
        given().auth().oauth2(userToken)
                .when().delete("/projects/" + projectId)
                .then().statusCode(403);

        // When/Then: ein Admin darf loeschen
        given().auth().oauth2(TestAuth.adminToken())
                .when().delete("/projects/" + projectId)
                .then().statusCode(204);

        // And: das Projekt ist weg, die Aufgabe bleibt ohne Projekt erhalten
        given().auth().oauth2(userToken).when().get("/projects")
                .then().statusCode(200).body("id", not(hasItem(projectId)));
        given().auth().oauth2(userToken).when().get("/tasks/" + taskId)
                .then().statusCode(200).body("projectId", is((Object) null));
    }
}
