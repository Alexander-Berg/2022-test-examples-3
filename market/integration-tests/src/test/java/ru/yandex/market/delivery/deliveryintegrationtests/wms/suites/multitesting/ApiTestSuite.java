package ru.yandex.market.delivery.deliveryintegrationtests.wms.suites.multitesting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.ExcludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

@DisplayName("Api suite")
@SelectPackages({"ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.servicebus",
        "ru.yandex.market.delivery.deliveryintegrationtests.wms.tests.api.inbound_management"})
@ExcludeTags("notForMultitesting")
@RunWith(JUnitPlatform.class)
public class ApiTestSuite {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(ApiTestSuite.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
