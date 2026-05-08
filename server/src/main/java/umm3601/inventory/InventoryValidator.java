package umm3601.inventory;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public class InventoryValidator {

  private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of(
      "item", "brand", "count", "size", "color", "quantity",
      "notes", "material", "type", "style", "bin", "skip", "limit");
  private static final int DEFAULT_LIMIT = 100;
  private static final int MAX_LIMIT = 250;

  public InventoryQuery validateQuery(Context ctx) {
    Map<String, List<String>> queryParams = ctx.queryParamMap();
    for (String key : queryParams.keySet()) {
      if (!ALLOWED_QUERY_PARAMS.contains(key)) {
        throw new BadRequestResponse("Unsupported inventory query parameter: " + key);
      }
    }

    return new InventoryQuery(
        new InventorySearchCriteria(
            normalizeOptional(ctx.queryParam("item")),
            normalizeOptional(ctx.queryParam("brand")),
            parseInteger("count", ctx.queryParam("count")),
            normalizeOptional(ctx.queryParam("size")),
            normalizeOptional(ctx.queryParam("color")),
            parseInteger("quantity", ctx.queryParam("quantity")),
            normalizeOptional(ctx.queryParam("notes")),
            normalizeOptional(ctx.queryParam("material")),
            normalizeOptional(ctx.queryParam("type")),
            normalizeOptional(ctx.queryParam("style")),
            parseInteger("bin", ctx.queryParam("bin"))),
        parseNonNegative("skip", ctx.queryParam("skip"), 0),
        parseBoundedLimit(ctx.queryParam("limit")));
  }

  public Inventory validateBody(Inventory inventory) {
    if (inventory == null) {
      throw new BadRequestResponse("Inventory request body is required");
    }
    if (inventory.item == null || inventory.item.isBlank()) {
      throw new BadRequestResponse("Inventory must have a non-empty item key");
    }
    if (inventory.count < 1) {
      throw new BadRequestResponse("Count must be >= 1");
    }
    if (inventory.quantity < 0) {
      throw new BadRequestResponse("Quantity must be >= 0");
    }
    return inventory;
  }

  public String validateId(String id, String resourceName) {
    if (id == null || id.isBlank()) {
      throw new BadRequestResponse(resourceName + " id is required");
    }
    return id.trim();
  }

  private String normalizeOptional(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new BadRequestResponse("Query values cannot be blank");
    }
    return trimmed;
  }

  private Integer parseInteger(String field, String value) {
    if (value == null) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      throw new BadRequestResponse(field + " must be an integer.");
    }
  }

  private int parseNonNegative(String field, String value, int defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    int parsed = parseInteger(field, value);
    if (parsed < 0) {
      throw new BadRequestResponse(field + " must be >= 0");
    }
    return parsed;
  }

  private int parseBoundedLimit(String value) {
    int limit = parseNonNegative("limit", value, DEFAULT_LIMIT);
    if (limit == 0 || limit > MAX_LIMIT) {
      throw new BadRequestResponse("limit must be between 1 and " + MAX_LIMIT);
    }
    return limit;
  }

  public record InventorySearchCriteria(
      String item,
      String brand,
      Integer count,
      String size,
      String color,
      Integer quantity,
      String notes,
      String material,
      String type,
      String style,
      Integer bin) {
  }

  public record InventoryQuery(InventorySearchCriteria criteria, int skip, int limit) {
  }
}
