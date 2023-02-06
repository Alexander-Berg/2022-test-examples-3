package ru.yandex.market.fulfillment.stockstorage.service.audit;

import java.time.Clock;
import java.time.LocalDateTime;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import ru.yandex.market.fulfillment.stockstorage.domain.dto.EventType;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.PayloadType;
import ru.yandex.market.fulfillment.stockstorage.domain.entity.UnitId;
import ru.yandex.market.fulfillment.stockstorage.events.stock.EventWrapper;
import ru.yandex.market.fulfillment.stockstorage.events.stock.StockEvent;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fulfillment.stockstorage.service.audit.EventAuditFieldConstants.UNIT_ID;

public class AbstractEventAuditServiceTest {

    private ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    AbstractEventAuditService abstractEventAuditService =
            new AbstractEventAuditService(eventPublisher, PayloadType.SKU, Clock.systemDefaultZone());

    @Test
    public void doLog() {
        abstractEventAuditService.doLogAsync(
                StockEvent.builder(LocalDateTime.now())
                        .withPayload(ImmutableMap.of(UNIT_ID, new Object()))
                        .withId("id")
                        .withEventType(EventType.SKU_CREATED)
                        .withPayloadType(PayloadType.SKU)
                        .build());
        verify(eventPublisher).publishEvent(any(EventWrapper.class));
    }

    @Test
    public void toEventAudit() {
        StockEvent stockEvent =
                abstractEventAuditService.toEventAudit(getPayload(), "id", EventType.SKU_CREATED);
        assertEquals(getPayload().size(), stockEvent.getPayload().size());
        assertEquals(getPayload().get(UNIT_ID), stockEvent.getPayload().get(UNIT_ID.getValue()));
        assertEquals("id", stockEvent.getTargetId());
        assertEquals(PayloadType.SKU, stockEvent.getPayloadType());
        assertEquals(EventType.SKU_CREATED, stockEvent.getEventType());
    }

    private ImmutableMap<EventAuditFieldConstants, Object> getPayload() {
        return ImmutableMap.of(UNIT_ID, new UnitId());
    }
}
