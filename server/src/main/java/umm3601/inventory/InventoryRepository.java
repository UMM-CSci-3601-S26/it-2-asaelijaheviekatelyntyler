package umm3601.inventory;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import com.mongodb.client.model.Filters;

import io.javalin.http.BadRequestResponse;
import umm3601.inventory.InventoryValidator.InventorySearchCriteria;

public class InventoryRepository {

  private final JacksonMongoCollection<Inventory> collection;

  public InventoryRepository(JacksonMongoCollection<Inventory> collection) {
    this.collection = collection;
  }

  public List<Inventory> findAll(InventorySearchCriteria criteria, int skip, int limit) {
    return collection.find(constructFilter(criteria))
        .skip(skip)
        .limit(limit)
        .into(new ArrayList<>());
  }

  public Inventory findById(String id) {
    return collection.find(eq("_id", parseObjectId(id, "inventory"))).first();
  }

  public void insert(Inventory item) {
    collection.insertOne(item);
  }

  public long update(String id, Inventory item) {
    item._id = id;
    return collection.replaceOne(eq("_id", parseObjectId(id, "inventory")), item).getMatchedCount();
  }

  public long delete(String id) {
    return collection.deleteOne(eq("_id", parseObjectId(id, "inventory"))).getDeletedCount();
  }

  private Bson constructFilter(InventorySearchCriteria criteria) {
    List<Bson> filters = new ArrayList<>();

    if (criteria.item() != null) {
      filters.add(multipleIntakeFilter("item", criteria.item()));
    }
    if (criteria.brand() != null) {
      filters.add(multipleIntakeFilter("brand", criteria.brand()));
    }
    if (criteria.size() != null) {
      filters.add(multipleIntakeFilter("size", criteria.size()));
    }
    if (criteria.color() != null) {
      filters.add(multipleIntakeFilter("color", criteria.color()));
    }
    if (criteria.quantity() != null) {
      filters.add(eq("quantity", criteria.quantity()));
    }
    if (criteria.count() != null) {
      filters.add(eq("count", criteria.count()));
    }
    if (criteria.notes() != null) {
      filters.add(regex("notes", Pattern.compile(Pattern.quote(criteria.notes()), Pattern.CASE_INSENSITIVE)));
    }
    if (criteria.material() != null) {
      filters.add(multipleIntakeFilter("material", criteria.material()));
    }
    if (criteria.style() != null) {
      filters.add(multipleIntakeFilter("style", criteria.style()));
    }
    if (criteria.bin() != null) {
      filters.add(eq("bin", criteria.bin()));
    }
    if (criteria.type() != null) {
      filters.add(multipleIntakeFilter("type", criteria.type()));
    }

    return filters.isEmpty() ? new Document() : and(filters);
  }

  private Bson multipleIntakeFilter(String field, String raw) {
    List<Pattern> patterns = Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> Pattern.compile(Pattern.quote(s), Pattern.CASE_INSENSITIVE))
        .toList();

    return Filters.in(field, patterns);
  }

  private ObjectId parseObjectId(String id, String resourceName) {
    try {
      return new ObjectId(id);
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested " + resourceName + " id wasn't a legal Mongo Object ID.");
    }
  }
}
