package umm3601.settings;

import io.javalin.http.BadRequestResponse;

public class SettingsValidator {

  public Settings validateSchools(Settings settings) {
    if (settings == null || settings.schools == null) {
      throw new BadRequestResponse("Request body must include a 'schools' array.");
    }
    return settings;
  }

  public Settings validateSupplyOrder(Settings settings) {
    if (settings == null || settings.supplyOrder == null) {
      throw new BadRequestResponse("Request body must include a 'supplyOrder' array.");
    }
    return settings;
  }

  public Settings.TimeAvailabilityLabels validateTimeAvailability(Settings.TimeAvailabilityLabels labels) {
    if (labels == null) {
      throw new BadRequestResponse("Request body must include time availability labels.");
    }
    return labels;
  }

  public Settings.DriveDay validateDriveDay(Settings.DriveDay driveDay) {
    if (driveDay == null) {
      throw new BadRequestResponse("Request body must include drive day details.");
    }
    if (driveDay.date == null || driveDay.date.isBlank()) {
      throw new BadRequestResponse("Drive day date is required.");
    }
    return driveDay;
  }
}
