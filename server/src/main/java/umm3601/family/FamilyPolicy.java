// Package declaration
package umm3601.family;

import io.javalin.http.ForbiddenResponse;
import umm3601.auth.Role;
import umm3601.common.AuthContext;

/**
 * Policy class for handling authorization related to Family operations.
 *
 * So not only are the routes protecting access to family data, but the policy
 * class is also enforcing role-based access control for each operation. This
 * ensures that only users with the appropriate permissions can perform actions
 * like reading family data, adding new families, deleting families, accessing
 * the dashboard, or exporting family data. Protecting the routes as well as
 * enforcing policies within the controller methods provides a robust security
 * model that helps prevent unauthorized access and ensures that sensitive family
 * data is only accessible to users with the necessary privileges.
 */

public class FamilyPolicy {

  // Authorize access to read a single family by ID. Requires at least volunteer
  // role.
  public void authorizeRead(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  // Authorize access to list all families. Requires at least volunteer role.
  public void authorizeList(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  // Authorize access to add a new family. Requires at least guardian role.
  public void authorizeAdd(AuthContext authContext) {
    if (!authContext.role().atLeast(Role.GUARDIAN)) {
      throw new ForbiddenResponse("Authenticated users only");
    }
  }

  // Authorize access to delete a family. Requires admin role.
  public void authorizeDelete(AuthContext authContext) {
    if (authContext.role() != Role.ADMIN) {
      throw new ForbiddenResponse("Only admins can delete families");
    }
  }

  // Authorize volunteers or admins to submit a delete request with rationale.
  public void authorizeRequestDelete(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  // Authorize admin-only operations on queued delete requests.
  public void authorizeManageDeleteRequests(AuthContext authContext) {
    if (authContext.role() != Role.ADMIN) {
      throw new ForbiddenResponse("Only admins can manage delete requests");
    }
  }

  // Authorize access to the dashboard. Requires at least volunteer role.
  public void authorizeDashboard(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  // Authorize access to export families as CSV. Requires at least volunteer role.
  public void authorizeExport(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  // Helper method to check if the user has at least volunteer role. If not,
  // throws a ForbiddenResponse.
  private void requireVolunteerOrAdmin(AuthContext authContext) {
    if (!authContext.role().atLeast(Role.VOLUNTEER)) {
      throw new ForbiddenResponse("Family access requires volunteer or admin privileges");
    }
  }
}
