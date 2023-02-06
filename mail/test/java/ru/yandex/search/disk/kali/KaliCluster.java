package ru.yandex.search.disk.kali;

import java.io.File;
import java.io.IOException;
import java.util.function.UnaryOperator;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.message.BasicHeader;
import org.junit.Assert;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.collection.Pattern;
import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.function.GenericConsumer;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.ExpectingHeaderHttpItem;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpItem;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.test.ValidatingHttpItem;
import ru.yandex.http.util.BadRequestException;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.HeadersParser;
import ru.yandex.http.util.NotImplementedException;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.http.util.server.BaseServerConfigBuilder;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.ocr.proxy.OcrProxy;
import ru.yandex.ocr.proxy.OcrProxyConfigBuilder;
import ru.yandex.parser.uri.CgiParams;
import ru.yandex.parser.uri.QueryConstructor;
import ru.yandex.parser.uri.ScanningCgiParams;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;
import ru.yandex.tikaite.config.TikaiteConfigBuilder;
import ru.yandex.tikaite.server.Server;

public class KaliCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    public static final String DJFS_TVM_CLIENT_ID = "4";
    public static final String DJFS_TVM2_TICKET = "3:serv:DJFSEPLn";
    public static final Header DJFS_TVM2_HEADER =
        new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, DJFS_TVM2_TICKET);

    public static final String APE_TVM_CLIENT_ID = "5";
    public static final String APE_TVM2_TICKET = "3:serv:TIKAITEn";
    public static final Header APE_TVM2_HEADER =
        new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, APE_TVM2_TICKET);

    public static final String UNISTORAGE_TVM_CLIENT_ID = "6";
    public static final String UNISTORAGE_TVM2_TICKET = "3:serv:UNISTORA";
    public static final Header UNISTORAGE_TVM2_HEADER = new BasicHeader(
        YandexHeaders.X_SRW_SERVICE_TICKET,
        UNISTORAGE_TVM2_TICKET);

    public static final Header SRW_NAMESPACE_HEADER =
        new BasicHeader(YandexHeaders.X_SRW_NAMESPACE, "disk");
    public static final Header SRW_KEY_TYPE_HEADER =
        new BasicHeader(YandexHeaders.X_SRW_KEY_TYPE, "STID");

    public static final String OCR_TEXT =
        "Куча осмысленного текста\nразделённого переводом строк";

    static {
        System.setProperty(
            "LUCENE_DISK_CONFIG_INCLUDE",
            "search_backend_thin.conf");
        System.setProperty(
            "FACE_KALI_ENABLED",
            "false");
    }

    private static final long TVM_RENEWAL_INTERVAL = 60000L;
    private static final String COKEMULATOR_URI = "/process/handler";
    private static final String MIMETYPE = "mimetype";
    private static final String LIMIT = "limit";
    private static final String STID = "stid";

    private final StaticServer tvm;
    private final StaticServer djfs;
    private final StaticServer lenulca;
    private final Server tikaite;
    private final StaticServer tikaiteSrw;
    private final TestSearchBackend lucene;
    private final StaticServer imageparser;
    private final StaticServer ocraas;
    private final OcrProxy ocrProxy;
    private final StaticServer callbacks;
    private final Kali kali;

    // CSOFF: MethodLength
    public KaliCluster(final TestBase testBase) throws Exception {
        this(testBase, new KaliConfigBuilder());
    }

    public KaliCluster(final TestBase testBase, final KaliConfigBuilder kaliConfig) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            tvm = new StaticServer(Configs.baseConfig("TVM"));
            chain.get().add(tvm);
            tvm.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"" + DJFS_TVM2_TICKET
                + "\"},\"5\":{\"ticket\":\"" + APE_TVM2_TICKET
                + "\"},\"6\":{\"ticket\":\"" + UNISTORAGE_TVM2_TICKET
                + "\"}}");
            tvm.start();

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            Tvm2ClientConfigBuilder tvm2ClientConfig =
                new Tvm2ClientConfigBuilder()
                    .destinationClientId(
                        DJFS_TVM_CLIENT_ID
                        + ',' + APE_TVM_CLIENT_ID
                        + ',' + UNISTORAGE_TVM_CLIENT_ID)
                    .renewalInterval(TVM_RENEWAL_INTERVAL);

            djfs = new StaticServer(Configs.baseConfig("DJFS"));
            chain.get().add(djfs);

            lenulca = new StaticServer(Configs.baseConfig("Lenulca"));
            chain.get().add(lenulca);

            TikaiteConfigBuilder tikaiteConfig = new TikaiteConfigBuilder();
            new BaseServerConfigBuilder(Configs.baseConfig("Tikaite"))
                .copyTo(tikaiteConfig);
            tikaiteConfig.storageConfig(Configs.hostConfig(lenulca));
            tikaiteConfig
                .textExtractorConfig()
                .receivedChainParserConfig()
                .yandexNetsConfig()
                .file(
                    new File(
                        Paths.getSandboxResourcesRoot() + "/yandex-nets.txt"));
            tikaite = new Server(tikaiteConfig.build());
            chain.get().add(tikaite);

            tikaiteSrw = new StaticServer(Configs.baseConfig("TikaiteSrw"));
            chain.get().add(tikaiteSrw);
            tikaiteSrw.register(
                new Pattern<>("/disk/handler", false),
                new ValidatingHttpItem(
                    new ProxyHandler(
                        tikaite.host(),
                        TikaiteUriConverter.INSTANCE),
                    ExpectingHeaderHttpItem.join(
                        APE_TVM2_HEADER,
                        SRW_NAMESPACE_HEADER,
                        SRW_KEY_TYPE_HEADER,
                        UNISTORAGE_TVM2_HEADER)
                        .andThen(StidValidator.INSTANCE)));

            lucene = new TestSearchBackend(
                testBase,
                new File(
                    Paths.getSourcePath(
                        "mail/search/disk/search_backend_disk_config/files"
                        + "/search_backend.conf")));
            chain.get().add(lucene);

            imageparser = new StaticServer(Configs.baseConfig("Imageparser"));
            chain.get().add(imageparser);
            imageparser.register(
                new Pattern<>(COKEMULATOR_URI, false),
                new StaticHttpItem(
                    "{\"i2t_hex\":\"BABA\",\"classes\":{\"wallpaper\":0.5,"
                    + "\"beautiful\":0.25},\"faces\":["
                    + "{\"x\":0.5,\"y\":0.5,\"width\":0.5,\"height\":0.5},"
                    + "{\"x\":0.25,\"y\":0.25,\"width\":0.75,\"height\":0.125"
                    + "}]}"));

            ocraas = new StaticServer(Configs.baseConfig("OCRaaS"));
            chain.get().add(ocraas);
            ocraas.register(
                new Pattern<>(COKEMULATOR_URI, false),
                new StaticHttpItem(OCR_TEXT));

            OcrProxyConfigBuilder ocrProxyConfig = new OcrProxyConfigBuilder();
            new BaseServerConfigBuilder(Configs.baseConfig("OcrProxy"))
                .copyTo(ocrProxyConfig);
            ocrProxyConfig.ocraasConfig(Configs.hostConfig(ocraas));
            ocrProxyConfig.ocrQueue("ocr");
            ocrProxyConfig.imageparserConfig(Configs.hostConfig(imageparser));
            ocrProxyConfig.cvQueue("cv");
            ocrProxyConfig.faceCallbacksQueue("face_in_queue");
            ocrProxyConfig.faceCallbacksConfig(Configs.targetConfig());
            ocrProxyConfig.indexerConfig(
                Configs.hostConfig(lucene.indexerHost()));
            ocrProxyConfig.searchConfig(
                Configs.hostConfig(lucene.searchHost()));
            ocrProxyConfig.tvm2ServiceConfig(tvm2ServiceConfig);
            ocrProxyConfig.tvm2ClientConfig(tvm2ClientConfig);
            ocrProxyConfig.apeTvmClientId(APE_TVM_CLIENT_ID);
            ocrProxyConfig.unistorageTvmClientId(UNISTORAGE_TVM_CLIENT_ID);
            ocrProxyConfig.ocrCallbacksConfig(Configs.targetConfig());
            ocrProxyConfig.ocrCallbacksQueue("ocr_callbacks");
            ocrProxyConfig.cvCallbacksConfig(Configs.targetConfig());
            ocrProxyConfig.cvCallbacksQueue("cv_callbacks");
            ocrProxy = new OcrProxy(ocrProxyConfig.build());
            chain.get().add(ocrProxy);

            callbacks =
                new StaticServer(Configs.baseConfig("Callbacks"));
            chain.get().add(callbacks);

            new BaseServerConfigBuilder(Configs.baseConfig("Kali"))
                .copyTo(kaliConfig);
            kaliConfig.djfsConfig(
                Configs.uriConfig(djfs, "/api/v1/indexer/resources"));
            kaliConfig.tikaiteConfig(Configs.hostConfig(tikaiteSrw));
            kaliConfig.searcherConfig(Configs.hostConfig(lucene.searchHost()));
            kaliConfig.indexerConfig(Configs.hostConfig(lucene.indexerHost()));
            kaliConfig.ocrProxyConfig(Configs.hostConfig(ocrProxy));
            kaliConfig.callbacksConfig(Configs.targetConfig());
            kaliConfig.callbacksQueue("callbacks");
            kaliConfig.tvm2ServiceConfig(tvm2ServiceConfig);
            kaliConfig.tvm2ClientConfig(tvm2ClientConfig);
            kaliConfig.djfsTvmClientId(DJFS_TVM_CLIENT_ID);
            kaliConfig.tikaiteTvmClientId(APE_TVM_CLIENT_ID);
            kaliConfig.unistorageTvmClientId(UNISTORAGE_TVM_CLIENT_ID);
            kaliConfig.faceConfig().host(callbacks.host()).connections(10);
            kali = new Kali(kaliConfig.build());
            chain.get().add(kali);

            reset(chain.release());
        }
    }
    // CSON: MethodLength

    public void start() throws IOException {
        djfs.start();
        lenulca.start();
        tikaite.start();
        tikaiteSrw.start();
        imageparser.start();
        ocraas.start();
        ocrProxy.start();
        callbacks.start();
        kali.start();
    }

    public StaticServer djfs() {
        return djfs;
    }

    public StaticServer ocraas() {
        return ocraas;
    }

    public StaticServer lenulca() {
        return lenulca;
    }

    public TestSearchBackend lucene() {
        return lucene;
    }

    public StaticServer callbacks() {
        return callbacks;
    }

    public Kali kali() {
        return kali;
    }

    public StaticServer imageparser() {
        return imageparser;
    }

    private enum TikaiteUriConverter implements UnaryOperator<String> {
        INSTANCE;

        @Override
        public String apply(final String uri) {
            try {
                CgiParams params = new CgiParams(uri);
                QueryConstructor query = new QueryConstructor(
                    "/get/" + params.getString(STID) + "?name=disk");
                String mimetype = params.getString(MIMETYPE, null);
                if (mimetype != null) {
                    query.append(MIMETYPE, mimetype);
                }
                String limit = params.getString(LIMIT, null);
                if (limit != null) {
                    query.append(LIMIT, limit);
                }
                return query.toString();
            } catch (BadRequestException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private enum StidValidator
        implements GenericConsumer<HttpRequest, NotImplementedException>
    {
        INSTANCE;

        @Override
        public void accept(final HttpRequest request)
            throws NotImplementedException
        {
            try {
                Assert.assertEquals(
                    new ScanningCgiParams(request).getString(STID),
                    new HeadersParser(request)
                        .getString(YandexHeaders.X_SRW_KEY));
            } catch (Throwable t) {
                throw new NotImplementedException("Stid validation failed", t);
            }
        }
    }
}

