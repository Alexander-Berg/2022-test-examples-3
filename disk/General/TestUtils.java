package ru.yandex.chemodan.app.stat;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.inside.admin.conductor.ConductorClient;

/**
 * @author Lev Tolmachev
 */
public class TestUtils {
    public static MongoClient testMongoClient() {
        ConductorClient client = new ConductorClient();
        ListF<String> hosts = client.getGroupHosts("disk_diskdb_tst");

        return new MongoClient(hosts.map(host -> new ServerAddress(host, 27018)));
    }

    public static DB testMongoDb() {
        return testMongoClient().getDB("download_stat_test");
    }

}
