package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductEndpointTest {

  private static final String PATH = "product";

  @Test
  @Order(1)
  public void testListAllProducts() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"));
  }

  @Test
  @Order(2)
  public void testGetSingleProduct() {
    given().when().get(PATH + "/1").then().statusCode(200).body(containsString("TONSTAD"));
  }

  @Test
  @Order(3)
  public void testGetNonExistentProduct() {
    given().when().get(PATH + "/9999").then().statusCode(404);
  }

  @Test
  @Order(4)
  public void testCreateProduct() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"NEWPRODUCT\", \"stock\": 10}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString("NEWPRODUCT"));
  }

  @Test
  @Order(5)
  public void testCreateProductWithIdShouldFail() {
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
  public void testUpdateProduct() {
    given()
        .contentType("application/json")
        .body("{\"name\": \"UPDATED\", \"stock\": 20}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("UPDATED"));
  }

  @Test
  @Order(7)
  public void testUpdateNonExistentProduct() {
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
  public void testUpdateProductWithNoName() {
    given()
        .contentType("application/json")
        .body("{\"stock\": 5}")
        .when()
        .put(PATH + "/1")
        .then()
        .statusCode(422);
  }

  @Test
  @Order(9)
  public void testDeleteProduct() {
    given().when().delete(PATH + "/2").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("KALLAX")));
  }

  @Test
  @Order(10)
  public void testDeleteNonExistentProduct() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }
}
