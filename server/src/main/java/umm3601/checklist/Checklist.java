package umm3601.checklist;

import java.util.List;

import org.mongojack.Id;
import org.mongojack.ObjectId;

import umm3601.supplylist.SupplyList;

@SuppressWarnings({"VisibilityModifier"})
class Checklist {
  @ObjectId @Id
  @SuppressWarnings({"MemberName"})
  public String _id;

  public String studentName;
  public String school;
  public String grade;
  public List<String> requestedSupplies;
  public List<Checklist.ChecklistItem> checklist;

  public static class ChecklistItem {
    public SupplyList supply;
    public Boolean completed = false;
    public Boolean unreceived = false;
    public String selectedOption;

    public ChecklistItem() {}

    ChecklistItem(SupplyList supply) {
      this.supply = supply;
    }
  }
}

