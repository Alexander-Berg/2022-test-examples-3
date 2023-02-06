package ru.yandex.mail.search.web;

import java.io.File;
import java.io.IOException;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.mail.search.web.config.AbstractPsProjectConfigBuilder;
import ru.yandex.mail.search.web.config.PsProjectConfig;
import ru.yandex.mail.search.web.config.WebApiConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.tvmauth.Version;

public class WebToolsCluster implements GenericAutoCloseable<IOException> {
    private static final long TVM_RENEWAL_INTERVAL = 60000L;

    private final StaticServer tvm2;
    private final WebApi webApi;
    private final GenericAutoCloseableChain<IOException> chain;
    private final StaticServer diskProxy;

    public WebToolsCluster() throws Exception {
        System.setProperty("BSCONFIG_INAME", "webtools");
        System.setProperty("BSCONFIG_IPORT", "0");
        System.setProperty("BSCONFIG_IDIR", "./");
        final String localhost = "http://localhost:0";
        System.setProperty("TVM_API_HOST", localhost);
        System.setProperty("TIKAITE_SRW_HOST", localhost);
        System.setProperty("TVM_CLIENT_ID", "11");
        System.setProperty("BLACKBOX_CLIENT_ID", "44");
        System.setProperty("CORP_BLACKBOX_CLIENT_ID", "5");
        System.setProperty("SECRET", "AAAAAAAAAAAAAAAAAAAAAA==");
        System.setProperty("SECRET", "AAAAAAAAAAAAAAAAAAAAAA==");
        System.setProperty("ROBOT_UID", "22");
        System.setProperty("STAFF_TOKEN", "AA");
        System.setProperty("MAIL_PROD_LUCENE_DROP_PASSWORD", "");
        System.setProperty("DISK_PROD_LUCENE_DROP_PASSWORD", "");

        String configPath =
            Paths.getSourcePath("mail/search/webtools/webtools_api/files/webtools.conf");
        IniConfig config = new IniConfig(new File(configPath));

        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("stdout");
        config.sections().remove("stderr");
        config.sections().remove("searchmap");
        config.section("service_mail_prod").sections().remove("searchmap");
        config.section("service_mail_prod").sections().remove("mop");
        config.section("service_mail_corp").sections().remove("searchmap");
        config.section("service_mail_test").sections().remove("searchmap");
        config.section("service_mail_test").sections().remove("mop");
        config.section("service_disk_prod").sections().remove("searchmap");
        config.section("service_disk_prod").sections().remove("mop");
        config.section("service_disk_test").sections().remove("searchmap");
        config.section("service_disk_test").sections().remove("mop");
        config.section("service_aceventura_prod").sections().remove("searchmap");
        config.section("service_messenger_chats_prod").sections().remove("searchmap");
        config.section("service_messenger_messages_prod").sections().remove("searchmap");

        config.section("service_district_prod").sections().remove("searchmap");
        config.section("service_district_test").sections().remove("searchmap");

        config.section("server").put("port", "0");
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            WebApiConfigBuilder webApiConfig = new WebApiConfigBuilder(config);

            final String clientId = "4";
            webApiConfig.blackboxTvm2(
                new Tvm2ClientConfigBuilder()
                    .destinationClientId(clientId)
                    .renewalInterval(TVM_RENEWAL_INTERVAL));
            webApiConfig.corpBlackboxTvm2(
                new Tvm2ClientConfigBuilder()
                    .destinationClientId(clientId)
                    .renewalInterval(TVM_RENEWAL_INTERVAL));

            this.diskProxy = new StaticServer(Configs.baseConfig());
            chain.get().add(diskProxy);
            diskProxy.start();
            webApiConfig.diskSearch().host(diskProxy.host());

            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm2.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"here the ticket\"}}");
            tvm2.start();

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            webApiConfig.tvm2ServiceConfig(tvm2ServiceConfig);
//            String sm = String.join(
//                "\n",
//                Files.readAllLines(new File(
//                    "/Users/vonidu/Downloads/searchmap_mail.txt").toPath()));
            String sm = "change_log iNum:1741,tag:sas2-0806_18065,"
                + "host:sas2-0806.search.yandex.net,shards:0-5,"
                + "zk:myt1-0441.search.yandex.net:17973/17974|"
                + "sas1-8532.search.yandex.net:17973/17974,"
                + "json_indexer_port:18069,search_port_ng:18066,"
                + "search_port:18065";
            webApiConfig.searchMapConfig().content(sm).file(null);
            for (AbstractPsProjectConfigBuilder<? extends PsProjectConfig> builder: webApiConfig.projects().values()) {
                builder.searchMapConfig().content(sm).file(null);
                builder.metricsUpdateWorkers(1);
                builder.totalShardsCount(10);
            }

            webApi = new WebApi(webApiConfig.build());
            chain.get().add(webApi);
            this.chain = chain.release();

            webApi.start();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    public WebApi webApi() {
        return webApi;
    }

    public StaticServer diskProxy() {
        return diskProxy;
    }
}
