package umm3601.family;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongojack.JacksonMongoCollection;

import io.javalin.http.BadRequestResponse;
import umm3601.common.BaseRepository;

public class FamilyRepository extends BaseRepository<Family> {

  public FamilyRepository(JacksonMongoCollection<Family> collection) {
    super(collection);
  }

  public Family findById(String id) {
    return collection.find(eq("_id", parseObjectId(id))).first();
  }

  public Family findByOwnerUserId(String ownerUserId) {
    return collection.find(eq("ownerUserId", ownerUserId)).first();
  }

  public List<Family> findAll() {
    return collection.find().into(new ArrayList<>());
  }

  public void insert(Family family) {
    collection.insertOne(family);
  }

  public void upsertByOwnerUserId(Family family) {
    Family existing = findByOwnerUserId(family.ownerUserId);
    if (existing == null) {
      collection.insertOne(family);
      return;
    }

    family._id = existing._id;
    collection.replaceOne(eq("_id", parseObjectId(existing._id)), family);
  }

  public long delete(String id) {
    return collection.deleteOne(eq("_id", parseObjectId(id))).getDeletedCount();
  }

  public long requestDeleteById(String id) {
    return requestDeleteById(id, "", null, null);
  }

  public long requestDeleteById(String id, String message, String requestedByUserId, String requestedAt) {
    return collection.updateOne(
        eq("_id", parseObjectId(id)),
        combine(
            set("deleteRequest.requested", true),
            set("deleteRequest.message", message),
            set("deleteRequest.requestedByUserId", requestedByUserId),
            set("deleteRequest.requestedAt", requestedAt)))
        .getModifiedCount();
  }

  public long clearDeleteRequestById(String id) {
    return collection.updateOne(
        eq("_id", parseObjectId(id)),
        unset("deleteRequest"))
        .getModifiedCount();
  }

  public List<Family> findDeleteRequests() {
    return collection.find(
      eq("deleteRequest.requested", true))
        .into(new ArrayList<>());
  }

  private ObjectId parseObjectId(String id) {
    try {
      return new ObjectId(id);
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("The requested family id wasn't a legal Mongo Object ID.");
    }
  }
}
