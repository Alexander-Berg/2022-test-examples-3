package ru.yandex.search.disk.proxy;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.message.BasicHeader;

import ru.yandex.client.producer.ProducerClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.erratum.ErratumConfigBuilder;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.geocoder.GeocoderConfigBuilder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.jniwrapper.JniWrapperConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.parser.searchmap.SearchMapConfigBuilder;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.search.proxy.UpstreamConfigBuilder;
import ru.yandex.search.proxy.UpstreamsConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class
ProxyCluster implements GenericAutoCloseable<IOException> {
    public static final String TVM2_TICKET = "3:serv:CIEaEPLn";
    public static final Header TVM2_HEADER =
        new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, TVM2_TICKET);
    private static final long TVM2_RENEWAL_INTERVAL = 60000L;
    private static final String DISK_SEARCH = "disk-search";
    private static final String ASTERISK = "*";
    private static final long DSSM_THRESHOLD = 6;
    private static final String DELETE = "/delete*";
    private static final String MODIFY = "/modify*";

    private final TestSearchBackend backend;
    private final StaticServer erratum;
    private final StaticServer geocoder;
    private final StaticServer producer;
    private final StaticServer tvm2;
    private final StaticServer djfs;
    private final StaticServer uaas;
    private final Proxy proxy;
    private final GenericAutoCloseableChain<IOException> chain;

    public ProxyCluster(final TestBase testBase) throws Exception {
        this(testBase, false);
    }

    public ProxyCluster(final TestBase testBase, final boolean useProducer)
        throws Exception
    {
        this(testBase, useProducer, false);
    }

    public ProxyCluster(
        final TestBase testBase,
        final ProxyConfig config)
        throws Exception
    {
        this(testBase, config, false, false, Integer.MAX_VALUE);
    }

    public ProxyCluster(
        final TestBase testBase,
        final boolean useProducer,
        final boolean fallbackToSearchMap)
        throws Exception
    {
        this(testBase, useProducer, fallbackToSearchMap, Integer.MAX_VALUE);
    }

    // CSOFF: ParameterNumber
    public ProxyCluster(
        final TestBase testBase,
        final boolean useProducer,
        final boolean fallbackToSearchMap,
        final long fatUserDocs)
        throws Exception
    {
        this(
            testBase,
            ProxyConfigDefaults.INSTANCE,
            useProducer,
            fallbackToSearchMap,
            fatUserDocs);
    }

    public ProxyCluster(
        final TestBase testBase,
        final ProxyConfig config,
        final boolean useProducer,
        final boolean fallbackToSearchMap,
        final long fatUserDocs)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            System.setProperty(
                "LUCENE_DISK_CONFIG_INCLUDE",
                "search_backend_thin.conf");
            System.setProperty(
                "FACE_INDEXER_HOST",
                "localhost");
            backend = new TestSearchBackend(
                testBase,
                new File(
                    Paths.getSourcePath(
                        "mail/search/disk/search_backend_disk_config/files"
                        + "/search_backend.conf")));
            chain.get().add(backend);

            erratum = new StaticServer(Configs.baseConfig("Erratum"));
            chain.get().add(erratum);
            erratum.add(ASTERISK, HttpStatus.SC_SERVICE_UNAVAILABLE);

            geocoder = new StaticServer(Configs.baseConfig("Geocoder"));
            chain.get().add(geocoder);
            geocoder.add(ASTERISK, HttpStatus.SC_SERVICE_UNAVAILABLE);

            String configPath =
                Paths.getSourcePath(
                    "mail/search/disk/yadisk_search_proxy_config/files"
                    + "/yadisk-search-proxy.conf");
            String secret = "1234567890123456789011";
            System.setProperty("BSCONFIG_IDIR", "");
            System.setProperty("HTTPS_PORT", "1");
            System.setProperty("HTTP_PORT", "0");
            System.setProperty("SEARCHMAP_PATH", configPath);
            System.setProperty("TVM_API_HOST", "tvm-api.yandex-team.ru");
            System.setProperty("TVM_CLIENT_ID", "186");
            System.setProperty("DISK_TVM_CLIENT_ID", "2000411");
            System.setProperty("SECRET", secret);
            System.setProperty("DJFS_HOST", "localhost");
            System.setProperty("PORTO_HOST", "localhost");
            String producerHost = "disk-producer-test.n.yandex-team.ru:80";
            System.setProperty("PRODUCER_HOST", producerHost);
            System.setProperty("PRODUCER_INDEXING_HOST", producerHost);
            System.setProperty("SERVER_NAME", "");
            System.setProperty("JKS_PASSWORD", "");
            System.setProperty("FACE_INDEXER_HOST", "localhost");
            System.setProperty("MOP_HOST", "localhost");

            IniConfig ini = new IniConfig(new File(configPath));
            ini.section("server").sections().remove("https");
            ini.sections().remove("mop");
            ProxyConfigBuilder builder = new ProxyConfigBuilder(config);
            builder.port(0);
            builder.connections(2);
            builder.searchMapConfig(
                new SearchMapConfigBuilder()
                    .content(
                        backend.searchMapRule(builder.diskService())
                        + backend.searchMapRule(builder.photosliceService())
                        + backend.searchMapRule(builder.ipddService())));
            builder.searchConfig(Configs.targetConfig());
            builder.indexerConfig(Configs.targetConfig());
            builder.upstreamsConfig(
                new UpstreamsConfigBuilder().asterisk(
                    new UpstreamConfigBuilder().connections(2)));
            builder.imagesFilter(
                ini.getString("proxy.images-filter", builder.imagesFilter()));
            builder.searchPostFilter(
                ini.getString(
                    "proxy.search-post-filter", builder.searchPostFilter()));
            builder.photosliceClientConfig(builder.searchConfig());

            ErratumConfigBuilder erratumBuilder = new ErratumConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(erratum))
                .copyTo(erratumBuilder);
            erratumBuilder.service(DISK_SEARCH);
            builder.misspellConfig(erratumBuilder);

            GeocoderConfigBuilder geocoderBuilder =
                new GeocoderConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(geocoder))
                .copyTo(geocoderBuilder);
            geocoderBuilder.origin(DISK_SEARCH);
            builder.geoSearchConfig(geocoderBuilder);
            builder.fatUserDocs(fatUserDocs);
            if (useProducer) {
                producer = new StaticServer(Configs.baseConfig("Producer"));
                chain.get().add(producer);
                producer.add(
                    DELETE,
                    new StaticHttpResource(
                        new ProxyHandler(backend.indexerPort())));
                producer.add(
                    MODIFY,
                    new StaticHttpResource(
                        new ProxyHandler(backend.indexerPort())));
                builder.producerServices(
                    Collections.singleton(builder.ipddService()));
                builder.producerClientConfig(
                    new ProducerClientConfigBuilder()
                        .connections(2)
                        .host(producer.host())
                        .fallbackToSearchMap(fallbackToSearchMap));
            } else {
                builder.producerServices(Collections.emptySet());
                producer = null;
            }

            builder.dssmConfig(
                new JniWrapperConfigBuilder()
                    .libraryName(
                        Paths.getBuildPath(
                            "mail/library/jniwrapper/dynamic_test"
                            + "/libjniwrapper-test.so"))
                    .ctorName("jniwrapper_test_copy_config_ctor")
                    .dtorName("jniwrapper_test_dtor")
                    .mainName("jniwrapper_test_copy_instance")
                    .config("010002"));
            builder.dssmThreshold(DSSM_THRESHOLD);

            builder.djfsTvmClientId("4");
            builder.geoTvmClientId("5");

            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"" + TVM2_TICKET + "\"},"
                + "\"5\":{\"ticket\":\"geocoder ticket\"}}");
            tvm2.start();

            djfs = new StaticServer(Configs.baseConfig("DJFS"));
            chain.get().add(djfs);

            uaas = new StaticServer(Configs.baseConfig("UAAS"));
            chain.get().add(uaas);

            builder.djfsConfig(
                Configs.uriConfig(
                    djfs,
                    "/api/v1/indexer/resources?service=disk-search"));

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret(secret);

            builder.tvm2ServiceConfig(tvm2ServiceConfig);
            builder.tvm2ClientConfig(
                new Tvm2ClientConfigBuilder()
                    .destinationClientId("4,5")
                    .renewalInterval(TVM2_RENEWAL_INTERVAL));
            builder.userSplitConfig(Configs.hostConfig(uaas));

            proxy = new Proxy(builder.build());
            chain.get().add(proxy);
            this.chain = chain.release();
        }
    }
    // CSON: ParameterNumber

    public void start() throws IOException {
        // backend already started
        if (producer != null) {
            producer.start();
        }
        erratum.start();
        geocoder.start();
        djfs.start();
        uaas.start();
        proxy.start();
    }

    public TestSearchBackend backend() {
        return backend;
    }

    public StaticServer erratum() {
        return erratum;
    }

    public StaticServer geocoder() {
        return geocoder;
    }

    public StaticServer producer() {
        return producer;
    }

    public StaticServer djfs() {
        return djfs;
    }

    public StaticServer uaas() {
        return uaas;
    }

    public Proxy proxy() {
        return proxy;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

