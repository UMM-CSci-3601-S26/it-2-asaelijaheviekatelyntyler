package umm3601.settings;

public class SettingsService {

  private final SettingsRepository repository;

  public SettingsService(SettingsRepository repository) {
    this.repository = repository;
  }

  public Settings getSettings() {
    Settings settings = repository.getSettings();
    if (settings == null) {
      return repository.buildDefaultSettings();
    }
    if (settings.supplyOrder == null) {
      settings.supplyOrder = new java.util.ArrayList<>();
    }
    if (settings.driveDay == null) {
      settings.driveDay = new Settings.DriveDay();
    }
    return settings;
  }

  public void updateSchools(Settings settings) {
    repository.updateSchools(settings.schools);
  }

  public void updateSupplyOrder(Settings settings) {
    repository.updateSupplyOrder(settings.supplyOrder);
  }

  public void updateTimeAvailability(Settings.TimeAvailabilityLabels labels) {
    repository.updateTimeAvailability(labels);
  }

  public void updateDriveDay(Settings.DriveDay driveDay) {
    repository.updateDriveDay(driveDay);
  }
}
