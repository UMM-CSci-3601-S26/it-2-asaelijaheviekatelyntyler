package umm3601.settings;

import io.javalin.http.ForbiddenResponse;
import umm3601.auth.Role;
import umm3601.common.AuthContext;

public class SettingsPolicy {

  public void authorizeRead(AuthContext authContext) {
    if (!authContext.role().atLeast(Role.VOLUNTEER)) {
      throw new ForbiddenResponse("Settings access requires volunteer or admin privileges");
    }
  }

  public void authorizeEdit(AuthContext authContext) {
    if (authContext.role() != Role.ADMIN) {
      throw new ForbiddenResponse("Only admins can modify settings");
    }
  }
}
