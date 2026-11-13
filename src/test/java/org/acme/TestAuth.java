package org.acme;

import static io.restassured.RestAssured.given;

/**
 * Hilfsmethode fuer die Integrationstests: holt ein JWT fuer einen Testbenutzer aus den Demo-Daten.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static String token(String username, String password) {
        return given()
                .contentType("application/json")
                .body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")
                .when().post("/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    public static String adminToken() {
        return token("admin", "admin123");
    }

    public static String userToken() {
        return token("anna", "anna1234");
    }
}
