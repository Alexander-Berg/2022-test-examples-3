package ru.yandex.mail.so2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.google.protobuf.util.JsonFormat;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Assert;

import ru.yandex.base64.Base64Decoder;
import ru.yandex.charset.Decoder;
import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.HttpAssert;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.CharsetUtils;
import ru.yandex.http.util.HeadersParser;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.client.ClientBuilder;
import ru.yandex.io.GenericCloseableAdapter;
import ru.yandex.mail.so.api.v1.SoRequest;
import ru.yandex.mail.so.cretur.Cretur;
import ru.yandex.mail.so.cretur.CreturCluster;
import ru.yandex.mail.so2.config.So2ConfigBuilder;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.test.util.Checker;
import ru.yandex.test.util.JsonChecker;
import ru.yandex.test.util.JsonSubsetChecker;
import ru.yandex.test.util.StringChecker;
import ru.yandex.test.util.TestBase;
import ru.yandex.test.util.YandexAssert;

public class So2Cluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String TVM_CLIENT_ID = "2001103";
    public static final String TVM_SECRET = "1234567890123456789011";
    public static final String BIGB_TVM_CLIENT_ID = "2001337";
    public static final String BIGB_TVM_TICKET = "bigb ticket";
    public static final String BLACKBOX_TVM_CLIENT_ID = "222";
    public static final String BLACKBOX_TVM_TICKET = "here the ticket";
    public static final String CORP_BLACKBOX_TVM_CLIENT_ID = "223";
    public static final String CORP_BLACKBOX_TVM_TICKET =
        "here the corp ticket";
    public static final String SENDERS_TVM_CLIENT_ID = "2000031";
    public static final String SENDERS_TVM_TICKET = "senders ticket";
    public static final String YADISK_TVM_CLIENT_ID = "132";
    public static final String YADISK_TVM_TICKET = "yadisk ticket";
    public static final String CRETUR_TVM_ID = "2035323";
    public static final String CRETUR_TVM_TICKET = "cretur ticket";

    private final CloseableHttpClient client;
    private final StaticServer spdaemon;
    private final FakeTvmServer tvm2;
    private final CreturCluster creturCluster;
    private final StaticServer senders;
    private final StaticServer templateMaster;
    private final StaticServer bigb;
    private final StaticServer blackbox;
    private final StaticServer corpBlackbox;
    private final StaticServer activityShingler;
    private final StaticServer yadisk;
    private final So2HttpServer so2;

    public So2Cluster(final TestBase testBase) throws Exception {
        this(testBase, "mail/so/daemons/so2/so2_config/files/so2.conf");
    }

    public So2Cluster(final TestBase testBase, final String configPath)
        throws Exception
    {
        this(testBase, configPath, "SO2");
    }

    public So2Cluster(
        final TestBase testBase,
        final String configPath,
        final String so2ClusterName)
        throws Exception
    {
        System.setProperty("jni-fasttext.reuse-models", "true");
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            chain.get().add(() -> testBase.clearContext());

            client =
                ClientBuilder.createClient(
                    new HttpTargetConfigBuilder(Configs.targetConfig())
                        .redirects(true)
                        .build(),
                    Configs.dnsConfig());
            chain.get().add(new GenericCloseableAdapter<>(client));

            spdaemon = new StaticServer(Configs.baseConfig("SpDaemon"));
            chain.get().add(spdaemon);

            spdaemon.register(
                new Pattern<>("", true),
                HeaderEchoHandler.INSTANCE);
            spdaemon.start();

            tvm2 = FakeTvmServer.fromContext(testBase, chain.get());

            tvm2.addTicket(BLACKBOX_TVM_CLIENT_ID, BLACKBOX_TVM_TICKET);
            tvm2.addTicket(
                CORP_BLACKBOX_TVM_CLIENT_ID,
                CORP_BLACKBOX_TVM_TICKET);
            tvm2.addTicket(BIGB_TVM_CLIENT_ID, BIGB_TVM_TICKET);
            tvm2.addTicket(SENDERS_TVM_CLIENT_ID, SENDERS_TVM_TICKET);
            tvm2.addTicket(YADISK_TVM_CLIENT_ID, YADISK_TVM_TICKET);
            tvm2.addTicket(CRETUR_TVM_ID, CRETUR_TVM_TICKET);

            System.setProperty(
                "ALL_CAS_DIR",
                Paths.getSourcePath(
                    "mail/tools/nanny_helpers/nanny_service_base/files"));
            creturCluster = new CreturCluster(testBase);
            chain.get().add(creturCluster);
            creturCluster.prepareDatabase();
            creturCluster.start();
            System.setProperty(
                "CRETUR_HOST",
                creturCluster.cretur().httpHost().toString());
            System.setProperty("CRETUR_TVM_ID", CRETUR_TVM_ID);

            senders = new StaticServer(Configs.baseConfig("Senders"));
            chain.get().add(senders);

            templateMaster =
                new StaticServer(Configs.baseConfig("TemplateMaster"));
            chain.get().add(templateMaster);

            bigb = new StaticServer(Configs.baseConfig("BigB"));
            chain.get().add(bigb);

            blackbox =
                StaticServer.fromContext(
                    testBase,
                    "Blackbox",
                    "BLACKBOX_HOST",
                    chain.get());

            corpBlackbox =
                StaticServer.fromContext(
                    testBase,
                    "CorpBlackbox",
                    "CORP_BLACKBOX_HOST",
                    chain.get());

            activityShingler =
                new StaticServer(Configs.baseConfig("ActivityShingler"));
            chain.get().add(activityShingler);

            yadisk = new StaticServer(Configs.baseConfig("YaDisk"));
            chain.get().add(yadisk);

            System.setProperty("NANNY_SERVICE_ID", "spdaemon-in");
            System.setProperty("EXECUTION_TIMEOUT", "1h");
            System.setProperty("SO2_PORT", Integer.toString(0));
            System.setProperty("ROOT", ".");
            System.setProperty("SPDAEMON_HOST", spdaemon.host().toString());
            System.setProperty("SENDERS_HOST", "localhost");
            System.setProperty(
                "SENDERS_PORT",
                Integer.toString(senders.port()));
            System.setProperty("SOSEARCH_PROXY_POOL_TIMEOUT", "1s");
            System.setProperty("SOSEARCH_PROXY_SOCKET_TIMEOUT", "1s");
            System.setProperty(
                "TEMPLATE_MASTER_HOST",
                templateMaster.host().toString());
            System.setProperty("TEMPLATE_MASTER_POOL_TIMEOUT", "1s");
            System.setProperty("TEMPLATE_MASTER_SOCKET_TIMEOUT", "1s");
            System.setProperty("BIGB_HOST", bigb.host().toString());
            System.setProperty("BIGB_TVM_CLIENT_ID", BIGB_TVM_CLIENT_ID);
            System.setProperty("SENDERS_TVM_CLIENT_ID", SENDERS_TVM_CLIENT_ID);
            System.setProperty("TVM_CLIENT_ID", TVM_CLIENT_ID);
            System.setProperty("SECRET", TVM_SECRET);
            System.setProperty(
                "BLACKBOX_TVM_CLIENT_ID",
                BLACKBOX_TVM_CLIENT_ID);
            System.setProperty(
                "CORP_BLACKBOX_TVM_CLIENT_ID",
                CORP_BLACKBOX_TVM_CLIENT_ID);
            System.setProperty("PANEL_TITLE", "spdaemon-in");
            System.setProperty(
                "PANEL_TAG",
                "itype=spdaemon;prj=so;nanny=spdaemon-in*;ctype=prod");
            System.setProperty("ROUTE", "in");
            System.setProperty("CPU_CORES", "4");
            System.setProperty("ZOO_QUEUE", "change_log");
            System.setProperty("ZOO_CORP_QUEUE", "corp_change_log");
            System.setProperty("TIMER_RESOLUTION", "100ms");
            System.setProperty(
                "SANITIZER_CONFIGS_ROOT",
                Paths.getSourcePath(
                    "mail/library/html/sanitizer/sanitizer2_config/"));
            System.setProperty("SPAM_SAMPLES", "spam-samples.json");
            System.setProperty(
                "ACTIVITY_HOST",
                activityShingler.host().toString());
            System.setProperty("YADISK_HOST", yadisk.host().toString());
            System.setProperty("YADISK_TVM_CLIENT_ID", YADISK_TVM_CLIENT_ID);
            IniConfig ini =
                new IniConfig(new File(Paths.getSourcePath(configPath)));
            ini.sections().remove("log");
            ini.sections().remove("accesslog");
            ini.sections().remove("stderr");
            ini.section("server").sections().remove("free-space-signals");
            ini.section("server").sections().remove("files-staters");

            IniConfig mainSection =
                ini.sectionOrNull("extract-modules.extract-module.main");
            if (mainSection != null) {
                mainSection.put(
                    "dsl-script",
                    Paths.getSourcePath(
                        "mail/so/daemons/so2/so2_config/files/"
                        + mainSection.getString("dsl-script")));
            }
            IniConfig nestedMailSection = ini.sectionOrNull(
                "extract-modules.extract-module.nested-mail");
            if (nestedMailSection != null) {
                nestedMailSection.put(
                    "dsl-script",
                    Paths.getSourcePath(
                        "mail/so/daemons/so2/so2_config/files/tikaite.dsl"));
            }
            IniConfig onlyFastTextSection = ini.sectionOrNull(
                "extract-modules.extract-module.only-fast-text");
            if (onlyFastTextSection != null) {
                onlyFastTextSection.put(
                    "dsl-script",
                    Paths.getSourcePath(
                        "mail/so/daemons/so2/so2_config/files/tikaite.dsl"));
            }
            IniConfig ocrSection =
                ini.sectionOrNull("extract-modules.extract-module.ocr");
            if (ocrSection != null) {
                ocrSection.put(
                    "dsl-script",
                    Paths.getSourcePath(
                        "mail/so/daemons/so2/so2_config/files/ocr.dsl"));
            }

            System.setProperty(
                "LIBUNPERSON",
                Paths.getBuildPath(
                    "mail/so/libs/unperson/jniwrapper"
                    + "/libunperson-jniwrapper.so"));
            System.setProperty(
                "LIBDEOBFUSCATOR",
                Paths.getBuildPath(
                    "mail/so/libs/deobfuscator_jniwrapper/jniwrapper"
                    + "/libdeobfuscator-jniwrapper.so"));

            ini.section("spam-samples")
                .put(
                    "samples-path",
                    Paths.getSourcePath(
                        "mail/so/libs/java/so_factors/test/resources/ru/yandex"
                        + "/mail/so/factors/hnsw/spam-samples.json"));

            System.setProperty(
                "FAST_TEXT_MODEL",
                Paths.getSandboxResourcesRoot()
                + "/taiga-epoch10.bin");

            System.setProperty(
                "HNSW_DSSM",
                Paths.getSandboxResourcesRoot()
                + "/DssmAllCleanWithAttachments");
            System.setProperty(
                "MAIL_DSSM",
                Paths.getSandboxResourcesRoot() + "/MailEmbedDssm.dssm");

            System.setProperty(
                "LIBOCRAAS",
                Paths.getBuildPath(
                    "yweb/disk/ocraas-jniwrapper/jniwrapper"
                    + "/libocraas-jniwrapper-jniwrapper.so"));
            System.setProperty(
                "OCRAAS_CONFIG",
                Paths.getSandboxResourcesRoot()
                + "/ocrdata/yt/ocr.OCRNNLiteEnRu.cfg");

            So2ConfigBuilder builder =
                new So2ConfigBuilder(ini).name(so2ClusterName);
            ini.checkUnusedKeys();

            so2 = new So2HttpServer(new So2ConfigBuilder(builder).build());
            chain.get().add(so2);
            reset(chain.release());
        }
    }

    public void start() throws IOException {
        senders.start();
        templateMaster.start();
        bigb.start();
        activityShingler.start();
        yadisk.start();
        so2.start();
    }

    @Override
    public void close() throws IOException {
        try (GenericAutoCloseableChain<IOException> guard = release()) {
            if (guard != null) {
                String stats = HttpAssert.stats(so2.host());
                HttpAssert.assertStat(
                    "so2-factors-access-violations_dmmm",
                    Integer.toString(0),
                    stats);
                HttpAssert.assertStat(
                    "so2-lua-errors-stater_dmmm",
                    Integer.toString(0),
                    stats);
            }
        }
    }

    public CloseableHttpClient client() {
        return client;
    }

    public Cretur cretur() {
        return creturCluster.cretur();
    }

    public StaticServer senders() {
        return senders;
    }

    public StaticServer templateMaster() {
        return templateMaster;
    }

    public StaticServer bigb() {
        return bigb;
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer corpBlackbox() {
        return corpBlackbox;
    }

    public StaticServer activityShingler() {
        return activityShingler;
    }

    public StaticServer yadisk() {
        return yadisk;
    }

    public So2HttpServer so2() {
        return so2;
    }

    public void check(
        final String uri,
        final String requestPayload,
        final String expectedSo2Data)
        throws Exception
    {
        check(uri, requestPayload, expectedSo2Data, false);
    }

    public void check(
        final String uri,
        final String requestPayload,
        final String expectedSo2Data,
        final boolean subset)
        throws Exception
    {
        HttpPost post = new HttpPost(so2.host() + uri);
        post.setEntity(
            new StringEntity(requestPayload, StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = client.execute(post)) {
            HttpAssert.assertStatusCode(HttpStatus.SC_OK, response);
            HeadersParser headersParser = new HeadersParser(response);
            if (uri.indexOf("format=protobuf-json") == -1) {
                YandexAssert.check(
                    new StringChecker(requestPayload),
                    CharsetUtils.toString(response.getEntity()));
                YandexAssert.check(
                    new StringChecker(uri),
                    headersParser.getString(YandexHeaders.URI));
            } else {
                SoRequest protobufRequest =
                    SoRequest.parseFrom(
                        CharsetUtils.toDecodable(response.getEntity())
                            .toByteArrayProcessable()
                            .content());
                SoRequest.Builder requestBuilder = SoRequest.newBuilder();
                JsonFormat.parser().merge(
                    requestPayload,
                    requestBuilder);
                Assert.assertEquals(protobufRequest, requestBuilder.build());
                YandexAssert.check(
                    new StringChecker(
                        uri.replace("format=protobuf-json", "format=protobuf")
                        + "&output-format=protobuf-json"),
                    headersParser.getString(YandexHeaders.URI));
            }

            String so2Data =
                headersParser.getString(So2HttpServer.SO2_DATA_HEADER);
            Base64Decoder base64Decoder = new Base64Decoder();
            base64Decoder.process(so2Data.toCharArray());
            Decoder decoder = new Decoder(StandardCharsets.UTF_8);
            base64Decoder.processWith(decoder);
            Checker checker;
            if (subset) {
                checker = new JsonSubsetChecker(expectedSo2Data);
            } else {
                checker = new JsonChecker(
                    expectedSo2Data,
                    JsonChecker.DEFAULT_PRECISION,
                    0.0001d);
            }
            YandexAssert.check(checker, decoder.toString());
        }
    }
}

