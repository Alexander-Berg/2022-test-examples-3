package ru.yandex.dispatcher;

import ru.yandex.msearch.Daemon;
import ru.yandex.test.util.TestBase;

public class ZooClusterConfig {
    private boolean startProducer = false;
    private boolean startConsumer = false;
    private Daemon searchBackend = null;
    private int queues = 1;

    private final TestBase testBase;

    public ZooClusterConfig(final TestBase testBase) {
        this.testBase = testBase;
    }

    public boolean startProducer() {
        return startProducer;
    }

    public ZooClusterConfig startProducer(final boolean startProducer) {
        this.startProducer = startProducer;
        return this;
    }

    public boolean startConsumer() {
        return startConsumer;
    }

    public ZooClusterConfig startConsumer(final boolean startConsumer) {
        this.startConsumer = startConsumer;
        return this;
    }

    public Daemon searchBackend() {
        return searchBackend;
    }

    public ZooClusterConfig searchBackend(final Daemon searchBackend) {
        this.searchBackend = searchBackend;
        return this;
    }

    public TestBase testBase() {
        return testBase;
    }

    public int queues() {
        return queues;
    }

    public ZooClusterConfig queues(final int queues) {
        this.queues = queues;
        return this;
    }
}
