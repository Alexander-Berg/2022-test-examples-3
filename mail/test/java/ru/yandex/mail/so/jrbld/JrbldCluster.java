package ru.yandex.mail.so.jrbld;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.jniwrapper.JniWrapperConfigBuilder;
import ru.yandex.mail.so.jrbld.config.CidrListSourceConfigBuilder;
import ru.yandex.mail.so.jrbld.config.IpCheckerConfigBuilder;
import ru.yandex.mail.so.jrbld.config.IpListSourceConfigBuilder;
import ru.yandex.mail.so.jrbld.config.IpRangeListSourceConfigBuilder;
import ru.yandex.mail.so.jrbld.config.JrbldConfigBuilder;
import ru.yandex.mail.so.jrbld.config.LuceneSourceConfigBuilder;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class JrbldCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String STATIC_INFO_PROVIDER = "static-info";
    public static final String STATIC_IPV4_LIST = "static-ipv4-list";
    public static final String STATIC_IPV4_RANGE = "static-ipv4-range";
    public static final String STATIC_IPV6_LIST = "static-ipv6-list";
    public static final String STATIC_CIDR_LIST = "static-cidr-list";
    public static final String STATIC_CHECKER = "static-checker";
    public static final String WHITELIST_LUCENE_CHECKER = "lucene-wl";
    public static final String WHITELIST_LUCENE_TYPE = "whitelist";
    public static final String BLACKLIST_LUCENE_CHECKER = "lucene-bl";
    public static final String BLACKLIST_LUCENE_TYPE = "blacklist";
    public static final String IPV4_AND_WL_CHECKER = "ipv4-and-wl-checker";
    public static final String PROXY_IPV4_AND_WL_CHECKER =
        "proxy-ipv4-and-wl-checker";

    public static final String RBL_SERVICE = "rlb";

    public static final long SHARDS = 7;

    private static final String RBL_TYPE = "rbl_type:";

    static {
        System.setProperty("INDEX_DIR", "");
        System.setProperty("SHARDS", "7");
        System.setProperty("OLD_SEARCH_PORT", "");
        System.setProperty("SEARCH_PORT", "");
        System.setProperty("DUMP_PORT", "");
        System.setProperty("INDEXER_PORT", "");
    }

    private final TestSearchBackend lucene;
    private final StaticServer producer;
    private final Jrbld jrbld;
    private final Path cidrList;

    public JrbldCluster(final TestBase testBase) throws Exception {
        this(testBase, true);
    }

    public JrbldCluster(
        final TestBase testBase,
        final boolean enableCheckersStaters)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            lucene = new TestSearchBackend(
                testBase,
                new File(
                    Paths.getSourcePath(
                        "mail/so/daemons/jrbld/jrbld_service_config"
                        + "/files/search_backend.conf")));
            chain.get().add(lucene);

            producer = new StaticServer(Configs.baseConfig("Producer"));
            chain.get().add(producer);
            producer.add(
                "/_status?service=" + RBL_SERVICE + "&prefix=*",
                "[{$localhost\0:100500}]");

            JrbldConfigBuilder builder = new JrbldConfigBuilder();
            builder.port(0);
            builder.connections(2);
            builder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(lucene.searchMapRule(RBL_SERVICE)));
            builder.searchConfig(Configs.targetConfig());
            builder.indexerConfig(Configs.targetConfig());
            builder.producerClientConfig(
                new ProducerClientConfigBuilder()
                    .connections(2)
                    .host(producer.host())
                    .fallbackToSearchMap(false));
            builder.upstreamsConfig(
                new UpstreamsConfigBuilder().asterisk(
                    new UpstreamConfigBuilder().connections(2)));

            builder.enableCheckersStaters(enableCheckersStaters);

            builder.jniInfoSourceConfigs().put(
                STATIC_INFO_PROVIDER,
                new JniWrapperConfigBuilder()
                    .libraryName(
                        Paths.getBuildPath(
                            "mail/library/jniwrapper/dynamic_test"
                            + "/libjniwrapper-test.so"))
                    .ctorName("jniwrapper_test_copy_config_ctor")
                    .dtorName("jniwrapper_test_dtor")
                    .mainName("jniwrapper_test_copy_instance")
                    .config("{\"ip-info\":\"AS119526\"}"));

            Path ipv4List = lucene.root().resolve("ipv4.txt");
            Files.write(
                ipv4List,
                Arrays.asList("4.8"),
                StandardCharsets.UTF_8);
            builder.ipListSourceConfigs().put(
                STATIC_IPV4_LIST,
                new IpListSourceConfigBuilder()
                    .value(1)
                    .file(ipv4List.toFile()));

            Path ipv4Range = lucene.root().resolve("ipv4-range.txt");
            Files.write(
                ipv4Range,
                Arrays.asList(
                    "125.0.0.1",
                    "127.0.0.1",
                    "#125.0.0.2",
                    "125.0.0.3-7",
                    "!125.0.0.4",
                    "125.5",
                    "!125.5.1",
                    "!126.0.0.0",
                    "128.2-6",
                    "!128.4",
                    "!129"),
                StandardCharsets.UTF_8);
            builder.ipRangeListSourceConfigs().put(
                STATIC_IPV4_RANGE,
                new IpRangeListSourceConfigBuilder()
                    .value(2)
                    .file(ipv4Range.toFile()));

            Path ipv6List = lucene.root().resolve("ipv6.txt");
            Files.write(
                ipv6List,
                Arrays.asList(
                    "::1",
                    "#fe80::225:90ff:fec3:be5a",
                    "fe80::225:90ff:fec3:be4a"),
                StandardCharsets.UTF_8);
            builder.ipListSourceConfigs().put(
                STATIC_IPV6_LIST,
                new IpListSourceConfigBuilder()
                    .value(2 + 1)
                    .file(ipv6List.toFile()));

            cidrList = lucene.root().resolve("cidr.txt");
            Files.write(
                cidrList,
                Arrays.asList(
                    "77.88.46.0/23",
                    "77.88.60.0/23",
                    "123.44.33.22",
                    "38.32.01.15"),
                StandardCharsets.UTF_8);
            builder.cidrListSourceConfigs().put(
                STATIC_CIDR_LIST,
                new CidrListSourceConfigBuilder()
                    .value(2 + 2)
                    .file(cidrList.toFile()));

            builder.ipCheckerConfigs().put(
                STATIC_CHECKER,
                new IpCheckerConfigBuilder()
                    .sources(
                        new LinkedHashSet<>(
                            Arrays.asList(
                                STATIC_IPV4_LIST,
                                STATIC_IPV6_LIST))));

            builder.luceneSourceConfigs().put(
                WHITELIST_LUCENE_CHECKER,
                new LuceneSourceConfigBuilder()
                    .value(2 + 2 + 1)
                    .query(RBL_TYPE + WHITELIST_LUCENE_TYPE)
                    .service(RBL_SERVICE)
                    .maxPrefix(SHARDS));

            builder.luceneSourceConfigs().put(
                BLACKLIST_LUCENE_CHECKER,
                new LuceneSourceConfigBuilder()
                    .value(2 + 2 + 2)
                    .query(RBL_TYPE + BLACKLIST_LUCENE_TYPE)
                    .service(RBL_SERVICE)
                    .maxPrefix(SHARDS));

            builder.ipCheckerConfigs().put(
                IPV4_AND_WL_CHECKER,
                new IpCheckerConfigBuilder()
                    .sources(
                        new LinkedHashSet<>(
                            Arrays.asList(
                                STATIC_IPV4_LIST,
                                WHITELIST_LUCENE_CHECKER))));
            builder.ipCheckerConfigs().put(
                PROXY_IPV4_AND_WL_CHECKER,
                new IpCheckerConfigBuilder()
                    .sources(Collections.singleton(IPV4_AND_WL_CHECKER)));

            jrbld = new Jrbld(new JrbldConfigBuilder(builder).build());
            chain.get().add(jrbld);

            reset(chain.release());
        }
    }

    public void start() throws Exception {
        producer.start();
        jrbld.start();
    }

    public TestSearchBackend lucene() {
        return lucene;
    }

    public StaticServer producer() {
        return producer;
    }

    public Jrbld jrbld() {
        return jrbld;
    }

    public Path cidrList() {
        return cidrList;
    }
}

