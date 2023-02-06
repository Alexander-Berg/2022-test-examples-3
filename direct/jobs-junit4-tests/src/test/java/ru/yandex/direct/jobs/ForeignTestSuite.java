package ru.yandex.direct.jobs;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.common.spring.TestingComponentWhiteListTest;
import ru.yandex.direct.jobs.i18n.I18NBundleJobsTest;
import ru.yandex.direct.testing.config.ProductionConfigContainsAllSettings;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProductionConfigContainsAllSettings.class,
        TestingComponentWhiteListTest.class,
        I18NBundleJobsTest.class
})
public class ForeignTestSuite {
}
