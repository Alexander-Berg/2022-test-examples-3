package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.time.Clock;
import java.time.OffsetDateTime;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class Payment3dsActualizerTest extends BaseFunctionalTest {

    public static final String CARD_PAYMETHOD = "card-1234567890223457882342432";
    public static final String CARD_PAYMETHOD2 = "card-1234567890223457882342433";
    public static final String APPLE_TOKEN_PAYMETHOD = "apple_token-123456789012344567890";

    private final Payment3dsActualizer payment3dsActualizer;
    private final TestOrderFactory testOrderFactory;
    private final Clock clock;

    @Test
    public void test3dsPassedAllowNo3ds() {
        OffsetDateTime clearedAt = OffsetDateTime.now(clock).minusHours(25);
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(order -> order
                        .setOrderState(EOrderState.FINISHED)
                        .setPaymentState(EPaymentOrderState.CLEARED)
                        .setFinishedAt(clearedAt)
                )
                .setupPayment(orderPayment -> orderPayment
                        .setTrustPaymethodId(CARD_PAYMETHOD)
                        .setAuthorizedAt(clearedAt)
                        .setClearedAt(clearedAt)
                        .setRequired_3ds(true)
                ).build()
        );

        OrderActualization orderActualization =
                TestOrderFactory.buildOrderActualization(TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalCost(399_00L))
                        .setupPayment(orderPayment -> orderPayment.setTrustPaymethodId(CARD_PAYMETHOD))
                        .build());

        orderActualization = payment3dsActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrderPayment().getPaymentRedirectUrl()).isNull();
        Assertions.assertThat(orderActualization.getOrderPayment().getRequired_3ds()).isFalse();
    }

    @Test
    public void test3dsPassedButStill3ds() {
        OffsetDateTime clearedAt = OffsetDateTime.now(clock).minusDays(1);
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(order -> order
                        .setOrderState(EOrderState.FINISHED)
                        .setPaymentState(EPaymentOrderState.CLEARED)
                        .setFinishedAt(clearedAt)
                )
                .setupPayment(orderPayment -> orderPayment
                        .setTrustPaymethodId(CARD_PAYMETHOD)
                        .setAuthorizedAt(clearedAt)
                        .setClearedAt(clearedAt)
                        .setRequired_3ds(true)
                ).build()
        );

        OrderActualization orderActualization =
                TestOrderFactory.buildOrderActualization(TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o.setTotalCost(400_00L))
                        .setupPayment(orderPayment -> orderPayment.setTrustPaymethodId(CARD_PAYMETHOD))
                        .build());

        orderActualization = payment3dsActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrderPayment().getPaymentRedirectUrl()).isNotNull();
        Assertions.assertThat(orderActualization.getOrderPayment().getRequired_3ds()).isTrue();
    }

    @Test
    public void testWith3ds2ndOrderADay() {
        long uid = 123456789000L;
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(order -> order
                        .setUid(uid)
                        .setOrderState(EOrderState.FINISHED)
                        .setPaymentState(EPaymentOrderState.CLEARED)
                        .setFinishedAt(OffsetDateTime.now(clock)))
                .setupPayment(orderPayment -> orderPayment
                        .setTrustPaymethodId(CARD_PAYMETHOD)
                        .setAuthorizedAt(OffsetDateTime.now(clock))
                        .setClearedAt(OffsetDateTime.now(clock))
                        .setRequired_3ds(true)).build());

        OrderActualization orderActualization =
                TestOrderFactory.buildOrderActualization(TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(order -> order.setUid(uid))
                        .setupPayment(orderPayment -> orderPayment.setTrustPaymethodId(CARD_PAYMETHOD))
                        .build());

        orderActualization = payment3dsActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrderPayment().getPaymentRedirectUrl()).isNotNull();
        Assertions.assertThat(orderActualization.getOrderPayment().getRequired_3ds()).isTrue();
    }

    @Test
    public void testWith3ds() {
        OrderActualization orderActualization =
                TestOrderFactory.buildOrderActualization(TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupPayment(orderPayment -> orderPayment.setTrustPaymethodId(CARD_PAYMETHOD2))
                        .build());

        orderActualization = payment3dsActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrderPayment().getPaymentRedirectUrl()).isNotNull();
        Assertions.assertThat(orderActualization.getOrderPayment().getRequired_3ds()).isTrue();
    }

    @Test
    public void test3dsFirstOrder() {
        testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(order -> order
                        .setOrderState(EOrderState.FINISHED)
                        .setPaymentState(EPaymentOrderState.CLEARED))
                .setupPayment(orderPayment -> orderPayment
                        .setTrustPaymethodId(CARD_PAYMETHOD)
                        .setAuthorizedAt(OffsetDateTime.now(clock))
                        .setClearedAt(OffsetDateTime.now(clock))
                        .setRequired_3ds(false)).build());

        OrderActualization orderActualization =
                TestOrderFactory.buildOrderActualization(TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupPayment(orderPayment -> orderPayment.setTrustPaymethodId(CARD_PAYMETHOD))
                        .build());

        orderActualization = payment3dsActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrderPayment().getPaymentRedirectUrl()).isNotNull();
        Assertions.assertThat(orderActualization.getOrderPayment().getRequired_3ds()).isTrue();
    }

    @Test
    public void testWithAppleToken() {
        OrderActualization orderActualization =
                TestOrderFactory.buildOrderActualization(TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupPayment(orderPayment -> orderPayment.setTrustPaymethodId(APPLE_TOKEN_PAYMETHOD))
                        .build());

        orderActualization = payment3dsActualizer.actualize(orderActualization);

        Assertions.assertThat(orderActualization.getOrderPayment().getPaymentRedirectUrl()).isNull();
        Assertions.assertThat(orderActualization.getOrderPayment().getRequired_3ds()).isFalse();
    }
}
