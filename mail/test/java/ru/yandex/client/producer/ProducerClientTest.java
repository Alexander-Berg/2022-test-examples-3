package ru.yandex.client.producer;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.http.test.ChainedHttpResource;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.SlowpokeHttpItem;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.nio.client.SharedConnectingIOReactor;
import ru.yandex.parser.searchmap.SearchMap;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.parser.searchmap.User;
import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.test.util.TestBase;

public class ProducerClientTest extends TestBase {
    private static final int PORT = 15443;
    private static final String PG = "pg";
    private static final String LOCALHOST = "localhost";
    private static final String HOLOCAUST = "holocaust";
    private static final String ALL_DOLLARS = "&all&json-type=dollar";
    private static final long SLEEP_INTERVAL = 100L;
    private static final long SLOWPOKE_TIME = 100L;
    private static final long STOPICOTODIN = 100501L;

    @Test
    public void test() throws Exception {
        String searchMap =
            "pg shards:0-65533,search_port:15443,host:localhost\n"
            + "pg shards:0-10000,search_port:15444,host:holocaust\n"
            + "pg shards:0-10000,json_indexer_port:8082,host:localhost";
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
                        .build(),
                    new SearchMap(
                        new SearchMapConfigBuilder()
                            .content(searchMap)
                            .build())))
        {
            reactor.start();
            client.start();
            String uri1 = "/_status?service=pg&prefix=500&allow_cached"
                + ALL_DOLLARS;
            String uri2 = "/_status?service=pg&prefix=20500&allow_cached"
                + ALL_DOLLARS;
            String hostslist = "[{\"localhost\":100500},{\"holocaust\":100500}"
                + ",{\"other\":100}]";
            producer.add(uri1, HttpStatus.SC_OK, hostslist);
            producer.add(
                uri2,
                HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION,
                hostslist);
            producer.start();
            final long prefix1 = 66034;
            Assert.assertEquals(
                new HashSet<>(
                    Arrays.asList(
                        new HttpHost(LOCALHOST, PORT),
                        new HttpHost(HOLOCAUST, PORT + 1))),
                new HashSet<>(
                    client.execute(
                        new User(PG, new LongPrefix(prefix1))).get()));
            final long prefix2 = 86034;
            Assert.assertEquals(
                Arrays.asList(new HttpHost(LOCALHOST, PORT)),
                client.execute(new User(PG, new LongPrefix(prefix2))).get());
        }
    }

    @Test
    public void testWithInfo() throws Exception {
        String searchMap =
            "pg shards:0-65533,search_port:15444,host:localhost\n"
            + "pg shards:0-10000,search_port:15445,host:holocaust\n"
            + "pg shards:0-10000,json_indexer_port:8083,host:localhost";
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
                        .build(),
                    new SearchMap(
                        new SearchMapConfigBuilder()
                            .content(searchMap)
                            .build())))
        {
            reactor.start();
            client.start();
            String uri1 = "/_status?service=pg&prefix=501&allow_cached"
                + ALL_DOLLARS;
            String uri2 = "/_status?service=pg&prefix=20501&allow_cached"
                + ALL_DOLLARS;
            String hostslist = "[{\"localhost\":100501},{\"holocaust\":100501}"
                + ",{\"other\":101}]";
            producer.add(uri1, HttpStatus.SC_OK, hostslist);
            producer.add(
                uri2,
                HttpStatus.SC_NON_AUTHORITATIVE_INFORMATION,
                hostslist);
            producer.start();
            final long prefix1 = 66035;
            Assert.assertEquals(
                new HashSet<>(
                    Arrays.asList(
                        new QueueHostInfo(
                            new HttpHost(LOCALHOST, PORT + 1), STOPICOTODIN),
                        new QueueHostInfo(
                            new HttpHost(HOLOCAUST, PORT + 2), STOPICOTODIN))),
                new HashSet<>(
                    client.executeWithInfo(
                        new User(PG, new LongPrefix(prefix1))).get()));
            final long prefix2 = 86035;
            Assert.assertEquals(
                Arrays.asList(new HttpHost(LOCALHOST, PORT + 1)),
                client.execute(new User(PG, new LongPrefix(prefix2))).get());
        }
    }

    @Test
    public void testDirectRequests() throws Exception {
        try (StaticServer zoo1 = new StaticServer(Configs.baseConfig("Zoo1"));
            StaticServer zoo2 = new StaticServer(Configs.baseConfig("Zoo2"));
            StaticServer zoo3 = new StaticServer(Configs.baseConfig("Zoo3"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            ProducerClient client =
                new ProducerClient(
                    reactor,
                    new ProducerClientConfigBuilder()
                        .host(new HttpHost(LOCALHOST, 1))
                        .connections(2 + 2)
                        .build(),
                    new SearchMap(
                        new SearchMapConfigBuilder()
                            .content(
                                "pg shards:0-65533,search_port:2,host:other,"
                                + "zk:localhost:1/" + zoo1.port()
                                + "|localhost:2/" + zoo2.port()
                                + "|localhost:3/" + zoo3.port()
                                + "\npg shards:0-65533,search_port:2,host:last"
                                + ",zk:localhost:1/" + zoo1.port() + '|'
                                + "localhost:2/" + zoo2.port() + '|'
                                + "localhost:3/" + zoo3.port())
                            .build())))
        {
            reactor.start();
            client.start();
            String uri = "/_status?service=pg&prefix=42&all&json-type=dollar";
            String hostslist =
                "[{\"localhost\":100500},{\"other\":100},{\"last\":2}]";
            zoo1.add(uri, hostslist);
            zoo2.add(uri, hostslist);
            zoo3.add(uri, hostslist);
            zoo1.start();
            zoo2.start();
            zoo3.start();
            final long prefix = 42;
            Assert.assertEquals(
                Arrays.asList(new HttpHost("other", 2)),
                client.execute(new User(PG, new LongPrefix(prefix))).get());
            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(1, zoo1.accessCount(uri));
            Assert.assertEquals(1, zoo2.accessCount(uri));
            Assert.assertEquals(1, zoo3.accessCount(uri));
        }
    }

    @Test
    public void testDirectRequestsMaxTotalTime() throws Exception {
        try (StaticServer zoo1 = new StaticServer(Configs.baseConfig("ZoO1"));
            StaticServer zoo2 = new StaticServer(Configs.baseConfig("ZoO2"));
            StaticServer zoo3 = new StaticServer(Configs.baseConfig("ZoO3"));
            SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                Configs.baseConfig(),
                Configs.dnsConfig());
            ProducerClient client =
                new ProducerClient(
                    reactor,
                    new ProducerClientConfigBuilder()
                        .host(new HttpHost(LOCALHOST, 1))
                        .connections(2 + 2)
                        .maxTotalTime((int) SLOWPOKE_TIME / 2)
                        .fallbackToSearchMap(true)
                        .build(),
                    new SearchMap(
                        new SearchMapConfigBuilder()
                            .content(
                                "pg shards:0-65533,search_port:2,host:other1,"
                                + "zk:localhost:2/" + zoo1.port()
                                + "|localhost:13/" + zoo2.port()
                                + "|localhost:14/" + zoo3.port())
                            .build())))
        {
            reactor.start();
            client.start();
            String uri = "/_status?service=pg&prefix=43&all&json-type=dollar";
            String hostslist =
                "[{\"localhost\":100500},{\"other1\":100},{\"last2\":3}]";
            final SlowpokeHttpItem slowResponse =
                new SlowpokeHttpItem(
                    new StaticHttpItem(hostslist),
                    SLOWPOKE_TIME);
            zoo1.add(uri, slowResponse);
            zoo2.add(uri, slowResponse);
            zoo3.add(uri, slowResponse);
            zoo1.start();
            zoo2.start();
            zoo3.start();
            final long prefix = 43;
            Assert.assertEquals(
                Arrays.asList(new HttpHost("other1", 2)),
                client.execute(new User(PG, new LongPrefix(prefix))).get());
            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(1, zoo1.accessCount(uri));
            Assert.assertEquals(1, zoo2.accessCount(uri));
            Assert.assertEquals(1, zoo3.accessCount(uri));
        }
    }

    @Test
    public void testUnknownZooHost() throws Exception {
        try (StaticServer zoo1 = new StaticServer(Configs.baseConfig("Zoo1"));
             StaticServer zoo2 = new StaticServer(Configs.baseConfig("Zoo2"));
             SharedConnectingIOReactor reactor = new SharedConnectingIOReactor(
                 Configs.baseConfig(),
                 Configs.dnsConfig());
             ProducerClient client =
                 new ProducerClient(
                     reactor,
                     new ProducerClientConfigBuilder()
                         .host(new HttpHost(LOCALHOST, 1))
                         .connections(2 + 2)
                         .fallbackToSearchMap(true)
                         .build(),
                     new SearchMap(
                         new SearchMapConfigBuilder()
                             .content(
                                 "pg shards:0-65533,search_port:2,host:other,"
                                     + "zk:localhost:1/" + zoo1.port()
                                     + "|localhost:2/" + zoo2.port()
                                     + "|unknwonhost:3/" + zoo1.port()
                                     + "\npg shards:0-65533,search_port:2,host:last"
                                     + ",zk:localhost:1/" + zoo1.port() + '|'
                                     + "localhost:2/" + zoo2.port() + '|'
                                     + "unknwonhost:3/" + zoo1.port())
                             .build())))
        {
            reactor.start();
            client.start();
            String uri = "/_status?service=pg&prefix=42&all&json-type=dollar";
            String hostslist =
                "[{\"localhost\":100500},{\"other\":100},{\"last\":2}]";
            zoo1.add(
                uri,
                new ChainedHttpResource(
                    new StaticHttpItem(hostslist),
                    new SlowpokeHttpItem(
                        new StaticHttpItem(HttpStatus.SC_SERVICE_UNAVAILABLE),
                        60)));
            zoo2.add(
                uri,
                new ChainedHttpResource(
                    new StaticHttpItem(hostslist),
                    new SlowpokeHttpItem(
                        new StaticHttpItem(HttpStatus.SC_SERVICE_UNAVAILABLE),
                        60)));
            zoo1.start();
            zoo2.start();
            final long prefix = 42;
            Assert.assertEquals(
                Arrays.asList(new HttpHost("other", 2)),
                client.execute(new User(PG, new LongPrefix(prefix))).get());
            Thread.sleep(SLEEP_INTERVAL);
            Assert.assertEquals(1, zoo1.accessCount(uri));
            Assert.assertEquals(1, zoo2.accessCount(uri));

            // fallback to searchmap
            Assert.assertEquals(
                Arrays.asList(new HttpHost("last", 2), new HttpHost("other", 2)),
                client.execute(new User(PG, new LongPrefix(prefix))).get());
        }
    }
}

