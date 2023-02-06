package ru.yandex.market.tpl.core.domain.yago;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.gateway.common.model.delivery.OrderStatusType;
import ru.yandex.market.tpl.core.domain.yago.history.YandexGoOrderHistoryRecord;

import static org.assertj.core.api.Assertions.assertThat;

class YandexGoOrderTest {

    public static final String EXTERNAL_ORDER_ID = "123";
    public static final long YANDEXGO_ORDER_ID = 1L;
    public static final String EXTERNAL_YANDEXGO_ORDER_ID = "external-yandexgo-order-id";
    public static final Long USER_SHIFT_ID = 456L;

    @Test
    void shouldSetCreatingStatus_whenInit() {
        // given
        YandexGoOrder yaGoOrder = createYandexGoOrder(YANDEXGO_ORDER_ID);

        YandexGoOrderCommand.Create command =
                new YandexGoOrderCommand.Create(
                        YANDEXGO_ORDER_ID,
                        EXTERNAL_ORDER_ID,
                        USER_SHIFT_ID,
                        0,
                        OffsetDateTime.now());
        // when
        yaGoOrder.init(command);

        // then
        assertThat(yaGoOrder.getStatus()).isEqualTo(OrderStatusType.ORDER_CREATED_BUT_NOT_APPROVED);
    }

    @Test
    void shouldSetCreatedStatusAndSetYandexGoOrderIdAndTrackId_whenConfirmed() {
        // given
        YandexGoOrder yaGoOrder = createYandexGoOrder(YANDEXGO_ORDER_ID).setExternalOrderId(EXTERNAL_ORDER_ID);
        yaGoOrder.prepareToCreateInYandexGo();

        YandexGoOrderCommand.Confirm command =
                new YandexGoOrderCommand.Confirm(yaGoOrder.getId(), EXTERNAL_YANDEXGO_ORDER_ID);

        // when
        yaGoOrder.confirm(command);

        // then
        assertThat(yaGoOrder.getStatus()).isEqualTo(OrderStatusType.ORDER_CREATED);
        assertThat(yaGoOrder.getExternalYandexGoOrderId()).isEqualTo(EXTERNAL_YANDEXGO_ORDER_ID);
        assertThat(yaGoOrder.getTrackId()).isEqualTo(EXTERNAL_YANDEXGO_ORDER_ID);
    }

    @Test
    void shouldSetUpdatedStatus_whenUpdateStatus() {
        // given
        YandexGoOrder yaGoOrder = createYandexGoOrder(YANDEXGO_ORDER_ID);

        YandexGoOrderCommand.UpdateStatus command =
                new YandexGoOrderCommand.UpdateStatus(YANDEXGO_ORDER_ID, OrderStatusType.ORDER_CANCELLED_BY_CUSTOMER);

        // when
        yaGoOrder.updateStatus(command);

        // then
        assertThat(yaGoOrder.getStatus()).isEqualTo(OrderStatusType.ORDER_CANCELLED_BY_CUSTOMER);
    }

    private YandexGoOrder createYandexGoOrder(long orderId) {
        YandexGoOrder yaGoOrder = new YandexGoOrder();
        yaGoOrder.setId(orderId);
        return yaGoOrder;
    }

    private void assertHistoryRecordAdded(List<YandexGoOrderHistoryRecord> historyRecords, OrderStatusType status) {
        assertThat(historyRecords).hasSize(1);
        assertThat(historyRecords.get(0).getStatus()).isEqualTo(status);
    }
}
