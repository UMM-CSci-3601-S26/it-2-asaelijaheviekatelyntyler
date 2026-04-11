package umm3601.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

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
import com.mongodb.client.MongoDatabase;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.UnauthorizedResponse;
import umm3601.users.UsersService;

@SuppressWarnings({ "MagicNumber" })
class AuthControllerSpec {

  private AuthController authController;
  private UsersService usersService;

  // Must be at least 32 bytes for HMAC-SHA256
  private static final String TEST_SECRET = "testSecretKeyThatIsLongEnoughForHS256Algorithm!!";

  private static MongoClient mongoClient;
  private static MongoDatabase db;

  @Mock
  private Context ctx;

  @Captor
  private ArgumentCaptor<Cookie> cookieCaptor;

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
  void setupEach() throws IOException {
    MockitoAnnotations.openMocks(this);
    db.getCollection("users").drop();
    usersService = new UsersService(db);
    authController = new AuthController(usersService, TEST_SECRET);
  }

  // ---- Login ----

  @Test
  void loginSucceedsWithValidCredentials() {
    String hash = PasswordUtils.hashPassword("password123");
    usersService.createUser("alice", hash, "Alice Smith", "volunteer");

    AuthController.LoginRequest req = new AuthController.LoginRequest();
    req.username = "alice";
    req.password = "password123";
    when(ctx.bodyAsClass(AuthController.LoginRequest.class)).thenReturn(req);

    authController.login(ctx);

    verify(ctx).cookie(cookieCaptor.capture());
    Cookie cookie = cookieCaptor.getValue();
    assertEquals("auth_token", cookie.getName());
    assertNotNull(cookie.getValue());
    assertEquals(true, cookie.isHttpOnly());
  }

  @Test
  void loginReturnsRoleInResponseBody() {
    String hash = PasswordUtils.hashPassword("password123");
    usersService.createUser("alice", hash, "Alice Smith", "volunteer");

    AuthController.LoginRequest req = new AuthController.LoginRequest();
    req.username = "alice";
    req.password = "password123";
    when(ctx.bodyAsClass(AuthController.LoginRequest.class)).thenReturn(req);

    authController.login(ctx);

    verify(ctx).json(java.util.Map.of("role", "volunteer"));
  }

  @Test
  void loginFailsWithWrongPassword() {
    String hash = PasswordUtils.hashPassword("correctPassword");
    usersService.createUser("bob", hash, "Bob Jones", "volunteer");

    AuthController.LoginRequest req = new AuthController.LoginRequest();
    req.username = "bob";
    req.password = "wrongPassword";
    when(ctx.bodyAsClass(AuthController.LoginRequest.class)).thenReturn(req);

    assertThrows(UnauthorizedResponse.class, () -> authController.login(ctx));
  }

  @Test
  void loginFailsWithUnknownUser() {
    AuthController.LoginRequest req = new AuthController.LoginRequest();
    req.username = "nobody";
    req.password = "password123";
    when(ctx.bodyAsClass(AuthController.LoginRequest.class)).thenReturn(req);

    assertThrows(UnauthorizedResponse.class, () -> authController.login(ctx));
  }

  // ---- Signup ----

  @Test
  void signupSucceedsWithValidInput() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "newuser";
    req.password = "password123";
    req.fullName = "New User";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    authController.signup(ctx);

    verify(ctx).cookie(cookieCaptor.capture());
    Cookie cookie = cookieCaptor.getValue();
    assertEquals("auth_token", cookie.getName());
    assertNotNull(cookie.getValue());
    assertEquals(true, cookie.isHttpOnly());
  }

  @Test
  void signupNewUserGetsVolunteerRole() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "newuser";
    req.password = "password123";
    req.fullName = "New User";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    authController.signup(ctx);

    verify(ctx).json(java.util.Map.of("role", "volunteer"));
  }

  @Test
  void signupFailsWithPasswordShorterThan8Chars() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "newuser";
    req.password = "short";
    req.fullName = "New User";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    assertThrows(BadRequestResponse.class, () -> authController.signup(ctx));
  }

  @Test
  void signupFailsWithMissingUsername() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "";
    req.password = "password123";
    req.fullName = "New User";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    assertThrows(BadRequestResponse.class, () -> authController.signup(ctx));
  }

  @Test
  void signupFailsWithMissingFullName() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "newuser";
    req.password = "password123";
    req.fullName = null;
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    assertThrows(BadRequestResponse.class, () -> authController.signup(ctx));
  }

  @Test
  void signupFailsWithDuplicateUsername() {
    String hash = PasswordUtils.hashPassword("password123");
    usersService.createUser("taken", hash, "Existing User", "volunteer");

    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "taken";
    req.password = "password123";
    req.fullName = "New User";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    assertThrows(BadRequestResponse.class, () -> authController.signup(ctx));
  }

  @Test
  void signupWithGuardianRoleGetsGuardianRole() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "guardianuser";
    req.password = "password123";
    req.fullName = "Guardian User";
    req.role = "guardian";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    authController.signup(ctx);

    verify(ctx).json(java.util.Map.of("role", "guardian"));
  }

  @Test
  void signupWithAdminRoleIsOverriddenToVolunteer() {
    AuthController.SignupRequest req = new AuthController.SignupRequest();
    req.username = "adminattempt";
    req.password = "password123";
    req.fullName = "Admin Attempt User";
    req.role = "admin";
    when(ctx.bodyAsClass(AuthController.SignupRequest.class)).thenReturn(req);

    authController.signup(ctx);

    verify(ctx).json(java.util.Map.of("role", "volunteer"));
  }

  // ---- Logout ----

  @Test
  void logoutClearsCookieWithMaxAgeZero() {
    authController.logout(ctx);

    verify(ctx).cookie(cookieCaptor.capture());
    Cookie cookie = cookieCaptor.getValue();
    assertEquals("auth_token", cookie.getName());
    assertEquals("", cookie.getValue());
    assertEquals(0, cookie.getMaxAge());
  }

  // ---- Me ----

  @Test
  void meReturnsRoleForValidCookie() {
    String token = JwtUtils.createToken("some-id", "admin", TEST_SECRET);
    when(ctx.cookie("auth_token")).thenReturn(token);

    authController.me(ctx);

    verify(ctx).json(java.util.Map.of("role", "admin"));
  }

  @Test
  void meThrowsUnauthorizedWhenNoCookie() {
    when(ctx.cookie("auth_token")).thenReturn(null);

    assertThrows(UnauthorizedResponse.class, () -> authController.me(ctx));
  }

  @Test
  void meThrowsUnauthorizedWithInvalidToken() {
    when(ctx.cookie("auth_token")).thenReturn("invalid.token.value");

    assertThrows(UnauthorizedResponse.class, () -> authController.me(ctx));
  }
}

