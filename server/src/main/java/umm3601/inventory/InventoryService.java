package umm3601.inventory;

import java.util.List;

import io.javalin.http.NotFoundResponse;
import umm3601.inventory.InventoryValidator.InventorySearchCriteria;

public class InventoryService {

  private final InventoryRepository repository;

  public InventoryService(InventoryRepository repository) {
    this.repository = repository;
  }

  public List<Inventory> getAll(InventorySearchCriteria criteria, int skip, int limit) {
    return repository.findAll(criteria, skip, limit);
  }

  public Inventory getById(String id) {
    Inventory inventory = repository.findById(id);
    if (inventory == null) {
      throw new NotFoundResponse("The requested inventory item was not found");
    }
    return inventory;
  }

  public void create(Inventory item) {
    repository.insert(item);
  }

  public void update(String id, Inventory item) {
    long matchedCount = repository.update(id, item);
    if (matchedCount == 0) {
      throw new NotFoundResponse("The requested inventory item was not found");
    }
  }

  public void delete(String id) {
    long deletedCount = repository.delete(id);
    if (deletedCount == 0) {
      throw new NotFoundResponse("The requested inventory item was not found");
    }
  }
}
