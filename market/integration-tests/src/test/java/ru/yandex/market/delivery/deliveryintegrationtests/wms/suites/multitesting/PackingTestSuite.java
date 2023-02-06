package ru.yandex.market.delivery.deliveryintegrationtests.wms.suites.multitesting;

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

@DisplayName("Packing Suite")
@SelectPackages("ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order")
@IncludeTags("PackingMultitestingSuite")
@RunWith(JUnitPlatform.class)
public class PackingTestSuite {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(PackingTestSuite.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
