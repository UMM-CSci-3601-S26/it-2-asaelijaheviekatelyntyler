package umm3601.supplylist;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

public class SupplyListValidator {

  private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of(
      "school", "grade", "teacher", "academicYear", "item", "brand",
      "count", "size", "color", "quantity", "notes", "material", "type", "style");

  public SupplyListSearchCriteria validateQuery(Context ctx) {
    Map<String, List<String>> queryParams = ctx.queryParamMap();
    for (String key : queryParams.keySet()) {
      if (!ALLOWED_QUERY_PARAMS.contains(key)) {
        throw new BadRequestResponse("Unsupported supply list query parameter: " + key);
      }
    }

    return new SupplyListSearchCriteria(
        normalizeOptional(ctx.queryParam("school")),
        normalizeOptional(ctx.queryParam("grade")),
        normalizeOptional(ctx.queryParam("teacher")),
        normalizeOptional(ctx.queryParam("academicYear")),
        normalizeOptional(ctx.queryParam("item")),
        normalizeOptional(ctx.queryParam("brand")),
        parseInteger("count", ctx.queryParam("count")),
        normalizeOptional(ctx.queryParam("size")),
        normalizeOptional(ctx.queryParam("color")),
        parseInteger("quantity", ctx.queryParam("quantity")),
        normalizeOptional(ctx.queryParam("notes")),
        normalizeOptional(ctx.queryParam("material")),
        normalizeOptional(ctx.queryParam("type")),
        normalizeOptional(ctx.queryParam("style")));
  }

  public SupplyList validateBody(SupplyList supplyList) {
    if (supplyList == null) {
      throw new BadRequestResponse("Supply list request body is required");
    }
    if (supplyList.school == null || supplyList.school.isBlank()) {
      throw new BadRequestResponse("school must be a non-empty string");
    }
    if (supplyList.grade == null || supplyList.grade.isBlank()) {
      throw new BadRequestResponse("grade must be a non-empty string");
    }
    if (supplyList.item == null || supplyList.item.isEmpty()) {
      throw new BadRequestResponse("item must be a non-empty list");
    }
    if (supplyList.count <= 0) {
      throw new BadRequestResponse("count must be a positive integer");
    }
    if (supplyList.quantity <= 0) {
      throw new BadRequestResponse("quantity must be a positive integer");
    }
    return supplyList;
  }

  public String validateId(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestResponse("Supply list id is required");
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

  public record SupplyListSearchCriteria(
      String school,
      String grade,
      String teacher,
      String academicYear,
      String item,
      String brand,
      Integer count,
      String size,
      String color,
      Integer quantity,
      String notes,
      String material,
      String type,
      String style) {
  }
}
