package ru.yandex.market.checkout.checkouter.b2b;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ChangeReason;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.common.report.model.ActualDeliveryOption;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.yandex.market.checkout.checkouter.cart.CartChange.PAYMENT;
import static ru.yandex.market.checkout.checkouter.cart.ChangeReason.PAYMENT_METHOD_MISMATCH;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryDates.deliveryDates;
import static ru.yandex.market.checkout.checkouter.order.ActualDeliveryUtils.mapDeliveryTypeToName;
import static ru.yandex.market.checkout.common.time.TestableClock.getInstance;

public class B2bPaymentOptionsTest extends AbstractWebTestBase {

    @Test
    @DisplayName("Актуализация без выбранной опции доставки")
    public void actualization_ifDeliveryIsNotSelectedThenOnlyB2bPrepaymentInPaymentOptions() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        parameters.configuration().cart().body().multiCart().getCarts().get(0)
                .setDelivery(DeliveryProvider.getEmptyDelivery());  // нет опции доставки

        MultiCart actualizedCart = orderCreateHelper.cart(parameters);

        Set<PaymentMethod> actualOptions = actualizedCart.getPaymentOptions();
        assertEquals(Set.of(PaymentMethod.B2B_ACCOUNT_PREPAYMENT), actualOptions);
    }

    @Test
    @DisplayName("Актуализация c выбранной опцией доставки")
    public void actualization_ifDeliceryIsSelectedThenOnlyB2bPrepaymentInPaymentOptions() {
        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        copyDeliveryParamsToUserOrderFromAvailableInShop(parameters);  // выбор одной из доступных опций

        MultiCart actualizedCart = orderCreateHelper.cart(parameters);

        Set<PaymentMethod> actualOptions = actualizedCart.getPaymentOptions();
        assertEquals(Set.of(PaymentMethod.B2B_ACCOUNT_PREPAYMENT), actualOptions);
    }

    private void copyDeliveryParamsToUserOrderFromAvailableInShop(Parameters parameters) {
        ActualDeliveryOption shopOption = getDeliveryOptionFromAvailableShopOptions(parameters);
        Delivery userChoice = getDeliveryUserChoice(parameters);
        userChoice.setType(DeliveryType.DELIVERY);
        userChoice.setServiceName(mapDeliveryTypeToName(DeliveryType.DELIVERY));
        userChoice.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        userChoice.setPrice(shopOption.getPrice());
        userChoice.setBuyerPrice(shopOption.getPrice());
        userChoice.setDeliveryDates(deliveryDates(getInstance(), shopOption.getDayFrom(), shopOption.getDayTo()));
    }

    private ActualDeliveryOption getDeliveryOptionFromAvailableShopOptions(Parameters parameters) {
        ActualDeliveryResult availableOptions = parameters.configuration()
                .cart().mockConfigurations().values().stream().findFirst()
                .get().getReportParameters().getActualDelivery().getResults().get(0);
        return availableOptions.getDelivery().get(0);
    }

    private Delivery getDeliveryUserChoice(Parameters parameters) {
        return parameters.configuration().cart().body().multiCart().getCarts().get(0).getDelivery();
    }

    @Test
    @DisplayName("Чекаут с выбранной опцией оплаты счетом")
    public void checkout_ifPaymentIsB2bPrepaymentThenOrderIsOk() throws Exception {
        // Given
        PaymentMethod pickedByUserPaymentOnCheckout = PaymentMethod.B2B_ACCOUNT_PREPAYMENT;

        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        disableBuiltInResponseChecks(parameters);
        copyDeliveryParamsToUserOrderFromAvailableInShop(parameters);

        parameters.configuration().checkout().response().setPrepareBeforeCheckoutAction(multiCart -> {
            multiCart.setPaymentMethod(pickedByUserPaymentOnCheckout);
        });

        MultiCart actualizedCart = orderCreateHelper.cart(parameters);

        // When
        MultiOrder multiOrder = orderCreateHelper.checkout(actualizedCart, parameters);

        // Then
        assertTrue(isEmpty(multiOrder.getOrderFailures()));
        assertFalse(isEmpty(multiOrder.getOrders()));
    }

    private void disableBuiltInResponseChecks(Parameters parameters) {
        parameters.configuration().cart().response().setCheckCartErrors(false);
        parameters.configuration().checkout().response().setCheckOrderCreateErrors(false);
        parameters.configuration().checkout().response().setUseErrorMatcher(false);
    }

    @Test
    @DisplayName("Чекаут с выбранной опцией оплаты картой")
    public void checkout_ifPaymentIsCardThenOrderIsFailureWithPaymentMismatch() throws Exception {
        // Given
        PaymentMethod pickedByUserPaymentOnCheckout = PaymentMethod.YANDEX;

        Parameters parameters = B2bCustomersTestProvider.defaultB2bParameters();
        disableBuiltInResponseChecks(parameters);
        copyDeliveryParamsToUserOrderFromAvailableInShop(parameters);

        parameters.configuration().checkout().response().setPrepareBeforeCheckoutAction(multiCart -> {
            multiCart.setPaymentMethod(pickedByUserPaymentOnCheckout);
        });

        MultiCart actualizedCart = orderCreateHelper.cart(parameters);

        // When
        MultiOrder multiOrder = orderCreateHelper.checkout(actualizedCart, parameters);

        // Then
        assertFalse(isEmpty(multiOrder.getOrderFailures()));
        assertTrue(isEmpty(multiOrder.getOrders()));
        assertTrue(hasFailure(multiOrder, PAYMENT, PAYMENT_METHOD_MISMATCH));
    }

    private boolean hasFailure(MultiOrder multiOrder, CartChange failType, ChangeReason failReason) {
        return Optional.of(multiOrder)
                .map(MultiOrder::getOrderFailures)
                .orElseGet(Collections::emptyList)
                .stream()
                .map(OrderFailure::getOrder)
                .map(Order::getChangesReasons)
                .filter(Objects::nonNull)
                .map(changes -> changes.get(failType))
                .filter(Objects::nonNull)
                .anyMatch(reasons -> reasons.contains(failReason));
    }
}
