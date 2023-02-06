package ru.yandex.market.delivery.deliveryintegrationtests.wms.suites.multitesting;

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

@DisplayName("Order suite")
@SelectPackages("ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order")
@ExcludePackages({
        "ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order.OutboundTest",
        "ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order.ShipOutboundByOrderKeyTest",
        "ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order.OrderWithShippingConveyorTest",
        "ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.selenium.order.OrderWithShortageTest"
})
@RunWith(JUnitPlatform.class)
public class OrderTestSuite {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(OrderTestSuite.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}

