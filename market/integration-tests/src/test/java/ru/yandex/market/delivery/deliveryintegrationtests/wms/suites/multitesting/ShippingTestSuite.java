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

@DisplayName("Shipping Suite")
@SelectPackages("ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order")
@IncludeTags("ShippingMultitestingSuite")
@RunWith(JUnitPlatform.class)
public class ShippingTestSuite {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(ShippingTestSuite.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
