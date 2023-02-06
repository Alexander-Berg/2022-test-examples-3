package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.util.PaymentUtil;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EProcessingMode;
import ru.yandex.market.global.db.jooq.enums.EShopOrderState;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;

import static ru.yandex.market.global.db.jooq.enums.EOrderDeliverySchedulingType.NOW;
import static ru.yandex.market.global.db.jooq.enums.EOrderDeliverySchedulingType.TO_REQUESTED_TIME;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InitialOrderStateActualizerTest extends BaseFunctionalTest {

    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true).build();
    private static final long BELOW_FRAUD_LIMIT = 601_00L;
    private static final long ABOVE_FRAUD_LIMIT = 701_00L;

    private final InitialOrderStateActualizer initialOrderStateActualizer;
    private final TestOrderFactory testOrderFactory;
    private final Clock clock;

    @Test
    public void test3dsPaymentMethodIsWaiting() {
        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalItemsCost(BELOW_FRAUD_LIMIT))
                        .setupDelivery(d -> d.setDeliverySchedulingType(NOW))
                        .setupPayment(p -> p.setRequired_3ds(true))
                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.WAITING_PAYMENT)
                        .setVisibleForShop(false)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

    @Test
    public void testNewState() {
        OrderModel existingFinishedOrder = testOrderFactory.createOrder(
                TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setOrderState(EOrderState.FINISHED))
                .setupPayment(orderPayment -> orderPayment
                        .setClearedAt(OffsetDateTime.now(clock))
                        .setRequired_3ds(true)
                )
                .build()
        );

        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalItemsCost(BELOW_FRAUD_LIMIT))
                        .setupDelivery(d -> d.setDeliverySchedulingType(NOW))
                        .setupPayment(p -> p.setRequired_3ds(false)
                                .setTrustPaymethodId(existingFinishedOrder.getOrderPayment().getTrustPaymethodId())
                        )
                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.NEW)
                        .setVisibleForShop(true)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

    @Test
    public void testScheduled() {
        OrderModel existingFinishedOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(o -> o.setOrderState(EOrderState.FINISHED))
                .setupPayment(o -> o.setRequired_3ds(true).setClearedAt(OffsetDateTime.now(clock)))
                .build()
        );

        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupDelivery(d -> d.setDeliverySchedulingType(TO_REQUESTED_TIME))
                        .setupPayment(p -> p.setRequired_3ds(false)
                                .setTrustPaymethodId(existingFinishedOrder.getOrderPayment().getTrustPaymethodId())
                        )
                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.SCHEDULED)
                        .setVisibleForShop(false)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

    @Test
    public void testWaitingPayment() {
        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupDelivery(d -> d.setDeliverySchedulingType(NOW))
                        .setupPayment(p -> p.setRequired_3ds(true))

                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.WAITING_PAYMENT)
                        .setVisibleForShop(false)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

    @Test
    public void testFraudSumWaitingPayment() {
        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalItemsCost(ABOVE_FRAUD_LIMIT))
                        .setupDelivery(d -> d.setDeliverySchedulingType(NOW))
                        .setupPayment(p -> p.setRequired_3ds(false))
                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.WAITING_PAYMENT)
                        .setVisibleForShop(false)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

    @Test
    public void testFraudApplePayFirstOrder() {
        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalItemsCost(BELOW_FRAUD_LIMIT))
                        .setupDelivery(d -> d.setDeliverySchedulingType(NOW))
                        .setupPayment(p -> p.setRequired_3ds(false)
                                .setTrustPaymethodId(PaymentUtil.APPLE_PAY_PAYMETHOD)
                        )
                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.WAITING_PAYMENT)
                        .setVisibleForShop(false)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

    @Test
    public void testNoFraudApplePaySecondOrder() {
        Order existingFinishedOrder = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupPayment(p -> p
                        .setTrustPaymethodId(PaymentUtil.APPLE_PAY_PAYMETHOD)
                        .setClearedAt(OffsetDateTime.now(clock)))
                .setupOrder(o -> o
                        .setOrderState(EOrderState.FINISHED)
                        .setPaymentState(EPaymentOrderState.CLEARED))
                .build()
        ).getOrder();

        OrderActualization orderActualization = TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalItemsCost(BELOW_FRAUD_LIMIT)
                                .setUid(existingFinishedOrder.getUid()))
                        .setupDelivery(d -> d.setDeliverySchedulingType(NOW))
                        .setupPayment(p -> p.setRequired_3ds(false)
                                .setTrustPaymethodId(PaymentUtil.APPLE_PAY_PAYMETHOD)
                        )
                        .build()
        );

        orderActualization = initialOrderStateActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrder())
                .usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setOrderState(EOrderState.NEW)
                        .setVisibleForShop(true)

                        .setShopState(EShopOrderState.NEW)
                        .setPaymentState(EPaymentOrderState.NEW)
                        .setDeliveryState(EDeliveryOrderState.NEW)
                        .setProcessingMode(EProcessingMode.AUTO)
                );
    }

}
