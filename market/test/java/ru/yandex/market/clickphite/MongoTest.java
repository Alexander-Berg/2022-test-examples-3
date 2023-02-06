package ru.yandex.market.clickphite;

import java.util.Date;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.WriteConcern;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"/>
 * @date 07/08/15
 */
@Ignore
public class MongoTest {

    @Test
    public void testDodo() throws Exception {
        String mongoUrl = "mongodb://volantis.market.yandex.net";
        MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoUrl));
        mongoClient.setWriteConcern(WriteConcern.MAJORITY);
        DB db = mongoClient.getDB("test");
        DBCollection collection = db.getCollection("x3");


        BulkWriteOperation operation = collection.initializeUnorderedBulkOperation();

        System.out.println(new Date());

        for (int i = 0; i < 1_000_000; i++) {
            BasicDBObject dbObject = new BasicDBObject();
            dbObject.append("table", "parallel_report");
            dbObject.append("time", i);
            dbObject.append("count", i * 1000);
            operation.insert(dbObject);
        }
        operation.execute();

        System.out.println(new Date());


    }
}
