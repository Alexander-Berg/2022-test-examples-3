package ru.yandex.market.deliveryintegrationtests.delivery.suites;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludePackages;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

@RunWith(JUnitPlatform.class)
@DisplayName("Regress suite")
@ExcludeTags(value = {"CapacityTest", "SlowTest", "CheckouterSmokeTest", "ExcludeRegress"})
@ExcludePackages("ru.yandex.market.deliveryintegrationtests.delivery.tests.taxiintegration")
@SelectPackages("ru.yandex.market.deliveryintegrationtests.delivery.tests")
public class RegressSuiteTest {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(RegressSuiteTest.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
