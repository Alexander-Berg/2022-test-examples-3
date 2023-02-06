package ru.yandex.mail.hackathon;

import java.io.IOException;

import ru.yandex.function.GenericAutoCloseable;
import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.mail.hackathon.config.HackathonConfigBuilder;
import ru.yandex.test.util.TestBase;

public class HackathonCluster implements GenericAutoCloseable<IOException> {
    private static final int CONNECTIONS = 1000;
    private static final int TIMEOUT = 10000;
    private static final int WORKERS = 4;

    private final Hackathon hackathon;
    private final GenericAutoCloseableChain<IOException> chain;

    public HackathonCluster(final TestBase testBase) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                new GenericAutoCloseableHolder<>(
                    new GenericAutoCloseableChain<>()))
        {
            HackathonConfigBuilder builder = new HackathonConfigBuilder();
            builder.port(0);
            builder.connections(CONNECTIONS);
            builder.timeout(TIMEOUT);
            builder.workers(WORKERS);

            hackathon = new Hackathon(builder.build());
            chain.get().add(hackathon);

            this.chain = chain.release();
        }
    }

    public Hackathon hackathon() {
        return hackathon;
    }

    public void start() throws IOException {
        hackathon.start();
    }

    @Override
    public void close() throws IOException {
        chain.close();
    }
}

