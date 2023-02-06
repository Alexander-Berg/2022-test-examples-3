package ru.yandex.direct.common;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.common.i18n.I18NBundleCurrencyTest;
import ru.yandex.direct.common.spring.TestingComponentWhiteListTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        I18NBundleCurrencyTest.class,
        TestingComponentWhiteListTest.class
})
public class ForeignTestSuite {
}
