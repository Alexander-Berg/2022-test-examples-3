package ru.yandex.market.tsum.core.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MongoConfigTest {

    @Test
    public void buildMongoUrl() {
        assertEquals(
            "mongodb://user:password@host1,host2,host3/database",
            MongoConfig.buildMongoUrl("host1,host2,host3", "user", "password", "database")
        );
        assertEquals(
            "mongodb://some-host", MongoConfig.buildMongoUrl("some-host", null, null, null)
        );
        assertEquals(
            "mongodb://some-host/some-db",
            MongoConfig.buildMongoUrl("some-host", null, null, "some-db")
        );
    }
}