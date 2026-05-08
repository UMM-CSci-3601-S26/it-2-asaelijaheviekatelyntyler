package umm3601.checklist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.conversions.Bson;

import umm3601.checklist.ChecklistValidator.ChecklistSearchCriteria;
import umm3601.common.MongoFilters;
import umm3601.family.Family.StudentInfo;
import umm3601.settings.Settings;
import umm3601.supplylist.SupplyList;

public class ChecklistService {

  static final String SCHOOL_KEY = "school";
  static final String GRADE_KEY = "grade";
  static final String STUDENT_NAME_KEY = "studentName";
  static final String[] HS_GRADES = {"9", "10", "11", "12"};

  private final ChecklistRepository repository;

  public ChecklistService(ChecklistRepository repository) {
    this.repository = repository;
  }

  public List<Checklist> generateDigitalChecklists() {
    List<SupplyList> rawSupplies = repository.getSupplyLists();
    Settings settings = repository.getSettings();

    List<SupplyList> orderedSupplies = rawSupplies;
    if (settings != null && settings.supplyOrder != null && !settings.supplyOrder.isEmpty()) {
      orderedSupplies = applySupplyOrder(rawSupplies, settings.supplyOrder);
    }

    List<SupplyList> expandedSupplies = expandHighSchoolSupplies(orderedSupplies);
    List<Checklist> checklists = repository.getFamilies().stream()
        .flatMap(family -> family.students.stream().map(student -> createChecklist(student, expandedSupplies)))
        .collect(Collectors.toList());

    repository.replaceAllChecklists(checklists);
    return checklists;
  }

  public List<Checklist> getStoredChecklists(ChecklistSearchCriteria criteria) {
    return repository.getStoredChecklists(constructFilter(criteria));
  }

  public List<Checklist> getStoredChecklistsForStudents(List<StudentInfo> students) {
    return repository.getStoredChecklistsForStudents(students);
  }

  public byte[] exportChecklistsPdf() {
    List<SupplyList> expandedSupplies = expandHighSchoolSupplies(repository.getSupplyLists());
    List<Checklist> checklists = repository.getFamilies().stream()
        .flatMap(family -> family.students.stream().map(student -> createChecklist(student, expandedSupplies)))
        .collect(Collectors.toList());

    StringBuilder pdf = new StringBuilder();
    pdf.append("%PDF-1.4\n");
    pdf.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
    pdf.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");

    StringBuilder text = new StringBuilder();
    text.append("BT /F1 12 Tf 50 750 Td\n");
    for (Checklist checklist : checklists) {
      text.append("(")
          .append("Student: ").append(checklist.studentName)
          .append(" (").append(checklist.school)
          .append(", Grade ").append(checklist.grade)
          .append(")) Tj T* ");

      for (Checklist.ChecklistItem item : checklist.checklist) {
        text.append("(")
            .append(" - ").append(item.supply)
            .append(" | completed: ").append(item.completed)
            .append(" | unreceived: ").append(item.unreceived)
            .append(" | option: ").append(item.selectedOption)
            .append(") Tj T* ");
      }
      text.append("() Tj T* ");
    }
    text.append("ET");

    pdf.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]")
        .append(" /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n");

    byte[] textBytes = text.toString().getBytes();
    pdf.append("4 0 obj << /Length ").append(textBytes.length).append(" >> stream\n");
    pdf.append(text).append("\nendstream endobj\n");
    pdf.append("5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");
    pdf.append("xref\n0 6\n0000000000 65535 f \n");
    pdf.append("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n");
    pdf.append(pdf.length()).append("\n%%EOF");
    return pdf.toString().getBytes();
  }

  Bson constructFilter(ChecklistSearchCriteria criteria) {
    List<Bson> filters = new ArrayList<>();
    if (criteria.school() != null) {
      filters.add(MongoFilters.caseInsensitiveExact(SCHOOL_KEY, criteria.school()));
    }
    if (criteria.grade() != null) {
      filters.add(MongoFilters.caseInsensitiveExact(GRADE_KEY, criteria.grade()));
    }
    if (criteria.studentName() != null) {
      filters.add(MongoFilters.caseInsensitiveExact(STUDENT_NAME_KEY, criteria.studentName()));
    }
    return MongoFilters.andAll(filters);
  }

  public Checklist createChecklist(StudentInfo student, List<SupplyList> allSupplies) {
    String studentSchool = normalizeSchool(student.school);
    String studentGrade = normalizeGrade(student.grade);
    List<Checklist.ChecklistItem> items = allSupplies.stream()
        .filter(supply -> supply.school != null && supply.grade != null
            && normalizeSchool(supply.school).equals(studentSchool)
            && normalizeGrade(supply.grade).equals(studentGrade))
        .map(Checklist.ChecklistItem::new)
        .collect(Collectors.toList());

    Checklist checklist = new Checklist();
    checklist.studentName = student.name;
    checklist.school = student.school;
    checklist.grade = student.grade;
    checklist.requestedSupplies = student.requestedSupplies;
    checklist.checklist = items;
    return checklist;
  }

  static String normalizeSchool(String school) {
    if (school == null) {
      return "";
    }
    return school.trim().toLowerCase().replaceAll("\\s+school$", "");
  }

  static String normalizeGrade(String grade) {
    if (grade == null) {
      return "";
    }
    return grade.trim().toLowerCase().replaceAll("[\\s\\-]", "");
  }

  static List<SupplyList> expandHighSchoolSupplies(List<SupplyList> supplies) {
    Set<String> existingKeys = new HashSet<>();
    for (SupplyList supply : supplies) {
      if (supply.school != null && supply.grade != null
          && !normalizeGrade(supply.grade).equals("highschool")) {
        existingKeys.add(normalizeSchool(supply.school) + "|" + normalizeGrade(supply.grade));
      }
    }

    List<SupplyList> result = new ArrayList<>();
    for (SupplyList supply : supplies) {
      if (supply.school != null && supply.grade != null
          && normalizeGrade(supply.grade).equals("highschool")) {
        for (String grade : HS_GRADES) {
          String key = normalizeSchool(supply.school) + "|" + normalizeGrade(grade);
          if (!existingKeys.contains(key)) {
            result.add(copyWithGrade(supply, grade));
          }
        }
      } else {
        result.add(supply);
      }
    }
    return result;
  }

  static SupplyList copyWithGrade(SupplyList source, String newGrade) {
    SupplyList copy = new SupplyList();
    copy.district = source.district;
    copy.school = source.school;
    copy.grade = newGrade;
    copy.teacher = source.teacher;
    copy.academicYear = source.academicYear;
    copy.item = source.item;
    copy.brand = source.brand;
    copy.size = source.size;
    copy.color = source.color;
    copy.type = source.type;
    copy.material = source.material;
    copy.style = source.style;
    copy.count = source.count;
    copy.quantity = source.quantity;
    copy.notes = source.notes;
    return copy;
  }

  static List<SupplyList> applySupplyOrder(
      List<SupplyList> supplies,
      List<Settings.SupplyItemOrder> supplyOrder) {
    Map<String, Integer> stagedIndex = new HashMap<>();
    Set<String> notGivenTerms = new HashSet<>();

    int pos = 0;
    for (Settings.SupplyItemOrder entry : supplyOrder) {
      if ("staged".equals(entry.status)) {
        stagedIndex.put(entry.itemTerm, pos++);
      } else if ("notGiven".equals(entry.status)) {
        notGivenTerms.add(entry.itemTerm);
      }
    }

    return supplies.stream()
        .filter(supply -> supply.item == null || supply.item.stream().noneMatch(notGivenTerms::contains))
        .sorted(Comparator.comparingInt(supply -> {
          if (supply.item == null) {
            return Integer.MAX_VALUE;
          }
          return supply.item.stream()
              .mapToInt(itemTerm -> stagedIndex.getOrDefault(itemTerm, Integer.MAX_VALUE))
              .min()
              .orElse(Integer.MAX_VALUE);
        }))
        .collect(Collectors.toList());
  }
}
