# Data Setup and Route Protection Guide

## Purpose

This document explains the current backend startup architecture, data wiring, and route protection flow in the server.

It is intended to make the refactor easy to explain to teammates and instructors, especially:

- the change to Main.java
- the simplification of Server.java
- the addition of Bootstrap.java

## Executive Summary

The server moved from a controller-interface startup model to a bootstrap + annotation model.

Before this change:

- Main.java built Mongo, JWT, middleware, and controllers directly.
- Server.java owned Javalin startup and controller route registration.
- Controllers often registered routes manually and enforced auth inside route lambdas.

After this change:

- Main.java is only the entrypoint delegate.
- Server.java only normalizes Mongo connection strings and creates MongoClient.
- Bootstrap.java owns app composition (env, db, middleware, controller construction, route registration, and app.start).
- Routes are discovered by annotations and protected centrally through SecuredHandler + AuthMiddleware.

---

## 1) Focused Changes in Main.java, Server.java, and Bootstrap.java

### Main.java (now minimal)

File: server/src/main/java/umm3601/Main.java

Current responsibility:

- Launch the application by calling Bootstrap.start().

What changed:

- Removed environment parsing, controller array construction, and direct server startup logic.
- Main is now a clean handoff point that is easy to test and reason about.

Why this helps:

- Keeps entrypoint simple.
- Reduces duplicate startup logic in one place.
- Makes bootstrap behavior easier to evolve without touching main.

### Server.java (now database connector utility)

File: server/src/main/java/umm3601/Server.java

Current responsibility:

- Build MongoClient from host string.
- Auto-normalize host to include mongodb:// unless already mongodb:// or mongodb+srv://.

What changed:

- Removed Javalin start/stop lifecycle ownership.
- Removed controller-array route setup model.
- Removed direct server port ownership.

Why this helps:

- Server.java now has one job (database connection helper).
- Avoids mixing web server concerns with db connection concerns.

### Bootstrap.java (new composition root)

File: server/src/main/java/umm3601/Bootstrap.java

Current responsibility:

- Read environment.
- Validate JWT secret.
- Connect Mongo and choose database.
- Build services/repositories/controllers.
- Install global middleware and exception handling.
- Register all annotated routes.
- Start Javalin on port 7000.

Why this helps:

- Centralized composition root pattern.
- Clear startup sequence for data + security + routes.
- Easier to document and debug.

---

## 2) Startup and Data Initialization Flow

### Environment variables

Bootstrap reads:

- MONGO_ADDR (default: localhost)
- MONGO_DB (default: dev)
- JWT_SECRET (required; startup fails if missing/blank)

Important behavior:

- Missing JWT_SECRET throws IllegalStateException immediately.
- This intentionally prevents insecure startup.

### Mongo connection setup

- Bootstrap calls Server.configureDatabase(mongoAddr).
- Server.configureDatabase normalizes the host string and creates MongoClient.
- Bootstrap gets MongoDatabase via mongoClient.getDatabase(dbName).

### Collection wiring in Bootstrap

Bootstrap explicitly creates typed collections and repositories/services around them:

- inventory
- families
- supplylist
- settings

Then it constructs domain services and controllers with those dependencies.

This is explicit dependency injection by constructor wiring (without a DI container).

### Core auth-related services initialized

- UsersService(db)
- PermissionsService(db)
- AuthMiddleware(jwtSecret)
- AuthController(usersService, jwtSecret, permissionsService)

### Seeded data expectations

From database seed files, the new model expects:

- users to use systemRole + jobRole (not the old role-only shape)
- permissions collection to include role-permission config document (or be auto-bootstrapped by PermissionsService)

Relevant seed files:

- database/seed/users.json
- database/seed/permissions.json
- database/seed/families.json
- database/seed/settings.json

---

## 3) Route Registration Architecture

The server now uses annotations for route mapping and protection.

### Route definition annotations

Controllers decorate methods with:

- @Route(path = ..., method = HttpMethod...)
- optional @RequireRole(...)
- optional @RequirePermission(...)

### Dynamic route registration

- Bootstrap calls RouteRegistrar.register(app, controller, permissionsService) for each controller.
- RouteRegistrar reflects over controller methods and registers every @Route method.
- Each route is bound through a SecuredHandler, not directly to method invocation.

### SecuredHandler enforcement

When request reaches a route:

1. If method has @RequireRole, SecuredHandler calls AuthMiddleware.requireRole.
2. If method has @RequirePermission, SecuredHandler calls AuthMiddleware.requirePermission.
3. If checks pass, controller method is invoked.

This gives a consistent, reusable authorization layer for all annotated routes.

---

## 4) Request Authentication and Authorization Flow

### Layer A: Global auth parsing (app.before)

In Bootstrap:

- app.before(...) runs for incoming requests.
- login/signup are bypassed.
- all other requests pass through authMiddleware.handle(ctx).

AuthMiddleware.handle:

- Reads token from auth_token cookie first.
- Falls back to Authorization: Bearer < token >.
- Validates JWT via JwtUtils.parseToken.
- Extracts and stores:
  - userId
  - systemRole (as Role enum)
  - jobRole (with volunteer_base default for volunteers)

If missing/invalid token:

- throws UnauthorizedResponse.

### Layer B: Annotation checks per route

SecuredHandler enforces @RequireRole and @RequirePermission.

Role check:

- requireRole(ctx, Role required) uses role hierarchy:
  - GUARDIAN < VOLUNTEER < ADMIN

Permission check:

- ADMIN bypasses permission restrictions.
- GUARDIAN is restricted to family_portal_access surface.
- VOLUNTEER uses effective permissions from jobRole inheritance.

### Layer C: Optional domain policy checks

Many controllers also call domain policies using AuthContext.from(ctx), for example:

- InventoryPolicy
- FamilyPolicy
- SupplyListPolicy
- ChecklistPolicy
- SettingsPolicy

This is a defense-in-depth model:

- middleware + annotation gate + domain policy gate.

---

## 5) Auth Data Contract (Important for Frontend + API)

### JWT claims now

Token carries:

- subject: user id
- systemRole
- jobRole (nullable for non-volunteers)
- issuedAt
- expiration (8 hours)

### Response profile shape now

Auth endpoints return access profile style payloads such as:

- systemRole
- permissions
- jobRole (when applicable)

This aligns route checks with permission-based frontend behavior.

---

## 6) Why This Design Is Better for Teaching and Team Development

### Separation of concerns

- Main.java: entrypoint only.
- Server.java: db client helper only.
- Bootstrap.java: composition + startup orchestration.
- AuthMiddleware/SecuredHandler: security enforcement.
- Controllers/services/policies: domain behavior.

### Better maintainability

- Add a new endpoint by adding one annotated method.
- Add a new controller by constructing it in Bootstrap and calling RouteRegistrar.register once.
- No large route-switch block required.

### Better consistency

- All protected routes share the same auth parsing and annotation enforcement path.
- Permission model (system role + job role) is unified across backend and client behavior.

---

## 7) Practical Walkthrough of a Protected Request

Example: GET /api/inventory

1. Request enters app.before in Bootstrap.
2. AuthMiddleware validates JWT and sets context attributes.
3. RouteRegistrar-mapped route resolves to SecuredHandler for InventoryController.getInventories.
4. SecuredHandler sees @RequirePermission("view_inventory") and calls requirePermission.
5. Permission passes, controller method runs.
6. Controller can still run policy checks and validation before calling service/repository.

If permission fails:

- ForbiddenResponse is thrown.
- ApiExceptionHandler returns structured error response.

---

## 8) Things to Mention in Demo / Report

Use these talking points with your team/teacher:

1. We introduced Bootstrap.java as the composition root to centralize startup and reduce coupling.
2. We simplified Main.java to one line so startup orchestration can evolve safely.
3. We reduced Server.java to a focused Mongo connection helper.
4. We moved route mapping to annotations and dynamic registration (RouteRegistrar).
5. We enforce route protection in a layered way (before-middleware + role/permission annotations + policy layer).
6. We upgraded auth identity from role-only to systemRole + jobRole + permissions.
7. We aligned backend auth responses with frontend route/permission guards.

---

## 9) Quick File Map (Most Relevant)

Startup and composition:

- server/src/main/java/umm3601/Main.java
- server/src/main/java/umm3601/Server.java
- server/src/main/java/umm3601/Bootstrap.java

Auth and route security:

- server/src/main/java/umm3601/middleware/AuthMiddleware.java
- server/src/main/java/umm3601/auth/Route.java
- server/src/main/java/umm3601/auth/RequireRole.java
- server/src/main/java/umm3601/auth/RequirePermission.java
- server/src/main/java/umm3601/auth/RouteRegistrar.java
- server/src/main/java/umm3601/auth/SecuredHandler.java
- server/src/main/java/umm3601/auth/AuthController.java
- server/src/main/java/umm3601/auth/PermissionsService.java
- server/src/main/java/umm3601/common/AuthContext.java

Data and seed shape:

- database/seed/users.json
- database/seed/permissions.json
- database/seed/families.json
- database/seed/settings.json

---

## 10) One-Sentence Architecture Statement

The current backend uses a Bootstrap composition root with annotation-based route registration and layered JWT + role/permission enforcement, which cleanly separates startup wiring, domain logic, and security.
