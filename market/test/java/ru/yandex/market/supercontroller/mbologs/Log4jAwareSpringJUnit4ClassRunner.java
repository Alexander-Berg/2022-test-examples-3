package ru.yandex.market.supercontroller.mbologs;

import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author amaslak
 */
public class Log4jAwareSpringJUnit4ClassRunner extends SpringJUnit4ClassRunner {

    public Log4jAwareSpringJUnit4ClassRunner(Class<?> clazz) throws InitializationError {
        super(clazz);
    }
}
