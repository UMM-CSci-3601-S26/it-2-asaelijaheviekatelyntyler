package umm3601.supplylist;

import io.javalin.http.ForbiddenResponse;
import umm3601.auth.Role;
import umm3601.common.AuthContext;

public class SupplyListPolicy {

  public void authorizeRead(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  public void authorizeCreate(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  public void authorizeEdit(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  public void authorizeDelete(AuthContext authContext) {
    if (authContext.role() != Role.ADMIN) {
      throw new ForbiddenResponse("Only admins can delete supply list items");
    }
  }

  private void requireVolunteerOrAdmin(AuthContext authContext) {
    if (!authContext.role().atLeast(Role.VOLUNTEER)) {
      throw new ForbiddenResponse("Supply list access requires volunteer or admin privileges");
    }
  }
}
