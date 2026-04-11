package umm3601.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
// Java Imports
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

// Com Imports
import com.mongodb.MongoClientSettings;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import io.javalin.http.Context;
import io.javalin.json.JavalinJackson;

public class UsersServiceSpec {
  private UsersService usersService;

  private static MongoClient mongoClient;
  private static MongoDatabase db;

  @SuppressWarnings("unused")
  private static JavalinJackson javalinJackson = new JavalinJackson();

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<ArrayList<Users>> usersArrayListCaptor;

  @Captor
  private ArgumentCaptor<Users> usersCaptor;

  @Captor
  private ArgumentCaptor<Map<String, String>> mapCaptor;

  @BeforeAll
  static void setup() {
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
  void setupEach() throws IOException {
    MockitoAnnotations.openMocks(this);

    db.getCollection("users").drop();

    usersService = new UsersService(db);
    usersService.createUser("testuser1", "hash1", "Test User 1", "user");
    usersService.createUser("testuser2", "hash2", "Test User 2", "admin");
  }

  @Test
  void findByUsernameReturnsUser() {
    Users user = usersService.findByUsername("testuser1");
    assertEquals("testuser1", user.username);
    assertEquals("hash1", user.passwordHash);
    assertEquals("Test User 1", user.fullName);
    assertEquals("user", user.role);
  }

  @Test
  void findByUsernameReturnsNullIfNotFound() {
    Users user = usersService.findByUsername("nonexistent");
    assertEquals(null, user);
  }

  @Test
  void createUserInsertsUser() {
    usersService.createUser("newuser", "newhash", "New User", "user");

    Users user = usersService.findByUsername("newuser");
    assertEquals("newuser", user.username);
    assertEquals("newhash", user.passwordHash);
    assertEquals("New User", user.fullName);
    assertEquals("user", user.role);
  }
}
