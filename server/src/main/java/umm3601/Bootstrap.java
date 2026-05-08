package umm3601;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import io.javalin.Javalin;

import umm3601.auth.*;
import umm3601.checklist.ChecklistController;
import umm3601.checklist.ChecklistRepository;
import umm3601.checklist.ChecklistService;
import umm3601.common.ApiExceptionHandler;
import umm3601.family.Family;
import umm3601.family.FamilyController;
import umm3601.family.FamilyPortalController;
import umm3601.family.FamilyPolicy;
import umm3601.family.FamilyRepository;
import umm3601.family.FamilyService;
import umm3601.family.FamilyValidator;
import umm3601.inventory.*;
import umm3601.middleware.AuthMiddleware;
import umm3601.settings.Settings;
import umm3601.settings.SettingsController;
import umm3601.settings.SettingsPolicy;
import umm3601.settings.SettingsRepository;
import umm3601.settings.SettingsService;
import umm3601.settings.SettingsValidator;
import umm3601.supplylist.SupplyList;
import umm3601.supplylist.SupplyListController;
import umm3601.supplylist.SupplyListPolicy;
import umm3601.supplylist.SupplyListRepository;
import umm3601.supplylist.SupplyListService;
import umm3601.supplylist.SupplyListValidator;
import umm3601.terms.TermsController;
import umm3601.users.UsersController;
import umm3601.users.UsersPolicy;
import umm3601.users.UsersService;
import umm3601.users.UsersValidator;

public class Bootstrap {
  private static final int DEFAULT_PORT = 7000;

  public static void start() {
    String jwtSecret = System.getenv("JWT_SECRET");
    if (jwtSecret == null || jwtSecret.isBlank()) {
      throw new IllegalStateException("JWT_SECRET must be set");
    }

    MongoDatabase db = connectToDatabase();

    UsersService usersService = new UsersService(db);
    PermissionsService permissionsService = new PermissionsService(db);
    AuthMiddleware authMiddleware = new AuthMiddleware(jwtSecret, usersService);

    Javalin app = createApp(authMiddleware);
    Object[] controllers = buildControllers(db, jwtSecret, usersService, permissionsService);
    registerRoutes(app, permissionsService, controllers);
    app.start(getPort());
  }

  private static String getEnv(String key, String fallback) {
    return System.getenv().getOrDefault(key, fallback);
  }

  private static MongoDatabase connectToDatabase() {
    String mongoAddr = getEnv("MONGO_ADDR", "localhost");
    String dbName = getEnv("MONGO_DB", "dev");
    MongoClient mongoClient = DatabaseConfig.configureDatabase(mongoAddr);
    return mongoClient.getDatabase(dbName);
  }

  private static Javalin createApp(AuthMiddleware authMiddleware) {
    Javalin app = Javalin.create();
    ApiExceptionHandler.register(app);
    app.before(authMiddleware::handle);
    return app;
  }

  private static Object[] buildControllers(
      MongoDatabase db,
      String jwtSecret,
      UsersService usersService,
      PermissionsService permissionsService) {
    JacksonMongoCollection<Inventory> inventoryCollection = buildCollection(
        db,
        "inventory",
        Inventory.class);
    JacksonMongoCollection<Family> familyCollection = buildCollection(
        db,
        "families",
        Family.class);
    JacksonMongoCollection<SupplyList> supplyListCollection = buildCollection(
        db,
        "supplylist",
        SupplyList.class);
    JacksonMongoCollection<Settings> settingsCollection = buildCollection(
        db,
        "settings",
        Settings.class);

    InventoryController inventoryController = new InventoryController(
        new InventoryService(new InventoryRepository(inventoryCollection)),
        new InventoryPolicy(),
        new InventoryValidator());

    FamilyRepository familyRepository = new FamilyRepository(familyCollection);
    FamilyService familyService = new FamilyService(familyRepository);
    FamilyValidator familyValidator = new FamilyValidator();

    FamilyController familyController = new FamilyController(
        familyService,
        usersService,
        new FamilyPolicy(),
        familyValidator);

    SupplyListController supplyListController = new SupplyListController(
        new SupplyListService(new SupplyListRepository(supplyListCollection)),
        new SupplyListPolicy(),
        new SupplyListValidator());

    ChecklistRepository checklistRepository = new ChecklistRepository(db);
    ChecklistService checklistService = new ChecklistService(checklistRepository);
    ChecklistController checklistController = new ChecklistController(checklistRepository);

    SettingsService settingsService = new SettingsService(new SettingsRepository(settingsCollection));
    SettingsController settingsController = new SettingsController(
        settingsService,
        new SettingsPolicy(),
        new SettingsValidator());

    FamilyPortalController familyPortalController = new FamilyPortalController(
        familyService,
        familyValidator,
        checklistService,
        settingsService,
        usersService);

    AuthController authController = new AuthController(usersService, jwtSecret, permissionsService);
    UsersController usersController = new UsersController(
        usersService,
        new UsersPolicy(),
        new UsersValidator(permissionsService));
    TermsController termsController = new TermsController(db);

    return new Object[] {
        inventoryController,
        familyController,
        supplyListController,
        checklistController,
        settingsController,
        familyPortalController,
        authController,
        usersController,
        termsController
    };
  }

  private static <T> JacksonMongoCollection<T> buildCollection(
      MongoDatabase db,
      String collectionName,
      Class<T> documentClass) {
    return JacksonMongoCollection.builder().build(
        db,
        collectionName,
        documentClass,
        UuidRepresentation.STANDARD);
  }

  private static void registerRoutes(Javalin app, PermissionsService permissionsService, Object[] controllers) {
    for (Object controller : controllers) {
      RouteRegistrar.register(app, controller, permissionsService);
    }
  }

  private static int getPort() {
    String port = getEnv("PORT", Integer.toString(DEFAULT_PORT));
    return Integer.parseInt(port);
  }
}
