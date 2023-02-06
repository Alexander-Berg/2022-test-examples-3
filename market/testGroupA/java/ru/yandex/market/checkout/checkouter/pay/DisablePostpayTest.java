package ru.yandex.market.checkout.checkouter.pay;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderTypeUtils;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

/**
 * @author : poluektov
 * date: 2022-02-24.
 */
public class DisablePostpayTest extends AbstractWebTestBase {
    @Test
    public void checkFbyPostpaidFiltering() {
        Parameters parameters = BlueParametersProvider.postpaidBlueOrderParameters();
        trustMockConfigurer.mockWholeTrust();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertTrue(order.isFulfilment());
        Assertions.assertEquals(PaymentType.POSTPAID, order.getPaymentType());

        checkouterFeatureWriter.writeValue(BooleanFeatureType.FORBID_FBY_POSTPAY, true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cart.getPaymentOptions().stream()
                .noneMatch(pm -> pm.getPaymentType() == PaymentType.POSTPAID), "Should not have postpaid");
        Assertions.assertTrue(cart.getPaymentOptions().stream()
                .anyMatch(pm -> pm.getPaymentType() == PaymentType.PREPAID), "Should have few postpaid options");
    }

    @Test
    public void checkPostpaidFiltering() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        trustMockConfigurer.mockWholeTrust();
        parameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        Order order = orderCreateHelper.createOrder(parameters);

        Assertions.assertTrue(OrderTypeUtils.isDbs(order));
        Assertions.assertEquals(PaymentType.POSTPAID, order.getPaymentType());

        checkouterFeatureWriter.writeValue(BooleanFeatureType.FORBID_POSTPAY_FOR_PREPAY_SHOPS, true);
        parameters.setCheckCartErrors(false);

        MultiCart cart = orderCreateHelper.cart(parameters);
        Assertions.assertTrue(cart.getPaymentOptions().stream()
                .noneMatch(pm -> pm.getPaymentType() == PaymentType.POSTPAID), "Should not have postpaid");
        Assertions.assertTrue(cart.getPaymentOptions().stream()
                .anyMatch(pm -> pm.getPaymentType() == PaymentType.PREPAID), "Should have few prepaid options");
    }
}
