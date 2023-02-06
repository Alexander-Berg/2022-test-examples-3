package ru.yandex.chemodan.app.dataapi.core.dao.test;

import org.junit.runners.model.InitializationError;

import ru.yandex.chemodan.app.dataapi.core.dao.support.DataApiRandomValueGenerator;
import ru.yandex.commune.random.RandomValueGenerator;
import ru.yandex.commune.test.random.RunWithRandomTestRunner;

/**
 * @author tolmalev
 */
public class DataApiRunWithRandomTestRunner extends RunWithRandomTestRunner {

    public DataApiRunWithRandomTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected RandomValueGenerator getRandomValueGenerator() {
        return new DataApiRandomValueGenerator();
    }
}
