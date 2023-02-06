package ru.yandex.reminders.util;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.misc.db.monica.JdbcMetricsSwitch;
import ru.yandex.misc.env.EnvironmentType;
import ru.yandex.misc.env.EnvironmentTypeReader;
import ru.yandex.misc.property.load.PropertiesLoader;
import ru.yandex.misc.version.AppName;
import ru.yandex.reminders.boot.Main;
import ru.yandex.reminders.boot.RemindersAppName;

@RunWith(SpringJUnit4ClassRunner.class)
public abstract class TestUtils {

    @BeforeClass
    public static void initializeProperties() {
        System.setProperty(EnvironmentTypeReader.YANDEX_ENVIRONMENT_TYPE_PROPERTY, EnvironmentType.TESTS.getValue());
        JdbcMetricsSwitch.ENABLED = true;
        JdbcMetricsSwitch.COMMAND_METRICS_ENABLED = true;

        // this line required for Ant unit test runner for classes with @RunWith(SpringJUnit4ClassRunner.class)
        // properties loaded here will be overridden later -- in loadProperties() on per-test basis
        PropertiesLoader.initialize(
                new TestsPropertiesLoadStrategy(RemindersAppName.API, Main.ROOT_PACKAGE_PATH, true)
        );
    }

    @Before
    public void loadProperties() {
        PropertiesLoader.initialize(new TestsPropertiesLoadStrategy(getAppName(), Main.ROOT_PACKAGE_PATH, true));
    }

    protected AppName getAppName() {
        return new AppName() {
            @Override
            public String serviceName() {
                return "reminders";
            }

            @Override
            public String appName() {
                return getApp();
            }
        };
    }

    protected String getApp() {
        return "tests";
    }

    private static long lastUsedNanoTime = 0;

    protected static synchronized long getUniqueNanoTime() {
        long result = System.nanoTime();
        if (result <= lastUsedNanoTime) {
            result = lastUsedNanoTime + 1;
        }
        lastUsedNanoTime = result;
        return result;
    }
}
