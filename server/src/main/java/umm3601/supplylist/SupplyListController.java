// Packages
package umm3601.supplylist;

import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import umm3601.auth.HttpMethod;
import umm3601.auth.RequirePermission;
import umm3601.auth.Route;
import umm3601.common.AuthContext;
import umm3601.middleware.AuthMiddleware;
import umm3601.supplylist.SupplyListValidator.SupplyListSearchCriteria;

/**
 * Controller for handling SupplyList-related API routes.
 *
 * Routes include:
 * - GET /api/supplylist → list all supply list items (with optional filters)
 * - GET /api/supplylist/{id} → get a single supply list item
 *
 * Supply List is the core data model for tracking what supplies students, and
 * will be used
 * help calculate supply demands.
 */

public class SupplyListController {
  @SuppressWarnings("SpellCheckingInspection")
  private static final String API_SUPPLYLIST = "/api/supplylist";
  private static final String API_SUPPLYLIST_BY_ID = "/api/supplylist/{id}";
  static final String SCHOOL_KEY = "school";
  static final String GRADE_KEY = "grade";
  static final String TEACHER_KEY = "teacher";
  static final String ACADEMIC_YEAR_KEY = "academicYear";
  static final String ITEM_KEY = "item";
  static final String BRAND_KEY = "brand";
  static final String COUNT_KEY = "count";
  static final String SIZE_KEY = "size";
  static final String COLOR_KEY = "color";
  static final String QUANTITY_KEY = "quantity";
  static final String NOTES_KEY = "notes";
  static final String MATERIAL_KEY = "material";
  static final String TYPE_KEY = "type";
  static final String STYLE_KEY = "style";

  private final SupplyListService service;
  private final SupplyListPolicy policy;
  private final SupplyListValidator validator;

  public SupplyListController(JacksonMongoCollection<SupplyList> supplyListCollection) {
    this(new SupplyListRepository(supplyListCollection));
  }

  public SupplyListController(SupplyListRepository repository) {
    this(new SupplyListService(repository), new SupplyListPolicy(), new SupplyListValidator());
  }

  // Backward-compatible constructor used by legacy tests.
  public SupplyListController(MongoDatabase database, AuthMiddleware authMiddleware) {
    this(JacksonMongoCollection.builder().build(
        database,
        "supplylist",
        SupplyList.class,
        UuidRepresentation.STANDARD));
  }

  public SupplyListController(
      SupplyListService service,
      SupplyListPolicy policy,
      SupplyListValidator validator) {
    this.service = service;
    this.policy = policy;
    this.validator = validator;
  }

  /**
   * GET /api/supplylist/{id}
   * Retrieves a single supply list item by its MongoDB ObjectId.
   */
  @Route(method = HttpMethod.GET, path = API_SUPPLYLIST_BY_ID)
  @RequirePermission("view_supply_lists")
  public void getList(Context ctx) {
    policy.authorizeRead(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"));
    ctx.json(service.getById(id));
    ctx.status(HttpStatus.OK);
  }

  /**
   * GET /api/supplylist
   * Retrieves all supply list items, with optional query parameters for
   * filtering.
   */
  @Route(method = HttpMethod.GET, path = API_SUPPLYLIST)
  @RequirePermission("view_supply_lists")
  public void getSupplyLists(Context ctx) {
    policy.authorizeRead(AuthContext.from(ctx));
    SupplyListSearchCriteria criteria = validator.validateQuery(ctx);
    ctx.json(service.getAll(criteria));
    ctx.status(HttpStatus.OK);
  }

  @Route(method = HttpMethod.POST, path = API_SUPPLYLIST)
  @RequirePermission("add_supply_list")
  public void addSupplyList(Context ctx) {
    policy.authorizeCreate(AuthContext.from(ctx));
    SupplyList newSupplyList = ctx.bodyValidator(SupplyList.class)
        .check(s -> s.school != null && !s.school.isBlank(), "school must be a non-empty string")
        .check(s -> s.grade != null && !s.grade.isBlank(), "grade must be a non-empty string")
        .check(s -> s.item != null && !s.item.isEmpty(), "item must be a non-empty list")
        .check(s -> s.count > 0, "count must be a positive integer")
        .check(s -> s.quantity > 0, "quantity must be a positive integer")
        .get();
    service.create(newSupplyList);
    ctx.status(HttpStatus.CREATED);
  }

  @Route(method = HttpMethod.DELETE, path = API_SUPPLYLIST_BY_ID)
  @RequirePermission("delete_supply_list")
  public void deleteSupplyList(Context ctx) {
    policy.authorizeDelete(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"));
    service.delete(id);
    ctx.status(HttpStatus.NO_CONTENT);
  }

  @Route(method = HttpMethod.PUT, path = API_SUPPLYLIST_BY_ID)
  @RequirePermission("edit_supply_list")
  public void editSupplyList(Context ctx) {
    policy.authorizeEdit(AuthContext.from(ctx));
    String id = validator.validateId(ctx.pathParam("id"));
    SupplyList updatedSupplyList = ctx.bodyValidator(SupplyList.class)
        .check(s -> s.school != null && !s.school.isBlank(), "school must be a non-empty string")
        .check(s -> s.grade != null && !s.grade.isBlank(), "grade must be a non-empty string")
        .check(s -> s.item != null && !s.item.isEmpty(), "item must be a non-empty list")
        .check(s -> s.count > 0, "count must be a positive integer")
        .check(s -> s.quantity > 0, "quantity must be a positive integer")
        .get();
    service.update(id, updatedSupplyList);
    ctx.status(HttpStatus.OK);
  }

  // Backward-compatible route registration used by legacy tests.
  public void addRoutes(Javalin server) {
    server.get(API_SUPPLYLIST, this::getSupplyLists);
    server.get(API_SUPPLYLIST_BY_ID, this::getList);
    server.post(API_SUPPLYLIST, this::addSupplyList);
    server.put(API_SUPPLYLIST_BY_ID, this::editSupplyList);
    server.delete(API_SUPPLYLIST_BY_ID, this::deleteSupplyList);
  }
}
