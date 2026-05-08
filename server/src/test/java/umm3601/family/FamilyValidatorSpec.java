package umm3601.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.javalin.http.BadRequestResponse;

class FamilyValidatorSpec {

  private FamilyValidator validator;

  @BeforeEach
  void setup() {
    validator = new FamilyValidator();
  }

  @Test
  void validateBodyPassesForValidEmail() {
    Family family = new Family();
    family.email = "valid@email.com";

    Family result = validator.validateBody(family);

    assertSame(family, result);
  }

  @Test
  void validateBodyRejectsNullFamily() {
    assertThrows(BadRequestResponse.class, () -> validator.validateBody(null));
  }

  @Test
  void validateBodyRejectsInvalidEmail() {
    Family family = new Family();
    family.email = "not-an-email";

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(family));
    assertEquals("Family must have a valid email", ex.getMessage());
  }

  @Test
  void validateBodyRejectsMissingEmail() {
    Family family = new Family();
    family.email = null;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateBody(family));
    assertEquals("Family must have a valid email", ex.getMessage());
  }

  @Test
  void validateIdRejectsBlank() {
    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateId(" "));
    assertEquals("Family id is required", ex.getMessage());
  }

  @Test
  void validateIdRejectsNull() {
    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validateId(null));
    assertEquals("Family id is required", ex.getMessage());
  }

  @Test
  void validateIdTrimsInput() {
    assertEquals("abc", validator.validateId("  abc "));
  }

  @Test
  void validatePortalFormBodyPassesWithValidData() {
    Family family = buildValidFamily();

    Family result = validator.validatePortalFormBody(family);

    assertSame(family, result);
  }

  @Test
  void validatePortalFormBodyRejectsMissingGuardianName() {
    Family family = buildValidFamily();
    family.guardianName = " ";

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validatePortalFormBody(family));
    assertEquals("Guardian name is required", ex.getMessage());
  }

  @Test
  void validatePortalFormBodyRejectsMissingAddress() {
    Family family = buildValidFamily();
    family.address = "";

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validatePortalFormBody(family));
    assertEquals("Address is required", ex.getMessage());
  }

  @Test
  void validatePortalFormBodyRejectsNoStudents() {
    Family family = buildValidFamily();
    family.students = List.of();

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validatePortalFormBody(family));
    assertEquals("At least one student is required", ex.getMessage());
  }

  @Test
  void validatePortalFormBodyRejectsNullStudents() {
    Family family = buildValidFamily();
    family.students = null;

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validatePortalFormBody(family));
    assertEquals("At least one student is required", ex.getMessage());
  }

  @Test
  void validatePortalFormBodyRejectsIncompleteStudent() {
    Family family = buildValidFamily();
    Family.StudentInfo student = new Family.StudentInfo();
    student.name = "Kid";
    student.grade = "";
    student.school = "MHS";
    family.students = List.of(student);

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validatePortalFormBody(family));
    assertEquals("Each student must include name, grade, and school", ex.getMessage());
  }

  @Test
  void validatePortalFormBodyRejectsNullStudentEntry() {
    Family family = buildValidFamily();
    family.students = Arrays.asList((Family.StudentInfo) null);

    BadRequestResponse ex = assertThrows(BadRequestResponse.class, () -> validator.validatePortalFormBody(family));
    assertEquals("Each student must include name, grade, and school", ex.getMessage());
  }

  private Family buildValidFamily() {
    Family family = new Family();
    family.email = "valid@email.com";
    family.guardianName = "Guardian";
    family.address = "123 Main";

    Family.StudentInfo student = new Family.StudentInfo();
    student.name = "Kid";
    student.grade = "2";
    student.school = "MHS";
    family.students = List.of(student);
    return family;
  }
}
