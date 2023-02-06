package ru.yandex.market.checkout.checkouter.actualization.multicart;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentIntegerFeatureType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentOptionHiddenReason;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentCollectionFeatureType.PAYMENT_DISABLE_METHOD;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CARD_ON_DELIVERY;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.CREDIT;
import static ru.yandex.market.checkout.checkouter.pay.PaymentMethod.YANDEX;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.bluePrepaidWithCustomPrice;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class DisabledPaymentMethodsTest extends AbstractWebTestBase {

    @AfterEach
    public void tearDown() {
        checkouterFeatureWriter.writeValue(PAYMENT_DISABLE_METHOD, Set.of());
        checkouterFeatureWriter.writeValue(PermanentIntegerFeatureType.PAYMENT_DISABLE_USER_PERCENT, 0);
    }

    @Test
    public void cardOnDeliveryOrderCheckoutTest() {
        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(CARD_ON_DELIVERY);

        // Проверяем что заказ успешно создается с данным способом оплаты
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder.getOrders(), hasSize(greaterThanOrEqualTo(1)));
        assertThat(multiOrder.getOrderFailuresCount(), equalTo(0));

        // Создаем корзину, отключаем опцию оплаты, создаем заказ с отключенной опцией
        // Проверяем, что чаказ создан неуспешно. Проверяем присутствие CartChange.PAYMENT
        parameters.setCheckOrderCreateErrors(false);
        multiOrder = orderCreateHelper.createMultiOrder(parameters,
                multiCart -> disablePaymentMethod(CARD_ON_DELIVERY));
        assertThat(multiOrder.getOrders(), nullValue());
        assertThat(multiOrder.getOrderFailuresCount(), greaterThanOrEqualTo(1));
        assertThat(multiOrder.getOrderFailures().iterator().next()
                .getOrder().getChanges(), contains(CartChange.PAYMENT));
    }

    @Test
    public void creditOrderCheckoutTest() {
        Parameters parameters = bluePrepaidWithCustomPrice(new BigDecimal(10_000));
        parameters.setPaymentMethod(CREDIT);

        // Проверяем что заказ успешно создается с данным способом оплаты
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        assertThat(multiOrder.getOrders(), hasSize(greaterThanOrEqualTo(1)));
        assertThat(multiOrder.getOrderFailuresCount(), equalTo(0));

        // Создаем корзину, отключаем опцию оплаты, создаем заказ с отключенной опцией
        // Проверяем, что чаказ создан неуспешно. Проверяем присутствие CartChange.PAYMENT
        parameters.setCheckOrderCreateErrors(false);
        multiOrder = orderCreateHelper.createMultiOrder(parameters, multiCart -> disablePaymentMethod(CREDIT));
        assertThat(multiOrder.getOrders(), nullValue());
        assertThat(multiOrder.getOrderFailuresCount(), greaterThanOrEqualTo(1));
        assertThat(multiOrder.getOrderFailures().iterator().next()
                .getOrder().getChanges(), contains(CartChange.PAYMENT));
    }

    @Test
    public void testCartResponseHasChanges() {
        disablePaymentMethod(CARD_ON_DELIVERY);

        Parameters parameters = new Parameters();
        parameters.setPaymentMethod(CARD_ON_DELIVERY);
        parameters.setCheckCartErrors(false);
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getCarts().iterator().next().getChanges(), contains(CartChange.PAYMENT));
    }

    @Test
    public void testCartResponseHasCorrectPaymentOptions() {
        Parameters parameters = defaultBlueOrderParameters();
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPaymentOptions(), hasItems(CARD_ON_DELIVERY, YANDEX));
        multiCart.getCarts().forEach(cart -> {
            assertThat(cart.getPaymentOptions(), hasItems(CARD_ON_DELIVERY, YANDEX));
            assertThat(
                    cart.getDeliveryOptions().stream().flatMap(option -> option.getPaymentOptions().stream())
                            .collect(Collectors.toList()),
                    hasItems(CARD_ON_DELIVERY)
            );
        });

        disablePaymentMethod(CARD_ON_DELIVERY, 23);

        multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPaymentOptions(), hasItems(CARD_ON_DELIVERY, YANDEX));
        multiCart.getCarts().forEach(cart -> {
            assertThat(cart.getPaymentOptions(), hasItems(CARD_ON_DELIVERY, YANDEX));
            assertThat(
                    cart.getDeliveryOptions().stream().flatMap(option -> option.getPaymentOptions().stream())
                            .collect(Collectors.toList()),
                    hasItems(CARD_ON_DELIVERY)
            );
        });

        disablePaymentMethod(CARD_ON_DELIVERY, 26);

        multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPaymentOptions(), not(hasItems(CARD_ON_DELIVERY)));
        assertThat(multiCart.getPaymentOptions(), hasItems(YANDEX));
        multiCart.getCarts().forEach(cart -> {
            assertThat(cart.getPaymentOptions(), not(hasItems(CARD_ON_DELIVERY)));
            assertThat(cart.getPaymentOptions(), hasItems(YANDEX));

            assertThat(
                    cart.getDeliveryOptions().stream()
                            .flatMap(option -> option.getPaymentOptions().stream())
                            .collect(Collectors.toList()),
                    allOf(
                            hasItems(YANDEX),
                            not(hasItems(CARD_ON_DELIVERY))
                    )
            );
            assertThat(
                    cart.getDeliveryOptions().stream()
                            .flatMap(option -> option.getHiddenPaymentOptions().stream())
                            .collect(Collectors.toList()),
                    hasItems(methodToOption(CARD_ON_DELIVERY))
            );
        });

        disablePaymentMethod(CARD_ON_DELIVERY, 100);

        multiCart = orderCreateHelper.cart(parameters);
        assertThat(multiCart.getPaymentOptions(), not(hasItems(CARD_ON_DELIVERY)));
        assertThat(multiCart.getPaymentOptions(), hasItems(YANDEX));
        multiCart.getCarts().forEach(cart -> {
            assertThat(cart.getPaymentOptions(), not(hasItems(CARD_ON_DELIVERY)));
            assertThat(cart.getPaymentOptions(), hasItems(YANDEX));

            assertThat(
                    cart.getDeliveryOptions().stream()
                            .flatMap(option -> option.getPaymentOptions().stream())
                            .collect(Collectors.toList()),
                    allOf(
                            hasItems(YANDEX),
                            not(hasItems(CARD_ON_DELIVERY))
                    )
            );
            assertThat(
                    cart.getDeliveryOptions().stream()
                            .flatMap(option -> option.getHiddenPaymentOptions().stream())
                            .collect(Collectors.toList()),
                    hasItems(methodToOption(CARD_ON_DELIVERY))
            );
        });

    }

    private void disablePaymentMethod(PaymentMethod paymentMethod) {
        disablePaymentMethod(paymentMethod, 100);
    }

    private void disablePaymentMethod(PaymentMethod paymentMethod, int percent) {
        checkouterFeatureWriter.writeValue(PAYMENT_DISABLE_METHOD, Set.of(paymentMethod.name()));
        checkouterFeatureWriter.writeValue(PermanentIntegerFeatureType.PAYMENT_DISABLE_USER_PERCENT, percent);
    }

    public static PaymentOption methodToOption(PaymentMethod method) {
        return new PaymentOption(method, PaymentOptionHiddenReason.TEMPORARY_UNAVAILABLE);
    }
}
