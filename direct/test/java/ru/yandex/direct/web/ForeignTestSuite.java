package ru.yandex.direct.web;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.common.spring.TestingComponentWhiteListTest;
import ru.yandex.direct.testing.config.ProductionConfigContainsAllSettings;
import ru.yandex.direct.web.i18n.I18NBundleWebTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProductionConfigContainsAllSettings.class,
        TestingComponentWhiteListTest.class,
        I18NBundleWebTest.class
})
public class ForeignTestSuite {
}
