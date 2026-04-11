package umm3601.auth;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilsSpec {
  private String secret;

  @BeforeEach
  void setupEach() {
    secret = "mySuperSecretKeyThatShouldBeLongEnough";
  }

  @Test
  void createTokenReturnsNonNullToken() {
    String token = JwtUtils.createToken("user123", "admin", secret);
    assertNotNull(token);
    assertFalse(token.isBlank());
  }

  @Test
  void parsedTokenHasCorrectSubject() {
    String token = JwtUtils.createToken("user123", "admin", secret);
    Claims claims = JwtUtils.parseToken(token, secret);
    assertEquals("user123", claims.getSubject());
  }

  @Test
  void parsedTokenHasCorrectRole() {
    String token = JwtUtils.createToken("user123", "admin", secret);
    Claims claims = JwtUtils.parseToken(token, secret);
    assertEquals("admin", claims.get("role", String.class));
  }

  @Test
  void tokenWithWrongSecretFailsToParse() {
    String token = JwtUtils.createToken("user123", "admin", secret);
    assertThrows(Exception.class, () -> JwtUtils.parseToken(token, "wrong-secret-key-that-is-long-enough!"));
  }
}
