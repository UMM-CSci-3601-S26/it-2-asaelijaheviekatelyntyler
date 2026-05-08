// Packages
package umm3601.inventory;

// Static Imports
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.auth.HttpMethod;
import umm3601.auth.RequirePermission;
import umm3601.auth.Route;
import umm3601.common.AuthContext;
import umm3601.inventory.InventoryValidator.InventoryQuery;
import umm3601.middleware.AuthMiddleware;

/**
 * Controller for handling Inventory-related API routes.
 *
 * Routes include:
 * - GET /api/inventory → list all inventory items (with optional filters)
 * - GET /api/inventory/{id} → get a single inventory item
 * - POST /api/inventory → add a new inventory item
 * - DELETE /api/inventory/{id} → delete an inventory item
 *
 * Inventory is the core data model for tracking available school supplies, and
 * is used for both display and fulfillment.
 */
public class InventoryController {

  private static final String API_INVENTORY = "/api/inventory";
  private static final String API_INVENTORY_BY_ID = "/api/inventory/{id}";

  private final InventoryService service;
  private final InventoryPolicy policy;
  private final InventoryValidator validator;

  public InventoryController(JacksonMongoCollection<Inventory> inventoryCollection) {
    this(new InventoryRepository(inventoryCollection));
  }

  public InventoryController(InventoryRepository repository) {
    this(new InventoryService(repository), new InventoryPolicy(), new InventoryValidator());
  }

  // Backward-compatible constructor used by legacy tests.
  public InventoryController(MongoDatabase database, AuthMiddleware authMiddleware) {
    this(JacksonMongoCollection.builder().build(
        database,
        "inventory",
        Inventory.class,
        UuidRepresentation.STANDARD));
  }

  public InventoryController(
      InventoryService service,
      InventoryPolicy policy,
      InventoryValidator validator) {
    this.service = service;
    this.policy = policy;
    this.validator = validator;
  }

  /**
   * GET /api/inventory/{id}
   */
  @Route(method = HttpMethod.GET, path = API_INVENTORY_BY_ID)
  @RequirePermission("view_inventory_item")
  public void getInventoryItem(Context ctx) {
    policy.authorizeRead(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"), "inventory");
    ctx.json(service.getById(id));
    ctx.status(HttpStatus.OK);
  }

  /**
   * GET /api/inventory
   * Retrieves all inventory items, with optional query parameters for filtering.
   */
  @Route(method = HttpMethod.GET, path = API_INVENTORY)
  @RequirePermission("view_inventory")
  public void getInventories(Context ctx) {
    policy.authorizeRead(AuthContext.from(ctx));
    InventoryQuery query = validator.validateQuery(ctx);
    List<Inventory> inventories = service.getAll(query.criteria(), query.skip(), query.limit());
    ctx.json(inventories);
    ctx.status(HttpStatus.OK);
  }

  /**
   * POST /api/inventory
   * Adds a new inventory item.
   *
   * Validation ensures:
   * - quantity is non-negative
   * - count is at least 1
   * - item name is present
   */
  @Route(method = HttpMethod.POST, path = API_INVENTORY)
  @RequirePermission("add_inventory_item")
  public void addInventory(Context ctx) {
    policy.authorizeCreate(AuthContext.from(ctx));
    Inventory newItem = ctx.bodyValidator(Inventory.class)
        .check(i -> i.item != null && !i.item.isBlank(), "Inventory must have a non-empty item key")
        .check(i -> i.count >= 1, "Quantity must be 1 or more")
        .check(i -> i.quantity >= 0, "Quantity must be >= 0")
        .get();
    service.create(newItem);
    ctx.json(Map.of("id", newItem._id));
    ctx.status(HttpStatus.CREATED);
  }

  /**
   * PUT /api/inventory/{id}
   * Replaces an existing inventory item with the request body.
   *
   * Returns 200 OK on success, 400 Bad Request for invalid input,
   * or 404 Not Found if the item does not exist.
   */
  @Route(method = HttpMethod.PUT, path = API_INVENTORY_BY_ID)
  @RequirePermission("edit_inventory_item")
  public void editInventory(Context ctx) {
    policy.authorizeEdit(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"), "inventory");
    Inventory updatedItem = ctx.bodyValidator(Inventory.class)
        .check(i -> i.item != null && !i.item.isBlank(), "Inventory must have a non-empty item key")
        .check(i -> i.count >= 1, "Count must be >= 1")
        .check(i -> i.quantity >= 0, "Quantity must be >= 0")
        .get();
    service.update(id, updatedItem);
    ctx.status(HttpStatus.OK);
  }

  /**
   * DELETE /api/inventory/{id}
   * Deletes an inventory item by its MongoDB ObjectId.
   *
   * Returns 200 OK if deletion was successful, or 404 Not Found if:
   * - the ID is invalid
   * - no inventory item with that ID exists
   */
  @Route(method = HttpMethod.DELETE, path = API_INVENTORY_BY_ID)
  @RequirePermission("delete_inventory_item")
  public void deleteInventory(Context ctx) {
    policy.authorizeDelete(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"), "inventory");

    // Legacy behavior expected by tests: malformed IDs throw IllegalArgumentException.
    new ObjectId(id);

    try {
      service.delete(id);
      ctx.status(HttpStatus.OK);
    } catch (NotFoundResponse e) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw e;
    }
  }

  // Backward-compatible route registration used by legacy tests.
  public void addRoutes(Javalin server) {
    server.get(API_INVENTORY, this::getInventories);
    server.get(API_INVENTORY_BY_ID, this::getInventoryItem);
    server.post(API_INVENTORY, this::addInventory);
    server.put(API_INVENTORY_BY_ID, this::editInventory);
    server.delete(API_INVENTORY_BY_ID, this::deleteInventory);
  }
}
