package ru.yandex.chemodan.app.djfs.core.db.mongo;

import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class MongoUtilTest {
    @Test
    public void mongodbExtendedJson() {
        BsonDocument document = new BsonDocument()
                .append("int64_value", new BsonInt64(1327609746000L))
                .append("datetime_value", new BsonDateTime(1327609746000L));
        Assert.equals("{ \"int64_value\" : 1327609746000, \"datetime_value\" : \"2012-01-26T20:29:06.000Z\" }",
                MongoUtil.serializeToJson(document));
    }

    @Test
    public void toNumber() {
        Assert.equals(1, MongoUtil.toNumber(1).asInt32().getValue());
        Assert.equals(0, MongoUtil.toNumber(0).asInt32().getValue());
        Assert.equals(-1, MongoUtil.toNumber(-1).asInt32().getValue());

        Assert.equals(1, MongoUtil.toNumber(1L).asInt32().getValue());
        Assert.equals(0, MongoUtil.toNumber(0L).asInt32().getValue());
        Assert.equals(-1, MongoUtil.toNumber(-1L).asInt32().getValue());

        Assert.equals(1327609746000L, MongoUtil.toNumber(1327609746000L).asInt64().getValue());
        Assert.equals(-1327609746000L, MongoUtil.toNumber(-1327609746000L).asInt64().getValue());
    }
}
