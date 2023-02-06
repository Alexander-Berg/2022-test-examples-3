package ru.yandex.direct.intapi;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.common.spring.TestingComponentWhiteListTest;
import ru.yandex.direct.intapi.i18n.I18NBundleIntapiTest;
import ru.yandex.direct.testing.config.ProductionConfigContainsAllSettings;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProductionConfigContainsAllSettings.class,
        TestingComponentWhiteListTest.class,
        I18NBundleIntapiTest.class
})
public class ForeignTestSuite {
}
