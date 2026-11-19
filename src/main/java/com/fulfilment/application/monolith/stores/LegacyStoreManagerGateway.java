package com.fulfilment.application.monolith.stores;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import java.nio.file.Files;
import java.nio.file.Path;

@ApplicationScoped
public class LegacyStoreManagerGateway {

  public void onStoreCreated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreCreatedEvent event) {
    createStoreOnLegacySystem(event.store);
  }

  public void onStoreUpdated(
      @Observes(during = TransactionPhase.AFTER_SUCCESS) StoreUpdatedEvent event) {
    updateStoreOnLegacySystem(event.store);
  }

  public void createStoreOnLegacySystem(Store store) {
    writeToFile(store);
  }

  public void updateStoreOnLegacySystem(Store store) {
    writeToFile(store);
  }

  private void writeToFile(Store store) {
    try {
      Path tempFile;

      tempFile = Files.createTempFile(store.name, ".txt");

      System.out.println("Temporary file created at: " + tempFile.toString());

      String content =
          "Store created. [ name ="
              + store.name
              + " ] [ items on stock ="
              + store.quantityProductsInStock
              + "]";
      Files.write(tempFile, content.getBytes());
      System.out.println("Data written to temporary file.");

      String readContent = new String(Files.readAllBytes(tempFile));
      System.out.println("Data read from temporary file: " + readContent);

      Files.delete(tempFile);
      System.out.println("Temporary file deleted.");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
