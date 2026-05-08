package umm3601.checklist;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.or;

import java.util.ArrayList;
import java.util.List;

import org.bson.UuidRepresentation;
import org.bson.conversions.Bson;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.MongoDatabase;

import umm3601.common.BaseRepository;
import umm3601.family.Family;
import umm3601.family.Family.StudentInfo;
import umm3601.settings.Settings;
import umm3601.settings.SettingsController;
import umm3601.supplylist.SupplyList;

public class ChecklistRepository extends BaseRepository<Checklist> {

  private final JacksonMongoCollection<Family> familyCollection;
  private final JacksonMongoCollection<SupplyList> supplyListCollection;
  private final JacksonMongoCollection<Settings> settingsCollection;

  public ChecklistRepository(MongoDatabase db) {
    this(
        JacksonMongoCollection.builder().build(
            db, "families", Family.class, UuidRepresentation.STANDARD),
        JacksonMongoCollection.builder().build(
            db, "supplylist", SupplyList.class, UuidRepresentation.STANDARD),
        JacksonMongoCollection.builder().build(
            db, "checklists", Checklist.class, UuidRepresentation.STANDARD),
        JacksonMongoCollection.builder().build(
            db, "settings", Settings.class, UuidRepresentation.STANDARD));
  }

  public ChecklistRepository(
      JacksonMongoCollection<Family> familyCollection,
      JacksonMongoCollection<SupplyList> supplyListCollection,
      JacksonMongoCollection<Checklist> checklistCollection,
      JacksonMongoCollection<Settings> settingsCollection) {
    super(checklistCollection);
    this.familyCollection = familyCollection;
    this.supplyListCollection = supplyListCollection;
    this.settingsCollection = settingsCollection;
  }

  public List<Family> getFamilies() {
    return familyCollection.find().into(new ArrayList<>());
  }

  public List<SupplyList> getSupplyLists() {
    return supplyListCollection.find().into(new ArrayList<>());
  }

  public Settings getSettings() {
    if (settingsCollection == null) {
      return null;
    }
    return settingsCollection.find(eq("_id", SettingsController.SETTINGS_ID)).first();
  }

  public List<Checklist> getStoredChecklists(Bson filter) {
    return findAll(filter);
  }

  public List<Checklist> getStoredChecklistsForStudents(List<StudentInfo> students) {
    if (students == null || students.isEmpty()) {
      return List.of();
    }

    List<Bson> studentFilters = students.stream()
        .map(student -> and(
            eq("studentName", student.name),
            eq("school", student.school),
            eq("grade", student.grade)))
        .toList();

    return findAll(or(studentFilters));
  }

  public void replaceAllChecklists(List<Checklist> checklists) {
    replaceAll(checklists);
  }
}
