package ru.yandex.mail.so.cwacf;

import java.io.IOException;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.http.util.server.BaseServerConfigBuilder;

public class CwacfCluster extends GenericAutoCloseableHolder<
    IOException,
    GenericAutoCloseableChain<IOException>>
{
    private final StaticServer upstream;
    private final Cwacf cwacf;

    public CwacfCluster() throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            upstream = new StaticServer(Configs.baseConfig("Upstream"));
            chain.get().add(upstream);

            CwacfConfigBuilder builder = new CwacfConfigBuilder();
            new BaseServerConfigBuilder(Configs.baseConfig())
                .copyTo(builder);
            builder.upstreamConfig(Configs.hostConfig(upstream));
            cwacf = new Cwacf(builder.build());
            chain.get().add(cwacf);

            reset(chain.release());
        }
    }

    public void start() throws IOException {
        upstream.start();
        cwacf.start();
    }

    public StaticServer upstream() {
        return upstream;
    }

    public Cwacf cwacf() {
        return cwacf;
    }
}

