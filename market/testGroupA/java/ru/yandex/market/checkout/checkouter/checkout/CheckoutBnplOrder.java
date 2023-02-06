package ru.yandex.market.checkout.checkouter.checkout;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.BnplInfo;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.prepaidBlueOrderParameters;

/**
 * @author : poluektov
 * date: 2021-06-10.
 */
public class CheckoutBnplOrder extends AbstractWebTestBase {

    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableBnpl(true);
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    public void bnplUnavailableForPostpaidOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertNotNull(cart.getBnplInfo());
        assertFalse(cart.getBnplInfo().isAvailable());
    }

    @Test
    public void bnplUnavailableForGooglePayOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPaymentMethod(PaymentMethod.GOOGLE_PAY);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertNotNull(cart.getBnplInfo());
        assertFalse(cart.getBnplInfo().isAvailable());
    }

    @Test
    public void bnplUnavailableForApplePayOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.setPaymentMethod(PaymentMethod.APPLE_PAY);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertNotNull(cart.getBnplInfo());
        assertFalse(cart.getBnplInfo().isAvailable());
    }

    @Test
    public void bnplAvailableForPrepaidOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertNotNull(cart.getBnplInfo());
        assertTrue(cart.getBnplInfo().isAvailable());
    }

    @Test
    public void bnplSelectedFlagShouldBeSaved() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);

        MultiCart cart = orderCreateHelper.cart(parameters);
        assertNotNull(cart.getBnplInfo());
        assertTrue(cart.getBnplInfo().isSelected());
    }

    @Test
    public void checkoutBnplOrder() {
        Parameters parameters = prepaidBlueOrderParameters();
        parameters.getItems().forEach(item -> item.setCategoryId(90864));
        parameters.getOrder().getDelivery().setPrice(BigDecimal.ONE);
        parameters.getOrder().getDelivery().setBuyerPrice(BigDecimal.ONE);
        parameters.setDeliveryType(DeliveryType.DELIVERY);
        parameters.getBuiltMultiCart().setBnplInfo(new BnplInfo());
        parameters.getBuiltMultiCart().getBnplInfo().setSelected(true);
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.isBnpl());
    }
}
