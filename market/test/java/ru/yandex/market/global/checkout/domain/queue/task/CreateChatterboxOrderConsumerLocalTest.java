package ru.yandex.market.global.checkout.domain.queue.task;

import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseLocalTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;

@Disabled
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CreateChatterboxOrderConsumerLocalTest extends BaseLocalTest {

    private final CreateChatterboxOrderConsumer consumer;
    private final TestOrderFactory orderFactory;

    @Test
    void testPublishTicket() {
        OrderModel order = orderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setId(1256L)
                        .setCreatedAt(OffsetDateTime.now())
                        .setPackUntil(OffsetDateTime.now().plusMinutes(15)))
                .setupDelivery(d -> d
                        .setShopName("Some Tel Aviv's Test Store")
                        .setShopPhone("+972585486783"))
                .build());

        TestQueueTaskRunner.runTaskThrowOnFail(consumer, order.getOrder().getId());
    }

}
