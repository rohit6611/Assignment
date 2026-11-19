package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CreateWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private CreateWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new CreateWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void shouldCreateWarehouseSuccessfully() {
    Warehouse warehouse = createWarehouse("NEW.001", "AMSTERDAM-001", 20, 5);

    when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(Collections.emptyList());

    useCase.create(warehouse);

    assertNotNull(warehouse.createdAt);
    verify(warehouseStore).create(warehouse);
  }

  @Test
  void shouldRejectDuplicateBusinessUnitCode() {
    Warehouse warehouse = createWarehouse("EXISTING.001", "AMSTERDAM-001", 20, 5);
    Warehouse existing = createWarehouse("EXISTING.001", "AMSTERDAM-001", 30, 10);

    when(warehouseStore.findByBusinessUnitCode("EXISTING.001")).thenReturn(existing);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Business unit code already exists: EXISTING.001", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectInvalidLocation() {
    Warehouse warehouse = createWarehouse("NEW.001", "INVALID-LOC", 20, 5);

    when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("INVALID-LOC")).thenReturn(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Invalid location: INVALID-LOC", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenMaxWarehousesReached() {
    Warehouse warehouse = createWarehouse("NEW.001", "ZWOLLE-001", 10, 5);
    Warehouse existingAtLocation = createWarehouse("OLD.001", "ZWOLLE-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("ZWOLLE-001"))
        .thenReturn(new Location("ZWOLLE-001", 1, 40));
    when(warehouseStore.findActiveByLocation("ZWOLLE-001"))
        .thenReturn(List.of(existingAtLocation));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Maximum number of warehouses reached at location: ZWOLLE-001", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenCapacityExceedsLocationMax() {
    Warehouse warehouse = createWarehouse("NEW.001", "AMSTERDAM-001", 90, 5);
    Warehouse existingAtLocation = createWarehouse("OLD.001", "AMSTERDAM-001", 20, 10);

    when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001"))
        .thenReturn(List.of(existingAtLocation));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Capacity exceeds maximum allowed at location: AMSTERDAM-001", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenStockExceedsCapacity() {
    Warehouse warehouse = createWarehouse("NEW.001", "AMSTERDAM-001", 10, 20);

    when(warehouseStore.findByBusinessUnitCode("NEW.001")).thenReturn(null);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(Collections.emptyList());

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.create(warehouse));
    assertEquals("Stock exceeds warehouse capacity", ex.getMessage());
    verify(warehouseStore, never()).create(any());
  }

  private Warehouse createWarehouse(String buCode, String location, int capacity, int stock) {
    Warehouse w = new Warehouse();
    w.businessUnitCode = buCode;
    w.location = location;
    w.capacity = capacity;
    w.stock = stock;
    return w;
  }
}
