/**
 * AuthMiddleware — validates JWTs and enforces role-based access control (RBAC)
 * on every protected API route.
 *
 * How it fits into a request
 * ──────────────────────────
 * Each protected route handler calls authMiddleware.handle(ctx) at the top of
 * the method.  This keeps the middleware opt-in per-route rather than being a
 * blanket Javalin before() hook, which makes it easy to see exactly which
 * endpoints are protected.
 *
 * Token extraction order
 * ──────────────────────
 * 1. HttpOnly cookie "auth_token"  — used by the Angular browser client.
 *    The cookie is set by AuthController on login/signup and is never readable
 *    by JavaScript, making it safe against XSS attacks.
 * 2. Authorization: Bearer <token> header — fallback for non-browser clients
 *    such as curl or automated test scripts.
 *
 * After a valid token is found, handle() stores userId and role as Javalin
 * context attributes so downstream handler code can read them with:
 *   String userId = ctx.attribute("userId");
 *   String role   = ctx.attribute("role");
 *
 * requireRole() is a static helper that reads the role attribute and throws
 * 403 Forbidden if it is not among the allowed values.
 */
package umm3601.middleware;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.ForbiddenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

import umm3601.auth.JwtUtils;

public class AuthMiddleware {

    private final String jwtSecret;

    public AuthMiddleware(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public void handle(Context ctx) {
        String path = ctx.path();

        // Public routes - no token required
        if (path.equals("/")
            || path.startsWith("/public")
            || path.equals("/api/auth/login")
            || path.equals("/api/auth/signup")
            || path.equals("/api/auth/logout")) {
            return;
        }

        // Prefer the HttpOnly cookie; fall back to Authorization header for API clients
        String token = ctx.cookie("auth_token");
        if (token == null) {
            String header = ctx.header("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring("Bearer ".length());
            }
        }

        if (token == null) {
            throw new UnauthorizedResponse("Missing token");
        }

        try {
            Claims claims = JwtUtils.parseToken(token, jwtSecret);
            ctx.attribute("userId", claims.getSubject());
            ctx.attribute("role", claims.get("role", String.class));
        } catch (JwtException e) {
            throw new UnauthorizedResponse("Invalid or expired token");
        }
    }

    public static void requireRole(Context ctx, String... allowed) {
        String role = ctx.attribute("role");
        for (String a : allowed) {
            if (a.equals(role)) return;
        }
        throw new ForbiddenResponse("Not allowed");
    }
}
