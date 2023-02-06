package ru.yandex.ocr.proxy;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.ByteArrayEntityFactory;
import ru.yandex.http.util.YandexHeaders;
import ru.yandex.io.IOStreamUtils;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.test.search.backend.TestDiskSearchBackend;
import ru.yandex.test.util.TestBase;

public class OcrProxyCluster implements GenericAutoCloseable<IOException> {
    public static final String TEXT_BY_DESC_URI =
        "/image2text__v012/text_by_desc";
    public static final String OCR_CALLBACKS_QUEUE = "ocr-callbacks";
    public static final String CV_CALLBACKS_QUEUE = "cv-callbacks";

    public static final String APE_TVM_CLIENT_ID = "4";
    public static final String APE_TVM2_TICKET = "3:serv:CIEaEPLn";
    public static final Header APE_TVM2_HEADER =
        new BasicHeader(YandexHeaders.X_YA_SERVICE_TICKET, APE_TVM2_TICKET);

    public static final String UNISTORAGE_TVM_CLIENT_ID = "5";
    public static final String UNISTORAGE_TVM2_TICKET = "3:serv:UNISTORA";
    public static final Header UNISTORAGE_TVM2_HEADER = new BasicHeader(
        YandexHeaders.X_SRW_SERVICE_TICKET,
        UNISTORAGE_TVM2_TICKET);

    private static final long TVM_RENEWAL_INTERVAL = 60000L;

    private final StaticServer ocraas;
    private final StaticServer imageparser;

    private final StaticServer ocrCallbacksBackend;
    private final StaticServer cvCallbacksBackend;
    private final StaticServer tvm;
    private final OcrProxy proxy;
    private final TestDiskSearchBackend lucene;
    private final GenericAutoCloseableChain<IOException> chain;

    public OcrProxyCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            this.lucene = new TestDiskSearchBackend(testBase);
            lucene.add();
            chain.get().add(this.lucene);

            ocraas = new StaticServer(Configs.baseConfig("OCRaaS"));
            chain.get().add(ocraas);
            imageparser = new StaticServer(Configs.baseConfig("Imageparser"));
            chain.get().add(imageparser);
            ocrCallbacksBackend =
                new StaticServer(Configs.baseConfig("OCR-Callbacks-Backend"));
            chain.get().add(ocrCallbacksBackend);
            cvCallbacksBackend =
                new StaticServer(Configs.baseConfig("CV-Callbacks-Backend"));
            chain.get().add(cvCallbacksBackend);
            tvm = new StaticServer(Configs.baseConfig("TVM"));
            chain.get().add(tvm);
            tvm.add(
                "/2/keys/?lib_version=" + Version.get(),
                IOStreamUtils.consume(
                    StaticServer.class.getResourceAsStream("tvm-keys.txt"))
                    .processWith(ByteArrayEntityFactory.INSTANCE));
            tvm.add(
                "/2/ticket/",
                "{\"4\":{\"ticket\":\"" + APE_TVM2_TICKET
                + "\"},\"5\":{\"ticket\":\"" + UNISTORAGE_TVM2_TICKET
                + "\"}}");
            tvm.start();

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789011");

            proxy = new OcrProxy(
                new OcrProxyConfigBuilder()
                    .port(0)
                    .connections(2)
                    .usePreviewStid(true)
                    .indexerConfig(Configs.hostConfig(lucene.indexerHost()))
                    .searchConfig(Configs.hostConfig(lucene.searchHost()))
                    .ocraasConfig(Configs.hostConfig(ocraas))
                    .ocrQueue("ocraas")
                    .imageparserConfig(Configs.hostConfig(imageparser))
                    .cvQueue("imageparser")
                    .tvm2ServiceConfig(tvm2ServiceConfig)
                    .tvm2ClientConfig(
                        new Tvm2ClientConfigBuilder()
                            .destinationClientId(
                                APE_TVM_CLIENT_ID
                                + ',' + UNISTORAGE_TVM_CLIENT_ID)
                            .renewalInterval(TVM_RENEWAL_INTERVAL))
                    .apeTvmClientId(APE_TVM_CLIENT_ID)
                    .unistorageTvmClientId(UNISTORAGE_TVM_CLIENT_ID)
                    .ocrCallbacksConfig(Configs.targetConfig())
                    .ocrCallbacksQueue(OCR_CALLBACKS_QUEUE)
                    .cvCallbacksConfig(Configs.targetConfig())
                    .cvCallbacksQueue(CV_CALLBACKS_QUEUE)
                    .faceCallbacksQueue(CV_CALLBACKS_QUEUE)
                    .faceCallbacksConfig(Configs.targetConfig())
                    .build());
            chain.get().add(proxy);
            this.chain = chain.release();
        }
    }

    public StaticServer ocraas() {
        return ocraas;
    }

    public StaticServer imageparser() {
        return imageparser;
    }

    public TestDiskSearchBackend lucene() {
        return lucene;
    }

    public StaticServer ocrCallbacksBackend() {
        return ocrCallbacksBackend;
    }

    public StaticServer cvCallbacksBackend() {
        return cvCallbacksBackend;
    }

    public OcrProxy proxy() {
        return proxy;
    }

    public void start() throws IOException {
        ocraas.start();
        imageparser.start();
        ocrCallbacksBackend.start();
        cvCallbacksBackend.start();
        proxy.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

