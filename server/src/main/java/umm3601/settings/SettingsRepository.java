package umm3601.settings;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.model.UpdateOptions;

public class SettingsRepository {

  private final JacksonMongoCollection<Settings> collection;

  public SettingsRepository(JacksonMongoCollection<Settings> collection) {
    this.collection = collection;
  }

  public Settings getSettings() {
    return collection.find(eq("_id", SettingsController.SETTINGS_ID)).first();
  }

  public void updateSchools(List<Settings.SchoolInfo> schools) {
    List<Document> schoolDocs = schools.stream()
        .map(school -> new Document("name", school.name).append("abbreviation", school.abbreviation))
        .collect(Collectors.toList());

    collection.updateOne(
        eq("_id", SettingsController.SETTINGS_ID),
        new Document("$set", new Document("schools", schoolDocs)),
        new UpdateOptions().upsert(true));
  }

  public void updateSupplyOrder(List<Settings.SupplyItemOrder> supplyOrder) {
    List<Document> orderDocs = supplyOrder.stream()
        .map(entry -> new Document("itemTerm", entry.itemTerm).append("status", entry.status))
        .collect(Collectors.toList());

    collection.updateOne(
        eq("_id", SettingsController.SETTINGS_ID),
        new Document("$set", new Document("supplyOrder", orderDocs)),
        new UpdateOptions().upsert(true));
  }

  public void updateTimeAvailability(Settings.TimeAvailabilityLabels labels) {
    Document taDoc = new Document()
        .append("earlyMorning", labels.earlyMorning)
        .append("lateMorning", labels.lateMorning)
        .append("earlyAfternoon", labels.earlyAfternoon)
        .append("lateAfternoon", labels.lateAfternoon);

    collection.updateOne(
        eq("_id", SettingsController.SETTINGS_ID),
        new Document("$set", new Document("timeAvailability", taDoc)),
        new UpdateOptions().upsert(true));
  }

  public void updateDriveDay(Settings.DriveDay driveDay) {
    Document driveDayDoc = new Document()
        .append("date", driveDay.date)
        .append("message", driveDay.message);

    collection.updateOne(
        eq("_id", SettingsController.SETTINGS_ID),
        new Document("$set", new Document("driveDay", driveDayDoc)),
        new UpdateOptions().upsert(true));
  }

  public Settings buildDefaultSettings() {
    Settings settings = new Settings();
    settings._id = SettingsController.SETTINGS_ID;
    settings.schools = new ArrayList<>();
    settings.timeAvailability = new Settings.TimeAvailabilityLabels();
    settings.supplyOrder = new ArrayList<>();
    settings.driveDay = new Settings.DriveDay();
    return settings;
  }
}
