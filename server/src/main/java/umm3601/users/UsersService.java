package umm3601.users;

import com.mongodb.client.MongoDatabase;
import org.bson.UuidRepresentation;
import org.mongojack.JacksonMongoCollection;

import static com.mongodb.client.model.Filters.eq;

/**
 * Will have to be expanded in the future to support more user-related operations,
 * but for now it just supports creating users and finding them by username.
 */

public class UsersService {
  private final JacksonMongoCollection<Users> users;

  public UsersService(MongoDatabase db) {
    users = JacksonMongoCollection.builder().build(
        db,
        "users",
        Users.class,
        UuidRepresentation.STANDARD);
  }

  public Users findByUsername(String username) {
    return users.find(eq("username", username)).first();
  }

  public void createUser(String username, String passwordHash, String fullName, String role) {
    Users user = new Users();
    user.username = username;
    user.passwordHash = passwordHash;
    user.fullName = fullName;
    user.role = role;
    users.insertOne(user);
  }
}
