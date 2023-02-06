package ru.yandex.direct.logviewercore;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import ru.yandex.direct.testing.config.ProductionConfigContainsAllSettings;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        ProductionConfigContainsAllSettings.class
})
public class ForeignTestSuite {
}
