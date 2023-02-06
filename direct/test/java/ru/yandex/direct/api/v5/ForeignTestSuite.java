package ru.yandex.direct.api.v5;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.api.v5.i18n.I18NBundleApiTest;
import ru.yandex.direct.common.spring.TestingComponentWhiteListTest;
import ru.yandex.direct.testing.config.ProductionConfigContainsAllSettings;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProductionConfigContainsAllSettings.class,
        TestingComponentWhiteListTest.class,
        I18NBundleApiTest.class
})
public class ForeignTestSuite {
}
