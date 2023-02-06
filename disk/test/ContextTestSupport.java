package ru.yandex.chemodan.util.test;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;

import ru.yandex.chemodan.boot.ChemodanMainSupport;
import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.misc.property.load.PropertiesLoader;

/**
 * @author tolmalev
 */
public abstract class ContextTestSupport {
    static {
        TestHelper.initialize();
    }

    @Test
    public void testContext() throws Exception {
        ConfigurableApplicationContext appContext = initContext();
        appContext.close();
    }

    protected ConfigurableApplicationContext initContext() {
        ChemodanMainSupport main = createMain();
        AppNameHolder.setIfNotPresent(main.applicationName());
        PropertiesLoader.initialize(new ChemodanPropertiesLoadStrategy(main.applicationName(), true));
        return main.loadApplicationContext();
    }

    public abstract ChemodanMainSupport createMain();
}
