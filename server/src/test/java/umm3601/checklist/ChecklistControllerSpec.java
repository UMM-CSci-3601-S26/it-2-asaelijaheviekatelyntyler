// Packages
package umm3601.checklist;

import static com.mongodb.client.model.Filters.eq;
// Static Imports
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Java Imports
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

// Org Imports
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mongojack.Id;

// Com Imports
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

// IO Imports
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.BodyValidator;
import io.javalin.validation.ValidationException;

/**
 * Tests for the ChecklistController using a real MongoDB "test" database.
 *
 * These tests make sure the controller behaves the way the rest of the app
 * expects it to. They cover:
 *  - Getting all families or a single checklist by ID
 *  - Handling bad or nonexistent IDs
 *  - Adding new families and checking that validation works
 *  - Deleting families and making sure the database updates correctly
 *  - Dashboard stats and CSV export formatting
 *  - Making sure the controller actually registers its routes
 *
 * Each test starts with a clean set of checklist documents so results are
 * predictable and easy to understand.
 */

// Tests for the Checklist Controller
@SuppressWarnings({ "MagicNumber" })
class ChecklistControllerSpec {

  private ChecklistController checklistController;

  private ObjectId testChecklistId;

  private static MongoClient mongoClient;
  private static MongoDatabase db;

  @SuppressWarnings("unused")
  private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Checklist>> checklistArrayListCaptor;

  @Captor
  private ArgumentCaptor<Checklist> checklistCaptor;

  @SuppressWarnings("unused")
  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @Captor
  private ArgumentCaptor<Map<String, Object>> dashboardCaptor;

  // Runs once before all the tests. This connects to a real MongoDB "test"
  // database so the controller is working with actual data instead of fake mocks.
  // Basically sets up the shared database the tests will use.
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

  // Runs before every test. We clear out the families collection, insert a small
  // set of sample families, and reset all the mocks. This keeps each test
  // independent so nothing gets messed up by a previous test.
  @BeforeEach
  void setupEach() throws IOException {
    MockitoAnnotations.openMocks(this);

    MongoCollection<Document> checklistDocuments = db.getCollection("families");
    checklistDocuments.drop();

    List<Document> testChecklists = new ArrayList<>();

    testChecklists.add(
      new Document()
        .append("school", "MAHS")
        .append("grade", "4")
        .append("studentName", "Elmo")
        .append("requestedSupplies", "headphones")
    );
    testChecklists.add(
      new Document()
        .append("school", "AHS")
        .append("grade", "8")
        .append("studentName", "johnny")
        .append("requestedSupplies", "backpack")
    );
testChecklists.add(
      new Document()
        .append("school", "SSHS")
        .append("grade", "2")
        .append("studentName", "Rocco")
        .append("requestedSupplies", "")
    );

    testChecklistId = new ObjectId();

    Document specialChecklist = new Document()
      .append("_id", testChecklistId)
      .append("school", "Nowhere")
      .append("grade", "12")
      .append("studentName", "bart")
      .append("requestedSupplies", "nothing");

    checklistDocuments.insertMany(testChecklists);
    checklistDocuments.insertOne(specialChecklist);

    checklistController = new ChecklistController(db);
  }

  // Checks that the controller actually registers all its routes with Javalin.
  // If someone removes or renames a route by accident, this test will catch it.  @Test
  @Test
  void addsRoutes() {
    Javalin mockServer = mock(Javalin.class);

    checklistController.addRoutes(mockServer);

    verify(mockServer, Mockito.atLeast(4)).get(any(), any());
    verify(mockServer, Mockito.atLeastOnce()).post(any(), any());
    verify(mockServer, Mockito.atLeastOnce()).patch(any(), any());
  }

  // Makes sure that asking for all families returns everything in the database.
  // Also checks that the controller responds with a 200 OK.  @Test
  @Test
  void canGetAllChecklists() throws IOException {
    when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());
    checklistController.getStoredChecklists(ctx);

    verify(ctx).json(checklistArrayListCaptor.capture());
    verify(ctx).status(HttpStatus.OK);

    assertEquals(
      db.getCollection("checklists").countDocuments(),
      checklistArrayListCaptor.getValue().size());
  }

  // @Test
  // void canCreateChecklist() throws IOException {
  //   when(ctx.queryParamMap()).thenReturn(Collections.emptyMap());
  //   checklistController.createChecklist(ctx);

  //   verify(ctx).json(checklistArrayListCaptor.capture());
  //   verify(ctx).status(HttpStatus.OK);

  //   assertEquals(
  //     db.getCollection("checklists").countDocuments(),
  //     checklistArrayListCaptor.getValue().size());
  // }

  // Looks up a checklist using a real ID and makes sure the controller returns the
  // correct checklist and a 200 OK status.  @Test
  // Checks that the CSV export endpoint produces a properly formatted CSV string,
  // including the header and the correct student counts for each checklist.  @Test
  /*
  @Test
  void exportFamiliesAsCSVProducesCorrectCSV() {
    checklistController.exportFamiliesAsCSV(ctx);
    ArgumentCaptor<String> resultCaptor = ArgumentCaptor.forClass(String.class);

    verify(ctx).result(resultCaptor.capture());
    verify(ctx).contentType("text/csv");
    verify(ctx).status(HttpStatus.OK);

    String csv = resultCaptor.getValue();

    // Check header
    assertTrue(csv.contains(
      "Guardian Name,Email,Address,Time Slot,Number of Students"));

    // Check Jane Doe (2 students)
    assertTrue(csv.contains(
      "\"Jane Doe\",\"jane@email.com\",\"123 Street\",\"10:00-11:00\",2"));

    // Check John Christensen (2 students)
    assertTrue(csv.contains(
      "\"John Christensen\",\"jchristensen@email.com\",\"713 Broadway\",\"8:00-9:00\",2"));

    // Check John Johnson (1 student)
    assertTrue(csv.contains(
      "\"John Johnson\",\"jjohnson@email.com\",\"456 Avenue\",\"2:00-3:00\",1"));

    // Check Bob Jones (1 student)
    assertTrue(csv.contains(
      "\"Bob Jones\",\"bob@email.com\",\"456 Oak Ave\",\"2:00-3:00\",1"));
  } */
}
