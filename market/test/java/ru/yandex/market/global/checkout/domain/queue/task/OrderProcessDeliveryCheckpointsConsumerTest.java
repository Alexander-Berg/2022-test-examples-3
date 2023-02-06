package ru.yandex.market.global.checkout.domain.queue.task;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Arrays;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.event.EventQueryService;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.order.OrderQueryService;
import ru.yandex.market.global.checkout.domain.order.update_delivery_status_task.OrderProcessDeliveryCheckpointsConsumer;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Event;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderDelivery;
import ru.yandex.market.logistic.api.client.DeliveryServiceClient;
import ru.yandex.market.logistic.api.model.common.OrderStatusType;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.response.GetOrderHistoryResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatus;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.DateTime;

import static ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState.NEW;
import static ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState.ORDER_CANCELED;
import static ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState.ORDER_PLACED;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PICKUP;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_PLACE_ORDER_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderEvent.DELIVERY_SEARCH_COURIER_OK;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.CANCELED;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.CANCELING;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.PROCESSING;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.COURIER_FOUND;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.COURIER_RECEIVED_ORDER;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_CREATED_DS;
import static ru.yandex.market.logistic.api.model.common.OrderStatusType.ORDER_IS_BEING_PREPARED_TO_BE_SENT_DS;
import static ru.yoomoney.tech.dbqueue.api.TaskExecutionResult.Type.FINISH;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderProcessDeliveryCheckpointsConsumerTest extends BaseFunctionalTest {
    private final OrderProcessDeliveryCheckpointsConsumer consumer;
    private final DeliveryServiceClient deliveryServiceClient;
    private final OrderQueryService orderQueryService;
    private final EventQueryService eventQueryService;
    private final TestOrderFactory testOrderFactory;
    private final Clock clock;

    @Test
    void testSaveLastHistoryItem() {
        OrderModel model = createOrder(PROCESSING, ORDER_PLACED);

        OrderStatus checkpoint = os(ORDER_IS_BEING_PREPARED_TO_BE_SENT_DS, OffsetDateTime.now(clock).minusMinutes(1));
        mockHistory(model.getOrderDelivery(), checkpoint);

        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, model.getOrder().getId());

        OrderDelivery delivery = orderQueryService.getDelivery(model.getOrder().getId());
        Assertions.assertThat(delivery.getDsApiCheckpoint())
                .isEqualTo((short) checkpoint.getStatusCode().getCode());
        Assertions.assertThat(delivery.getDsApiCheckpointSetAt())
                .isEqualTo(checkpoint.getSetDate().getOffsetDateTime());
    }

    @Test
    void testSkipProcessed() {
        OrderModel model = createOrder(PROCESSING, NEW);

        OrderStatus checkpoint1 = os(ORDER_CREATED_DS, model.getOrder().getCreatedAt().plusMinutes(1));
        OrderStatus checkpoint2 = os(COURIER_FOUND, model.getOrder().getCreatedAt().plusMinutes(2));
        OrderStatus checkpoint3 = os(COURIER_RECEIVED_ORDER, model.getOrder().getCreatedAt().plusMinutes(3));

        mockHistory(model.getOrderDelivery(), checkpoint1);
        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, model.getOrder().getId());
        Assertions.assertThat(eventQueryService.getEvents(model.getOrder().getId())).map(Event::getEvent)
                .containsOnly(DELIVERY_PLACE_ORDER_OK);

        mockHistory(model.getOrderDelivery(), checkpoint1, checkpoint2);
        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, model.getOrder().getId());
        Assertions.assertThat(eventQueryService.getEvents(model.getOrder().getId())).map(Event::getEvent)
                .containsExactly(DELIVERY_PLACE_ORDER_OK, DELIVERY_SEARCH_COURIER_OK);

        mockHistory(model.getOrderDelivery(), checkpoint1, checkpoint2, checkpoint3);
        TestQueueTaskRunner.runTaskOnceThrowOnFail(consumer, model.getOrder().getId());
        Assertions.assertThat(eventQueryService.getEvents(model.getOrder().getId())).map(Event::getEvent)
                .containsExactly(DELIVERY_PLACE_ORDER_OK, DELIVERY_SEARCH_COURIER_OK, DELIVERY_PICKUP);
    }

    @Test
    void testStopOnFinishedOrder() {
        OrderModel model = createOrder(CANCELED, NEW);

        OrderStatus checkpoint1 = os(ORDER_CREATED_DS, model.getOrder().getCreatedAt().plusMinutes(1));

        mockHistory(model.getOrderDelivery(), checkpoint1);
        TaskExecutionResult result = TestQueueTaskRunner.runTaskAndReturnResult(consumer, model.getOrder().getId());

        Assertions.assertThat(result.getActionType())
                .isEqualTo(FINISH);
        Assertions.assertThat(eventQueryService.getEvents(model.getOrder().getId())).map(Event::getEvent)
                .isEmpty();
    }

    @Test
    void testStopOnFinishedDelivery() {
        OrderModel model = createOrder(CANCELING, ORDER_CANCELED);

        OrderStatus checkpoint1 = os(ORDER_CREATED_DS, model.getOrder().getCreatedAt().plusMinutes(1));

        mockHistory(model.getOrderDelivery(), checkpoint1);
        TaskExecutionResult result = TestQueueTaskRunner.runTaskAndReturnResult(consumer, model.getOrder().getId());

        Assertions.assertThat(result.getActionType())
                .isEqualTo(FINISH);
        Assertions.assertThat(eventQueryService.getEvents(model.getOrder().getId())).map(Event::getEvent)
                .isEmpty();
    }

    private OrderModel createOrder(
            EOrderState orderState,
            EDeliveryOrderState deliveryOrderState
    ) {
        return testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setOrderState(orderState)
                        .setDeliveryState(deliveryOrderState)
                )
                .setupDelivery(d -> d
                        .setDsApiCheckpoint(null)
                        .setDsApiCheckpointSetAt(null)
                )
                .build()
        );
    }


    private void mockHistory(OrderDelivery orderDelivery, OrderStatus... orderStatuses) {
        Mockito.when(deliveryServiceClient.getOrderHistory(Mockito.any(), Mockito.any()))
                .thenReturn(new GetOrderHistoryResponse(new OrderStatusHistory(
                        Arrays.asList(orderStatuses),
                        new ResourceId(
                                orderDelivery.getOrderId().toString(), null, orderDelivery.getTaxiPartnerId()
                        )
                )));
    }

    private OrderStatus os(OrderStatusType orderStatusType, OffsetDateTime dateTime) {
        return new OrderStatus(orderStatusType, DateTime.fromOffsetDateTime(dateTime), null, null);
    }
}
