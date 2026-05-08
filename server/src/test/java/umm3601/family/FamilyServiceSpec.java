package umm3601.family;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.javalin.http.NotFoundResponse;

@SuppressWarnings("MagicNumber")
class FamilyServiceSpec {

  @Mock
  private FamilyRepository repository;

  private FamilyService service;

  @BeforeEach
  void setupEach() {
    MockitoAnnotations.openMocks(this);
    service = new FamilyService(repository);
  }

  @Test
  void getByOwnerUserIdThrowsWhenMissing() {
    when(repository.findByOwnerUserId("guardian-1")).thenReturn(null);

    NotFoundResponse exception = assertThrows(NotFoundResponse.class, () -> service.getByOwnerUserId("guardian-1"));
    assertEquals("No family profile exists for this guardian yet", exception.getMessage());
  }

  @Test
  void deleteThrowsWhenRepositoryDeletesNothing() {
    when(repository.delete("family-1")).thenReturn(0L);

    NotFoundResponse exception = assertThrows(NotFoundResponse.class, () -> service.delete("family-1"));
    assertTrue(exception.getMessage().contains("Was unable to delete Family IDfamily-1"));
  }

  @Test
  void requestDeleteByVolunteerThrowsWhenRepositoryUpdatesNothing() {
    when(repository.requestDeleteById(
      "family-1",
      "Please remove",
      "volunteer-1",
      "2026-04-24T12:00:00Z")).thenReturn(0L);

    NotFoundResponse exception = assertThrows(
        NotFoundResponse.class,
        () -> service.requestDeleteByVolunteer("family-1", "Please remove", "volunteer-1"));

    assertTrue(exception.getMessage().contains("Was unable to request delete for Family IDfamily-1"));
  }

  @Test
  void clearDeleteRequestThrowsWhenRepositoryUpdatesNothing() {
    when(repository.clearDeleteRequestById("family-1")).thenReturn(0L);

    NotFoundResponse exception = assertThrows(NotFoundResponse.class, () -> service.clearDeleteRequest("family-1"));
    assertTrue(exception.getMessage().contains("Was unable to clear delete request for Family IDfamily-1"));
  }

  @Test
  void getDashboardStatsIgnoresFamiliesWithoutStudents() {
    Family firstFamily = new Family();
    firstFamily.students = null;

    Family.StudentInfo student = new Family.StudentInfo();
    student.school = "Roosevelt";
    student.grade = "5";

    Family secondFamily = new Family();
    secondFamily.students = List.of(student);

    when(repository.findAll()).thenReturn(List.of(firstFamily, secondFamily));

    Map<String, Object> stats = service.getDashboardStats();

    assertEquals(2, stats.get("totalFamilies"));
    assertEquals(Map.of("Roosevelt", 1), stats.get("studentsPerSchool"));
    assertEquals(Map.of("5", 1), stats.get("studentsPerGrade"));
  }

  @Test
  void requestDeleteByVolunteerDelegatesToRepository() {
    when(repository.requestDeleteById("family-1")).thenReturn(1L);

    service.requestDeleteByVolunteer("family-1");

    verify(repository).requestDeleteById("family-1");
  }
}
