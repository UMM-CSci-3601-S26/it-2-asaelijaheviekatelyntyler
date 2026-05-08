package umm3601.checklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import umm3601.family.Family;
import umm3601.family.Family.StudentInfo;
import umm3601.settings.Settings;
import umm3601.supplylist.SupplyList;

@SuppressWarnings("MagicNumber")
class ChecklistServiceSpec {

  @Mock
  private ChecklistRepository repository;

  private ChecklistService service;

  @BeforeEach
  void setupEach() {
    MockitoAnnotations.openMocks(this);
    service = new ChecklistService(repository);
  }

  @Test
  void generateDigitalChecklistsAppliesConfiguredSupplyOrder() {
    Family.StudentInfo student = new Family.StudentInfo();
    student.name = "Ana";
    student.school = "MAHS";
    student.grade = "4";

    Family family = new Family();
    family.students = List.of(student);

    SupplyList notebook = new SupplyList();
    notebook.school = "MAHS";
    notebook.grade = "4";
    notebook.item = List.of("notebook");

    SupplyList pencil = new SupplyList();
    pencil.school = "MAHS";
    pencil.grade = "4";
    pencil.item = List.of("pencil");

    Settings settings = new Settings();
    Settings.SupplyItemOrder stagedNotebook = new Settings.SupplyItemOrder();
    stagedNotebook.itemTerm = "notebook";
    stagedNotebook.status = "staged";
    Settings.SupplyItemOrder notGivenPencil = new Settings.SupplyItemOrder();
    notGivenPencil.itemTerm = "pencil";
    notGivenPencil.status = "notGiven";
    settings.supplyOrder = List.of(stagedNotebook, notGivenPencil);

    when(repository.getFamilies()).thenReturn(List.of(family));
    when(repository.getSupplyLists()).thenReturn(List.of(pencil, notebook));
    when(repository.getSettings()).thenReturn(settings);

    List<Checklist> generated = service.generateDigitalChecklists();

    assertEquals(1, generated.size());
    assertEquals(1, generated.get(0).checklist.size());
    assertEquals(List.of("notebook"), generated.get(0).checklist.get(0).supply.item);
    verify(repository).replaceAllChecklists(generated);
  }

  @Test
  void exportChecklistsPdfIncludesStudentAndSupplyText() {
    Family.StudentInfo student = new Family.StudentInfo();
    student.name = "Jordan";
    student.school = "AHS";
    student.grade = "8";

    Family family = new Family();
    family.students = List.of(student);

    SupplyList supply = new SupplyList();
    supply.school = "AHS";
    supply.grade = "8";
    supply.item = List.of("folder");

    when(repository.getFamilies()).thenReturn(List.of(family));
    when(repository.getSupplyLists()).thenReturn(List.of(supply));

    String pdf = new String(service.exportChecklistsPdf());

    assertTrue(pdf.startsWith("%PDF-1.4"));
    assertTrue(pdf.contains("Student: Jordan"));
    assertTrue(pdf.contains("folder"));
  }

  @Test
  void getStoredChecklistsForStudentsDelegatesToRepository() {
    Family.StudentInfo student = new Family.StudentInfo();
    student.name = "Micah";
    student.school = "MAHS";
    student.grade = "6";

    Checklist checklist = new Checklist();
    checklist.studentName = "Micah";

    when(repository.getStoredChecklistsForStudents(List.of(student))).thenReturn(List.of(checklist));

    List<Checklist> result = service.getStoredChecklistsForStudents(List.of(student));

    assertEquals(1, result.size());
    assertEquals("Micah", result.get(0).studentName);
  }

  @Test
  void generateDigitalChecklistsFallsBackWhenSettingsMissingOrEmpty() {
    StudentInfo student = new StudentInfo();
    student.name = "Noah";
    student.school = "MAHS";
    student.grade = "4";

    Family family = new Family();
    family.students = List.of(student);

    SupplyList supply = new SupplyList();
    supply.school = "MAHS";
    supply.grade = "4";
    supply.item = List.of("binder");

    when(repository.getFamilies()).thenReturn(List.of(family));
    when(repository.getSupplyLists()).thenReturn(List.of(supply));
    when(repository.getSettings()).thenReturn(null);

    List<Checklist> withoutSettings = service.generateDigitalChecklists();
    assertEquals(1, withoutSettings.size());
    assertEquals(1, withoutSettings.get(0).checklist.size());

    Settings settings = new Settings();
    settings.supplyOrder = List.of();
    when(repository.getSettings()).thenReturn(settings);

    List<Checklist> withEmptyOrder = service.generateDigitalChecklists();
    assertEquals(1, withEmptyOrder.size());
    assertEquals(List.of("binder"), withEmptyOrder.get(0).checklist.get(0).supply.item);
  }

  @Test
  void constructFilterBuildsPartialAndEmptyFilters() {
    ChecklistValidator.ChecklistSearchCriteria emptyCriteria =
        new ChecklistValidator.ChecklistSearchCriteria(null, null, null);
    ChecklistValidator.ChecklistSearchCriteria schoolOnlyCriteria =
        new ChecklistValidator.ChecklistSearchCriteria("MAHS", null, null);
    ChecklistValidator.ChecklistSearchCriteria fullCriteria =
        new ChecklistValidator.ChecklistSearchCriteria("MAHS", "4", "Ana");

    Bson emptyFilter = service.constructFilter(emptyCriteria);
    Bson schoolOnlyFilter = service.constructFilter(schoolOnlyCriteria);
    Bson fullFilter = service.constructFilter(fullCriteria);

    assertNotNull(emptyFilter);
    assertNotNull(schoolOnlyFilter);
    assertNotNull(fullFilter);
    assertTrue(schoolOnlyFilter.toString().contains("school"));
    assertTrue(fullFilter.toString().contains("studentName"));
    assertTrue(fullFilter.toString().contains("grade"));
  }

  @Test
  void applySupplyOrderHandlesNullItemsAndMultipleStagedTerms() {
    SupplyList nullItemSupply = new SupplyList();
    nullItemSupply.item = null;

    SupplyList markerSupply = new SupplyList();
    markerSupply.item = List.of("marker", "paper");

    Settings.SupplyItemOrder first = new Settings.SupplyItemOrder();
    first.itemTerm = "marker";
    first.status = "staged";

    Settings.SupplyItemOrder second = new Settings.SupplyItemOrder();
    second.itemTerm = "paper";
    second.status = "staged";

    List<SupplyList> ordered = ChecklistService.applySupplyOrder(
        List.of(nullItemSupply, markerSupply),
        List.of(first, second));

    assertEquals(2, ordered.size());
    assertEquals(List.of("marker", "paper"), ordered.get(0).item);
    assertEquals(null, ordered.get(1).item);
  }

  @Test
  void expandHighSchoolLeavesNonHighSchoolSuppliesUntouched() {
    SupplyList gradeSpecific = new SupplyList();
    gradeSpecific.school = "AHS";
    gradeSpecific.grade = "8";
    gradeSpecific.item = List.of("folder");

    List<SupplyList> result = ChecklistService.expandHighSchoolSupplies(List.of(gradeSpecific));

    assertEquals(1, result.size());
    assertEquals("8", result.get(0).grade);
  }
}
