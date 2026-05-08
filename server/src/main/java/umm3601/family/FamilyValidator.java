package umm3601.family;

import java.util.List;

import io.javalin.http.BadRequestResponse;

public class FamilyValidator {

  public static final String EMAIL_REGEX = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$";

  public Family validateBody(Family family) {
    if (family == null) {
      throw new BadRequestResponse("Family request body is required");
    }
    if (family.email == null || !family.email.matches(EMAIL_REGEX)) {
      throw new BadRequestResponse("Family must have a valid email");
    }
    return family;
  }

  public Family validatePortalFormBody(Family family) {
    validateBody(family);
    if (family.guardianName == null || family.guardianName.isBlank()) {
      throw new BadRequestResponse("Guardian name is required");
    }
    if (family.address == null || family.address.isBlank()) {
      throw new BadRequestResponse("Address is required");
    }
    List<Family.StudentInfo> students = family.students;
    if (students == null || students.isEmpty()) {
      throw new BadRequestResponse("At least one student is required");
    }
    for (Family.StudentInfo student : students) {
      if (student == null
          || student.name == null || student.name.isBlank()
          || student.grade == null || student.grade.isBlank()
          || student.school == null || student.school.isBlank()) {
        throw new BadRequestResponse("Each student must include name, grade, and school");
      }
    }
    return family;
  }

  public String validateId(String id) {
    if (id == null || id.isBlank()) {
      throw new BadRequestResponse("Family id is required");
    }
    return id.trim();
  }
}
