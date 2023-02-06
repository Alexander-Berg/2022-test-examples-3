package ru.yandex.market.deliveryintegrationtests.delivery.suites;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.RunListener;
import ru.yandex.market.deliveryintegrationtests.delivery.tests.ondemand.lavka.LavkaHourSlotTest;

@RunWith(JUnitPlatform.class)
@DisplayName("LavkaHourSlotTest test suite")
@SelectClasses({LavkaHourSlotTest.class})
public class HourSlotSuiteTest {

    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(HourSlotSuiteTest.class);
        Assertions.assertTrue(run.wasSuccessful(), "Есть проблемы в запуске автотестов");
    }
}
