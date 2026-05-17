package com.example;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;

@QuarkusTest
class NotificationResourceTest {

    @Test
    void testHealthEndpoint() {
        given()
                .when().get("/q/health")
                .then()
                .statusCode(200)
                .body("status", is("UP"));
    }

    @Test
    void testGetAllWithoutToken_shouldReturn401() {
        given()
                .when().get("/api/notifications")
                .then()
                .statusCode(401);
    }

    @Test
    void testGetMyWithoutToken_shouldReturn401() {
        given()
                .when().get("/api/notifications/my")
                .then()
                .statusCode(401);
    }

    @Test
    void testOpenApiEndpoint() {
        given()
                .when().get("/openapi")
                .then()
                .statusCode(200)
                .body(notNullValue());
    }
}
