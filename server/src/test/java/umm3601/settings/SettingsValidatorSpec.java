package umm3601.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.http.BadRequestResponse;

class SettingsValidatorSpec {

  private SettingsValidator validator;

  @BeforeEach
  void setupEach() {
    validator = new SettingsValidator();
  }

  @Test
  void validateSchoolsRejectsMissingSchoolsArray() {
    BadRequestResponse exception = assertThrows(BadRequestResponse.class,
      () -> validator.validateSchools(new Settings()));
    assertEquals("Request body must include a 'schools' array.", exception.getMessage());
  }

  @Test
  void validateSchoolsRejectsNullSettings() {
    BadRequestResponse exception = assertThrows(BadRequestResponse.class,
      () -> validator.validateSchools(null));
    assertEquals("Request body must include a 'schools' array.", exception.getMessage());
  }

  @Test
  void validateSupplyOrderRejectsMissingArray() {
    BadRequestResponse exception = assertThrows(BadRequestResponse.class,
      () -> validator.validateSupplyOrder(new Settings()));
    assertEquals("Request body must include a 'supplyOrder' array.", exception.getMessage());
  }

  @Test
  void validateSupplyOrderRejectsNullSettings() {
    BadRequestResponse exception = assertThrows(BadRequestResponse.class,
      () -> validator.validateSupplyOrder(null));
    assertEquals("Request body must include a 'supplyOrder' array.", exception.getMessage());
  }

  @Test
  void validateTimeAvailabilityRejectsNullBody() {
    BadRequestResponse exception =
        assertThrows(BadRequestResponse.class,
          () -> validator.validateTimeAvailability(null));
    assertEquals("Request body must include time availability labels.", exception.getMessage());
  }

  @Test
  void validateDriveDayRejectsNullOrBlankDate() {
    BadRequestResponse nullBody =
        assertThrows(BadRequestResponse.class,
          () -> validator.validateDriveDay(null));
    assertEquals("Request body must include drive day details.", nullBody.getMessage());

    Settings.DriveDay nullDate = new Settings.DriveDay();
    nullDate.date = null;

    BadRequestResponse nullDateException =
        assertThrows(BadRequestResponse.class,
          () -> validator.validateDriveDay(nullDate));
    assertEquals("Drive day date is required.", nullDateException.getMessage());

    Settings.DriveDay blankDate = new Settings.DriveDay();
    blankDate.date = "   ";

    BadRequestResponse blankDateException =
        assertThrows(BadRequestResponse.class,
          () -> validator.validateDriveDay(blankDate));
    assertEquals("Drive day date is required.", blankDateException.getMessage());
  }

  @Test
  void validatorsReturnValidBodies() {
    Settings settings = new Settings();
    settings.schools = java.util.List.of(new Settings.SchoolInfo());
    settings.supplyOrder = java.util.List.of(new Settings.SupplyItemOrder());

    Settings.TimeAvailabilityLabels labels = new Settings.TimeAvailabilityLabels();
    Settings.DriveDay driveDay = new Settings.DriveDay();
    driveDay.date = "2026-08-16";

    assertSame(settings, validator.validateSchools(settings));
    assertSame(settings, validator.validateSupplyOrder(settings));
    assertSame(labels, validator.validateTimeAvailability(labels));
    assertSame(driveDay, validator.validateDriveDay(driveDay));
  }
}
