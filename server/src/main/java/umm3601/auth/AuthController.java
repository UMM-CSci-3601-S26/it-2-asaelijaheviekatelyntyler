/**
 * AuthController — handles all authentication endpoints.
 *
 * Authentication flow overview
 * ─────────────────────────────
 * 1. LOGIN  (POST /api/auth/login)
 *    • Client sends { username, password } in the JSON body.
 *    • Controller looks up the user in MongoDB via UsersService.
 *    • jBCrypt verifies the plain-text password against the stored hash.
 *    • On success: JwtUtils.createToken() builds a signed JWT (HMAC-SHA256)
 *      that embeds the user's MongoDB _id and role, valid for 8 hours.
 *    • The JWT is written into an HttpOnly, SameSite=Strict cookie called
 *      "auth_token". HttpOnly means JavaScript in the browser can never
 *      read it, which eliminates token theft via XSS.
 *    • Only { role } is returned in the JSON body so the client knows which
 *      pages to display — the token itself never touches JavaScript.
 *
 * 2. SIGNUP (POST /api/auth/signup)
 *    • Validates that username, password (≥8 chars), and fullName are present.
 *    • Checks uniqueness of username in MongoDB.
 *    • Hashes the password with bcrypt (cost 12) via PasswordUtils before
 *      storing it — passwords are never persisted in plain text.
 *    • Creates the user with role = "volunteer" (no self-service admin).
 *    • Issues the same HttpOnly cookie as login.
 *
 * 3. LOGOUT (POST /api/auth/logout)
 *    • Overwrites the auth_token cookie with an empty value and maxAge=0,
 *      which tells the browser to delete it immediately.
 *
 * 4. ME     (GET /api/auth/me)
 *    • Reads the auth_token cookie, parses and validates the JWT, and
 *      returns { role }. Used by the Angular app to restore the session
 *      state after a page reload without storing the token in JS memory.
 *
 * Authorization for all other routes is handled separately in
 * AuthMiddleware — see that class for details.
 */
package umm3601.auth;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.http.SameSite;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.http.BadRequestResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import umm3601.users.Users;
import umm3601.users.UsersService;
import umm3601.Controller;

import java.util.Map;

public class AuthController implements Controller {

    private final UsersService userService;
    private final String jwtSecret;

    public AuthController(UsersService userService, String jwtSecret) {
        this.userService = userService;
        this.jwtSecret = jwtSecret;
    }

    @Override
    public void addRoutes(Javalin server) {
        server.post("/api/auth/login", this::login);
        server.post("/api/auth/signup", this::signup);
        server.post("/api/auth/logout", this::logout);
        server.get("/api/auth/me", this::me);
    }

    public static class LoginRequest {
        public String username;
        public String password;
    }

    public static class SignupRequest {
        public String username;
        public String password;
        public String fullName;
        // Only "volunteer" and "guardian" are accepted for self-registration.
        // "admin" can only be assigned by an admin after the fact.
        public String role;
    }

    public void login(Context ctx) {
        LoginRequest req = ctx.bodyAsClass(LoginRequest.class);

        Users user = userService.findByUsername(req.username);
        if (user == null) {
            throw new UnauthorizedResponse("Invalid username or password");
        }

        if (!PasswordUtils.checkPassword(req.password, user.passwordHash)) {
            throw new UnauthorizedResponse("Invalid username or password");
        }

        String token = JwtUtils.createToken(user._id, user.role, jwtSecret);
        ctx.cookie(buildAuthCookie(token));
        ctx.json(Map.of("role", user.role));
    }

    public void signup(Context ctx) {
        SignupRequest req = ctx.bodyAsClass(SignupRequest.class);

        // Validate input
        if (req.username == null || req.username.trim().isEmpty()) {
            throw new BadRequestResponse("Username is required");
        }
        if (req.password == null || req.password.length() < 8) {
            throw new BadRequestResponse("Password must be at least 8 characters");
        }
        if (req.fullName == null || req.fullName.trim().isEmpty()) {
            throw new BadRequestResponse("Full name is required");
        }

        // Only volunteer and guardian are self-registerable; admin must be promoted by an admin.
        String role = (req.role != null && req.role.equals("guardian")) ? "guardian" : "volunteer";

        // Check if username already exists
        if (userService.findByUsername(req.username) != null) {
            throw new BadRequestResponse("Username already exists");
        }

        // Hash password and create user
        String hashedPassword = PasswordUtils.hashPassword(req.password);
        userService.createUser(req.username, hashedPassword, req.fullName, role);

        // Get the newly created user and create a token
        Users user = userService.findByUsername(req.username);
        String token = JwtUtils.createToken(user._id, user.role, jwtSecret);
        ctx.cookie(buildAuthCookie(token));
        ctx.json(Map.of("role", user.role));
    }

    public void logout(Context ctx) {
        // Overwrite the cookie with an empty value and maxAge=0.
        // SameSite=Strict and HttpOnly are kept so that the Set-Cookie
        // header matches the original cookie attributes — some browsers
        // require this for deletion to succeed.
        ctx.cookie(new Cookie("auth_token", "", "/", 0, false, 0, true, null, null, SameSite.STRICT));
        ctx.status(200);
    }

    public void me(Context ctx) {
        String token = ctx.cookie("auth_token");
        if (token == null) {
            throw new UnauthorizedResponse("Not authenticated");
        }
        try {
            Claims claims = JwtUtils.parseToken(token, jwtSecret);
            ctx.json(Map.of("role", claims.get("role", String.class)));
        } catch (JwtException e) {
            throw new UnauthorizedResponse("Invalid or expired token");
        }
    }

    private Cookie buildAuthCookie(String token) {
        // Cookie attributes:
        //   path=/          — sent with every request to this server
        //   maxAge=28800    — browser deletes the cookie after 8 hours (matches JWT expiry)
        //   secure=false    — the reverse proxy (Caddy) handles TLS termination;
        //                     the Javalin process only sees plain HTTP internally.
        //                     Set secure=true if Javalin is exposed directly over HTTPS.
        //   httpOnly=true   — cookie is invisible to JavaScript, blocking XSS theft
        //   SameSite=STRICT — cookie is NOT sent on cross-site requests,
        //                     which prevents CSRF attacks
        return new Cookie("auth_token", token, "/", 8 * 60 * 60, false, 0, true, null, null, SameSite.STRICT);
    }
}
