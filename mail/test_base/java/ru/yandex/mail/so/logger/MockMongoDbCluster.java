package ru.yandex.mail.so.logger;

import java.io.IOException;

import ru.yandex.logger.PrefixedLogger;

public class MockMongoDbCluster implements Cluster {
    public static final String DB_NAME = "rules";

    private final PrefixedLogger logger;
    private int port;
    //private Fongo mongo;

    public MockMongoDbCluster(final PrefixedLogger logger) {
        this.logger = logger;
    }

    @Override
    public void start() throws IOException {
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public PrefixedLogger logger() {
        return logger;
    }

    /*public Fongo mongod() {
        return mongo;
    }*/
}
