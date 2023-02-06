package ru.yandex.zora.proxy;

import java.io.File;
import java.io.IOException;

import ru.yandex.client.tvm2.Tvm2ClientConfigBuilder;
import ru.yandex.client.tvm2.Tvm2ServiceConfigBuilder;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.config.HttpHostConfigBuilder;
import ru.yandex.http.config.HttpTargetConfigBuilder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.passport.tvmauth.Version;
import ru.yandex.zora.proxy.config.ZoraProxyConfigBuilder;

public class ZoraCluster implements GenericAutoCloseable<IOException> {
    private static final long TVM_RENEWAL_INTERVAL = 60000L;
    private static final String ZORA_SERVER = "ZoraServer";

    private final StaticServer zoraServer;
    private final StaticServer tvm2;
    private final ZoraProxy proxy;

    private final GenericAutoCloseableChain<IOException> chain;

    public ZoraCluster() throws Exception {
        try (GenericAutoCloseableHolder<
            IOException,
            GenericAutoCloseableChain<IOException>> chain =
                 new GenericAutoCloseableHolder<>(
                     new GenericAutoCloseableChain<>()))
        {
            zoraServer =
                new StaticServer(Configs.baseConfig(ZORA_SERVER));
            chain.get().add(zoraServer);
            zoraServer.start();

            tvm2 = new StaticServer(Configs.baseConfig("TVM2"));
            chain.get().add(tvm2);
            tvm2.start();
            tvm2.add(
                "/2/keys/?lib_version=" + Version.get(),
                new File(
                    StaticServer.class.getResource("tvm-keys.txt").toURI()));
            tvm2.add(
                "/2/ticket/",
                "{\"100\":{\"ticket\":\"here the ticket\"}}");

            Tvm2ServiceConfigBuilder tvm2ServiceConfig =
                new Tvm2ServiceConfigBuilder();
            new HttpHostConfigBuilder(Configs.hostConfig(tvm2))
                .copyTo(tvm2ServiceConfig);
            tvm2ServiceConfig.clientId(1);
            tvm2ServiceConfig.secret("1234567890123456789012");

            ZoraProxyConfigBuilder builder = new ZoraProxyConfigBuilder();
            builder.port(0).connections(2);
            builder.tvm2ServiceConfig(tvm2ServiceConfig);
            builder.tvm2Config(
                new Tvm2ClientConfigBuilder()
                    .destinationClientId("100")
                    .renewalInterval(TVM_RENEWAL_INTERVAL));
            builder.zoraclConfig(
                new HttpTargetConfigBuilder(
                    Configs.targetConfig()).proxy(zoraServer.host()));
            proxy = new ZoraProxy(builder.build());
            chain.get().add(proxy);

            proxy.start();

            this.chain = chain.release();
        }
    }

    public StaticServer zoraServer() {
        return zoraServer;
    }

    public StaticServer tvm2() {
        return tvm2;
    }

    public ZoraProxy proxy() {
        return proxy;
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}
