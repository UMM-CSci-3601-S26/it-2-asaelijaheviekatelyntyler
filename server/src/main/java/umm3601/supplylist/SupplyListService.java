package umm3601.supplylist;

import java.util.List;

import io.javalin.http.NotFoundResponse;
import umm3601.supplylist.SupplyListValidator.SupplyListSearchCriteria;

public class SupplyListService {

  private final SupplyListRepository repository;

  public SupplyListService(SupplyListRepository repository) {
    this.repository = repository;
  }

  public SupplyList getById(String id) {
    SupplyList supplyList = repository.findById(id);
    if (supplyList == null) {
      throw new NotFoundResponse("The requested supply list item was not found");
    }
    return supplyList;
  }

  public List<SupplyList> getAll(SupplyListSearchCriteria criteria) {
    return repository.findAll(criteria);
  }

  public void create(SupplyList supplyList) {
    repository.insert(supplyList);
  }

  public void update(String id, SupplyList supplyList) {
    long matchedCount = repository.update(id, supplyList);
    if (matchedCount == 0) {
      throw new NotFoundResponse("The requested supply list item was not found");
    }
  }

  public void delete(String id) {
    long deletedCount = repository.delete(id);
    if (deletedCount == 0) {
      throw new NotFoundResponse("The requested supply list item was not found");
    }
  }
}
