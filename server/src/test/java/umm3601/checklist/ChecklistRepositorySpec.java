package umm3601.checklist;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import umm3601.family.Family;
import umm3601.settings.Settings;
import umm3601.settings.SettingsController;
import umm3601.supplylist.SupplyList;

@SuppressWarnings("MagicNumber")
class ChecklistRepositorySpec {

  private static MongoClient mongoClient;
  private static MongoDatabase db;

  private ChecklistRepository repository;

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
  static void teardownAll() {
    db.drop();
    mongoClient.close();
  }

  @BeforeEach
  void setupEach() {
    db.getCollection("families").drop();
    db.getCollection("supplylist").drop();
    db.getCollection("checklists").drop();
    db.getCollection("settings").drop();

    repository = new ChecklistRepository(db);

    db.getCollection("families").insertOne(new Document()
        .append("guardianName", "Jordan Smith")
        .append("students", List.of(new Document()
            .append("name", "Avery")
            .append("school", "MAHS")
            .append("grade", "4")
            .append("requestedSupplies", List.of("headphones")))));

    db.getCollection("supplylist").insertOne(new Document()
        .append("school", "MAHS")
        .append("grade", "4")
        .append("item", List.of("notebook")));

    db.getCollection("checklists").insertMany(List.of(
        new Document()
            .append("studentName", "Avery")
            .append("school", "MAHS")
            .append("grade", "4")
            .append("requestedSupplies", List.of("headphones"))
            .append("checklist", List.of()),
        new Document()
            .append("studentName", "Taylor")
            .append("school", "AHS")
            .append("grade", "8")
            .append("requestedSupplies", List.of())
            .append("checklist", List.of())));

    db.getCollection("settings").insertOne(new Document()
        .append("_id", SettingsController.SETTINGS_ID)
        .append("supplyOrder", List.of(new Document()
            .append("itemTerm", "notebook")
            .append("status", "staged"))));
  }

  @Test
  void getFamiliesAndSupplyListsReturnStoredDocuments() {
    List<Family> families = repository.getFamilies();
    List<SupplyList> supplyLists = repository.getSupplyLists();

    assertEquals(1, families.size());
    assertEquals("Jordan Smith", families.get(0).guardianName);
    assertEquals(1, supplyLists.size());
    assertEquals(List.of("notebook"), supplyLists.get(0).item);
  }

  @Test
  void getSettingsReturnsSingletonDocumentOrNullWhenMissing() {
    Settings settings = repository.getSettings();
    assertEquals(1, settings.supplyOrder.size());

    db.getCollection("settings").drop();

    assertNull(repository.getSettings());
  }

  @Test
  void getStoredChecklistsForStudentsReturnsMatchesAndHandlesEmptyList() {
    Family.StudentInfo matchingStudent = new Family.StudentInfo();
    matchingStudent.name = "Avery";
    matchingStudent.school = "MAHS";
    matchingStudent.grade = "4";

    List<Checklist> matches = repository.getStoredChecklistsForStudents(List.of(matchingStudent));

    assertEquals(1, matches.size());
    assertEquals("Avery", matches.get(0).studentName);
    assertTrue(repository.getStoredChecklistsForStudents(List.of()).isEmpty());
    assertTrue(repository.getStoredChecklistsForStudents(null).isEmpty());
  }

  @Test
  void replaceAllChecklistsOverwritesExistingDocuments() {
    Checklist replacement = new Checklist();
    replacement.studentName = "Replaced Student";
    replacement.school = "SSHS";
    replacement.grade = "3";
    replacement.requestedSupplies = List.of();
    replacement.checklist = List.of();

    repository.replaceAllChecklists(List.of(replacement));

    MongoCollection<Document> checklistCollection = db.getCollection("checklists");
    assertEquals(1, checklistCollection.countDocuments());
    assertEquals("Replaced Student", checklistCollection.find().first().getString("studentName"));
  }
}
