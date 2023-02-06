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

@RunWith(JUnitPlatform.class)
@DisplayName("TM Pipeline suite")
@SelectPackages("ru.yandex.market.deliveryintegrationtests.delivery.tests.transportation")
public class TmPipelineSuiteTest {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(TmPipelineSuiteTest.class);
        Assertions.assertTrue(run.wasSuccessful(), "Есть проблемы в запуске автотестов");
    }
}
