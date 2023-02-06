package ru.yandex.market.ir.tms.utils;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

/**
 * @author amaslak
 */
public class Log4jAwareClassRunner extends BlockJUnit4ClassRunner {

    public Log4jAwareClassRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }
}
