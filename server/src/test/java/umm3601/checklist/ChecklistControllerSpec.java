package umm3601.checklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import umm3601.auth.PermissionsService;
import umm3601.auth.Role;
import umm3601.auth.RouteRegistrar;
import umm3601.family.Family.StudentInfo;
import umm3601.settings.Settings;
import umm3601.supplylist.SupplyList;

@SuppressWarnings({"MagicNumber"})
class ChecklistControllerSpec {

  private ChecklistController checklistController;
  private ChecklistService checklistService;
  private ChecklistValidator checklistValidator;

  private static MongoClient mongoClient;
  private static MongoDatabase db;

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<List<Checklist>> checklistListCaptor;

  @BeforeAll
  static void setupAll() {
    String mongoAddr = System.getenv().getOrDefault("MONGO_ADDR", "localhost");

    mongoClient = MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(mongoAddr))))
            .build());
    db = mongoClient.getDatabase("test");
  }

  @AfterAll
  static void teardown() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() {
    MockitoAnnotations.openMocks(this);

    seedStoredChecklists();

    checklistController = new ChecklistController(db);
    checklistService = new ChecklistService(mock(ChecklistRepository.class));
    checklistValidator = new ChecklistValidator();

    when(ctx.attribute("userId")).thenReturn("test-user");
    when(ctx.attribute("role")).thenReturn(Role.ADMIN);
  }

  private void seedStoredChecklists() {
    MongoCollection<Document> checklistDocuments = db.getCollection("checklists");
    checklistDocuments.drop();

    checklistDocuments.insertMany(List.of(
        new Document()
            .append("school", "MAHS")
            .append("grade", "4")
            .append("studentName", "Elmo")
            .append("requestedSupplies", List.of("headphones"))
            .append("checklist", List.of()),
        new Document()
            .append("school", "AHS")
            .append("grade", "8")
            .append("studentName", "johnny")
            .append("requestedSupplies", List.of("backpack"))
            .append("checklist", List.of()),
        new Document()
            .append("school", "SSHS")
            .append("grade", "2")
            .append("studentName", "Rocco")
            .append("requestedSupplies", List.of())
            .append("checklist", List.of())));
  }

  @Test
  void addsRoutes() {
    Javalin mockServer = mock(Javalin.class);
    RouteRegistrar.register(mockServer, checklistController, mock(PermissionsService.class));

    verify(mockServer, atLeastOnce()).get(any(), any());
    verify(mockServer, atLeastOnce()).post(any(), any());
  }

  @Test
  void canGetAllChecklists() {
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());

    checklistController.getStoredChecklists(ctx);

    verify(ctx).json(checklistListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);
    assertEquals(3, checklistListCaptor.getValue().size());
  }

  @Test
  void filterChecklistsBySchoolAndGrade() {
    when(ctx.queryParamMap()).thenReturn(Map.of("school", List.of("MAHS"), "grade", List.of("4")));
    when(ctx.queryParam("school")).thenReturn("MAHS");
    when(ctx.queryParam("grade")).thenReturn("4");

    checklistController.getStoredChecklists(ctx);

    verify(ctx).json(checklistListCaptor.capture());
    List<Checklist> result = checklistListCaptor.getValue();
    assertEquals(1, result.size());
    assertEquals("Elmo", result.get(0).studentName);
  }

  @Test
  void filterChecklistsByStudentNameAlias() {
    when(ctx.queryParamMap()).thenReturn(Map.of("name", List.of("Elmo")));
    when(ctx.queryParam("name")).thenReturn("Elmo");
    when(ctx.queryParam("studentName")).thenReturn(null);

    checklistController.getStoredChecklists(ctx);

    verify(ctx).json(checklistListCaptor.capture());
    List<Checklist> result = checklistListCaptor.getValue();
    assertEquals(1, result.size());
    assertEquals("Elmo", result.get(0).studentName);
  }

  @Test
  void validatorRejectsUnsupportedQueryParam() {
    when(ctx.queryParamMap()).thenReturn(Map.of("district", List.of("Somewhere")));

    assertThrows(BadRequestResponse.class, () -> checklistValidator.validateSearch(ctx));
  }

  @Test
  void validatorRejectsBlankStudentName() {
    when(ctx.queryParamMap()).thenReturn(Map.of("studentName", List.of("   ")));
    when(ctx.queryParam("studentName")).thenReturn("   ");
    when(ctx.queryParam("name")).thenReturn(null);

    assertThrows(BadRequestResponse.class, () -> checklistValidator.validateSearch(ctx));
  }

  @Test
  void controllerGenerateDigitalChecklistsReturnsCreated() {
    ChecklistService service = mock(ChecklistService.class);
    ChecklistController controller = new ChecklistController(service, new ChecklistPolicy(), new ChecklistValidator());
    List<Checklist> generated = List.of(new Checklist());
    when(service.generateDigitalChecklists()).thenReturn(generated);

    controller.generateDigitalChecklists(ctx);

    verify(ctx).json(generated);
    verify(ctx).status(HttpStatus.CREATED);
  }

  @Test
  void controllerExportChecklistsPdfReturnsPdf() {
    ChecklistService service = mock(ChecklistService.class);
    ChecklistController controller = new ChecklistController(service, new ChecklistPolicy(), new ChecklistValidator());
    byte[] pdf = "pdf".getBytes();
    when(service.exportChecklistsPdf()).thenReturn(pdf);

    controller.exportChecklistsPdf(ctx);

    verify(ctx).contentType("application/pdf");
    verify(ctx).header("Content-Disposition", "inline; filename=checklists.pdf");
    verify(ctx).result(pdf);
    verify(ctx).status(HttpStatus.OK);
  }

  @Test
  void createChecklistMakesTheRightChecklist() {
    StudentInfo student = new StudentInfo();
    student.name = "Elmo";
    student.school = "MAHS";
    student.grade = "4";
    student.requestedSupplies = List.of("headphones");

    SupplyList supply = new SupplyList();
    supply.school = "MAHS";
    supply.grade = "4";
    supply.item = Arrays.asList("Notebook");

    Checklist result = checklistService.createChecklist(student, List.of(supply));

    assertEquals("Elmo", result.studentName);
    assertEquals("MAHS", result.school);
    assertEquals("4", result.grade);
    assertEquals(List.of("headphones"), result.requestedSupplies);
    assertEquals(1, result.checklist.size());
    assertTrue(result.checklist.get(0).supply.item.contains("Notebook"));
    assertEquals(false, result.checklist.get(0).completed);
    assertEquals(false, result.checklist.get(0).unreceived);
    assertEquals(null, result.checklist.get(0).selectedOption);
  }

  @Test
  void createChecklistExcludesSuppliesForDifferentSchoolOrGrade() {
    StudentInfo student = new StudentInfo();
    student.name = "Elmo";
    student.school = "MAHS";
    student.grade = "4";
    student.requestedSupplies = List.of();

    SupplyList wrongSchool = new SupplyList();
    wrongSchool.school = "AHS";
    wrongSchool.grade = "4";

    SupplyList wrongGrade = new SupplyList();
    wrongGrade.school = "MAHS";
    wrongGrade.grade = "8";

    Checklist result = checklistService.createChecklist(student, List.of(wrongSchool, wrongGrade));
    assertEquals(0, result.checklist.size());
  }

  @Test
  void normalizeSchoolHandlesNull() {
    assertEquals("", ChecklistService.normalizeSchool(null));
  }

  @Test
  void normalizeSchoolStripsTrailingSchool() {
    assertEquals("morris area high", ChecklistService.normalizeSchool("Morris Area High School"));
  }

  @Test
  void normalizeGradeHandlesNull() {
    assertEquals("", ChecklistService.normalizeGrade(null));
  }

  @Test
  void normalizeGradeStripsHyphensAndSpaces() {
    assertEquals("4thgrade", ChecklistService.normalizeGrade("4th-Grade"));
  }

  private SupplyList makeSchoolSupply(String school, String grade, String item) {
    SupplyList supply = new SupplyList();
    supply.school = school;
    supply.grade = grade;
    supply.item = Arrays.asList(item);
    supply.brand = new SupplyList.AttributeOptions();
    supply.brand.allOf = new ArrayList<>();
    supply.brand.anyOf = new ArrayList<>();
    supply.color = new SupplyList.AttributeOptions();
    supply.color.allOf = new ArrayList<>();
    supply.color.anyOf = new ArrayList<>();
    supply.type = new SupplyList.AttributeOptions();
    supply.type.allOf = new ArrayList<>();
    supply.type.anyOf = new ArrayList<>();
    supply.style = new SupplyList.AttributeOptions();
    supply.style.allOf = new ArrayList<>();
    supply.style.anyOf = new ArrayList<>();
    supply.material = new SupplyList.AttributeOptions();
    supply.material.allOf = new ArrayList<>();
    supply.material.anyOf = new ArrayList<>();
    supply.size = "";
    supply.quantity = 1;
    supply.count = 1;
    supply.notes = "";
    return supply;
  }

  @Test
  void expandHighSchoolExpandsToFourGrades() {
    SupplyList hs = makeSchoolSupply("MAHS", "High School", "Notebook");
    List<SupplyList> result = ChecklistService.expandHighSchoolSupplies(List.of(hs));

    assertEquals(4, result.size());
    List<String> grades = result.stream().map(s -> s.grade).toList();
    assertTrue(grades.contains("9"));
    assertTrue(grades.contains("10"));
    assertTrue(grades.contains("11"));
    assertTrue(grades.contains("12"));
  }

  @Test
  void expandHighSchoolSkipsGradeWithExistingEntry() {
    SupplyList existing10 = makeSchoolSupply("MAHS", "10", "Pencils");
    SupplyList hs = makeSchoolSupply("MAHS", "High School", "Notebook");

    List<SupplyList> result = ChecklistService.expandHighSchoolSupplies(List.of(existing10, hs));
    long grade10Count = result.stream().filter(s -> "10".equals(s.grade)).count();
    assertEquals(1, grade10Count);
    assertEquals(4, result.size());
  }

  @Test
  void copyWithGradePreservesFieldsAndChangesGrade() {
    SupplyList source = makeSchoolSupply("MAHS", "High School", "Ruler");
    source.teacher = "Smith";
    source.academicYear = "2025-2026";
    source.quantity = 3;
    source.count = 2;
    source.notes = "Brand new";

    SupplyList copy = ChecklistService.copyWithGrade(source, "11");

    assertEquals("11", copy.grade);
    assertEquals("MAHS", copy.school);
    assertTrue(copy.item.contains("Ruler"));
    assertEquals("Smith", copy.teacher);
    assertEquals("2025-2026", copy.academicYear);
    assertEquals(3, copy.quantity);
    assertEquals(2, copy.count);
    assertEquals("Brand new", copy.notes);
    assertEquals(null, copy._id);
  }

  private Settings.SupplyItemOrder orderEntry(String term, String status) {
    Settings.SupplyItemOrder entry = new Settings.SupplyItemOrder();
    entry.itemTerm = term;
    entry.status = status;
    return entry;
  }

  private SupplyList makeSupply(String... items) {
    SupplyList supply = new SupplyList();
    supply.item = items.length > 0 ? Arrays.asList(items) : null;
    return supply;
  }

  @Test
  void applySupplyOrderNotGivenTermsAreExcluded() {
    SupplyList pencilSupply = makeSupply("pencil");
    SupplyList notebookSupply = makeSupply("notebook");

    List<SupplyList> result = ChecklistService.applySupplyOrder(
        List.of(pencilSupply, notebookSupply),
        List.of(orderEntry("pencil", "notGiven"), orderEntry("notebook", "staged")));

    assertEquals(1, result.size());
    assertTrue(result.get(0).item.contains("notebook"));
  }

  @Test
  void applySupplyOrderUnstagedComesAfterStaged() {
    SupplyList folderSupply = makeSupply("folder");
    SupplyList notebookSupply = makeSupply("notebook");

    List<SupplyList> result = ChecklistService.applySupplyOrder(
        List.of(folderSupply, notebookSupply),
        List.of(orderEntry("notebook", "staged"), orderEntry("folder", "unstaged")));

    assertEquals(2, result.size());
    assertTrue(result.get(0).item.contains("notebook"));
    assertTrue(result.get(1).item.contains("folder"));
  }
}
