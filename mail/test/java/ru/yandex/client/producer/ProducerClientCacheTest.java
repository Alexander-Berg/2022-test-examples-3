package ru.yandex.client.producer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.parser.searchmap.SearchMap;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class ProducerClientCacheTest extends TestBase {
    private static final int PORT = 15443;
    private static final String PG = "pg";
    private static final String LOCALHOST = "localhost";
    private static final String HOLOCAUST = "holocaust";
    private static final long CACHE_TTL = 5000L;
    private static final long CACHE_UPDATE_INTERVAL = 1000L;
    private static final String NEW_PREFIX = "new_";
    private static final String SEARCHMAP =
        "pg shards:0-65533,search_port:15443,host:localhost\n"
            + "pg shards:0-10000,search_port:15444,host:holocaust\n"
            + "pg shards:0-10000,json_indexer_port:8082,host:localhost"
            + "pg shards:0-65533,search_port:15443,host:new_localhost\n"
            + "pg shards:0-10000,search_port:15444,host:new_holocaust\n";

    @Test
    public void testOrder() throws Exception {
        final long prefix = 66035;
        try (StaticServer producer = new StaticServer(Configs.baseConfig());
             SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                 Configs.baseConfig(),
                 Configs.dnsConfig());
             ProducerClient client =
                 new ProducerClient(
                     reactor,
                     new ProducerClientConfigBuilder()
                         .host(new HttpHost(LOCALHOST, producer.port()))
                         .connections(2)
                         .allowCached(true)
                         .cacheTtl(CACHE_TTL)
                         .cacheUpdateInterval(CACHE_UPDATE_INTERVAL)
                         .build(),
                     new SearchMap(
                         new SearchMapConfigBuilder()
                             .content(SEARCHMAP)
                             .build())))
        {
            reactor.start();
            client.start();
            String uri =
                "/_status?service=pg&prefix=501&allow_cached&all"
                    + "&json-type=dollar";

            String hostslist = "[{\"localhost\":100511}"
                + ",{\"holocaust\":100510}"
                + ",{\"loloto\":100400}]";

            producer.add(uri, HttpStatus.SC_OK, hostslist);
            producer.start();

            Assert.assertEquals(
                Collections.singletonList(new HttpHost(LOCALHOST, PORT)),
                client.execute(
                    new User(PG, new LongPrefix(prefix))).get());
            producer.add(uri, HttpStatus.SC_OK, hostslist);
            Assert.assertEquals(
                Collections.singletonList(
                    new HttpHost(LOCALHOST, PORT)),
                client.execute(
                    new User(PG, new LongPrefix(prefix))).get());

            String hostslist4 = "[{\"new_holocaust\":100512}"
                + ",{\"new_localhost\":100510}]";

            producer.add(uri, HttpStatus.SC_OK, hostslist4);
            Thread.sleep(CACHE_TTL + CACHE_UPDATE_INTERVAL);

            Assert.assertEquals(
                Arrays.asList(
                    new HttpHost(NEW_PREFIX + HOLOCAUST, PORT + 1)),
                client.execute(
                    new User(PG, new LongPrefix(prefix))).get());
            producer.add(uri, HttpStatus.SC_OK, hostslist4);
            Assert.assertEquals(
                Arrays.asList(
                    new HttpHost(NEW_PREFIX + HOLOCAUST, PORT + 1)),
                client.execute(
                    new User(PG, new LongPrefix(prefix))).get());
        }
    }

    @Test
    public void testCache() throws Exception {
        try (StaticServer producer = new StaticServer(Configs.baseConfig());
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            ProducerClient client =
                new ProducerClient(
                    reactor,
                    new ProducerClientConfigBuilder()
                        .host(new HttpHost(LOCALHOST, producer.port()))
                        .connections(2)
                        .allowCached(true)
                        .cacheTtl(CACHE_TTL)
                        .cacheUpdateInterval(CACHE_UPDATE_INTERVAL)
                        .build(),
                    new SearchMap(
                        new SearchMapConfigBuilder()
                            .content(SEARCHMAP)
                            .build())))
        {
            reactor.start();
            client.start();
            String uri = "/_status?service=pg&prefix=500&allow_cached"
                + "&all&json-type=dollar";
            String hostslist = "[{\"localhost\":100500},{\"holocaust\":100500}"
                + ",{\"other\":101}]";
            producer.add(uri, HttpStatus.SC_OK, hostslist);
            producer.start();
            final long prefix = 66034;
            Assert.assertEquals(
                new HashSet<>(
                    Arrays.asList(
                        new HttpHost(LOCALHOST, PORT),
                        new HttpHost(HOLOCAUST, PORT + 1))),
                new HashSet<>(
                    client.execute(
                        new User(PG, new LongPrefix(prefix))).get()));
            System.out.println("First request processed");
            Assert.assertEquals(
                new HashSet<>(
                    Arrays.asList(
                        new HttpHost(LOCALHOST, PORT),
                        new HttpHost(HOLOCAUST, PORT + 1))),
                new HashSet<>(
                    client.execute(
                        new User(PG, new LongPrefix(prefix))).get()));
            System.out.println("Second request processed");
            String hostslist2 = "[{\"new_localhost\":100500}"
                + ",{\"new_holocaust\":100500}"
                + ",{\"other\":100}]";
            producer.add(uri, HttpStatus.SC_OK, hostslist2);
            // After first update, cache worker will continue cache entry
            // updating for up to CACHE_TTL ms
            // After that, cache entry will be still valid for another
            // CACHE_TTL ms
            Thread.sleep(CACHE_TTL * 2);
            Assert.assertEquals(
                new HashSet<>(
                    Arrays.asList(
                        new HttpHost(NEW_PREFIX + LOCALHOST, PORT),
                        new HttpHost(NEW_PREFIX + HOLOCAUST, PORT + 1))),
                new HashSet<>(
                    client.execute(
                        new User(PG, new LongPrefix(prefix))).get()));
            System.out.println("Third request processed");
        }
    }
}

