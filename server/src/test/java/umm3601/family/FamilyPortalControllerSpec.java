package umm3601.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import umm3601.auth.Role;
import umm3601.checklist.Checklist;
import umm3601.checklist.ChecklistService;
import umm3601.settings.Settings;
import umm3601.settings.SettingsService;
import umm3601.users.UsersService;

class FamilyPortalControllerSpec {

  private FamilyPortalController controller;

  @Mock
  private FamilyService familyService;

  @Mock
  private FamilyValidator familyValidator;

  @Mock
  private ChecklistService checklistService;

  @Mock
  private SettingsService settingsService;

  @Mock
  private UsersService usersService;

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<Map<String, Object>> mapCaptor;

  @Captor
  private ArgumentCaptor<Family> familyCaptor;

  @Captor
  private ArgumentCaptor<List<Checklist>> checklistCaptor;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    controller = new FamilyPortalController(
      familyService,
      familyValidator,
      checklistService,
      settingsService,
      usersService
    );
    when(ctx.attribute("userId")).thenReturn("guardian-user-1");
    when(ctx.attribute("systemRole")).thenReturn(Role.GUARDIAN);
  }

  @Test
  void getPortalSummaryReturnsFamilyAndAssignedTimeslot() {
    Family family = buildFamily(true, "9:00-10:00 AM");
    when(familyService.getByOwnerUserId("guardian-user-1")).thenReturn(family);
    when(settingsService.getSettings()).thenReturn(buildSettings());

    controller.getPortalSummary(ctx);

    verify(ctx).json(mapCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    Map<String, Object> payload = mapCaptor.getValue();
    assertEquals(true, payload.get("profileComplete"));
    assertEquals("assigned", payload.get("timeSlotStatus"));
    assertEquals("9:00-10:00 AM", payload.get("timeSlot"));
  }

  @Test
  void getPortalSummaryHandlesMissingFamilyAsPending() {
    when(familyService.getByOwnerUserId("guardian-user-1")).thenThrow(new NotFoundResponse("none"));
    when(settingsService.getSettings()).thenReturn(buildSettings());

    controller.getPortalSummary(ctx);

    verify(ctx).json(mapCaptor.capture());
    Map<String, Object> payload = mapCaptor.getValue();
    assertEquals(false, payload.get("profileComplete"));
    assertEquals("pending", payload.get("timeSlotStatus"));
    assertEquals("to be assigned", payload.get("timeSlot"));
  }

  @Test
  void getPortalSummaryRejectsNonGuardian() {
    when(ctx.attribute("systemRole")).thenReturn(Role.VOLUNTEER);

    assertThrows(ForbiddenResponse.class, () -> controller.getPortalSummary(ctx));
  }

  @Test
  void upsertPortalFormSetsOwnerAndDefaultTimeslot() {
    Family submitted = buildFamily(false, " ");
    when(ctx.bodyAsClass(Family.class)).thenReturn(submitted);
    when(familyValidator.validatePortalFormBody(submitted)).thenReturn(submitted);

    controller.upsertPortalForm(ctx);

    verify(familyService).upsertByOwnerUserId(familyCaptor.capture());
    Family saved = familyCaptor.getValue();
    assertEquals("guardian-user-1", saved.ownerUserId);
    assertEquals(true, saved.profileComplete);
    assertEquals("to be assigned", saved.timeSlot);
    verify(ctx).status(HttpStatus.OK);
  }

  @Test
  void getPortalChecklistReturnsStudentChecklists() {
    Family family = buildFamily(true, "9:00-10:00 AM");
    Checklist checklist = new Checklist();
    checklist.studentName = "Kid";

    when(familyService.getByOwnerUserId("guardian-user-1")).thenReturn(family);
    when(checklistService.getStoredChecklistsForStudents(family.students)).thenReturn(List.of(checklist));

    controller.getPortalChecklist(ctx);

    verify(ctx).json(checklistCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(1, checklistCaptor.getValue().size());
  }

  @Test
  void getPortalChecklistRejectsIncompleteProfile() {
    Family family = buildFamily(false, "9:00-10:00 AM");
    when(familyService.getByOwnerUserId("guardian-user-1")).thenReturn(family);

    assertThrows(ForbiddenResponse.class, () -> controller.getPortalChecklist(ctx));
  }

  @Test
  void getPortalDriveDayReturnsPendingWhenTimeslotBlank() {
    Family family = buildFamily(true, " ");
    Settings settings = buildSettings();
    when(familyService.getByOwnerUserId("guardian-user-1")).thenReturn(family);
    when(settingsService.getSettings()).thenReturn(settings);

    controller.getPortalDriveDay(ctx);

    verify(ctx).json(mapCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals("pending", mapCaptor.getValue().get("timeSlotStatus"));
  }

  @Test
  void getPortalDriveDayRejectsIncompleteProfile() {
    Family family = buildFamily(false, "9:00-10:00 AM");
    when(familyService.getByOwnerUserId("guardian-user-1")).thenReturn(family);

    assertThrows(ForbiddenResponse.class, () -> controller.getPortalDriveDay(ctx));
  }

  private Family buildFamily(boolean profileComplete, String timeSlot) {
    Family family = new Family();
    family.profileComplete = profileComplete;
    family.timeSlot = timeSlot;
    Family.StudentInfo student = new Family.StudentInfo();
    student.name = "Kid";
    student.grade = "2";
    student.school = "MHS";
    family.students = List.of(student);
    return family;
  }

  private Settings buildSettings() {
    Settings settings = new Settings();
    Settings.DriveDay driveDay = new Settings.DriveDay();
    driveDay.date = "2026-08-15";
    driveDay.message = "Be on time";
    settings.driveDay = driveDay;
    settings.schools = List.of();
    settings.timeAvailability = new Settings.TimeAvailabilityLabels();
    return settings;
  }
}
