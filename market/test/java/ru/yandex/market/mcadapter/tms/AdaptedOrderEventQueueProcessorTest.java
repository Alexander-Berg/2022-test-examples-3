package ru.yandex.market.mcadapter.tms;

import java.sql.Timestamp;
import java.time.Duration;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.mcadapter.AbstractFunctionalTest;
import ru.yandex.market.mcadapter.config.ClockForTests;
import ru.yandex.market.mcadapter.db.jooq.enums.AdaptedOrdersEventQueueRecordStatus;
import ru.yandex.market.mcadapter.db.jooq.tables.records.AdaptedOrdersEventQueueRecord;
import ru.yandex.market.mcadapter.logbroker.producer.LogbrokerAdaptedOrderEvent;
import ru.yandex.market.mcadapter.model.event.AdaptedOrderEvent;
import ru.yandex.market.mcadapter.model.event.AdaptedOrderEventType;
import ru.yandex.market.mcadapter.service.AdaptedOrderEventService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;

class AdaptedOrderEventQueueProcessorTest  extends AbstractFunctionalTest {
    @Autowired
    private AdaptedOrderEventService adaptedOrderEventService;
    @Autowired
    private AdaptedOrderEventQueueProcessor adaptedOrderEventQueueProcessor;
    @Autowired
    private LogbrokerEventPublisher<LogbrokerEvent<?>> logbrokerEventPublisher;
    @Autowired
    private ClockForTests clock;

    @Test
    public void shouldPublishAdaptedOrderEventNormally() {
        adaptedOrderEventService.publishEventAsync(
                AdaptedOrderEvent.builder()
                        .setPuid(1L)
                        .setType(AdaptedOrderEventType.CREATED)
                        .setUniqueKey("key")
                        .setCreatedTimestamp(Timestamp.from(clock.instant()))
                        .build()
        );

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
                .publishEvent(any());

        assertThat(adaptedOrderEventService.getAllEvents(), contains(
                allOf(hasProperty("status", equalTo(AdaptedOrdersEventQueueRecordStatus.SENT)))
        ));
    }

    @Test
    public void shouldRetryPublishingAdaptedOrderEvent() {
        Mockito.doThrow(RuntimeException.class).when(logbrokerEventPublisher).publishEvent(any());

        adaptedOrderEventService.publishEventAsync(
                AdaptedOrderEvent.builder()
                        .setPuid(1L)
                        .setType(AdaptedOrderEventType.CREATED)
                        .setUniqueKey("key")
                        .setCreatedTimestamp(Timestamp.from(clock.instant()))
                        .build()
        );

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
                .publishEvent(any());

        assertThat(adaptedOrderEventService.getAllEvents(), contains(
                allOf(hasProperty("status", equalTo(AdaptedOrdersEventQueueRecordStatus.IN_QUEUE)))
        ));


        Mockito.doNothing().when(logbrokerEventPublisher).publishEvent(any());
        Mockito.clearInvocations(logbrokerEventPublisher);

        clock.spendTime(Duration.ofMinutes(10));

        adaptedOrderEventQueueProcessor.process(Duration.ofMinutes(1));

        Mockito.verify(logbrokerEventPublisher, Mockito.times(1))
                .publishEvent(any());

        assertThat(adaptedOrderEventService.getAllEvents(), contains(
                allOf(hasProperty("status", equalTo(AdaptedOrdersEventQueueRecordStatus.SENT)))
        ));
    }
}
