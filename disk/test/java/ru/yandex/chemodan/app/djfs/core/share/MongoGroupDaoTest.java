package ru.yandex.chemodan.app.djfs.core.share;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2List;
import ru.yandex.chemodan.app.djfs.core.legacy.exception.LegacyUserIsReadOnlyException;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.util.UuidUtils;
import ru.yandex.misc.lang.StringUtils;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class MongoGroupDaoTest extends DjfsTestBase {
    @Test
    public void longPathDeserialization() {
        String id = UuidUtils.randomToHexString();
        String path = "/disk/" + StringUtils.repeat("a", 1000);

        MongoCollection<Document> collection = commonMongoClient.getDatabase("groups").getCollection("groups");
        collection.insertOne(new Document(Tuple2List.<String, Object>fromPairs(
                "_id", id,
                "owner", "1",
                "path", path,
                "size", 1
        ).toMap()));

        Option<Group> group = mongoGroupDao.find(id);
        Assert.some(group);
        Assert.equals(path, group.get().getPath().getPath());
    }

    @Test
    public void increaseSize() {
        String id = UuidUtils.randomToHexString();

        MongoCollection<Document> collection = commonMongoClient.getDatabase("groups").getCollection("groups");
        collection.insertOne(new Document(Tuple2List.<String, Object>fromPairs(
                "_id", id,
                "owner", "1",
                "path", "/disk/shared",
                "size", 10
        ).toMap()));

        mongoGroupDao.increaseSize(id, 100L);

        Option<Group> group = mongoGroupDao.find(id);
        Assert.some(110L, group.map(Group::getSize));
    }

    @Test
    public void increaseSizeReadonly() {
        String id = UuidUtils.randomToHexString();

        MongoCollection<Document> collection = commonMongoClient.getDatabase("groups").getCollection("groups");
        collection.insertOne(new Document(Tuple2List.<String, Object>fromPairs(
                "_id", id,
                "owner", "1",
                "path", "/disk/shared",
                "size", 10
        ).toMap()));
        mongoGroupDao = new MongoGroupDao(commonMongoClient, () -> true);
        Assert.assertThrows(() -> mongoGroupDao.increaseSize(id, 100L), LegacyUserIsReadOnlyException.class);
        Option<Group> group = mongoGroupDao.find(id);
        Assert.some(10L, group.map(Group::getSize));
    }

    @Test
    public void removeReadonly() {
        String id = UuidUtils.randomToHexString();

        MongoCollection<Document> collection = commonMongoClient.getDatabase("groups").getCollection("groups");
        collection.insertOne(new Document(Tuple2List.<String, Object>fromPairs(
                "_id", id,
                "owner", "1",
                "path", "/disk/shared",
                "size", 10
        ).toMap()));
        mongoGroupDao = new MongoGroupDao(commonMongoClient, () -> true);
        Option<Group> group = mongoGroupDao.find(id);
        Assert.some(group);
        Assert.assertThrows(() -> mongoGroupDao.remove(id), LegacyUserIsReadOnlyException.class);
        group = mongoGroupDao.find(id);
        Assert.some(group);
    }
}
