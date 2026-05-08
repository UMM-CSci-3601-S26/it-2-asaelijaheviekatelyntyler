package umm3601.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

class InventoryValidatorSpec {

  private InventoryValidator validator;

  @Mock
  private Context ctx;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    validator = new InventoryValidator();
  }

  @Test
  void validateBodyPassesForValidInventory() {
    Inventory inventory = new Inventory();
    inventory.item = "Pencils";
    inventory.count = 1;
    inventory.quantity = 0;

    Inventory result = validator.validateBody(inventory);

    assertSame(inventory, result);
  }

  @Test
  void validateBodyRejectsNull() {
    assertThrows(BadRequestResponse.class, () -> validator.validateBody(null));
  }

  @Test
  void validateBodyRejectsBlankItem() {
    Inventory inventory = new Inventory();
    inventory.item = "   ";
    inventory.count = 1;
    inventory.quantity = 0;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(inventory));
    assertEquals("Inventory must have a non-empty item key", ex.getMessage());
  }

  @Test
  void validateBodyRejectsLowCount() {
    Inventory inventory = new Inventory();
    inventory.item = "Pencils";
    inventory.count = 0;
    inventory.quantity = 0;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(inventory));
    assertEquals("Count must be >= 1", ex.getMessage());
  }

  @Test
  void validateBodyRejectsNegativeQuantity() {
    Inventory inventory = new Inventory();
    inventory.item = "Pencils";
    inventory.count = 1;
    inventory.quantity = -1;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(inventory));
    assertEquals("Quantity must be >= 0", ex.getMessage());
  }

  @Test
  void validateIdRejectsBlank() {
    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateId(" ", "inventory"));
    assertEquals("inventory id is required", ex.getMessage());
  }

  @Test
  void validateIdTrimsInput() {
    String id = validator.validateId("  abc123  ", "inventory");
    assertEquals("abc123", id);
  }

  @Test
  void validateQueryRejectsUnsupportedParam() {
    when(ctx.queryParamMap()).thenReturn(Map.of("unsupported", List.of("1")));

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("Unsupported inventory query parameter: unsupported", ex.getMessage());
  }

  @Test
  void validateQueryRejectsNonIntegerBin() {
    when(ctx.queryParamMap()).thenReturn(Map.of("bin", List.of("a")));
    when(ctx.queryParam("bin")).thenReturn("a");

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("bin must be an integer.", ex.getMessage());
  }

  @Test
  void validateQueryRejectsNegativeSkip() {
    when(ctx.queryParamMap()).thenReturn(Map.of("skip", List.of("-1")));
    when(ctx.queryParam("skip")).thenReturn("-1");

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("skip must be >= 0", ex.getMessage());
  }

  @Test
  void validateQueryRejectsZeroLimit() {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("0")));
    when(ctx.queryParam("limit")).thenReturn("0");

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("limit must be between 1 and 250", ex.getMessage());
  }

  @Test
  void validateQueryRejectsTooLargeLimit() {
    when(ctx.queryParamMap()).thenReturn(Map.of("limit", List.of("999")));
    when(ctx.queryParam("limit")).thenReturn("999");

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("limit must be between 1 and 250", ex.getMessage());
  }

  @Test
  @SuppressWarnings("MagicNumber")
  void validateQueryParsesCriteriaSkipAndLimit() {
    when(ctx.queryParamMap()).thenReturn(Map.of(
        "item", List.of("Pencil"),
        "count", List.of("1"),
        "skip", List.of("2"),
        "limit", List.of("10")));
    when(ctx.queryParam("item")).thenReturn("Pencil");
    when(ctx.queryParam("count")).thenReturn("1");
    when(ctx.queryParam("skip")).thenReturn("2");
    when(ctx.queryParam("limit")).thenReturn("10");

    InventoryValidator.InventoryQuery query = validator.validateQuery(ctx);

    assertNotNull(query);
    assertEquals("Pencil", query.criteria().item());
    assertEquals(1, query.criteria().count());
    assertEquals(2, query.skip());
    assertEquals(10, query.limit());
  }
}
