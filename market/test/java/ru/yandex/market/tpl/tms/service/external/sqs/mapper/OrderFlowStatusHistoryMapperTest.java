package ru.yandex.market.tpl.tms.service.external.sqs.mapper;

import java.time.Clock;
import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.les.OrderNewCheckpointEvent;
import ru.yandex.market.logistics.les.tracker.enums.ApiVersion;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.domain.order.OrderFlowStatusHistory;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
class OrderFlowStatusHistoryMapperTest extends TplTmsAbstractTest {

    private final OrderFlowStatusHistoryMapper mapper;
    private final SortingCenterService sortingCenterService;
    private final Clock clock;

    private long eventId;
    private long trackId;
    private String yandexId;
    private Instant orderFlowStatusUpdatedAt;
    private String expectedToken;

    @BeforeEach
    void init() {
        eventId = 456L;
        trackId = 123L;
        yandexId = "987654321";
        orderFlowStatusUpdatedAt = clock.instant();

        DeliveryService deliveryService = sortingCenterService.findDsById(DeliveryService.DEFAULT_DS_ID);
        expectedToken = deliveryService.getToken();
    }

    @Test
    void map() {
        OrderFlowStatusHistory event = getEvent();

        OrderNewCheckpointEvent lesEvent = mapper.map(
                event
        );

        assertThat(lesEvent.getStatus()).isEqualTo(OrderFlowStatus.CREATED.getCode());
        assertThat(lesEvent.getYandexEntityId()).isEqualTo(yandexId);
        assertThat(lesEvent.getPartnerEntityId()).isEqualTo("" + trackId);
        assertThat(lesEvent.getCheckpointTs()).isEqualTo(orderFlowStatusUpdatedAt);
        assertThat(lesEvent.getApiVersion()).isEqualTo(ApiVersion.DS);
        assertThat(lesEvent.getToken().getValue()).isEqualTo(expectedToken);
    }

    private OrderFlowStatusHistory getEvent() {
        OrderFlowStatusHistory event = new OrderFlowStatusHistory();
        event.setId(eventId);
        event.setOrderId(trackId);
        event.setExternalOrderId(yandexId);
        event.setDeliveryServiceId(DeliveryService.DEFAULT_DS_ID);
        event.setOrderFlowStatusAfter(OrderFlowStatus.CREATED);
        event.setDsApiCheckpoint(OrderFlowStatus.CREATED.getCode());
        event.setOrderFlowStatusUpdatedAt(orderFlowStatusUpdatedAt);
        return event;
    }
}
