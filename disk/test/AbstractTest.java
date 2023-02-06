package ru.yandex.chemodan.util.test;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import ru.yandex.chemodan.boot.ChemodanInitContextConfiguration;
import ru.yandex.chemodan.boot.ChemodanPropertiesLoadStrategy;
import ru.yandex.chemodan.test.TestHelper;
import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.commune.alive2.location.TestLocationResolverConfiguration;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.spring.ApplicationContextUtils;
import ru.yandex.misc.spring.context.EnvironmentTypeTestsContextConfiguration;
import ru.yandex.misc.test.TestBase;
import ru.yandex.misc.version.AppName;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author vavinov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ChemodanInitContextConfiguration.class,
        TestLocationResolverConfiguration.class,
        EnvironmentTypeTestsContextConfiguration.class},
        loader = AbstractTest.TestAnnotationConfigContextLoader.class)
public abstract class AbstractTest extends TestBase {

    @BeforeClass
    public static void setup() {
        TestHelper.initialize();
    }

    public static class TestAnnotationConfigContextLoader extends AnnotationConfigContextLoader {
        @Override
        protected void customizeContext(GenericApplicationContext context) {
            PropertiesLoader.initialize(
                    new ChemodanPropertiesLoadStrategy(createAppName(), true));
            ApplicationContextUtils.registerSingleton(context, "appName", createAppName());
        }
    }

    private static AppName createAppName() {
        AppNameHolder.setIfNotPresent(new SimpleAppName("chemodan", "uploader"));
        return AppNameHolder.get();
    }
}
