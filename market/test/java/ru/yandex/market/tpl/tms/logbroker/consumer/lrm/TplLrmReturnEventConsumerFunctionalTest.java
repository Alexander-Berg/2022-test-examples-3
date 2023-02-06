package ru.yandex.market.tpl.tms.logbroker.consumer.lrm;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.logbroker.consumer.LogbrokerMessage;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.tms.test.TplTmsAbstractTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RequiredArgsConstructor
class TplLrmReturnEventConsumerFunctionalTest extends TplTmsAbstractTest {

    private final OrderGenerateService orderGenerateService;
    private final TplLrmReturnEventConsumer tplLrmReturnEventConsumer;
    private final OrderRepository orderRepository;
    private final DbQueueTestUtil dbQueueTestUtil;

    @Test
    void processSuccess() {
        //given
        Order order = orderGenerateService.createOrder(OrderGenerateService
                .OrderGenerateParam
                .builder()
                .flowStatus(OrderFlowStatus.TRANSMITTED_TO_RECIPIENT_AND_NOT_PACK_RETURN_BOXES)
                .build());

        LogbrokerMessage message = getMessage(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED, order.getExternalOrderId());

        //when
        tplLrmReturnEventConsumer.accept(message);

        //then
        Optional<Order> updatedOrderO = orderRepository.findByExternalOrderId(order.getExternalOrderId());

        assertTrue(updatedOrderO.isPresent());
        assertEquals(updatedOrderO.get().getDsApiCheckpoint(), OrderFlowStatus.TRANSMITTED_TO_RECIPIENT.getCode());
        assertEquals(updatedOrderO.get().getOrderFlowStatus(), OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);

        dbQueueTestUtil.assertQueueHasSize(QueueType.DLQ_LRM_RETURN_EVENT, 0);
    }

    @Test
    void processFailure_whenInappropriateFlowStatus() {
        //given
        Order order = orderGenerateService.createOrder(OrderGenerateService
                .OrderGenerateParam
                .builder()
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        LogbrokerMessage message = getMessage(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED, order.getExternalOrderId());

        //when
        tplLrmReturnEventConsumer.accept(message);

        //then
        dbQueueTestUtil.assertQueueHasSize(QueueType.DLQ_LRM_RETURN_EVENT, 1);
    }

    @Test
    void processFailure_whenOrderNotExists() {
        //given
        Order order = orderGenerateService.createOrder(OrderGenerateService
                .OrderGenerateParam
                .builder()
                .flowStatus(OrderFlowStatus.CREATED)
                .build());

        LogbrokerMessage message = getMessage(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED, "FAKE_ORDER_ID");

        tplLrmReturnEventConsumer.accept(message);

        //then
        dbQueueTestUtil.assertQueueHasSize(QueueType.DLQ_LRM_RETURN_EVENT, 1);
    }

    private LogbrokerMessage getMessage(ReturnEventType eventType, String orderId) {
        return new LogbrokerMessage(
                "",
                "{" +
                        "\"id\":294," +
                        "\"returnId\":1," +
                        "\"eventType\":\"" + eventType + "\"," +
                        "\"orderExternalId\":\"" + orderId + "\"," +
                        "\"created\":\"2021-09-28T11:40:07.969476Z\"," +
                        "\"payload\":null" +
                    "}"
        );
    }
}
