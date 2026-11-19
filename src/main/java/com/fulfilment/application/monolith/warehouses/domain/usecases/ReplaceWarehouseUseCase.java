package com.fulfilment.application.monolith.warehouses.domain.usecases;

import com.fulfilment.application.monolith.warehouses.domain.models.Location;
import com.fulfilment.application.monolith.warehouses.domain.models.Warehouse;
import com.fulfilment.application.monolith.warehouses.domain.ports.LocationResolver;
import com.fulfilment.application.monolith.warehouses.domain.ports.ReplaceWarehouseOperation;
import com.fulfilment.application.monolith.warehouses.domain.ports.WarehouseStore;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class ReplaceWarehouseUseCase implements ReplaceWarehouseOperation {

  private final WarehouseStore warehouseStore;
  private final LocationResolver locationResolver;

  public ReplaceWarehouseUseCase(
      WarehouseStore warehouseStore, LocationResolver locationResolver) {
    this.warehouseStore = warehouseStore;
    this.locationResolver = locationResolver;
  }

  @Override
  public void replace(Warehouse newWarehouse) {
    Warehouse existing =
        warehouseStore.findByBusinessUnitCode(newWarehouse.businessUnitCode);
    if (existing == null) {
      throw new IllegalArgumentException(
          "No active warehouse found with business unit code: "
              + newWarehouse.businessUnitCode);
    }

    Location location = locationResolver.resolveByIdentifier(newWarehouse.location);
    if (location == null) {
      throw new IllegalArgumentException(
          "Invalid location: " + newWarehouse.location);
    }

    if (newWarehouse.capacity < existing.stock) {
      throw new IllegalArgumentException(
          "New warehouse capacity cannot accommodate the stock from the warehouse being replaced");
    }

    if (!newWarehouse.stock.equals(existing.stock)) {
      throw new IllegalArgumentException(
          "Stock of the new warehouse must match the stock of the previous warehouse");
    }

    List<Warehouse> warehousesAtLocation =
        warehouseStore.findActiveByLocation(newWarehouse.location);

    int currentCount = warehousesAtLocation.size();
    boolean sameLocation = existing.location.equals(newWarehouse.location);
    int adjustedCount = sameLocation ? currentCount - 1 : currentCount;

    if (adjustedCount + 1 > location.maxNumberOfWarehouses) {
      throw new IllegalArgumentException(
          "Maximum number of warehouses reached at location: " + newWarehouse.location);
    }

    int totalCapacity = warehousesAtLocation.stream().mapToInt(w -> w.capacity).sum();
    int adjustedCapacity = sameLocation ? totalCapacity - existing.capacity : totalCapacity;

    if (adjustedCapacity + newWarehouse.capacity > location.maxCapacity) {
      throw new IllegalArgumentException(
          "Capacity exceeds maximum allowed at location: " + newWarehouse.location);
    }

    existing.archivedAt = LocalDateTime.now();
    warehouseStore.update(existing);

    newWarehouse.createdAt = LocalDateTime.now();
    newWarehouse.archivedAt = null;
    warehouseStore.create(newWarehouse);
  }
}
