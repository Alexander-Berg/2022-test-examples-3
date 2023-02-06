package ru.yandex.market.loyalty.admin.tms;

import java.util.concurrent.CompletableFuture;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.bolts.collection.Unit;
import ru.yandex.inside.solomon.pusher.SolomonPusher;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.test.TestFor;
import ru.yandex.misc.monica.solomon.sensors.PushSensorsData;
import ru.yandex.misc.monica.solomon.sensors.Sensor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.either;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.when;

@TestFor(SolomonPusherTmsProcessor.class)
public class SolomonPusherTmsProcessorTest extends MarketLoyaltyAdminMockedDbTest {

    @Autowired
    private SolomonPusherTmsProcessor solomonPusherTmsProcessor;

    @MockBean
    private SolomonPusher solomonPusher;

    @Captor
    ArgumentCaptor<PushSensorsData> pushSensorsDataArgumentCaptor;

    @Test
    public void pushTest() throws Exception {
        when(solomonPusher.push(any())).thenReturn(CompletableFuture.completedFuture(Unit.U));

        solomonPusherTmsProcessor.pushStatisticToSolomon();

        then(solomonPusher).should().push(pushSensorsDataArgumentCaptor.capture());

        PushSensorsData pushedData = pushSensorsDataArgumentCaptor.getValue();
        assertThat(pushedData.commonLabels, allOf(
                hasEntry("project", "market-loyalty"),
                either(hasEntry("environment", "LOCAL")).or(hasEntry("environment", "TESTING")),
                hasEntry("sensor", "table-statistic"),
                hasEntry("service", "back"),
                hasEntry("cluster", "stable")));

        for (Sensor sensor : pushedData.sensors) {
            assertThat(sensor.labels, allOf(
                        hasKey("statistic_type"),
                        hasKey("table_name")));
            assertThat(sensor.value, greaterThanOrEqualTo(0D));
        }
    }
}
