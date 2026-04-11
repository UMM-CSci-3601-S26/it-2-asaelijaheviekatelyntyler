/**
 * JwtUtils — thin wrapper around the jjwt library for creating and parsing
 * JSON Web Tokens used by this application.
 *
 * Token structure
 * ───────────────
 * Header  : { alg: "HS256", typ: "JWT" }
 * Payload : { sub: "<MongoDB _id>", role: "<user role>", iat: <epoch>, exp: <epoch+8h> }
 * Signature: HMAC-SHA256 of header.payload, signed with the JWT_SECRET key.
 *
 * The secret is read from the JWT_SECRET environment variable at startup
 * (see Main.java).  A missing secret causes a hard crash so the server
 * never runs with an insecure default in production.
 *
 * createToken() — called after a successful login or signup to issue a fresh
 *                 token embedded in an HttpOnly cookie.
 *
 * parseToken()  — called by AuthMiddleware on every protected request to
 *                 verify the signature and expiry, then extract the claims.
 *                 Throws JwtException (caught by the middleware) on any
 *                 tampered, malformed, or expired token.
 */
package umm3601.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtils {

    public static String createToken(String userId, String role, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
            .subject(userId)
            .claim("role", role)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 8)) // 8 hours
            .signWith(key)
            .compact();
    }

    public static Claims parseToken(String token, String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
