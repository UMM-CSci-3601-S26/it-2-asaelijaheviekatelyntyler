package umm3601.users;

import org.mongojack.Id;
import org.mongojack.ObjectId;

@SuppressWarnings({ "VisibilityModifier" })
public class Users {
  @ObjectId
  @Id
  @SuppressWarnings({ "MemberName" })
  public String _id;
  public String username;
  public String passwordHash;
  public String fullName;
  public String role;

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Users)) {
      return false;
    }
    Users other = (Users) obj;
    return _id != null && _id.equals(other._id);
  }
}
