// Package
package umm3601.settings;

import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import umm3601.auth.HttpMethod;
import umm3601.auth.RequirePermission;
import umm3601.auth.RequireRole;
import umm3601.auth.Role;
import umm3601.auth.Route;
import umm3601.common.AuthContext;
import umm3601.middleware.AuthMiddleware;

/**
 * Controller for the singleton app settings document.
 *
 * Routes:
 *  - GET  /api/settings                     → returns the full settings document
 *  - PATCH /api/settings/schools            → replaces the schools list
 *  - PATCH /api/settings/timeAvailability   → replaces the time availability labels
 *
 * Patching by section prevents one tab from overwriting another's changes.
 * All patch operations use upsert so the document is created on first write.
 */
public class SettingsController {

  // The fixed _id used for the singleton settings document
  public static final String SETTINGS_ID = "app-settings";

  private static final String API_SETTINGS = "/api/settings";
  private static final String API_SETTINGS_SCHOOLS = "/api/settings/schools";
  private static final String API_SETTINGS_TIME = "/api/settings/timeAvailability";
  private static final String API_SETTINGS_SUPPLY_ORDER = "/api/settings/supplyOrder";
  private static final String API_SETTINGS_DRIVE_DAY = "/api/settings/driveDay";

  private final SettingsService service;
  private final SettingsPolicy policy;
  private final SettingsValidator validator;

  public SettingsController(JacksonMongoCollection<Settings> settingsCollection) {
    this(new SettingsRepository(settingsCollection));
  }

  public SettingsController(SettingsRepository repository) {
    this(new SettingsService(repository), new SettingsPolicy(), new SettingsValidator());
  }

  // Backward-compatible constructor used by legacy tests.
  public SettingsController(MongoDatabase database, AuthMiddleware authMiddleware) {
    this(JacksonMongoCollection.builder().build(
        database,
        "settings",
        Settings.class,
        UuidRepresentation.STANDARD));
  }

  public SettingsController(
      SettingsService service,
      SettingsPolicy policy,
      SettingsValidator validator) {
    this.service = service;
    this.policy = policy;
    this.validator = validator;
  }

  /**
   * GET /api/settings
   * Returns the settings document, or a safe default if none exists yet.
   */
  @Route(method = HttpMethod.GET, path = API_SETTINGS)
  @RequirePermission("view_settings")
  public void getSettings(Context ctx) {
    policy.authorizeRead(AuthContext.from(ctx));
    ctx.json(service.getSettings());
    ctx.status(HttpStatus.OK);
  }

  /**
   * PATCH /api/settings/schools
   * Replaces the schools list. Body: { "schools": [{ "name": "...", "abbreviation": "..." }] }
   */
  @Route(method = HttpMethod.PATCH, path = API_SETTINGS_SCHOOLS)
  @RequirePermission("edit_schools")
  public void updateSchools(Context ctx) {
    policy.authorizeEdit(AuthContext.from(ctx));
    Settings body = validator.validateSchools(ctx.bodyAsClass(Settings.class));
    service.updateSchools(body);
    ctx.status(HttpStatus.OK);
  }

  /**
   * PATCH /api/settings/supplyOrder
   * Replaces the supply item ordering used when generating checklists.
   * Body: { "supplyOrder": [{ "supplyId": "...", "status": "staged|unstaged|notGiven" }] }
   */
  @Route(method = HttpMethod.PATCH, path = API_SETTINGS_SUPPLY_ORDER)
  @RequirePermission("edit_supply_order")
  public void updateSupplyOrder(Context ctx) {
    policy.authorizeEdit(AuthContext.from(ctx));
    Settings body = validator.validateSupplyOrder(ctx.bodyAsClass(Settings.class));
    service.updateSupplyOrder(body);
    ctx.status(HttpStatus.OK);
  }

  /**
   * PATCH /api/settings/timeAvailability
   * Replaces the time availability labels.
   * Body: { "earlyMorning": "8:00–9:00 AM", "lateMorning": "...", ... }
   */
  @Route(method = HttpMethod.PATCH, path = API_SETTINGS_TIME)
  @RequirePermission("edit_time_availability")
  public void updateTimeAvailability(Context ctx) {
    policy.authorizeEdit(AuthContext.from(ctx));
    Settings.TimeAvailabilityLabels labels =
        validator.validateTimeAvailability(ctx.bodyAsClass(Settings.TimeAvailabilityLabels.class));
    service.updateTimeAvailability(labels);
    ctx.status(HttpStatus.OK);
  }

  /**
   * PATCH /api/settings/driveDay
   * Updates drive-day details used in the family portal.
   * Body: { "date": "2026-08-16", "message": "Please arrive 10 minutes early" }
   */
  @Route(method = HttpMethod.PATCH, path = API_SETTINGS_DRIVE_DAY)
  @RequireRole(Role.ADMIN)
  public void updateDriveDay(Context ctx) {
    policy.authorizeEdit(AuthContext.from(ctx));
    Settings.DriveDay driveDay = validator.validateDriveDay(ctx.bodyAsClass(Settings.DriveDay.class));
    service.updateDriveDay(driveDay);
    ctx.status(HttpStatus.OK);
  }

  // Backward-compatible route registration used by legacy tests.
  public void addRoutes(Javalin server) {
    server.get(API_SETTINGS, this::getSettings);
    server.patch(API_SETTINGS_SCHOOLS, this::updateSchools);
    server.patch(API_SETTINGS_SUPPLY_ORDER, this::updateSupplyOrder);
    server.patch(API_SETTINGS_TIME, this::updateTimeAvailability);
    server.patch(API_SETTINGS_DRIVE_DAY, this::updateDriveDay);
  }
}
