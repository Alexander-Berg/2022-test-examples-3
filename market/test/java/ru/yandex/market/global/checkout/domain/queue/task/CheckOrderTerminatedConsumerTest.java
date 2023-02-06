package ru.yandex.market.global.checkout.domain.queue.task;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.order.OrderQueryService;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.queue.TestQueueTaskRunner;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

import static ru.yandex.market.global.db.jooq.enums.EOrderState.CANCELED;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.CANCELING;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.FINISHED;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.PROCESSING;
import static ru.yandex.market.global.db.jooq.enums.EOrderState.SCHEDULED;
import static ru.yandex.market.global.db.jooq.enums.EProcessingMode.AUTO;
import static ru.yandex.market.global.db.jooq.enums.EProcessingMode.MANUAL;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CheckOrderTerminatedConsumerTest extends BaseFunctionalTest {
    private final CheckOrderTerminatedConsumer checkOrderTerminatedConsumer;
    private final TestOrderFactory testOrderFactory;
    private final OrderQueryService orderQueryService;

    @Test
    public void testSwitchProcessingToManual() {
        Order before = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(AUTO)
                        .setOrderState(PROCESSING)
                )
                .build()
        ).getOrder();

        TestQueueTaskRunner.runTaskThrowOnFail(checkOrderTerminatedConsumer, before.getId());

        Order after = orderQueryService.get(before.getId());
        Assertions.assertThat(after.getProcessingMode()).isEqualTo(MANUAL);
    }

    @Test
    public void testSwitchCancelingToManual() {
        Order before = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(AUTO)
                        .setOrderState(CANCELING)
                )
                .build()
        ).getOrder();

        TestQueueTaskRunner.runTaskThrowOnFail(checkOrderTerminatedConsumer, before.getId());

        Order after = orderQueryService.get(before.getId());
        Assertions.assertThat(after.getProcessingMode()).isEqualTo(MANUAL);
    }

    @Test
    public void testDoNotSwitchScheduledToManual() {
        Order before = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(AUTO)
                        .setOrderState(SCHEDULED)
                )
                .build()
        ).getOrder();

        TestQueueTaskRunner.runTaskThrowOnFail(checkOrderTerminatedConsumer, before.getId());

        Order after = orderQueryService.get(before.getId());
        Assertions.assertThat(after.getProcessingMode()).isEqualTo(AUTO);
    }

    @Test
    public void testDoNotSwitchFinishedToManual() {
        Order before = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(AUTO)
                        .setOrderState(FINISHED)
                )
                .build()
        ).getOrder();

        TestQueueTaskRunner.runTaskThrowOnFail(checkOrderTerminatedConsumer, before.getId());

        Order after = orderQueryService.get(before.getId());
        Assertions.assertThat(after.getProcessingMode()).isEqualTo(AUTO);
    }

    @Test
    public void testDoNotSwitchCanceledToManual() {
        Order before = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o
                        .setProcessingMode(AUTO)
                        .setOrderState(CANCELED)
                )
                .build()
        ).getOrder();

        TestQueueTaskRunner.runTaskThrowOnFail(checkOrderTerminatedConsumer, before.getId());

        Order after = orderQueryService.get(before.getId());
        Assertions.assertThat(after.getProcessingMode()).isEqualTo(AUTO);
    }

}
