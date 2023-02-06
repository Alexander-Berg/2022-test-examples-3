package ru.yandex.market.replenishment.autoorder.service.solomon;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.monlib.metrics.labels.Labels;
public class AutoorderSolomonPushServiceTest extends FunctionalTest {

    @Autowired
    private AutoorderSolomonPushService autoorderSolomonPushService;

    @Test
    public void test() {
        autoorderSolomonPushService.push("testSensor", 1L, Labels.of());
    }
}
