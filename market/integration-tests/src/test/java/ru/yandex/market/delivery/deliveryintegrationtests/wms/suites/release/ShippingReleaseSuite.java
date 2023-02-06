package ru.yandex.market.delivery.deliveryintegrationtests.wms.suites.release;

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

@DisplayName("Shipping Release Suite")
@SelectPackages("ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order")
@IncludeTags("ShippingReleaseSuite")
@RunWith(JUnitPlatform.class)
public class ShippingReleaseSuite {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(ShippingReleaseSuite.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
