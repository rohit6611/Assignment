package com.fulfilment.application.monolith.warehouses.domain.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReplaceWarehouseUseCaseTest {

  private WarehouseStore warehouseStore;
  private LocationResolver locationResolver;
  private ReplaceWarehouseUseCase useCase;

  @BeforeEach
  void setUp() {
    warehouseStore = mock(WarehouseStore.class);
    locationResolver = mock(LocationResolver.class);
    useCase = new ReplaceWarehouseUseCase(warehouseStore, locationResolver);
  }

  @Test
  void shouldReplaceWarehouseSuccessfully() {
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    Warehouse replacement = createWarehouse("MWH.001", "AMSTERDAM-001", 60, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(List.of(existing));

    useCase.replace(replacement);

    verify(warehouseStore).update(argThat(w -> w.archivedAt != null));
    verify(warehouseStore).create(argThat(w -> w.createdAt != null && w.archivedAt == null));
  }

  @Test
  void shouldRejectWhenNoExistingWarehouse() {
    Warehouse replacement = createWarehouse("NONEXISTENT", "AMSTERDAM-001", 50, 10);

    when(warehouseStore.findByBusinessUnitCode("NONEXISTENT")).thenReturn(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals(
        "No active warehouse found with business unit code: NONEXISTENT", ex.getMessage());
    verify(warehouseStore, never()).update(any());
    verify(warehouseStore, never()).create(any());
  }

  @Test
  void shouldRejectWhenCapacityCannotAccommodateExistingStock() {
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 30);
    Warehouse replacement = createWarehouse("MWH.001", "AMSTERDAM-001", 20, 30);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals(
        "New warehouse capacity cannot accommodate the stock from the warehouse being replaced",
        ex.getMessage());
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void shouldRejectWhenStockDoesNotMatch() {
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    Warehouse replacement = createWarehouse("MWH.001", "AMSTERDAM-001", 60, 20);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals(
        "Stock of the new warehouse must match the stock of the previous warehouse",
        ex.getMessage());
    verify(warehouseStore, never()).update(any());
  }

  @Test
  void shouldRejectWhenInvalidLocation() {
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    Warehouse replacement = createWarehouse("MWH.001", "INVALID-LOC", 60, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("INVALID-LOC")).thenReturn(null);

    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> useCase.replace(replacement));
    assertEquals("Invalid location: INVALID-LOC", ex.getMessage());
  }

  @Test
  void shouldArchiveOldAndCreateNew() {
    Warehouse existing = createWarehouse("MWH.001", "AMSTERDAM-001", 50, 10);
    Warehouse replacement = createWarehouse("MWH.001", "AMSTERDAM-001", 70, 10);

    when(warehouseStore.findByBusinessUnitCode("MWH.001")).thenReturn(existing);
    when(locationResolver.resolveByIdentifier("AMSTERDAM-001"))
        .thenReturn(new Location("AMSTERDAM-001", 5, 100));
    when(warehouseStore.findActiveByLocation("AMSTERDAM-001")).thenReturn(List.of(existing));

    useCase.replace(replacement);

    assertNotNull(existing.archivedAt);
    verify(warehouseStore).update(existing);

    assertNotNull(replacement.createdAt);
    verify(warehouseStore).create(replacement);
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
