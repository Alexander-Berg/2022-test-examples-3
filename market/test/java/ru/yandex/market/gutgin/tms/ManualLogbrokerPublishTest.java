package ru.yandex.market.gutgin.tms;

import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.gutgin.tms.config.TestManualLogbrokerConfig;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.logbroker.model.LogbrokerCluster;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

/**
 * Ручной тест для проверки возможности публикации события в логброкер.
 */
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestManualLogbrokerConfig.class})
public class ManualLogbrokerPublishTest {
    @Autowired
    LogbrokerService logbrokerService;

    @Autowired
    LogbrokerCluster logbrokerCluster;

    @Test
    public void shouldPublishEvent() {
        logbrokerService.publishEvent(new LogbrokerEvent<String>() {
            private final String payload = "TEST_EVENT";
            @NotNull
            @Override
            public byte[] getBytes() {
                return payload.getBytes();
            }

            @NotNull
            @Override
            public String getPayload() {
                return payload;
            }
        });
    }
}
