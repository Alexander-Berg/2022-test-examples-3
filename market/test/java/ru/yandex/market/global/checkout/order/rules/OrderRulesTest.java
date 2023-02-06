package ru.yandex.market.global.checkout.order.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;

import ru.yandex.market.global.checkout.domain.order.OrderRepository;
import ru.yandex.market.global.checkout.domain.order.OrderRulesService;
import ru.yandex.market.global.checkout.domain.queue.task.FireEventProducer;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderEvent;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

@Getter
@AllArgsConstructor
public class OrderRulesTest {
    private final AtomicInteger minute;
    private final List<Queue<EOrderEvent>> timeline;
    private final ArrayList<Class<? extends QueueProducer<?>>> invocations;
    private final OrderRulesService orderRulesService;
    private final OrderRepository orderRepository;
    private final long orderId;

    public void run() {
        for (minute.set(0); minute.get() < timeline.size(); minute.incrementAndGet()) {
            Queue<EOrderEvent> orderEvents = timeline.get(minute.get());
            if (orderEvents == null || orderEvents.isEmpty()) {
                continue;
            }

            for (EOrderEvent event = orderEvents.poll(); event != null; event = orderEvents.poll()) {
                orderRulesService.onOrderEvent(orderId, event);
            }
        }
    }

    public void assertAllProducersInvokedOnlyOnce() {
        List<Class<? extends QueueProducer<?>>> uniqueInvocations = invocations.stream()
                .filter(c -> c != FireEventProducer.class)
                .collect(Collectors.toList());

        Assertions.assertThat(uniqueInvocations.size())
                .isEqualTo(new HashSet<>(uniqueInvocations).size());
    }

    public void assertThatProducerInvokedNTimes(Class<? extends QueueProducer<?>> producer, int n) {
        long uniqueInvocations = invocations.stream()
                .filter(c -> c == producer)
                .count();

        Assertions.assertThat(uniqueInvocations).isEqualTo(n);
    }

    public void assertFinalOrderStateIs(
            EProcessingMode processingMode,
            EOrderState orderState,
            EDeliveryOrderState deliveryOrderState,
            EPaymentOrderState paymentOrderState,
            EShopOrderState shopOrderState
    ) {
        Assertions.assertThat(orderRepository.fetchOneById(orderId))
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .build()
                )
                .isEqualTo(new Order()
                        .setProcessingMode(processingMode)
                        .setOrderState(orderState)
                        .setDeliveryState(deliveryOrderState)
                        .setPaymentState(paymentOrderState)
                        .setShopState(shopOrderState)
                );
    }
}
