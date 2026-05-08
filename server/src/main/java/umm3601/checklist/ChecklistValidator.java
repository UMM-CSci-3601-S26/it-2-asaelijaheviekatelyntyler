// Package declaration
package umm3601.checklist;

// Java Imports
import java.util.List;
import java.util.Map;
import java.util.Set;

// IO Imports
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;

/**
 * ChecklistValidator is a class responsible for validating incoming HTTP
 * request parameters related to checklist operations. It ensures that query
 * parameters for searching checklists are valid, such as checking for allowed
 * parameters, ensuring that string values are not blank, and enforcing maximum
 * length constraints. The validateSearch method processes the query parameters
 * from the Javalin Context and returns a ChecklistSearchCriteria record
 * containing the validated search criteria. If any validation checks fail, it
 * throws a BadRequestResponse with an appropriate error message, which results
 * in a 400 Bad Request response being sent back to the client. This class helps
 * maintain data integrity and provides clear feedback to clients when their
 * requests do not meet the expected format or constraints.
 *
 * Why create a separate ChecklistValidator class?
 * _______________________________________________________________
 * 1. Separation of Concerns: By isolating validation logic in a dedicated class,
 * we keep our controller and service classes focused on their primary
 * responsibilities. This promotes cleaner code and makes it easier to maintain
 * and understand the flow of the application.
 * 2. Reusability: The ChecklistValidator can be reused across different parts of
 * the application or even in other applications that require similar validation
 * logic, without duplicating code.
 * 3. Testability: By having a separate class for validation, we can easily write
 * unit tests for the validation logic without needing to involve the controller
 * or service layers, leading to more focused and effective tests.
 * 4. Clarity: It provides a clear and centralized place for all validation
 * rules related to checklists, making it easier for developers to understand
 * and maintain the validation logic.
 */

public class ChecklistValidator {
  // Define a set of allowed query parameters for checklist search to prevent unexpected parameters from being processed
  private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of("school", "grade", "studentName", "name");
  // Define a maximum length for query parameters to prevent excessively long input
  private static final int MAX_QUERY_LENGTH = 120;

  /**
   * Validates the search query parameters from the Javalin Context and
   * constructs a ChecklistSearchCriteria record. It checks for the presence of only
   * allowed query parameters, ensures that string values are not blank, and enforces
   * a maximum length constraint. If any validation fails, it throws a BadRequestResponse
   * with an appropriate error message, which results in a 400 Bad Request response
   * being sent back to the client.
   *
   * @param ctx The Javalin Context containing the HTTP request and response information, including query parameters.
   * @return A ChecklistSearchCriteria record containing the validated search criteria.
   */
  public ChecklistSearchCriteria validateSearch(Context ctx) {
    Map<String, List<String>> queryParams = ctx.queryParamMap();
    for (String key : queryParams.keySet()) {
      if (!ALLOWED_QUERY_PARAMS.contains(key)) {
        throw new BadRequestResponse("Unsupported checklist query parameter: " + key);
      }
    }

    return new ChecklistSearchCriteria(
        validateOptional("school", ctx.queryParam("school")),
        validateOptional("grade", ctx.queryParam("grade")),
        validateOptional(
          "studentName",
          firstNonBlank(ctx.queryParam("studentName"),
          ctx.queryParam("name"))));
  }

  /**
   * Returns the first non-blank string from the provided primary and fallback values.
   * If the primary value is non-null, it is returned; otherwise, the fallback value is returned.
   * This method is useful for handling cases where multiple query parameters may be used
   * for the same purpose (e.g., "studentName" and "name") and we want to prioritize one
   * over the other while still allowing for backward compatibility.
   *
   * @param primary The primary value to check.
   * @param fallback The fallback value to use if the primary is null.
   * @return The first non-blank string from the provided values.
   */
  private String firstNonBlank(String primary, String fallback) {
    if (primary != null) {
      return primary;
    }
    return fallback;
  }

  private String validateOptional(String field, String value) {
    if (value == null) {
      return null;
    }

    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      throw new BadRequestResponse(field + " cannot be blank");
    }
    if (trimmed.length() > MAX_QUERY_LENGTH) {
      throw new BadRequestResponse(field + " is too long");
    }
    return trimmed;
  }

  public record ChecklistSearchCriteria(String school, String grade, String studentName) {
    // This record is used to encapsulate the validated search criteria for checklist queries.
    // It contains three fields: school, grade, and studentName, which represent the search parameters
    // that can be used to filter checklists. By using a record, we get an immutable data structure
    // with built-in methods for equality, hashing, and string representation, making it a convenient
    // way to pass around search criteria in our application.
  }
}
