package ru.yandex.market.global.checkout.domain.queue.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;

@Slf4j
@Disabled
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class DeliveryCourierCancelConsumerLocalTest extends BaseLocalTest {

    private final DeliveryCourierCancelConsumer consumer;
    private final TestOrderFactory orderFactory;

    @Test
    void testPublishTicket() {
        OrderModel order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setId(3L))
                .setupDelivery(d -> d.setTaxiPartnerId("2440b86b-a8f9-4b36-94bc-1851f25f7fa6"))
                .build());

        TestQueueTaskRunner.runTaskThrowOnFail(consumer, order.getOrder().getId());
    }

}
