package umm3601.checklist;

import java.util.List;

import umm3601.supplylist.SupplyList;

@SuppressWarnings({ "VisibilityModifier" })
class Checklist {
  public String _id;
  public String studentName;
  public String school;
  public String grade;
  public List<String> requestedSupplies;
  public List<Checklist.ChecklistItem> checklist;

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Checklist)) {
      return false;
    }
    Checklist other = (Checklist) obj;
    return this._id != null && this._id.equals(other._id);
  }

  @Override
  public int hashCode() {
    return _id != null ? _id.hashCode() : 0;
  }

  public static class ChecklistItem {
    // We want to keep a reference to the full supply details for
    // filtering/inventory purposes, but we also want to initialize the checklist
    // item state based on the supply item
    public SupplyList supply;

    // These fields are used to track the state of the checklist item as the user
    // interacts with it
    // They are not preassigned to any particular value because the user may
    // interact with the checklist in different ways
    public Boolean completed; // Volunteer marked this as given
    public Boolean unreceived; // Supply ran out — needs delivery to school

    // This field is used to track which option the user selected for this checklist
    // item, if any
    public String selectedOption;

    // Required by Jackson for deserialization from MongoDB
    ChecklistItem() {
    }

    // This constructor is used to create a checklist item from a supply item
    ChecklistItem(SupplyList supply) {
      // We want to keep a reference to the full supply details for
      // filtering/inventory purposes, but we also want to initialize the checklist
      // item state based on the supply item
      this.supply = supply;

      this.completed = false;
      this.unreceived = false;
      this.selectedOption = null;
    }
  }
}
