package ru.yandex.parser.searchmap;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpHost;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.search.prefix.LongPrefix;
import ru.yandex.search.prefix.StringPrefix;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class SearchMapTest extends TestBase {
    private static final int JSON_PORT = 8082;
    private static final int SEARCH_PORT = 8088;
    private static final int HTTP_PORT = 80;
    private static final String LOCALHOST = "localhost";
    private static final String YARU = "ya.ru";
    private static final String YANDEXRU = "yandex.ru";
    private static final String MDB160 = "mdb160";

    public SearchMapTest() {
        super(false, 0L);
    }

    private ImmutableSearchMapConfig config(final String content)
        throws Exception
    {
        return config(content, false);
    }

    private ImmutableSearchMapConfig config(
        final String content,
        final boolean ignoreSkipSearch)
        throws Exception
    {
        return new SearchMapConfigBuilder()
            .content(content)
            .ignoreSkipSearch(ignoreSkipSearch)
            .build();
    }

    @Test
    public void test() throws Exception {
        SearchMap searchMap = new SearchMap(config(
            "mdb160 host:localhost,json_indexer_port:8082,search_port:8088"
            + ",shards:0-9999\n"
            + "mdb160 host:ya.ru,raw_memory_indexer_port:8080"
            + ",json_indexer_port:8082,search_port:8088,shards:10000-65533\n"
            + "mdb303 host:localhost,json_indexer_port:8082,"
            + "raw_memory_indexer_port:8080,search_port:8088,shards:0-65533\n"
            + "mdb10,mdb11 host:yandex.ru,raw_memory_indexer_port:8082,"
            + "search_port:8088,shards:0-65533\n"));
        String mdb10 = "mdb10";
        String mdb11 = "mdb11";
        String mdb303 = "mdb303";
        Assert.assertEquals(
            new HashSet<String>(Arrays.asList(mdb10, mdb11, MDB160, mdb303)),
            searchMap.names());
        Assert.assertEquals(
            new HashSet<HttpHost>(
                Arrays.asList(
                    new HttpHost(LOCALHOST, JSON_PORT),
                    new HttpHost(YARU, JSON_PORT),
                    new HttpHost(YANDEXRU, JSON_PORT))),
            searchMap.indexerHosts());
        Assert.assertEquals(
            new HashSet<HttpHost>(
                Arrays.asList(
                    new HttpHost(LOCALHOST, SEARCH_PORT),
                    new HttpHost(YARU, SEARCH_PORT),
                    new HttpHost(YANDEXRU, SEARCH_PORT))),
            searchMap.searchHosts());
        Assert.assertEquals(
            Collections.singletonList(new HttpHost(LOCALHOST, JSON_PORT)),
            searchMap.indexerHosts(new User(MDB160, new LongPrefix(0L))));
        Assert.assertEquals(
            Collections.singletonList(new HttpHost(YANDEXRU, SEARCH_PORT)),
            searchMap.searchHosts(new User(mdb10, new LongPrefix(0L))));
        Assert.assertEquals(
            searchMap.hosts(new User(mdb10, new LongPrefix(0L))),
            searchMap.hosts(new User(mdb11, new LongPrefix(0L))));
        Assert.assertEquals(
            Collections.emptyList(),
            searchMap.indexerHosts(new User("mdb100500", new LongPrefix(0L))));
        Assert.assertEquals(
            Collections.emptyList(),
            searchMap.searchHosts(
                new User('m' + "db" + '1', new LongPrefix(0L))));
        // test interning
        final long last = 9999L;
        SearchMapShard first =
            searchMap.hosts(new User(MDB160, new LongPrefix(0L)));
        SearchMapShard second =
            searchMap.hosts(new User(MDB160, new LongPrefix(last)));
        SearchMapShard third =
            searchMap.hosts(new User(mdb303, new LongPrefix(0L)));
        Assert.assertSame(first, second);
        Assert.assertSame(first, third);
    }

    @Test
    public void testWrongComponentsNumber() throws Exception {
        try {
            new SearchMap(config("\n# comment\n\nsomething wrong here"));
            Assert.fail();
        } catch (ParseException e) {
            return;
        }
    }

    @Test
    public void testNoHost() throws Exception {
        String line = "mdb160 hsto:localhost";
        try {
            new SearchMap(config(line));
            Assert.fail();
        } catch (ParseException e) {
            return;
        }
    }

    @Test
    public void testNoSearchPort() throws Exception {
        SearchMap searchMap = new SearchMap(config(
            "mdb160 host:localhost,json_indexer_port:80,shards:0-0\n"));
        Assert.assertEquals(
            Collections.emptyList(),
            searchMap.searchHosts(new User(MDB160, new LongPrefix(0L))));
        Assert.assertEquals(
            Collections.singletonList(new HttpHost(LOCALHOST, HTTP_PORT)),
            searchMap.indexerHosts(new User(MDB160, new LongPrefix(0L))));
        Assert.assertEquals(Collections.emptySet(), searchMap.searchHosts());
    }

    @Test
    public void testNoIndexerPort() throws Exception {
        SearchMap searchMap = new SearchMap(config(
            "mdb160 host:localhost,search_port:80,shards:0-0\n"));
        Assert.assertEquals(
            Collections.singletonList(new HttpHost(LOCALHOST, HTTP_PORT)),
            searchMap.searchHosts(new User(MDB160, new LongPrefix(0L))));
        Assert.assertEquals(
            Collections.emptyList(),
            searchMap.indexerHosts(new User(MDB160, new LongPrefix(0L))));
        Assert.assertEquals(Collections.emptySet(), searchMap.indexerHosts());
    }

    @Test
    public void testNoShards() throws Exception {
        String line = "mdb10 host:localhost,json_indexer_port:1,search_port:1";
        try {
            new SearchMap(config(line));
            Assert.fail();
        } catch (ParseException e) {
            return;
        }
    }

    @Test
    public void testBadShards() throws Exception {
        String line =
            "m1 host:localhost,search_port:1,json_indexer_port:8000,shards:10";
        try {
            new SearchMap(config(line));
            Assert.fail();
        } catch (ParseException e) {
            return;
        }
    }

    @Test
    public void testBadPort() throws Exception {
        String line =
            "m1 host:localhost,json_indexer_port:hi,search_port:1,shards:0-"
            + (SearchMap.SHARDS_COUNT - 1);
        try {
            new SearchMap(config(line));
            Assert.fail();
        } catch (ParseException e) {
            YandexAssert.assertInstanceOf(
                NumberFormatException.class,
                e.getCause());
        }
    }

    @Test
    public void testBadReload() throws Exception {
        String line = "mdb160 host:localhost,json_indexer_port:1,"
            + "raw_memory_indexer_port:hi,search_port_ng:2,search_port:3,"
            + "skip_search:yes,shards:0-" + (SearchMap.SHARDS_COUNT - 1)
            + ",something:10:20,put-in:lalala,zk:host1:12/20|host2:33";
        boolean passed = false;
        try {
            SearchMap searchMap = new SearchMap(config(line));
            Assert.assertEquals(
                Collections.emptyList(),
                searchMap.searchHosts(new User(MDB160, new LongPrefix(1L))));
            Assert.assertEquals(
                Collections.singletonList(new HttpHost(LOCALHOST, 1)),
                searchMap.indexerHosts(new User(MDB160, new LongPrefix(1L))));
            searchMap = new SearchMap(config(line, true));
            Assert.assertEquals(
                Collections.singletonList(new HttpHost(LOCALHOST, 2)),
                searchMap.searchHosts(new User(MDB160, new LongPrefix(1L))));
            Set<SearchMapHost> hosts = searchMap.hosts();
            YandexAssert.assertSize(1, hosts);
            SearchMapHost host = hosts.iterator().next();
            Assert.assertEquals(new HttpHost(LOCALHOST, 2), host.searchHost());
            Assert.assertEquals(
                new HttpHost(LOCALHOST, 1),
                host.indexerHost());
            // CSOFF: MagicNumber
            Assert.assertEquals(
                searchMap.hosts(new User(MDB160, new LongPrefix(0L))).zk(),
                Arrays.asList(
                    new ZooKeeperAddress(new HttpHost("host1", 20), 12),
                    new ZooKeeperAddress(new HttpHost("host2", 83), 33)));
            // CSON: MagicNumber

            passed = true;
            searchMap.reload();
            Assert.fail();
        } catch (NullPointerException e) {
            Assert.assertTrue(passed);
        }
    }

    @Test
    public void testPrefixType() throws Exception {
        SearchMap searchMap = new SearchMap(config(
            "mdb200 host:localhost,json_indexer_port:8082,search_port:8088"
            + ",shards:0-9997,prefix_type:string\n"
            + "mdb200 host:ya.ru,prefix_type:string"
            + ",json_indexer_port:8082,search_port:8088,shards:9998-65533\n"
            + "mdb1 host:ya.ru,prefix_type:long,search_port:80,shards:0-10"));
        String mdb200 = "mdb200";
        Assert.assertEquals(
            Collections.singletonList(new HttpHost(YARU, JSON_PORT)),
            searchMap.indexerHosts(new User(mdb200, new StringPrefix("123"))));
        // Check prefix type mismatch on lookup
        Assert.assertEquals(
            Collections.emptyList(),
            searchMap.indexerHosts(new User(mdb200, new LongPrefix(1L))));
    }

    @Test
    public void testPrefixTypeMismatch() throws Exception {
        try {
            new SearchMap(config(
                "mdb202 host:localhost,json_indexer_port:8083,search_port:8088"
                + ",shards:0-9998,prefix_type:string\n"
                + "mdb202 host:ya.ru,search_port:8088,shards:9999-65533"));
            Assert.fail();
        } catch (ParseException e) {
            return;
        }
    }

    @Test
    public void testMalformedLineWithoutSpace() throws Exception {
        try {
            new SearchMap(
                config("md200host:host,json_indexer_port:8,search_port:9\n"));
            Assert.fail();
        } catch (ParseException e) {
            return;
        }
    }
}

