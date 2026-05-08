// Package declaration
package umm3601.checklist;

// IO Imports
import io.javalin.http.ForbiddenResponse;

// Application Imports
import umm3601.auth.Role;
import umm3601.common.AuthContext;

/**
 * ChecklistPolicy is a class responsible for defining authorization policies
 * for checklist-related operations. It contains methods that enforce access
 * control based on the user's role, ensuring that only users with appropriate
 * permissions (volunteers or admins) can perform certain actions related to
 * checklists, such as reading stored checklists, generating new checklists, and
 * exporting checklists as PDFs. Each method checks the user's role from the
 * AuthContext and throws a ForbiddenResponse if the user does not have
 * sufficient privileges to access the requested resource or perform the desired
 * action.
 *
 */

public class ChecklistPolicy {

  public void authorizeStoredChecklistRead(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  public void authorizeChecklistGeneration(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  public void authorizeChecklistExport(AuthContext authContext) {
    requireVolunteerOrAdmin(authContext);
  }

  private void requireVolunteerOrAdmin(AuthContext authContext) {
    if (!authContext.role().atLeast(Role.VOLUNTEER)) {
      throw new ForbiddenResponse("Checklist access requires volunteer or admin privileges");
    }
  }
}
