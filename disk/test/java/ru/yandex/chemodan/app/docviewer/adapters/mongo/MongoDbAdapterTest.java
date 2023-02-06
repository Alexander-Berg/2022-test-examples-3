package ru.yandex.chemodan.app.docviewer.adapters.mongo;

import java.util.Arrays;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.chemodan.app.docviewer.DocviewerSpringTestBase;
import ru.yandex.misc.random.Random2;
import ru.yandex.misc.test.Assert;

public class MongoDbAdapterTest extends DocviewerSpringTestBase {
    @Autowired
    @Qualifier("mongoDbAdapter")
    private MongoDbAdapter mongoDbAdapter;

    @Test
    public void test() {
        DBCollection collection = recreateTestCollection();

        DBObject first = new BasicDBObject();
        first.put("key1", "value1");
        first.put("key2", "value2");

        DBObject second = new BasicDBObject();
        second.put("key1", "value3");
        second.put("key2", "value4");

        collection.insert(first, second);
        collection.ensureIndex("key1");

        DBObject query = new BasicDBObject();
        query.put("key1", "value3");
        DBCursor cursor = collection.find(query);

        Assert.equals(1, cursor.size());
        Assert.isTrue(cursor.hasNext());
        DBObject found = cursor.next();
        Assert.equals("value4", found.get("key2"));

        collection.drop();
    }

    private void testWith(Object value) {
        DBCollection collection = recreateTestCollection();

        DBObject first = new BasicDBObject();
        String keyValue = Random2.R.nextAlnum(15);
        first.put("key", keyValue);
        first.put("value", value);
        collection.insert(first);

        DBObject query = new BasicDBObject();
        query.put("key", keyValue);
        DBCursor cursor = collection.find(query);

        Assert.equals(1, cursor.size());
        Assert.isTrue(cursor.hasNext());
        DBObject found = cursor.next();

        if (value.getClass().isArray()) {
            Assert.arraysEquals(value, found.get("value"));
        } else {
            Assert.equals(value, found.get("value"));
        }

        collection.drop();
    }

    @Test
    public void testWithDouble() {
        testWith(1.0);
    }

    @Test
    public void testWithInteger() {
        testWith(1);
    }

    @Test
    public void testWithListOfDoubles() {
        testWith(Arrays.asList(1d, 2d));
    }

    @Test
    public void testWithListOfStrings() {
        testWith(Arrays.asList("value-1", "value-2"));
    }

    @Test
    public void testWithLong() {
        testWith(1L);
    }

    @Test
    public void testWithString() {
        testWith("some-string");
    }

    private DBCollection recreateTestCollection() {
        String collectionName = "test";
        return mongoDbAdapter.getDatabase().getCollection(collectionName);
    }
}
