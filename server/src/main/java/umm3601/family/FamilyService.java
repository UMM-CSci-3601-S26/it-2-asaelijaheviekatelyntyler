package umm3601.family;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;

import io.javalin.http.NotFoundResponse;

public class FamilyService {

  private final FamilyRepository repository;

  public FamilyService(FamilyRepository repository) {
    this.repository = repository;
  }

  public Family getById(String id) {
    Family family = repository.findById(id);
    if (family == null) {
      throw new NotFoundResponse("The requested family was not found");
    }
    return family;
  }

  public Family getByOwnerUserId(String ownerUserId) {
    Family family = repository.findByOwnerUserId(ownerUserId);
    if (family == null) {
      throw new NotFoundResponse("No family profile exists for this guardian yet");
    }
    return family;
  }

  public List<Family> getAll() {
    return repository.findAll();
  }

  public void create(Family family) {
    repository.insert(family);
  }

  public void upsertByOwnerUserId(Family family) {
    repository.upsertByOwnerUserId(family);
  }

  public void delete(String id) {
    long deletedCount = repository.delete(id);
    if (deletedCount == 0) {
      throw new NotFoundResponse(
          "Was unable to delete Family ID"
              + id
              + "; perhaps illegal Family ID or an ID for an item not in the system?");
    }
  }

  public void requestDeleteByVolunteer(String id) {
    long updatedCount = repository.requestDeleteById(id);
    if (updatedCount == 0) {
      throw new NotFoundResponse(
          "Was unable to request delete for Family ID"
              + id
              + "; perhaps illegal Family ID or an ID for an item not in the system?");
    }
  }

  public void requestDeleteByVolunteer(String id, String message, String requestedByUserId) {
    long updatedCount = repository.requestDeleteById(
        id,
        message == null ? "" : message,
        requestedByUserId,
        Instant.now().toString());
    if (updatedCount == 0) {
      throw new NotFoundResponse(
          "Was unable to request delete for Family ID"
              + id
              + "; perhaps illegal Family ID or an ID for an item not in the system?");
    }
  }

  public List<Family> getDeleteRequests() {
    return repository.findDeleteRequests();
  }

  public void clearDeleteRequest(String id) {
    long updatedCount = repository.clearDeleteRequestById(id);
    if (updatedCount == 0) {
      throw new NotFoundResponse(
          "Was unable to clear delete request for Family ID"
              + id
              + "; perhaps illegal Family ID or an ID for an item not in the system?");
    }
  }

  public Map<String, Object> getDashboardStats() {
    List<Family> families = repository.findAll();
    Map<String, Integer> studentsPerSchool = new HashMap<>();
    Map<String, Integer> studentsPerGrade = new HashMap<>();

    for (Family family : families) {
      if (family.students == null) {
        continue;
      }
      for (Family.StudentInfo student : family.students) {
        studentsPerSchool.merge(student.school, 1, Integer::sum);
        studentsPerGrade.merge(student.grade, 1, Integer::sum);
      }
    }

    Map<String, Object> result = new HashMap<>();
    result.put("studentsPerSchool", studentsPerSchool);
    result.put("studentsPerGrade", studentsPerGrade);
    result.put("totalFamilies", families.size());
    return result;
  }

  public String exportFamiliesCsv() {
    List<Family> families = repository.findAll();
    StringBuilder csv = new StringBuilder();
    csv.append("Guardian Name,Email,Address,Time Slot,Number of Students\n");

    for (Family family : families) {
      int studentCount = family.students != null ? family.students.size() : 0;
      csv.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d\n",
          family.guardianName,
          family.email,
          family.address,
          family.timeSlot,
          studentCount));
    }

    return csv.toString();
  }
}
