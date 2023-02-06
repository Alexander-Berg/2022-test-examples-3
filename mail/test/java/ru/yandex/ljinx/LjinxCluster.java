package ru.yandex.ljinx;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.collection.IntInterval;
import ru.yandex.collection.IntIntervalSet;
import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHttpItem;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.ljinx.function.LjinxFunctionFactory;
import ru.yandex.logger.BackendAccessLoggerConfigDefaults;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class LjinxCluster implements GenericAutoCloseable<IOException> {
    public static final String NO_CACHE_PLEASE = "no-cAche-please";
    public static final String NO_CACHE = "nocache";
    public static final String MEMCACHE = "memcache";
    public static final String LUCENE = "lucene-test";
    public static final String LUCENE2 = "lucene-test2";
    public static final String MDSCACHE = "mdscache";
    public static final String PP_URL1 = "/test1";
    public static final String PP_URL2 = "/test2";
    public static final String PP_URL3 = "/test3*";
    public static final String PP_URL4 = "/test4";
    public static final String PP_URL5 = "/test5";
    public static final String PP_URL6 = "/test6";
    public static final String PP_URL7 = "/test7";
    public static final String PP_MDS_URL = "/test-mds";
    public static final String PP_PASS_HEADERS_URL = "/test-pass-headers";
    public static final String PP_SIBLINGS = "/test-siblings";
    public static final String PP_SIBLINGS_HASH = "/test-hash-siblings";
    public static final String PP_SIBLINGS_SEARCHMAP =
        "/test-searchmap-siblings";
    public static final String PP_SIBLINGS_SET_PORT =
        "/test-searchmap-set-port";
    public static final String PP_SIBLINGS_CONCAT = "/test-concat-siblings";
    public static final String PP_SIBLINGS_RANDOM =
        "/test-random-shuffle-siblings";
    public static final String PP_SIBLINGS_TRUNCATE =
        "/test-truncate-siblings";
    public static final String PP_HITS_LOADING = "/test-load-hits-to-memory";
    public static final String PP_UNIQUE_ID = "/test-unique-id";
    public static final String PP_GZIP = "/test-gzip";
    public static final String PP_REPLACEMENT = "/test-replacement";
    public static final String PP_REPLACEMENT_NEW_PATH = "/new/path";
    public static final String PP_REPLACEMENT2 = "/test-test-replacement";
    public static final String PP_REPLACEMENT_NEW_PATH2 = "/replacement";
    public static final String PP_REPLACEMENT3 = "/extra/path";
    public static final String UNIQUE_ID = "unique-id()";
    public static final String X_MY_HEADER = "X-My-Header";
    public static final int SIBLINGS_TIMEOUT = 2000;

    public static final String MDS_NAMESPACE = "ps-cache";
    public static final String MDS_STID =
        "320.mail:01010101.E1010101:123456789101112131415161718192";
    public static final String MDS_TVM_CLIENT_ID = "2";
    public static final String MDS_TVM_SECRET = "1234567890123456789012";
    public static final String MDS_TVM2_TICKET = "3:serv:MDSSTORA";
    public static final Header MDS_TVM2_HEADER =
        new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, MDS_TVM2_TICKET);
    public static final String TVM_TICKET = "2:53:1522749939:116:CDcQ";
    public static final long MDS_STORE_TTL = LuceneCacheStorageConfig.DEFAULT_MINIMAL_TTL + 1L;
    public static final int LONG_STRING_SIZE = 20000;

    private static final int CONNECTIONS = 1000;
    private static final int TIMEOUT = 10000;
    private static final int SIBLINGS_COUNT = 4;

    private static final long TVM_RENEWAL_INTERVAL = 60000L;

    private final StaticServer upstream1;
    private final StaticServer upstream2;
    private final StaticServer gzipUpstream;
    private final StaticServer mdsRead;
    private final StaticServer mdsWrite;
    private final StaticServer tvm2;
    private final TestSearchBackend lucene;
    private final List<StaticServer> ljinxProxies;
    private final List<Ljinx> ljinxs;
    private final GenericAutoCloseableChain<IOException> chain;

    public LjinxCluster(final TestBase testBase, final boolean useLucene)
        throws Exception
    {
        this(testBase, useLucene, false);
    }

    //CSOFF: MethodLength
    public LjinxCluster(
        final TestBase testBase,
        final boolean useLucene,
        final boolean gzip)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            upstream1 = new StaticServer(Configs.baseConfig("Upstream1"));
            chain.get().add(upstream1);

            upstream2 = new StaticServer(Configs.baseConfig("Upstream2"));
            chain.get().add(upstream2);

            gzipUpstream =
                new StaticServer(
                    new BaseServerConfigBuilder(
                        Configs.baseConfig("GzipUpstream"))
                        .gzip(true)
                        .build());
            chain.get().add(gzipUpstream);

            mdsRead =
                new StaticServer(Configs.baseConfig("MdsRead"));
            chain.get().add(mdsRead);
            mdsRead.add("/get-" + MDS_NAMESPACE + '/' + MDS_STID, genStringData());
            mdsRead.start();

            mdsWrite =
                new StaticServer(Configs.baseConfig("MdsWrite"));
            chain.get().add(mdsWrite);
            mdsWrite.add(
                "/upload-" + MDS_NAMESPACE + "/*",
                new ExpectingHttpItem(genStringData(), mdsResponse()));
            mdsWrite.start();

            tvm2 = new StaticServer(Configs.baseConfig("tvm2"));
            chain.get().add(tvm2);
            tvm2.add("/ticket/", TVM_TICKET);
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"1\":{\"ticket\":\"" + MDS_TVM2_TICKET + "\"}}");
            tvm2.start();

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            Tvm2ClientConfigBuilder tvm2ClientConfig =
                new Tvm2ClientConfigBuilder()
                    .destinationClientId(MDS_TVM_CLIENT_ID)
                    .renewalInterval(TVM_RENEWAL_INTERVAL);

            LjinxConfigBuilder builder = new LjinxConfigBuilder();
            builder.tvm2ServiceConfig(tvm2ServiceConfig);

            ProxyPassesConfigBuilder ppcb = new ProxyPassesConfigBuilder();
            ppcb.asterisk(
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream1.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(NO_CACHE));
            ppcb.proxyPass(
                new Pattern<>(PP_URL1, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream1.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(NO_CACHE));
            ppcb.proxyPass(
                new Pattern<>(PP_URL2, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .noCacheHeaders(Collections.singleton(NO_CACHE_PLEASE)));
            if (useLucene) {
                ppcb.proxyPass(
                    new Pattern<>(
                        Character.toString(':') + upstream2.port() + "/test3",
                        true),
                    new ProxyPassConfigBuilder()
                        .connections(CONNECTIONS)
                        .timeout(TIMEOUT)
                        .host(upstream2.host())
                        .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                        .cacheStorage(LUCENE)
                        .cacheCodes(
                            IntIntervalSet.create(
                                Arrays.asList(
                                    new IntInterval(
                                        HttpStatus.SC_OK,
                                        HttpStatus.SC_OK))))
                        .cacheCodeTTL(
                            HttpStatus.SC_CONFLICT,
                            TimeUnit.SECONDS.toMillis(2))
                        .noCacheHeaders(
                            Collections.singleton(NO_CACHE_PLEASE)));
                ppcb.proxyPass(
                    new Pattern<>(PP_URL6, false),
                    new ProxyPassConfigBuilder()
                        .connections(CONNECTIONS)
                        .timeout(TIMEOUT)
                        .host(upstream2.host())
                        .cacheKey(
                            LjinxFunctionFactory.INSTANCE.apply(
                                "request(X-my-header)"))
                        .cacheStorage(LUCENE)
                        .cacheCodes(
                            IntIntervalSet.create(
                                Arrays.asList(
                                    new IntInterval(
                                        HttpStatus.SC_OK,
                                        HttpStatus.SC_CREATED))))
                        .passHeaders(
                            Collections.singletonList("x-My-header")));
                ppcb.proxyPass(
                    new Pattern<>(PP_URL7, false),
                    new ProxyPassConfigBuilder()
                        .connections(CONNECTIONS)
                        .timeout(TIMEOUT)
                        .host(upstream2.host())
                        .cacheKey(
                            LjinxFunctionFactory.INSTANCE.apply(
                                "request-md5(x-my-header)"))
                        .cacheStorage(LUCENE2)
                        .cacheCodes(
                            IntIntervalSet.create(
                                Arrays.asList(
                                    new IntInterval(
                                        HttpStatus.SC_OK,
                                        HttpStatus.SC_CREATED))))
                        .passHeaders(
                            Collections.singletonList("x-my-Header")));
                ppcb.proxyPass(
                    new Pattern<>(
                        Character.toString(':') + upstream2.port()
                        + PP_HITS_LOADING,
                        false),
                    new ProxyPassConfigBuilder()
                        .connections(CONNECTIONS)
                        .timeout(TIMEOUT)
                        .host(upstream2.host())
                        .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                        .cacheStorage(LUCENE2)
                        .cacheCodes(
                            IntIntervalSet.create(
                                Arrays.asList(
                                    new IntInterval(
                                        HttpStatus.SC_OK,
                                        HttpStatus.SC_CONFLICT))))
                        .cacheCodeTTL(
                            HttpStatus.SC_CONFLICT,
                            TimeUnit.SECONDS.toMillis(2))
                        .noCacheHeaders(
                            Collections.singleton(NO_CACHE_PLEASE)));
                ppcb.proxyPass(
                    new Pattern<>(
                        Character.toString(':') + upstream2.port()
                        + PP_MDS_URL,
                        false),
                    new ProxyPassConfigBuilder()
                        .connections(CONNECTIONS)
                        .timeout(TIMEOUT)
                        .host(upstream2.host())
                        .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                        .cacheStorage(MDSCACHE)
                        .cacheCodes(
                            IntIntervalSet.create(
                                Arrays.asList(
                                    new IntInterval(
                                        HttpStatus.SC_OK,
                                        HttpStatus.SC_CONFLICT))))
                        .cacheCodeTTL(
                            HttpStatus.SC_CONFLICT,
                            TimeUnit.SECONDS.toMillis(2)));
            }
            //proxied
            ppcb.proxyPass(
                new Pattern<>(
                    Character.toString(':') + upstream2.port() + PP_URL4,
                    false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CONFLICT))))
                    .cacheCodeTTL(
                        HttpStatus.SC_CONFLICT,
                        TimeUnit.SECONDS.toMillis(2))
                    .noCacheHeaders(Collections.singleton(NO_CACHE_PLEASE)));
            ppcb.proxyPass(
                new Pattern<>(
                    Character.toString(':') + upstream2.port() + PP_URL5,
                    false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(
                        LjinxFunctionFactory.INSTANCE.apply(
                            "get(param1,get(param2,qwe))"))
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CONFLICT))))
                    .cacheCodeTTL(
                        HttpStatus.SC_CONFLICT,
                        TimeUnit.SECONDS.toMillis(2))
                    .noCacheHeaders(Collections.singleton(NO_CACHE_PLEASE)));
            ppcb.proxyPass(
                new Pattern<>(PP_PASS_HEADERS_URL, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(
                        LjinxFunctionFactory.INSTANCE.apply(
                            "request-md5()"))
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CONFLICT))))
                    .cacheCodeTTL(
                        HttpStatus.SC_CONFLICT,
                        TimeUnit.SECONDS.toMillis(2))
                    .noCacheHeaders(Collections.singleton(NO_CACHE_PLEASE))
                    .passHeaders(
                        Collections.singletonList(HttpHeaders.AUTHORIZATION)));
            ppcb.proxyPass(
                new Pattern<>(PP_UNIQUE_ID, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(LjinxFunctionFactory.INSTANCE.apply(UNIQUE_ID))
                    .cacheStorage(NO_CACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED)))));

            if (useLucene) {
                lucene = new TestSearchBackend(
                    testBase,
                    new File(
                        Paths.getSourcePath(
                            "mail/library/http/ljinx/test/resources"
                            + "/search_backend.conf")));
                chain.get().add(lucene);
            } else {
                lucene = null;
            }

            CacheStoragesConfigBuilder cscb = new CacheStoragesConfigBuilder();
            cscb.cacheStorage(
                NO_CACHE,
                CacheStorageType.NULL.create(
                    null,
                    null));
            cscb.cacheStorage(
                MEMCACHE,
                CacheStorageType.MEMORY.create(
                    new IniConfig(new StringReader("capacity = 10M")),
                    MemoryCacheStorageConfig.DEFAULTS));
            if (useLucene) {
                String config =
                    "capacity = 10M\n"
                    + "search.connections = 10\n"
                    + "search.host = localhost:"
                        + lucene.searchPort() + '\n'
                    + "search.timeout = 10s\n"
                    + "index.connections = 10\n"
                    + "index.host = localhost:"
                        + lucene.indexerPort() + '\n'
                    + "index.timeout = 10s\n";
                cscb.cacheStorage(
                    LUCENE,
                    CacheStorageType.LUCENE.create(
                        new IniConfig(new StringReader(config)),
                        MemoryCacheStorageConfig.DEFAULTS));
                cscb.cacheStorage(
                    LUCENE2,
                    CacheStorageType.LUCENE.create(
                        new IniConfig(
                            new StringReader(
                                "load-hits-to-memory = true\n" + config)),
                        MemoryCacheStorageConfig.DEFAULTS));
                String mdsConfig = config
                    + "to-write.host = localhost:" + mdsWrite.port() + '\n'
                    + "to-write.connections = " + CONNECTIONS + '\n'
                    + "to-read.host = localhost:" + mdsRead.port() + '\n'
                    + "to-read.connections = " + CONNECTIONS + '\n'
                    + "namespace = " + MDS_NAMESPACE + '\n'
                    + "store-ttl = " + MDS_STORE_TTL + '\n'
                    + "tvm2.destination-client-id = "
                    + MDS_TVM_CLIENT_ID + '\n'
                    + "tvm2.renewal-interval = "
                    + TVM_RENEWAL_INTERVAL + '\n'
                    + "tvm2.client-id = " + MDS_TVM_CLIENT_ID + '\n'
                    + "tvm2.secret = " + MDS_TVM_SECRET + '\n'
                    + '\n' + "tvm2.host = localhost:" + tvm2.port() + '\n'
                    + "tvm2.connections = " + CONNECTIONS + '\n';
                MdsCacheStorageConfig mdsCacheStorageConfig =
                    (MdsCacheStorageConfig) CacheStorageType.MDS.create(
                        new IniConfig(new StringReader(mdsConfig)),
                        LuceneCacheStorageConfig.DEFAULTS);
                mdsCacheStorageConfig.tvm2ServiceConfig(tvm2ServiceConfig);
                mdsCacheStorageConfig.tvm2ClientConfig(tvm2ClientConfig);
                cscb.cacheStorage(MDSCACHE, mdsCacheStorageConfig);
            }
            builder.cacheStoragesConfig(cscb);
            builder.port(0);
            builder.connections(CONNECTIONS);
            builder.timeout(TIMEOUT);
            builder.workers(2 + 2);
            builder.gzip(gzip);

            ljinxProxies = new ArrayList<>(SIBLINGS_COUNT);
            StringBuilder siblings = new StringBuilder();
            StringBuilder searchMap = new StringBuilder();
            for (int i = 0; i < SIBLINGS_COUNT; ++i) {
                StaticServer proxy =
                    new StaticServer(Configs.baseConfig("Proxy" + i));
                chain.get().add(proxy);
                ljinxProxies.add(proxy);
            }
            ljinxProxies.sort(ProxiesComparator.INSTANCE);

            for (int i = 0; i < SIBLINGS_COUNT; ++i) {
                if (i != 0) {
                    siblings.append(',');
                }
                StaticServer proxy = ljinxProxies.get(i);
                HttpHost host = proxy.host();
                siblings.append(host);
                searchMap.append("change_log host:");
                searchMap.append(host.getHostName());
                searchMap.append(",search_port_ng:");
                searchMap.append(host.getPort() - 2);
                searchMap.append(",shards:0-65533\n");
            }
            searchMap.append(
                "change_log2 host:localhost,search_port_ng:0,shards:0-65533");
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(
                        LjinxFunctionFactory.INSTANCE.apply(
                            "request(X-Ya-Service-Ticket)"))
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "shuffle-hosts(" + siblings
                                    + ",mod(get(uid,get(suid)),"
                                    + SIBLINGS_COUNT + ')'))
                            .passHeaders(
                                Collections.singletonList(
                                    YandexHeaders.X_YA_SERVICE_TICKET))));
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS_HASH, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "shuffle-hosts" + '(' + siblings
                                    + ",hash(get(stid)))"))));
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS_SEARCHMAP, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .searchMapConfig(
                        new SearchMapConfigBuilder()
                            .content(new String(searchMap)))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "increment-port(searchmap-hosts(change_log"
                                    + ",hash(get(stid))),2)"))));
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS_SET_PORT, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .searchMapConfig(
                        new SearchMapConfigBuilder()
                            .content(new String(searchMap)))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "set-port(searchmap-hosts(change_log2"
                                    + ",hash(get(stid))),"
                                    + ljinxProxies.get(0).host().getPort()
                                    + ')'))));
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS_CONCAT, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "concat-hosts(get(lhs),get(rhs))"))));
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS_RANDOM, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(
                        LjinxFunctionFactory.INSTANCE.apply("request(A)"))
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "random-shuffle-hosts(" + siblings
                                    + ",4)"))
                            .passHeaders(
                                Collections.singletonList(
                                    YandexHeaders.X_YA_SERVICE_TICKET))));
            ppcb.proxyPass(
                new Pattern<>(PP_SIBLINGS_TRUNCATE, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream2.host())
                    .cacheKey(
                        LjinxFunctionFactory.INSTANCE.apply("request(B)"))
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .siblingsConfig(
                        new SiblingsConfigBuilder()
                            .connections(CONNECTIONS)
                            .timeout(SIBLINGS_TIMEOUT)
                            .hosts(
                                LjinxFunctionFactory.INSTANCE.apply(
                                    "truncate-hosts(shuffle-hosts(" + siblings
                                    + ",0),1)"))
                            .passHeaders(
                                Collections.singletonList(
                                    YandexHeaders.X_YA_SERVICE_TICKET))));
            ppcb.proxyPass(
                new Pattern<>(PP_GZIP, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(gzipUpstream.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(MEMCACHE)
                    .cacheCodes(
                        IntIntervalSet.create(
                            Arrays.asList(
                                new IntInterval(
                                    HttpStatus.SC_OK,
                                    HttpStatus.SC_CREATED))))
                    .noCacheHeaders(Collections.singleton(NO_CACHE_PLEASE)));
            ppcb.proxyPass(
                new Pattern<>(PP_REPLACEMENT, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream1.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(NO_CACHE)
                    .pattern(java.util.regex.Pattern.compile(PP_REPLACEMENT))
                    .replacement(PP_REPLACEMENT_NEW_PATH));
            ppcb.proxyPass(
                new Pattern<>(PP_REPLACEMENT2, false),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream1.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(NO_CACHE)
                    .pattern(java.util.regex.Pattern.compile("t.*st."))
                    .replacement(""));
            ppcb.proxyPass(
                new Pattern<>(PP_REPLACEMENT3 + '/', true),
                new ProxyPassConfigBuilder()
                    .connections(CONNECTIONS)
                    .timeout(TIMEOUT)
                    .host(upstream1.host())
                    .cacheKey(ProxyPassConfigDefaults.INSTANCE.cacheKey())
                    .cacheStorage(NO_CACHE)
                    .pattern(java.util.regex.Pattern.compile(PP_REPLACEMENT3))
                    .replacement(""));
            builder.proxyPassesConfig(ppcb);

            ljinxs = new ArrayList<>(SIBLINGS_COUNT);
            for (int i = 0; i < SIBLINGS_COUNT; ++i) {
                Ljinx ljinx = new Ljinx(builder.name("Ljinx" + i).build());
                chain.get().add(ljinx);
                ljinxs.add(ljinx);
                ljinxProxies.get(i).add(
                    "*",
                    new StaticHttpResource(
                        new ProxyHandler(
                            ljinx.host(),
                            YandexHeaders.X_CACHE_STATUS,
                            YandexHeaders.X_LJINX_IGNORE_SIBLINGS,
                            YandexHeaders.X_YA_SERVICE_TICKET,
                            HttpHeaders.ACCEPT_CHARSET,
                            HttpHeaders.ACCEPT_ENCODING,
                            HttpHeaders.REFERER,
                            BackendAccessLoggerConfigDefaults
                                .X_PROXY_SESSION_ID)));
            }
            this.chain = chain.release();
        }
    }
    //CSON: MethodLength

    public StaticServer upstream1() {
        return upstream1;
    }

    public StaticServer upstream2() {
        return upstream2;
    }

    public StaticServer gzipUpstream() {
        return gzipUpstream;
    }

    public StaticServer mdsRead() {
        return mdsRead;
    }

    public StaticServer mdsWrite() {
        return mdsWrite;
    }

    public StaticServer tvm2() {
        return tvm2;
    }

    public Ljinx ljinx() {
        return ljinx(0);
    }

    public Ljinx ljinx(final int i) {
        return ljinxs.get(i);
    }

    public StaticServer ljinxProxy(final int i) {
        return ljinxProxies.get(i);
    }

    public void start() throws IOException {
        upstream1.start();
        upstream2.start();
        gzipUpstream.start();
        for (StaticServer server: ljinxProxies) {
            server.start();
        }
        for (Ljinx ljinx: ljinxs) {
            ljinx.start();
        }
    }

    public TestSearchBackend lucene() {
        return lucene;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public static String genStringData() {
        StringBuilder sb = new StringBuilder();
        sb.append("a".repeat(LONG_STRING_SIZE));
        return new String(sb);
    }

    protected static String mdsResponse() {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding");
        sb.append("=\"utf-8\"?> <post obj=\"" + MDS_NAMESPACE + ".filename1");
        sb.append("\" id=\"1cfb5927c72b10a364a9608163082fc87dedfb8c7beec4863");
        sb.append("6f027435dd418ab5fc71dc3c6c95162b4a334369b6304108520fc6d46");
        sb.append("05767e017c0561f3e2bea2\" groups=\"3\" size=\"100\" key=\"");
        sb.append(MDS_STID + ">\n<complete addr=\"141.8.145.55:1032\" path=");
        sb.append("\"/src/storage/8/data-0.0\" group=\"223\" status=\"0\"/> ");
        sb.append("<complete addr=\"141.8.145.116:1032\" path=\"/srv/storage");
        sb.append("/8/data-0.0\" group=\"221\" status=\"0\"/> <complete addr");
        sb.append("=\"141.8.145.119:1029\" path=\"/srv/storage/5/data-0.0\" ");
        sb.append("group=\"225\" status=\"0\"/> <written>3</written> </post>");
        return new String(sb);
    }

    private enum ProxiesComparator implements Comparator<StaticServer> {
        INSTANCE;

        @Override
        public int compare(final StaticServer a, final StaticServer b) {
            try {
                return Integer.compare(a.port(), b.port());
            } catch (IOException e) {
                return 0;
            }
        }
    }
}

