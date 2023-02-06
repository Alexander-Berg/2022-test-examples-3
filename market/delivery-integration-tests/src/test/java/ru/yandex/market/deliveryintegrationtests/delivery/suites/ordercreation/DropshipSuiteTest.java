package ru.yandex.market.deliveryintegrationtests.delivery.suites.ordercreation;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

@RunWith(JUnitPlatform.class)
@DisplayName("Dropship order creation")
@IncludeTags("DropshipOrderCreationTest")
@SelectPackages("ru.yandex.market.deliveryintegrationtests.delivery.tests")
public class DropshipSuiteTest {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(DropshipSuiteTest.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
