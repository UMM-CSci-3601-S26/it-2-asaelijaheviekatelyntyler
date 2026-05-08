package umm3601.common;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import umm3601.auth.Role;
import umm3601.family.FamilyPolicy;
import umm3601.settings.SettingsPolicy;
import umm3601.users.UsersPolicy;

class PolicySpec {

  @Test
  void settingsPolicyAllowsVolunteerReadButRestrictsEditToAdmins() {
    SettingsPolicy policy = new SettingsPolicy();

    assertDoesNotThrow(() -> policy.authorizeRead(auth(Role.VOLUNTEER)));
    assertDoesNotThrow(() -> policy.authorizeEdit(auth(Role.ADMIN)));

    ForbiddenResponse readException =
        assertThrows(ForbiddenResponse.class, () -> policy.authorizeRead(auth(Role.GUARDIAN)));
    assertEquals("Settings access requires volunteer or admin privileges", readException.getMessage());

    ForbiddenResponse editException =
        assertThrows(ForbiddenResponse.class, () -> policy.authorizeEdit(auth(Role.VOLUNTEER)));
    assertEquals("Only admins can modify settings", editException.getMessage());
  }

  @Test
  void usersPolicyAllowsAdminsOnly() {
    UsersPolicy policy = new UsersPolicy();

    assertDoesNotThrow(() -> policy.authorizeManage(auth(Role.ADMIN)));

    ForbiddenResponse exception =
        assertThrows(ForbiddenResponse.class, () -> policy.authorizeManage(auth(Role.VOLUNTEER)));
    assertEquals("Only admins can manage users", exception.getMessage());
  }

  @Test
  void familyPolicyEnforcesExpectedRoleBoundaries() {
    FamilyPolicy policy = new FamilyPolicy();

    assertDoesNotThrow(() -> policy.authorizeRead(auth(Role.VOLUNTEER)));
    assertDoesNotThrow(() -> policy.authorizeAdd(auth(Role.GUARDIAN)));
    assertDoesNotThrow(() -> policy.authorizeDelete(auth(Role.ADMIN)));
    assertDoesNotThrow(() -> policy.authorizeManageDeleteRequests(auth(Role.ADMIN)));

    ForbiddenResponse readException =
        assertThrows(ForbiddenResponse.class, () -> policy.authorizeRead(auth(Role.GUARDIAN)));
    assertEquals("Family access requires volunteer or admin privileges", readException.getMessage());

    ForbiddenResponse deleteException =
        assertThrows(ForbiddenResponse.class, () -> policy.authorizeDelete(auth(Role.VOLUNTEER)));
    assertEquals("Only admins can delete families", deleteException.getMessage());

    ForbiddenResponse manageException =
        assertThrows(ForbiddenResponse.class, () -> policy.authorizeManageDeleteRequests(auth(Role.VOLUNTEER)));
    assertEquals("Only admins can manage delete requests", manageException.getMessage());
  }

  private AuthContext auth(Role role) {
    Context ctx = mock(Context.class);
    when(ctx.attribute("userId")).thenReturn("policy-user");
    when(ctx.attribute("systemRole")).thenReturn(role);
    when(ctx.path()).thenReturn("/policy-test");
    return AuthContext.from(ctx);
  }
}
