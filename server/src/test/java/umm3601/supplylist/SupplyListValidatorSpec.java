package umm3601.supplylist;

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

class SupplyListValidatorSpec {

  private SupplyListValidator validator;

  @Mock
  private Context ctx;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    validator = new SupplyListValidator();
  }

  @Test
  void validateBodyPassesForValidSupplyList() {
    SupplyList supplyList = new SupplyList();
    supplyList.school = "MHS";
    supplyList.grade = "1";
    supplyList.item = List.of("Pencil");
    supplyList.count = 1;
    supplyList.quantity = 1;

    SupplyList result = validator.validateBody(supplyList);

    assertSame(supplyList, result);
  }

  @Test
  void validateBodyRejectsNull() {
    assertThrows(BadRequestResponse.class, () -> validator.validateBody(null));
  }

  @Test
  void validateBodyRejectsMissingSchool() {
    SupplyList supplyList = new SupplyList();
    supplyList.school = " ";
    supplyList.grade = "1";
    supplyList.item = List.of("Pencil");
    supplyList.count = 1;
    supplyList.quantity = 1;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(supplyList));
    assertEquals("school must be a non-empty string", ex.getMessage());
  }

  @Test
  void validateBodyRejectsMissingGrade() {
    SupplyList supplyList = new SupplyList();
    supplyList.school = "MHS";
    supplyList.grade = " ";
    supplyList.item = List.of("Pencil");
    supplyList.count = 1;
    supplyList.quantity = 1;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(supplyList));
    assertEquals("grade must be a non-empty string", ex.getMessage());
  }

  @Test
  void validateBodyRejectsMissingItem() {
    SupplyList supplyList = new SupplyList();
    supplyList.school = "MHS";
    supplyList.grade = "1";
    supplyList.item = List.of();
    supplyList.count = 1;
    supplyList.quantity = 1;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(supplyList));
    assertEquals("item must be a non-empty list", ex.getMessage());
  }

  @Test
  void validateBodyRejectsNonPositiveCount() {
    SupplyList supplyList = new SupplyList();
    supplyList.school = "MHS";
    supplyList.grade = "1";
    supplyList.item = List.of("Pencil");
    supplyList.count = 0;
    supplyList.quantity = 1;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(supplyList));
    assertEquals("count must be a positive integer", ex.getMessage());
  }

  @Test
  void validateBodyRejectsNonPositiveQuantity() {
    SupplyList supplyList = new SupplyList();
    supplyList.school = "MHS";
    supplyList.grade = "1";
    supplyList.item = List.of("Pencil");
    supplyList.count = 1;
    supplyList.quantity = 0;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(supplyList));
    assertEquals("quantity must be a positive integer", ex.getMessage());
  }

  @Test
  void validateIdRejectsBlank() {
    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateId("  "));
    assertEquals("Supply list id is required", ex.getMessage());
  }

  @Test
  void validateIdTrimsInput() {
    assertEquals("abc", validator.validateId("  abc "));
  }

  @Test
  void validateQueryRejectsUnsupportedParam() {
    when(ctx.queryParamMap()).thenReturn(Map.of("district", List.of("x")));

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("Unsupported supply list query parameter: district", ex.getMessage());
  }

  @Test
  void validateQueryRejectsNonIntegerCount() {
    when(ctx.queryParamMap()).thenReturn(Map.of("count", List.of("a")));
    when(ctx.queryParam("count")).thenReturn("a");

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateQuery(ctx));
    assertEquals("count must be an integer.", ex.getMessage());
  }

  @Test
  @SuppressWarnings("MagicNumber")
  void validateQueryParsesSearchCriteria() {
    when(ctx.queryParamMap()).thenReturn(Map.of(
        "school", List.of("MHS"),
        "grade", List.of("1"),
        "count", List.of("2"),
        "quantity", List.of("5")));
    when(ctx.queryParam("school")).thenReturn("MHS");
    when(ctx.queryParam("grade")).thenReturn("1");
    when(ctx.queryParam("count")).thenReturn("2");
    when(ctx.queryParam("quantity")).thenReturn("5");

    SupplyListValidator.SupplyListSearchCriteria criteria = validator.validateQuery(ctx);

    assertNotNull(criteria);
    assertEquals("MHS", criteria.school());
    assertEquals("1", criteria.grade());
    assertEquals(2, criteria.count());
    assertEquals(5, criteria.quantity());
  }
}
