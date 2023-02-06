package ru.yandex.market.pipelinetests.suites;

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
@DisplayName("B2B Create/Cancel order Test")
@IncludeTags("B2BFulfillmentOrderTest")
@SelectPackages("ru.yandex.market.pipelinetests.tests")
public class B2BFulfillmentSuiteTest {
    @Test
    public void runSuite() {
        JUnitCore junit = new JUnitCore();
        junit.addListener(new RunListener());
        Result run = junit.run(B2BFulfillmentSuiteTest.class);
        Assertions.assertTrue(run.wasSuccessful(), "Запуск не успешный");
    }
}
