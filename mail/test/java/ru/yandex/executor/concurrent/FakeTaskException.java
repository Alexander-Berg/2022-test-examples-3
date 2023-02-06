package ru.yandex.executor.concurrent;

public class FakeTaskException extends Exception {
    private static final long serialVersionUID = 0L;

    private final long retryInterval;

    public FakeTaskException(final long retryInterval) {
        this.retryInterval = retryInterval;
    }

    public long retryInterval() {
        return retryInterval;
    }
}

