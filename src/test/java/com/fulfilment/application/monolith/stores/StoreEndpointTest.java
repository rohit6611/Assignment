package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StoreEndpointTest {

  private static final String PATH = "store";

  @Test
  @Order(1)
  void testListAllStores() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  @Test
  @Order(2)
  void testGetSingleStore() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("TONSTAD"));
  }

  @Test
  @Order(3)
  void testGetNonExistentStore() {
    given().when().get(PATH + "/9999").then().statusCode(404);
  }

  @Test
  @Order(4)
  void testCreateStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"NEWSTORE\", \"quantityProductsInStock\": 7}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("NEWSTORE"));
  }

  @Test
  @Order(5)
  void testCreateStoreWithIdShouldFail() {
    given()
        .contentType("application/json")
        .body("{\"id\": 99, \"name\": \"BAD\"}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  @Order(6)
  void testUpdateStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"UPDATED\", \"quantityProductsInStock\": 20}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("UPDATED"));
  }

  @Test
  @Order(7)
  void testUpdateNonExistentStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"X\"}")
        .when()
        .put(PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(8)
  void testUpdateStoreWithNoName() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 5}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(9)
  void testPatchStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"PATCHED\", \"quantityProductsInStock\": 15}")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("PATCHED"));
  }

  @Test
  @Order(10)
  void testPatchNonExistentStore() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"X\"}")
        .when()
        .patch(PATH + "/9999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(11)
  void testPatchStoreWithNoName() {
    given()
        .contentType("application/json")
        .body("{\"quantityProductsInStock\": 5}")
        .when()
        .patch(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(12)
  void testDeleteStore() {
    given().when().delete(PATH + "/2").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("KALLAX")));
  }

  @Test
  @Order(13)
  void testDeleteNonExistentStore() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }
}
