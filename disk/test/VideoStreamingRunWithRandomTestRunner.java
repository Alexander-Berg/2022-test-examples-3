package ru.yandex.chemodan.app.videostreaming.test;

import org.junit.runners.model.InitializationError;

import ru.yandex.commune.random.RandomValueGenerator;
import ru.yandex.commune.test.random.RunWithRandomTestRunner;

/**
 * @author tolmalev
 */
public class VideoStreamingRunWithRandomTestRunner extends RunWithRandomTestRunner {
    public VideoStreamingRunWithRandomTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected RandomValueGenerator getRandomValueGenerator() {
        return new VideoStreamingRandomValueGenerator();
    }
}
