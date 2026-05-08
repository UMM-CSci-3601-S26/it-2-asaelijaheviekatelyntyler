// Package declaration
package umm3601.checklist;

// Java Imports
import java.util.List;

// Com Imports
import com.mongodb.client.MongoDatabase;

// IO Imports
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

// Application Imports
import umm3601.auth.HttpMethod;
import umm3601.auth.RequirePermission;
import umm3601.auth.Route;
import umm3601.checklist.ChecklistValidator.ChecklistSearchCriteria;
import umm3601.common.AuthContext;

/**
 * ChecklistController is a Javalin controller class responsible for handling
 * HTTP requests related to checklists. It defines route handler methods for
 * generating digital checklists, retrieving stored checklists, and exporting
 * checklists as PDFs. Each method is annotated with @Route to specify the HTTP
 * method and path, and @RequirePermission to enforce permission checks for
 * accessing these routes.
 */

public class ChecklistController {

  private static final String API_CHECKLIST = "/api/checklists";
  private static final String API_CHECKLIST_EXPORT_PDF = "/checklists/export/pdf";

  private final ChecklistService service;
  private final ChecklistPolicy policy;
  private final ChecklistValidator validator;

  // Constructors
  public ChecklistController(MongoDatabase db) {
    this(new ChecklistRepository(db));
  }

  // For testing purposes
  public ChecklistController(ChecklistRepository repository) {
    this(repository, new ChecklistPolicy(), new ChecklistValidator());
  }

  // For testing purposes
  ChecklistController(ChecklistRepository repository, ChecklistPolicy policy, ChecklistValidator validator) {
    this(new ChecklistService(repository), policy, validator);
  }

  // For testing purposes
  ChecklistController(ChecklistService service, ChecklistPolicy policy, ChecklistValidator validator) {
    this.service = service;
    this.policy = policy;
    this.validator = validator;
  }

  /**
   * Generates digital checklists based on predefined templates and criteria. This
   * route is protected by the "manage_checklist" permission, ensuring that only
   * authorized users can generate new checklists. The method first checks if the
   * user has the necessary permissions to generate checklists using the
   * ChecklistPolicy. If authorized, it calls the ChecklistService to generate the
   * digital checklists and returns them in the response with a 201 Created
   * status.
   *
   * @param ctx The Javalin Context object representing the HTTP request and
   *            response.
   */
  @Route(method = HttpMethod.POST, path = API_CHECKLIST)
  @RequirePermission("manage_checklist")
  public void generateDigitalChecklists(Context ctx) {
    policy.authorizeChecklistGeneration(AuthContext.from(ctx));
    List<Checklist> checklists = service.generateDigitalChecklists();
    ctx.json(checklists);
    ctx.status(HttpStatus.CREATED);
  }

  /**
   * Retrieves stored checklists based on search criteria provided in the request.
   * This route is protected by the "view_checklist" permission, ensuring that
   * only authorized users can access stored checklists. The method first checks if
   * the user has the necessary permissions to read stored checklists using the
   * ChecklistPolicy. If authorized, it validates the search criteria from the
   * request using the ChecklistValidator, retrieves the matching checklists from
   * the ChecklistService, and returns them in the response with a 200 OK status.
   *
   * @param ctx The Javalin Context object representing the HTTP request and
   *            response.
   */
  @Route(method = HttpMethod.GET, path = API_CHECKLIST)
  @RequirePermission("view_checklist")
  public void getStoredChecklists(Context ctx) {
    policy.authorizeStoredChecklistRead(AuthContext.from(ctx));
    ChecklistSearchCriteria criteria = validator.validateSearch(ctx);
    ctx.json(service.getStoredChecklists(criteria));
    ctx.status(HttpStatus.OK);
  }

  /**
   * Exports checklists as a PDF document. This route is protected by the
   * "manage_checklist" permission, ensuring that only authorized users can export
   * checklists. The method first checks if the user has the necessary permissions
   * to export checklists using the ChecklistPolicy. If authorized, it calls the
   * ChecklistService to generate the PDF export of the checklists, sets the appropriate
   * content type and headers for a PDF response, and returns the PDF data in the
   * response with a 200 OK status.
   *
   * @param ctx The Javalin Context object representing the HTTP request and response.
   */
  @Route(method = HttpMethod.GET, path = API_CHECKLIST_EXPORT_PDF)
  @RequirePermission("manage_checklist")
  public void exportChecklistsPdf(Context ctx) {
    policy.authorizeChecklistExport(AuthContext.from(ctx));
    ctx.contentType("application/pdf");
    ctx.header("Content-Disposition", "inline; filename=checklists.pdf");
    ctx.result(service.exportChecklistsPdf());
    ctx.status(HttpStatus.OK);
  }
}
