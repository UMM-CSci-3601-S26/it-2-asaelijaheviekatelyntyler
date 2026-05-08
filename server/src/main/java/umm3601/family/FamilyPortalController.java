package umm3601.family;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.auth.HttpMethod;
import umm3601.auth.RequirePermission;
import umm3601.auth.Route;
import umm3601.auth.Role;
import umm3601.checklist.Checklist;
import umm3601.checklist.ChecklistService;
import umm3601.common.AuthContext;
import umm3601.settings.Settings;
import umm3601.settings.SettingsService;
import umm3601.users.UsersService;

public class FamilyPortalController {

  private static final String API_PORTAL_BASE = "/api/family-portal";
  private static final String API_PORTAL_FORM = "/api/family-portal/form";
  private static final String API_PORTAL_CHECKLIST = "/api/family-portal/checklist";
  private static final String API_PORTAL_DRIVE_DAY = "/api/family-portal/drive-day";

  private final FamilyService familyService;
  private final FamilyValidator familyValidator;
  private final ChecklistService checklistService;
  private final SettingsService settingsService;
  private final UsersService usersService;

  public FamilyPortalController(
      FamilyService familyService,
      FamilyValidator familyValidator,
      ChecklistService checklistService,
      SettingsService settingsService,
      UsersService usersService) {
    this.familyService = familyService;
    this.familyValidator = familyValidator;
    this.checklistService = checklistService;
    this.settingsService = settingsService;
    this.usersService = usersService;
  }

  @Route(method = HttpMethod.GET, path = API_PORTAL_BASE)
  @RequirePermission("family_portal_access")
  public void getPortalSummary(Context ctx) {
    AuthContext authContext = requireGuardian(AuthContext.from(ctx));

    Family family = null;
    try {
      family = familyService.getByOwnerUserId(authContext.userId());
    } catch (NotFoundResponse ignored) {
      // No profile yet is expected for first-time guardian users.
    }

    Settings settings = settingsService.getSettings();

    Map<String, Object> summary = new LinkedHashMap<>();
    summary.put("profileComplete", family != null && family.profileComplete);
    summary.put("family", family);
    summary.put("driveDay", settings.driveDay);
    summary.put("timeSlot", family == null ? "to be assigned" : family.timeSlot);
    summary.put("timeSlotStatus", (family == null || family.timeSlot == null || family.timeSlot.isBlank()) ? "pending" : "assigned");
    summary.put("schools", settings.schools);
    summary.put("timeAvailability", settings.timeAvailability);

    ctx.json(summary);
    ctx.status(HttpStatus.OK);
  }

  @Route(method = HttpMethod.PUT, path = API_PORTAL_FORM)
  @RequirePermission("family_portal_access")
  public void upsertPortalForm(Context ctx) {
    AuthContext authContext = requireGuardian(AuthContext.from(ctx));

    Family submittedFamily = familyValidator.validatePortalFormBody(ctx.bodyAsClass(Family.class));
    submittedFamily.ownerUserId = authContext.userId();
    submittedFamily.profileComplete = true;

    if (submittedFamily.timeSlot == null || submittedFamily.timeSlot.isBlank()) {
      submittedFamily.timeSlot = "to be assigned";
    }

    familyService.upsertByOwnerUserId(submittedFamily);
    usersService.updateUserEmailById(authContext.userId(), submittedFamily.email);

    ctx.status(HttpStatus.OK);
    ctx.json(Map.of("profileComplete", true));
  }

  @Route(method = HttpMethod.GET, path = API_PORTAL_CHECKLIST)
  @RequirePermission("family_portal_access")
  public void getPortalChecklist(Context ctx) {
    AuthContext authContext = requireGuardian(AuthContext.from(ctx));

    Family family = familyService.getByOwnerUserId(authContext.userId());
    requireCompletedProfile(family);

    List<Checklist> checklists = checklistService.getStoredChecklistsForStudents(family.students);
    ctx.json(checklists);
    ctx.status(HttpStatus.OK);
  }

  @Route(method = HttpMethod.GET, path = API_PORTAL_DRIVE_DAY)
  @RequirePermission("family_portal_access")
  public void getPortalDriveDay(Context ctx) {
    AuthContext authContext = requireGuardian(AuthContext.from(ctx));

    Family family = familyService.getByOwnerUserId(authContext.userId());
    requireCompletedProfile(family);

    Settings settings = settingsService.getSettings();

    Map<String, Object> response = new LinkedHashMap<>();
    response.put("driveDay", settings.driveDay);
    response.put("timeSlot", family.timeSlot);
    response.put("timeSlotStatus", (family.timeSlot == null || family.timeSlot.isBlank()) ? "pending" : "assigned");

    ctx.json(response);
    ctx.status(HttpStatus.OK);
  }

  private AuthContext requireGuardian(AuthContext authContext) {
    if (authContext.role() != Role.GUARDIAN) {
      throw new ForbiddenResponse("Family portal is for guardian accounts only");
    }
    return authContext;
  }

  private void requireCompletedProfile(Family family) {
    if (!family.profileComplete) {
      throw new ForbiddenResponse("Complete the family form before accessing this resource");
    }
  }
}
