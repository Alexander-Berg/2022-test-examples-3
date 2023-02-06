package ru.yandex.cokemulator;

import java.io.IOException;

import org.apache.http.entity.ContentType;

import ru.yandex.devtools.test.Paths;
import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.http.test.Configs;
import ru.yandex.http.test.StaticServer;
import ru.yandex.jniwrapper.JniWrapperConfigBuilder;

public class CokemulatorCluster implements GenericAutoCloseable<IOException> {
    public static final String LIBRARY_NAME =
        Paths.getBuildPath(
            "mail/library/jniwrapper/dynamic_test/libjniwrapper-test.so");

    private final StaticServer storage;
    private final StaticServer tikaite;
    private final Cokemulator cokemulator;
    private final GenericAutoCloseableChain<IOException> chain;

    public CokemulatorCluster(final String main) throws Exception {
        this(
            new CokemulatorConfigBuilder()
                .jniWrapperConfig(
                    new JniWrapperConfigBuilder()
                        .workers(1)
                        .queueSize(2)
                        .mainName(main)));
    }

    public CokemulatorCluster(final CokemulatorConfigBuilder builder)
        throws Exception
    {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            storage = new StaticServer(Configs.baseConfig("Storage"));
            chain.get().add(storage);
            tikaite = new StaticServer(Configs.baseConfig("Tikaite"));
            chain.get().add(tikaite);
            builder
                .port(0)
                .connections(2)
                .concurrency(1)
                .tikaiteConfig(Configs.hostConfig(tikaite))
                .contentType(ContentType.TEXT_PLAIN);
            if (builder.storageConfig().host() == null) {
                builder.storageConfig(Configs.hostConfig(storage));
            }
            builder.jniWrapperConfig().libraryName(LIBRARY_NAME);
            cokemulator = new Cokemulator(builder.build());
            chain.get().add(cokemulator);
            this.chain = chain.release();
        }
    }

    public void start() throws IOException {
        storage.start();
        tikaite.start();
        cokemulator.start();
    }

    public StaticServer storage() {
        return storage;
    }

    public StaticServer tikaite() {
        return tikaite;
    }

    public Cokemulator cokemulator() {
        return cokemulator;
    }

    public String uri() throws IOException {
        return "http://localhost:" + cokemulator.port();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

