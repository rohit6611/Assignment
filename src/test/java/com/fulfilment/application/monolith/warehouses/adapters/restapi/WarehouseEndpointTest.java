package com.fulfilment.application.monolith.warehouses.adapters.restapi;

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
public class WarehouseEndpointTest {

  private static final String PATH = "warehouse";

  @Test
  @Order(1)
  void testListAllWarehouses() {
    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(
            containsString("MWH.001"),
            containsString("MWH.012"),
            containsString("MWH.023"));
  }

  @Test
  @Order(2)
  void testGetWarehouseById() {
    given()
        .when()
        .get(PATH + "/1")
        .then()
        .statusCode(200)
        .body(containsString("MWH.001"), containsString("ZWOLLE-001"));
  }

  @Test
  @Order(3)
  void testGetNonExistentWarehouse() {
    given().when().get(PATH + "/9999").then().statusCode(404);
  }

  @Test
  @Order(4)
  void testCreateWarehouseDuplicateBuCode() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.001\",\"location\":\"AMSTERDAM-001\",\"capacity\":10,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(5)
  void testCreateWarehouseInvalidLocation() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"NEW.099\",\"location\":\"INVALID-LOC\",\"capacity\":10,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(6)
  void testCreateWarehouseMaxWarehousesReached() {
    // ZWOLLE-001 allows max 1 warehouse and already has MWH.001
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"NEW.099\",\"location\":\"ZWOLLE-001\",\"capacity\":10,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(7)
  void testCreateWarehouseExceedsLocationCapacity() {
    // AMSTERDAM-001 maxCapacity=100, MWH.012 already uses 50, so 60 more would exceed
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"NEW.099\",\"location\":\"AMSTERDAM-001\",\"capacity\":60,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(8)
  void testCreateWarehouseStockExceedsCapacity() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"NEW.099\",\"location\":\"HELMOND-001\",\"capacity\":10,\"stock\":20}")
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  @Order(9)
  void testCreateWarehouseSuccess() {
    // HELMOND-001: maxWarehouses=1, maxCapacity=45, currently empty
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"NEW.001\",\"location\":\"HELMOND-001\",\"capacity\":30,\"stock\":5}")
        .when()
        .post(PATH)
        .then()
        .statusCode(200)
        .body(containsString("NEW.001"), containsString("HELMOND-001"));

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(containsString("NEW.001"));
  }

  @Test
  @Order(10)
  void testArchiveWarehouse() {
    // Archive MWH.001 (id=1)
    given().when().delete(PATH + "/1").then().statusCode(204);

    given()
        .when()
        .get(PATH)
        .then()
        .statusCode(200)
        .body(not(containsString("ZWOLLE-001")));
  }

  @Test
  @Order(11)
  void testArchiveNonExistentWarehouse() {
    given().when().delete(PATH + "/9999").then().statusCode(404);
  }

  @Test
  @Order(12)
  void testReplaceWarehouseNonExistentBuCode() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"NONEXISTENT\",\"location\":\"AMSTERDAM-001\",\"capacity\":50,\"stock\":5}")
        .when()
        .post(PATH + "/NONEXISTENT/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(13)
  void testReplaceWarehouseStockMismatch() {
    // MWH.012 has stock=5, sending stock=99
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.012\",\"location\":\"AMSTERDAM-001\",\"capacity\":60,\"stock\":99}")
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(14)
  void testReplaceWarehouseCapacityTooSmall() {
    // MWH.012 has stock=5, new capacity=2 can't hold it
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.012\",\"location\":\"AMSTERDAM-001\",\"capacity\":2,\"stock\":5}")
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(15)
  void testReplaceWarehouseInvalidLocation() {
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.012\",\"location\":\"INVALID\",\"capacity\":60,\"stock\":5}")
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  @Order(16)
  void testReplaceWarehouseSuccess() {
    // Replace MWH.023 at TILBURG-001 (stock=27, capacity=30)
    // TILBURG-001: maxWarehouses=1, maxCapacity=40
    given()
        .contentType("application/json")
        .body("{\"businessUnitCode\":\"MWH.023\",\"location\":\"TILBURG-001\",\"capacity\":35,\"stock\":27}")
        .when()
        .post(PATH + "/MWH.023/replacement")
        .then()
        .statusCode(200)
        .body(containsString("MWH.023"), containsString("TILBURG-001"));
  }
}
