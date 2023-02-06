package ru.yandex.market.deliveryintegrationtests.delivery.suites;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;

@DisplayName("Pvz suite")
@RunWith(JUnitPlatform.class)
@SelectPackages("ru.yandex.market.deliveryintegrationtests.delivery.tests.ondemand.pvz")
public class PvzSuiteTest {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(PvzSuiteTest.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
