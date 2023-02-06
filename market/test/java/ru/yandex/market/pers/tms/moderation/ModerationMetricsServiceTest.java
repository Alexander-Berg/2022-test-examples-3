package ru.yandex.market.pers.tms.moderation;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.tms.MockedPersTmsTest;


public class ModerationMetricsServiceTest extends MockedPersTmsTest {
    @Autowired
    private ModerationMetricsService metricsService;

    @Test
    public void test() {
        //Yes, it's a bad practice to write tests without assertions and expectations.
        //However, this test is better than nothing while ModerationMetricsService is just writing logs
        metricsService.logProcessedGradesCount();
        metricsService.logModerationReadyGradesCount();
    }

}