package umm3601.users;

import io.javalin.http.ForbiddenResponse;
import umm3601.auth.Role;
import umm3601.common.AuthContext;

public class UsersPolicy {
  public void authorizeManage(AuthContext auth) {
    if (auth.role() != Role.ADMIN) {
      throw new ForbiddenResponse("Only admins can manage users");
    }
  }
}
