package umm3601;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;

import umm3601.middleware.AuthMiddleware;
import umm3601.auth.AuthController;
import umm3601.checklist.ChecklistController;
import umm3601.family.FamilyController;
import umm3601.inventory.InventoryController;
import umm3601.settings.SettingsController;
import umm3601.supplylist.SupplyListController;
import umm3601.terms.TermsController;
import umm3601.users.UsersService;

public class Main {

  public static void main(String[] args) {
    // Read MongoDB host and DB name, falling back to defaults.
    String mongoAddr = Main.getEnvOrDefault("MONGO_ADDR", "localhost");
    String databaseName = Main.getEnvOrDefault("MONGO_DB", "dev");

    String jwtSecret = System.getenv("JWT_SECRET");
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET environment variable must be set.");
    }

    // Create MongoDB client and database reference.
    MongoClient mongoClient = Server.configureDatabase(mongoAddr);
    MongoDatabase database = mongoClient.getDatabase(databaseName);

    AuthMiddleware authMiddleware = new AuthMiddleware(jwtSecret);

    // Controllers used by the server.
    final Controller[] controllers = Main.getControllers(database, jwtSecret, authMiddleware);

    // Start the server.
    Server server = new Server(mongoClient, controllers);
    server.startServer();
  }

  /**
   * Returns an environment variable or a default value.
   */
  static String getEnvOrDefault(String envName, String defaultValue) {
    return System.getenv().getOrDefault(envName, defaultValue);
  }

  /**
   * Returns the controllers used by the server.
   */
  static Controller[] getControllers(MongoDatabase database, String jwtSecret, AuthMiddleware authMiddleware) {
    UsersService userService = new UsersService(database);

    Controller[] controllers = new Controller[] {
        // Add controllers here as you create them.
        // e.g., new UserController(database)
        new FamilyController(database, authMiddleware),
        new InventoryController(database, authMiddleware),
        new SupplyListController(database, authMiddleware),
        new ChecklistController(database, authMiddleware),
        new SettingsController(database, authMiddleware),
        new AuthController(userService, jwtSecret),
        new TermsController(database)
    };
    return controllers;
  }
}
