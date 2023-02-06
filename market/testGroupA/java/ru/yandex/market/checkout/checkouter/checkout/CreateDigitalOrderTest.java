package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.CartChange;
import ru.yandex.market.checkout.checkouter.cart.ChangeReason;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CreateDigitalOrderTest extends AbstractWebTestBase {

    @Test
    void shouldCreateDigitalOrder() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        Order order = orderCreateHelper.createOrder(whiteParameters);

        assertEquals(Color.WHITE, order.getRgb());
        assertEquals(213L, order.getDelivery().getRegionId());
        assertEquals(DeliveryPartnerType.SHOP, order.getDelivery().getDeliveryPartnerType());
        assertEquals(DeliveryType.DIGITAL, order.getDelivery().getType());
    }

    @Test
    public void createDigitalOrderWithDigitalItemWrongPaimentMethod() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        whiteParameters.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);
        whiteParameters.setCheckCartErrors(false);
        whiteParameters.setCheckOrderCreateErrors(false);

        //
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);

        //
        Order orderResult = multiOrder.getOrderFailures().get(0).getOrder();
        assertThat(orderResult.getChangesReasons(), hasKey(CartChange.PAYMENT));
        assertEquals(ChangeReason.PAYMENT_METHOD_MISMATCH.name(),
                orderResult.getChangesReasons().get(CartChange.PAYMENT).get(0).getCode());
        assertThat(orderResult.getChanges(), hasItem(CartChange.PAYMENT));
    }

    @Test
    public void createDigitalOrderWithDigitalItemSuccess() {
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();

        var multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);
        assertNull(multiOrder.getOrderFailures());
    }

    @Test
    public void createDigitalOrderWithDigitalAndNotDigitalItemFailed() {
        OrderItem orderItem1 = OrderItemProvider.buildOrderItemDigital("1");
        OrderItem orderItem2 = OrderItemProvider.buildOrderItem("2");
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        whiteParameters.getOrder().setItems(List.of(orderItem1, orderItem2));

        Assertions.assertThrows(AssertionError.class, () -> {
            var multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);
        });
    }

    @Test
    public void checkPushDeliveryForDigitalOrderTwoOption() {
        // create digital order
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();
        // set response with two option
        whiteParameters.setPushApiDeliveryResponse(
                DeliveryResponseProvider.buildPostDeliveryResponse(),
                DeliveryResponseProvider.buildDigitalDeliveryResponse());

        //
        Assertions.assertThrows(AssertionError.class, () -> {
                    MultiOrder multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);
                }
        );
    }

    @Test
    public void checkPushDeliveryForDigitalOrderWrongPostOption() {
        // create digital order
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();

        // set response with two option
        whiteParameters.setPushApiDeliveryResponse(
                DeliveryResponseProvider.buildPostDeliveryResponse());

        Assertions.assertThrows(AssertionError.class, () -> {
                    MultiOrder multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);
                }
        );
    }

    @Test
    public void checkPushDeliveryForDigitalOrderWrongDeliveryType() {
        // create digital order
        Parameters whiteParameters = WhiteParametersProvider.digitalOrderPrameters();

        // set response with two option
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDigitalDeliveryResponse();
        deliveryResponse.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        whiteParameters.setPushApiDeliveryResponse(
                deliveryResponse);

        Assertions.assertThrows(AssertionError.class, () -> {
                    MultiOrder multiOrder = orderCreateHelper.createMultiOrder(whiteParameters);
                }
        );
    }
}
