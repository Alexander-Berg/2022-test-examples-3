package ru.yandex.passport;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import com.opentable.db.postgres.embedded.LiquibasePreparer;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.apache.http.HttpHost;
import org.postgresql.PGProperty;

import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.address.config.AddressProxyConfigBuilder;
import ru.yandex.test.util.TestBase;

public class AddressProxyCluster implements GenericAutoCloseable<IOException> {
    public static final String PASSP_TVM_ID = "2029754";
    public static final String PASSP_TVM_ID_2 = "2010644";
    public static final String PAY_TVM_ID = "2024739";
    public static final String TAXI_TVM_ID = "2014130";
    public static final String USLUGI_TVM_ID = "2002420";
    public static final String EDA_TVM_ID = "2011664";
    public static final String TICKET_PASSP_PASSP =
        "3:serv:CBAQ__________9_IggIuvF7ELrxew:QwvVRscd2lKh-DA_ocbwbl3HS2ZGiViHXo_EaPeZyM4bS_v7mVqzLz_Xs0hOw_1DOd3Hr" +
            "UeYe_N_IglRsFmVHRxOhU7y3K2Rb6xIVblpCNrAi8auupKhkqFj0E6vc2Hg0XeVYaeXqki4pOq4j0tRS93kEhHUJykTT1-VJrJCAKg";
    public static final String TICKET_PAY_PASSP =
        "3:serv:CBAQ__________9_IggIo8p7ELrxew:L7nIT-G-lVhRnWKwVtMsv-OifGe4v3gqEJqtiMuJ2Zs-IuKvA8HSGoDc8Zy4qJJZW3nF3F" +
            "80uOozebe-ksQvgg24kBbkkCFGNBbdZdzB39wtBb1oG6Kt9YdHcfQ1gHvOnWtdvv00rwAUcZl6241EPpdMlibUCoUalNZwc8t_SGs";
    public static final String TICKET_TAXI_PASSP =
        "3:serv:CBAQ__________9_IggIsvd6ELrxew:VTju6a635vqOtVAeEWtM9al1yppHMymi0vKmPrg3uGvFUsuLHoTNJc0m0gFGc4cN9DxRRWljzp35Ye-_LxGSL-26Uq68Bqap-IuidLlUxXpKG53ME2ukBoQ_osIbIrryO-u-NEHKweGVL1J75D7pPizddPLsADnIZfREbNZQ_cc";

    // CSOFF: MultipleStringLiterals
//    private static final String SEARCH_BACKEND_CONFIG =
//        Paths.getSourcePath(
//            "mail/search/aceventura/aceventura_backend/files"
//                + "/aceventura_search_backend.conf");
//    private static final String PROXY_CONFIG = "/home/vonidu/Projects/PS/repo/arc2/arcadia/mail/search/passport/address/address_proxy/files/address_proxy.conf";
    private static final String PROXY_CONFIG =
        Paths.getSourcePath(
            "mail/search/passport/address/address_proxy/files/address_proxy.conf");
    private final AddressProxy proxy;
    private final StaticServer tvm2;
    private final StaticServer geocoder;
    private final StaticServer homeWorkDataSync;
    private final StaticServer deliveryDataSync;
    private final StaticServer blackbox;

//    private final TestSearchBackend searchBackend;
//    private final StaticServer producer;
//    private final StaticServer mailSearch;

    private final GenericAutoCloseableChain<IOException> chain;

    //private final EmbeddedPostgres postgres;

    public AddressProxyCluster(final TestBase base) throws Exception {
        try (GenericAutoCloseableHolder<IOException, GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            System.setProperty("TVM_API_HOST", "");
            System.setProperty("TVM_CLIENT_ID", "");
            System.setProperty("TVM_ALLOWED_SRCS", "");
            System.setProperty("SECRET", "");
            System.setProperty("SERVER_NAME", "");
            System.setProperty("JKS_PASSWORD", "");
            System.setProperty("INDEX_PATH", "");
            System.setProperty("MAIL_SEARCH_TVM_ID", "0");
            System.setProperty("TAXI_TVM_ID", "0");
            System.setProperty("YT_ACCESS_LOG", "");
            System.setProperty("CPU_CORES", "2");
            System.setProperty("SEARCH_THREADS", "2");
            System.setProperty("MERGE_THREADS", "2");
            System.setProperty("LIMIT_SEARCH_REQUESTS", "20");
            System.setProperty("INDEX_THREADS", "2");
            System.setProperty("SERVER_PORT", "0");
            System.setProperty("pgaas_password_rw", "localpassword");
            System.setProperty("JDBC_URL", "localurl");
            System.setProperty("DATASYNC_HOST", "localhost");
            System.setProperty("BSCONFIG_IDIR", ".");

            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            Configs.setupTvmKeys(tvm2);
            tvm2.start();

            System.setProperty("TVM_API_HOST", tvm2.host().toString());
            System.setProperty("TVM_CLIENT_ID", PASSP_TVM_ID);
            System.setProperty("TOOLS_TRIP_TVM", "100500");
            System.setProperty("PAY_TVM_CLIENT_ID", PAY_TVM_ID);
            System.setProperty("EDA_TVM_ID", EDA_TVM_ID);
            System.setProperty("EDA_JOBS_TVM_ID", EDA_TVM_ID);
            System.setProperty("TAXI_TVM_CLIENT_ID", TAXI_TVM_ID);
            System.setProperty("PASSPORT_TVM_CLIENT_ID", PASSP_TVM_ID_2);
            System.setProperty("USLUGI_TVM_CLIENT_ID", USLUGI_TVM_ID);
            System.setProperty("MARKET_LOAN_TVM_ID", USLUGI_TVM_ID);
            System.setProperty("YANDEX_ID_TVM_ID", USLUGI_TVM_ID);
            System.setProperty("SECRET", "1234567890123456789012");
            System.setProperty("ALLOWED_SRCS", PASSP_TVM_ID + "," + PAY_TVM_ID + "," + PASSP_TVM_ID_2 + "," + TAXI_TVM_ID);

            PostgresWrapper wrapper = new PostgresWrapper();
            //this.postgres = wrapper.postgres();
            chain.get().add(wrapper);
            PreparedDbProvider provider =
                PreparedDbProvider.forPreparer(LiquibasePreparer.forClasspathLocation("passport_table.xml"), new ArrayList<>());

            String url = provider.createDatabase();

            Properties pgProps = org.postgresql.Driver.parseURL(url, null);
            String phHost = pgProps.getProperty(PGProperty.PG_HOST.getName());
            int pgPort = Integer.parseInt(pgProps.getProperty(PGProperty.PG_PORT.getName()));
            String dbName = pgProps.getProperty(PGProperty.PG_DBNAME.getName());

            System.out.println("Connect url " + url);

            AddressProxyConfigBuilder builder =
                new AddressProxyConfigBuilder(patchProxyConfig(new IniConfig(new File(PROXY_CONFIG))));
            builder.auths().auths().get(new Pattern<>("/address/*", true)).bypassLoopback(false);
            builder.auths().auths().get(new Pattern<>("/contact/*", true)).bypassLoopback(false);
            builder.passportDbConfig().url(url);
            provider =
                PreparedDbProvider.forPreparer(LiquibasePreparer.forClasspathLocation("market_address.xml"), new ArrayList<>());

            url = provider.createDatabase();
            builder.marketDbConfig().url(url);

            builder.passportPgClientConfig().hosts(Collections.singletonList(new HttpHost(phHost, pgPort))).database(dbName).user("postgres").password("postgres");

            geocoder = new StaticServer(Configs.baseConfig());
            chain.get().add(geocoder);
            geocoder.start();

            blackbox = new StaticServer(Configs.baseConfig());
            chain.get().add(blackbox);
            blackbox.start();
            builder.blackboxConfig(Configs.hostConfig(blackbox));

            homeWorkDataSync = new StaticServer(Configs.baseConfig());
            chain.get().add(homeWorkDataSync);
            homeWorkDataSync.start();
            builder.homeWorkDatasyncConfig(Configs.hostConfig(homeWorkDataSync));

            deliveryDataSync = new StaticServer(Configs.baseConfig());
            chain.get().add(deliveryDataSync);
            deliveryDataSync.start();
            builder.deliveryDatasyncConfig(Configs.hostConfig(deliveryDataSync));

            builder.regionBaseConfig().url(
                new URL("file://" + Paths.getSandboxResourcesRoot() + "/region_tree.dump"));
//            builder.regionBaseConfig().url(
//                new URL("file:///home/vonidu/Downloads/region_tree.dump"));
            URI uri = builder.geocoderConfig().uri();
            uri = new URI(uri.getScheme(), uri.getUserInfo(), geocoder.host().getHostName(), geocoder.host().getPort(),
                uri.getPath(), uri.getQuery(), uri.getFragment());
            builder.geocoderConfig().uri(uri);
//            searchBackend =
//                new TestSearchBackend(base, new File(LUCENE_CONFIG).toPath());
//            chain.get().add(searchBackend);

//            producer = new StaticServer(Configs.baseConfig());
//            chain.get().add(producer);
//            producer.start();
//            producer.add(
//                "*",
//                new StaticHttpResource(
//                    new ProxyHandler(searchBackend().indexerPort())));
//
//            mailSearch = new StaticServer(Configs.baseConfig());
//            chain.get().add(mailSearch);
//            mailSearch.start();
//
//            System.setProperty("PRODUCER_HOST", producer.host().toString());
//
//            AceVenturaProxyConfigBuilder builder =
//                new AceVenturaProxyConfigBuilder(
//                    patchProxyConfig(
//                        new IniConfig(
//                            new File(PROXY_CONFIG))));
//            System.setProperty("ACEVENTURA_QUEUE", "aceventura_change_log");
//            builder.searchMapConfig().content(
//                searchBackend.searchMapRule(
//                    AceVenturaConstants.ACEVENTURA_QUEUE));
//
//            builder.mailSearch().host(mailSearch.host());
            proxy = new AddressProxy(builder.build());

            this.chain = chain.release();
            proxy.start();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    private IniConfig patchProxyConfig(
        final IniConfig config)
        throws Exception
    {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        //config.sections().remove("tvm2");
        //config.sections().remove("auth");
        config.section("passport-pgclient").put("pem-certificate", null);
        config.section("region-base").put("url", null);
        IniConfig server = config.section("server");
        server.sections().remove("free-space-signals");
        server.put("port", "0");
        //config.section("auth").section("address").section("*").sections().remove("bypass-loopback");
        //config.section("auth").section("contact").section("*").sections().remove("bypass-loopback");
        //config.section("searchmap").put("file", null);
        return config;
    }

    public AddressProxy proxy() {
        return proxy;
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer geocoder() {
        return geocoder;
    }

    public StaticServer homeWorkDataSync() {
        return homeWorkDataSync;
    }

    public StaticServer deliveryDataSync() {
        return deliveryDataSync;
    }

    //    public AceVenturaProxy proxy() {
//        return proxy;
//    }
//
//    public TestSearchBackend searchBackend() {
//        return searchBackend;
//    }
//
//    public StaticServer producer() {
//        return producer;
//    }
//
//    public StaticServer mailSearch() {
//        return mailSearch;
//    }
//
//    public void addStatus(final AceVenturaPrefix prefix) {
//        producer.add(
//            "/_status?service=aceventura_change_log&prefix="
//                + (prefix.hash() % SearchMap.SHARDS_COUNT)
//                + "&allow_cached&all&json-type=dollar",
//
//            new StaticHttpResource(
//                new StaticHttpItem(
//                    HttpStatus.SC_OK,
//                    "[{\"localhost\":100500}]")));
//    }
    // CSON: MultipleStringLiterals

    private static class PostgresWrapper implements GenericAutoCloseable<IOException> {
        private final EmbeddedPostgres postgres;

        public PostgresWrapper() throws IOException {
            this.postgres = EmbeddedPostgres.start();
        }

        public EmbeddedPostgres postgres() {
            return postgres;
        }

        @Override
        public void close() throws IOException {
            postgres.close();
        }
    }
}
