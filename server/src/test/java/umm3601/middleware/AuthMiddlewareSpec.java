package umm3601.middleware;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import umm3601.auth.JwtUtils;

@SuppressWarnings({ "MagicNumber" })
public class AuthMiddlewareSpec {

  private static final String TEST_SECRET = "testSecretKeyThatIsLongEnoughForHS256Algorithm!!";

  private AuthMiddleware middleware;

  @Mock
  private Context ctx;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    middleware = new AuthMiddleware(TEST_SECRET);
  }

  // ---- Public / whitelisted paths ----

  @Test
  void handleAllowsRootPath() {
    when(ctx.path()).thenReturn("/");
    assertDoesNotThrow(() -> middleware.handle(ctx));
  }

  @Test
  void handleAllowsPublicPath() {
    when(ctx.path()).thenReturn("/public/logo.png");
    assertDoesNotThrow(() -> middleware.handle(ctx));
  }

  @Test
  void handleAllowsAuthLoginPath() {
    when(ctx.path()).thenReturn("/api/auth/login");
    assertDoesNotThrow(() -> middleware.handle(ctx));
  }

  @Test
  void handleAllowsAuthSignupPath() {
    when(ctx.path()).thenReturn("/api/auth/signup");
    assertDoesNotThrow(() -> middleware.handle(ctx));
  }

  @Test
  void handleAllowsAuthLogoutPath() {
    when(ctx.path()).thenReturn("/api/auth/logout");
    assertDoesNotThrow(() -> middleware.handle(ctx));
  }

  // ---- Protected paths with valid tokens ----

  @Test
  void handleSetsAttributesFromValidCookieToken() {
    String token = JwtUtils.createToken("user123", "volunteer", TEST_SECRET);
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn(token);

    assertDoesNotThrow(() -> middleware.handle(ctx));

    verify(ctx).attribute("userId", "user123");
    verify(ctx).attribute("role", "volunteer");
  }

  @Test
  void handleSetsAttributesFromBearerHeader() {
    String token = JwtUtils.createToken("user456", "admin", TEST_SECRET);
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn(null);
    when(ctx.header("Authorization")).thenReturn("Bearer " + token);

    assertDoesNotThrow(() -> middleware.handle(ctx));

    verify(ctx).attribute("userId", "user456");
    verify(ctx).attribute("role", "admin");
  }

  @Test
  void handlePrefersCookieOverBearerHeader() {
    String cookieToken = JwtUtils.createToken("cookieUser", "volunteer", TEST_SECRET);
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn(cookieToken);

    assertDoesNotThrow(() -> middleware.handle(ctx));

    verify(ctx).attribute("userId", "cookieUser");
    verify(ctx).attribute("role", "volunteer");
  }

  // ---- Missing or invalid tokens ----

  @Test
  void handleThrowsUnauthorizedWhenNoToken() {
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn(null);
    when(ctx.header("Authorization")).thenReturn(null);

    UnauthorizedResponse ex = assertThrows(UnauthorizedResponse.class, () -> middleware.handle(ctx));
    assert ex.getMessage().contains("Missing token");
  }

  @Test
  void handleThrowsUnauthorizedWhenAuthHeaderHasNoBearer() {
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn(null);
    when(ctx.header("Authorization")).thenReturn("Token somevalue");

    assertThrows(UnauthorizedResponse.class, () -> middleware.handle(ctx));
  }

  @Test
  void handleThrowsUnauthorizedWithInvalidJwt() {
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn("this.is.notvalid");

    UnauthorizedResponse ex = assertThrows(UnauthorizedResponse.class, () -> middleware.handle(ctx));
    assert ex.getMessage().contains("Invalid or expired token");
  }

  @Test
  void handleThrowsUnauthorizedWithTokenSignedByWrongSecret() {
    String wrongSecretToken = JwtUtils.createToken("user789", "admin", "aCompletelyDifferentSecretKeyForTesting!!");
    when(ctx.path()).thenReturn("/api/families");
    when(ctx.cookie("auth_token")).thenReturn(wrongSecretToken);

    assertThrows(UnauthorizedResponse.class, () -> middleware.handle(ctx));
  }

  // ---- requireRole() ----

  @Test
  void requireRolePassesWhenRoleMatches() {
    when(ctx.attribute("role")).thenReturn("admin");
    assertDoesNotThrow(() -> AuthMiddleware.requireRole(ctx, "admin"));
  }

  @Test
  void requireRolePassesWhenRoleIsInList() {
    when(ctx.attribute("role")).thenReturn("volunteer");
    assertDoesNotThrow(() -> AuthMiddleware.requireRole(ctx, "admin", "volunteer"));
  }

  @Test
  void requireRoleThrowsForbiddenWhenRoleNotInList() {
    when(ctx.attribute("role")).thenReturn("volunteer");
    assertThrows(ForbiddenResponse.class, () -> AuthMiddleware.requireRole(ctx, "admin"));
  }

  @Test
  void requireRoleThrowsForbiddenWhenRoleIsNull() {
    when(ctx.attribute("role")).thenReturn(null);
    assertThrows(ForbiddenResponse.class, () -> AuthMiddleware.requireRole(ctx, "admin", "volunteer"));
  }
}
