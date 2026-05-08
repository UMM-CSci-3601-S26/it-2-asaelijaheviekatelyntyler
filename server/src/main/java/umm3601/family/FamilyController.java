// Packages
package umm3601.family;

import java.util.Map;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.middleware.AuthMiddleware;
import umm3601.auth.HttpMethod;
import umm3601.auth.RequirePermission;
import umm3601.auth.Route;
import umm3601.common.AuthContext;
import umm3601.users.UsersService;

/**
 * Controller for handling Family-related API routes.
 *
 * Routes include:
 * - GET /api/family → list all families
 * - GET /api/family/{id} → get a single family
 * - POST /api/family → add a new family
 * - DELETE /api/family/{id} → delete a family
 * - GET /api/dashboard → aggregated student statistics
 * - GET /api/family/export → export families as CSV
 *
 * Families are the core registration unit, and dashboard stats
 * rely on embedded student data.
 */

public class FamilyController {

  private static final String API_FAMILY = "/api/families";
  private static final String API_DASHBOARD = "/api/dashboard";
  private static final String API_FAMILY_BY_ID = "/api/families/{id}";
  private static final String API_FAMILY_DELETE_REQUEST = "/api/families/{id}/delete-request";
  private static final String API_FAMILY_DELETE_REQUESTS = "/api/families/delete-requests";
  private static final String API_FAMILY_EXPORT = "/api/families/export";

  private final FamilyService service;
  private final UsersService usersService;
  private final FamilyPolicy policy;
  private final FamilyValidator validator;

  // Constructors
  public FamilyController(JacksonMongoCollection<Family> familyCollection) {
    this(new FamilyRepository(familyCollection), null);
  }

  // Backward-compatible constructor used by legacy tests.
  public FamilyController(FamilyRepository repository) {
    this(repository, null);
  }

  public FamilyController(FamilyRepository repository, UsersService usersService) {
    this(new FamilyService(repository), usersService, new FamilyPolicy(), new FamilyValidator());
  }

  // Backward-compatible constructor used by legacy tests.
  public FamilyController(MongoDatabase database, AuthMiddleware authMiddleware) {
    this(
      new FamilyService(new FamilyRepository(JacksonMongoCollection.builder().build(
        database,
        "families",
        Family.class,
        UuidRepresentation.STANDARD))),
      new UsersService(database),
      new FamilyPolicy(),
      new FamilyValidator());
  }

  public FamilyController(FamilyService service, FamilyPolicy policy, FamilyValidator validator) {
    this(service, null, policy, validator);
  }

  public FamilyController(FamilyService service, UsersService usersService, FamilyPolicy policy, FamilyValidator validator) {
    this.service = service;
    this.usersService = usersService;
    this.policy = policy;
    this.validator = validator;
  }

  /**
   * GET /api/families/{id}
   * Retrieves a single family by MongoDB ObjectId.
   *
   * Includes error handling for:
   * - invalid ObjectId format
   * - non-existent family
   */
  @Route(method = HttpMethod.GET, path = API_FAMILY_BY_ID)
  @RequirePermission("view_family")
  public void getFamily(Context ctx) {
    policy.authorizeRead(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"));
    ctx.json(service.getById(id));
    ctx.status(HttpStatus.OK);
  }

  /**
   * GET /api/families
   * Returns all registered families.
   */
  @Route(method = HttpMethod.GET, path = API_FAMILY)
  @RequirePermission("view_families")
  public void getFamilies(Context ctx) {
    policy.authorizeList(AuthContext.from(ctx));
    ctx.json(service.getAll());
    ctx.status(HttpStatus.OK);
  }

  /**
   * POST /api/families
   * Adds a new family registration.
   *
   * Validation ensures:
   * - valid email format
   *
   * Future improvements (Iteration 2):
   * - Validate that students list is not empty
   * - Validate that grade/school fields are present
   * - Validate requestedSupplies against Supply collection
   */
  @Route(method = HttpMethod.POST, path = API_FAMILY)
  @RequirePermission("add_family")
  public void addNewFamily(Context ctx) {
    policy.authorizeAdd(AuthContext.from(ctx));
    String body = ctx.body();
    Family newFamily = ctx.bodyValidator(Family.class)
        .check(fam -> fam.email != null && fam.email.matches(FamilyValidator.EMAIL_REGEX),
            "Family must have a valid email; body was " + body)
        .get();
    service.create(newFamily);
    ctx.json(Map.of("id", newFamily._id));
    ctx.status(HttpStatus.CREATED);
  }

  /**
   * DELETE /api/families/{id}
   * Removes a family registration.
   *
   * Returns 200 OK if deletion was successful, or 404 Not Found if:
   * - the ID is invalid
   * - no family with that ID exists
   */
  @Route(method = HttpMethod.DELETE, path = API_FAMILY_BY_ID)
  @RequirePermission("delete_family")
  public void deleteFamily(Context ctx) {
    policy.authorizeDelete(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"));
    try {
      Family family = service.getById(id);
      service.delete(id);
      if (usersService != null && family.ownerUserId != null && !family.ownerUserId.isBlank()) {
        usersService.deleteGuardianById(family.ownerUserId);
      }
      ctx.status(HttpStatus.OK);
    } catch (NotFoundResponse e) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw e;
    }
  }

  @Route(method = HttpMethod.POST, path = API_FAMILY_DELETE_REQUEST)
  @RequirePermission("request_family_delete")
  public void requestToDeleteFamily(Context ctx) {
    AuthContext auth = AuthContext.from(ctx);
    policy.authorizeRequestDelete(auth);
    String id = validator.validateId(ctx.pathParam("id"));
    String rawBody = ctx.body();
    DeleteRequestBody requestBody = (rawBody == null || rawBody.isBlank()) ? null : ctx.bodyAsClass(DeleteRequestBody.class);
    String message = requestBody == null || requestBody.message == null ? "" : requestBody.message.trim();
    if (message.isBlank()) {
      message = "Requested by volunteer";
    }
    try {
      service.requestDeleteByVolunteer(id, message, auth.userId());
      ctx.status(HttpStatus.OK);
    } catch (NotFoundResponse e) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw e;
    }
  }

  @Route(method = HttpMethod.GET, path = API_FAMILY_DELETE_REQUESTS)
  @RequirePermission("delete_family")
  public void getDeleteRequests(Context ctx) {
    policy.authorizeManageDeleteRequests(AuthContext.from(ctx));
    ctx.json(service.getDeleteRequests());
    ctx.status(HttpStatus.OK);
  }

  @Route(method = HttpMethod.DELETE, path = API_FAMILY_DELETE_REQUEST)
  @RequirePermission("delete_family")
  public void restoreFamilyDeleteRequest(Context ctx) {
    policy.authorizeManageDeleteRequests(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"));
    try {
      service.clearDeleteRequest(id);
      ctx.status(HttpStatus.OK);
    } catch (NotFoundResponse e) {
      ctx.status(HttpStatus.NOT_FOUND);
      throw e;
    }
  }

  /**
   * GET /api/dashboard
   * Computes summary statistics for:
   * - students per school
   * - students per grade
   * - total families
   *
   * Because students are embedded inside families,
   * this requires only one database query.
   *
   * Future improvements (Iteration 2):
   * - total students
   * - filterable for per district, grade, and school.
   */
  @Route(method = HttpMethod.GET, path = API_DASHBOARD)
  @RequirePermission("view_dashboard_stats")
  public void getDashboardStats(Context ctx) {
    policy.authorizeDashboard(AuthContext.from(ctx));
    ctx.json(service.getDashboardStats());
  }

  /**
   * GET /api/family/export
   * Exports a simple CSV of family-level data.
   *
   * Note: This does NOT export student-level details.
   * Future teams may expand this to include:
   * - requested supplies
   * - filtering options
   */
  @Route(method = HttpMethod.GET, path = API_FAMILY_EXPORT)
  @RequirePermission("export_families_csv")
  public void exportFamiliesAsCSV(Context ctx) {
    policy.authorizeExport(AuthContext.from(ctx));
    ctx.contentType("text/csv");
    ctx.header("Content-Disposition", "attachment; filename=families.csv");
    ctx.status(HttpStatus.OK);
    ctx.result(service.exportFamiliesCsv());
  }

  // Backward-compatible route registration used by legacy tests.
  public void addRoutes(Javalin server) {
    server.get(API_FAMILY, this::getFamilies);
    server.post(API_FAMILY, this::addNewFamily);
    server.get(API_FAMILY_EXPORT, this::exportFamiliesAsCSV);
    server.get(API_DASHBOARD, this::getDashboardStats);
    server.get(API_FAMILY_BY_ID, this::getFamily);
    server.delete(API_FAMILY_BY_ID, this::deleteFamily);
  }

  public static class DeleteRequestBody {
    public String message;
  }
}
