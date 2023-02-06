package ru.yandex.antifraud_runner;

import java.io.IOException;

import ru.yandex.function.GenericAutoCloseableChain;
import ru.yandex.function.GenericAutoCloseableHolder;
import ru.yandex.test.util.TestBase;

public class Cluster extends GenericAutoCloseableHolder<
        IOException,
        GenericAutoCloseableChain<IOException>> {
    private final Runner runner;

    public Cluster(TestBase env) throws Exception {
        try (GenericAutoCloseableHolder<
                IOException,
                GenericAutoCloseableChain<IOException>> chain =
                     new GenericAutoCloseableHolder<>(
                             new GenericAutoCloseableChain<>())) {

            runner = new Runner();
            reset(chain.release());
        }
    }

    public Runner getRunner() {
        return runner;
    }
}

