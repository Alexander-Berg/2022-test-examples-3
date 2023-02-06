package ru.yandex.passport;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;

import ru.yandex.base64.Base64Encoder;
import ru.yandex.collection.Pattern;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.FakeTvmServer;
import ru.yandex.http.test.ProxyHandler;
import ru.yandex.http.test.StaticHttpResource;
import ru.yandex.http.test.StaticServer;
import ru.yandex.parser.config.IniConfig;
import ru.yandex.passport.config.DocumentsProxyConfigBuilder;
import ru.yandex.test.search.backend.TestSearchBackend;
import ru.yandex.test.util.TestBase;

public class DocumentsProxyCluster implements GenericAutoCloseable<IOException> {
    private static final String PROXY_CONFIG = System.getenv("ARCADIA_SOURCE_ROOT") +
            "/mail/search/passport/data_provider/documents_proxy/files/documents_proxy.conf";
    private static final String LUCENE_CONFIG = System.getenv("ARCADIA_SOURCE_ROOT") +
            "/mail/search/passport/data_provider/documents_proxy/files/documents_backend.conf";

    public static final String TICKET_PASSP_PASSP =
            "3:serv:CBAQ__________9_IggIuvF7ELrxew:QwvVRscd2lKh-DA_ocbwbl3HS2ZGiViHXo_EaPeZyM4bS_v7mVqzLz_Xs0hOw_1DOd3Hr" +
                    "UeYe_N_IglRsFmVHRxOhU7y3K2Rb6xIVblpCNrAi8auupKhkqFj0E6vc2Hg0XeVYaeXqki4pOq4j0tRS93kEhHUJykTT1-VJrJCAKg";

    private final DocumentsProxy proxy;
    private final FakeTvmServer tvm2;
    private final StaticServer blackbox;
    private final StaticServer disk;
    private final StaticServer avatars;
    private final StaticServer producer;

    private final TestSearchBackend searchBackend;

    private final GenericAutoCloseableChain<IOException> chain;


    public DocumentsProxyCluster(final TestBase base) throws Exception {
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
            System.setProperty("BLACKBOX_HOST", "http://pass-test.yandex.ru");
            System.setProperty("BSCONFIG_IDIR", ".");
            System.setProperty("QUEUE_NAME", "documents");

            tvm2 = FakeTvmServer.fromContext(base, chain.get());
            tvm2.addTicket(2010646, TICKET_PASSP_PASSP);
            tvm2.addTicket(2002150, TICKET_PASSP_PASSP);
            tvm2.addTicket(2000031, TICKET_PASSP_PASSP);

            avatars = new StaticServer(Configs.baseConfig());
            chain.get().add(avatars);
            avatars.start();

            System.setProperty("SECRET", "1234567890123456789012");
            System.setProperty("TVM_CLIENT_ID", "2033601");
            System.setProperty("PASSPORT_TVM_ID", "2010646");
            System.setProperty("PASSPORT_FRONT_TVM_ID", "2021183");
            System.setProperty("PASSPORT_AUTOFILL_FRONT_TVM_ID", "2024245");
            System.setProperty("AVATAR_INT_HOST", avatars.host().toString());
            System.setProperty("AVATAR_EXT_HOST", avatars.host().toString());
            System.setProperty("MARKET_TVM_ID", "2033365");
            System.setProperty("AVATAR_CLIENT_ID", "2002150");
            System.setProperty("DISK_COPY_CLIENT_ID", "2000031");
            System.setProperty("BLACKBOX_TVM_ID", "222");
            System.setProperty("DISK_PROXY_HOST", "new-msearch-proxy.mail.yandex.net:8051");
            System.setProperty("ALLOWED_TVMS", "2033601,2010646,2021183,2024245,2033365");
            System.setProperty("TVM_ENV", "test");

            System.setProperty("AES_KEY", buildAesKey());
            System.setProperty("AES_IV", buildAesIv());

            DocumentsProxyConfigBuilder builder =
                new DocumentsProxyConfigBuilder(patchProxyConfig(new IniConfig(new File(PROXY_CONFIG))));
            builder.auths().auths().get(new Pattern<>("/document/*", true)).bypassLoopback(false);

            blackbox = new StaticServer(Configs.baseConfig());
            chain.get().add(blackbox);
            blackbox.start();
            builder.blackbox(Configs.hostConfig(blackbox));

            searchBackend =
                new TestSearchBackend(base, new File(LUCENE_CONFIG).toPath());
            chain.get().add(searchBackend);

            disk = new StaticServer(Configs.baseConfig());
            chain.get().add(disk);
            disk.start();
            builder.disk(Configs.hostConfig(disk));
            producer = new StaticServer(Configs.baseConfig());
            chain.get().add(producer);

            StaticHttpResource proxyHandler =
                new StaticHttpResource(
                    new ProxyHandler(searchBackend.indexerPort()));

            producer.add("/update?*", proxyHandler);
            producer.add("/add?*", proxyHandler);
            producer.add("/delete?*", proxyHandler);
            producer.add("/modify?*", proxyHandler);

            producer.add("/_status*", "[{$localhost\0:100500}]");
            producer.start();
            builder.searchMapConfig().content(searchBackend.searchMapRule("documents"));
            builder.producerClientConfig().host(producer.host());
            //System.setProperty("CACHE_SEARCH_PORT", Integer.toString(searchBackend.searchPort()));
            //System.setProperty("CACHE_MODIFY_PORT", Integer.toString(searchBackend.indexerPort()));

            proxy = new DocumentsProxy(builder.build());

            this.chain = chain.release();
            proxy.start();
        }
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }

    private IniConfig patchProxyConfig(final IniConfig config) {
        config.sections().remove("log");
        config.sections().remove("accesslog");
        config.sections().remove("searchmap");
        config.section("server").sections().remove("https");
        IniConfig server = config.section("server");
        server.put("port", "0");
        return config;
    }

    private static String buildAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        Key key = keyGenerator.generateKey();
        byte[] bytes = key.getEncoded();
        Base64Encoder encoder = new Base64Encoder();
        encoder.process(bytes);
        return encoder.toString();
    }

    private static String buildAesIv() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        Base64Encoder encoder = new Base64Encoder();
        encoder.process(bytes);
        return encoder.toString();
    }


    public DocumentsProxy proxy() {
        return proxy;
    }

    public StaticServer tvm2() {
        return tvm2;
    }

    public StaticServer blackbox() {
        return blackbox;
    }

    public StaticServer disk() {
        return disk;
    }

    public StaticServer avatars() {
        return avatars;
    }

    public TestSearchBackend searchBackend() {
        return searchBackend;
    }
}
