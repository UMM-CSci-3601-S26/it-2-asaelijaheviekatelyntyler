package umm3601.supplylist;

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
import umm3601.supplylist.SupplyListValidator.SupplyListSearchCriteria;

public class SupplyListRepository {

  private final JacksonMongoCollection<SupplyList> collection;

  public SupplyListRepository(JacksonMongoCollection<SupplyList> collection) {
    this.collection = collection;
  }

  public SupplyList findById(String id) {
    return collection.find(eq("_id", parseObjectId(id))).first();
  }

  public List<SupplyList> findAll(SupplyListSearchCriteria criteria) {
    return collection.find(constructFilter(criteria)).into(new ArrayList<>());
  }

  public void insert(SupplyList supplyList) {
    collection.insertOne(supplyList);
  }

  public long update(String id, SupplyList supplyList) {
    supplyList._id = id;
    return collection.replaceOne(eq("_id", parseObjectId(id)), supplyList).getMatchedCount();
  }

  public long delete(String id) {
    return collection.deleteOne(eq("_id", parseObjectId(id))).getDeletedCount();
  }

  private Bson constructFilter(SupplyListSearchCriteria criteria) {
    List<Bson> filters = new ArrayList<>();

    if (criteria.school() != null) {
      filters.add(multipleIntakeFilter("school", criteria.school()));
    }
    if (criteria.grade() != null) {
      filters.add(multipleIntakeFilter("grade", criteria.grade()));
    }
    if (criteria.teacher() != null) {
      filters.add(multipleIntakeFilter("teacher", criteria.teacher()));
    }
    if (criteria.academicYear() != null) {
      filters.add(multipleIntakeFilter("academicYear", criteria.academicYear()));
    }
    if (criteria.item() != null) {
      filters.add(multipleIntakeFilter("item", criteria.item()));
    }
    if (criteria.brand() != null) {
      filters.add(attributeOptionsFilter("brand", criteria.brand()));
    }
    if (criteria.color() != null) {
      filters.add(attributeOptionsFilter("color", criteria.color()));
    }
    if (criteria.size() != null) {
      filters.add(multipleIntakeFilter("size", criteria.size()));
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
      filters.add(attributeOptionsFilter("material", criteria.material()));
    }
    if (criteria.type() != null) {
      filters.add(attributeOptionsFilter("type", criteria.type()));
    }
    if (criteria.style() != null) {
      filters.add(attributeOptionsFilter("style", criteria.style()));
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

  private Bson attributeOptionsFilter(String field, String raw) {
    return Filters.or(
        multipleIntakeFilter(field + ".allOf", raw),
        multipleIntakeFilter(field + ".anyOf", raw));
  }

  private ObjectId parseObjectId(String id) {
    try {
      return new ObjectId(id);
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested supply list id wasn't a legal Mongo Object ID.");
    }
  }
}
