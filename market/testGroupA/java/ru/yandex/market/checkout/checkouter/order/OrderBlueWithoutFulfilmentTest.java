package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Фарма aka Алкоголь aka Click and collect
 */
public class OrderBlueWithoutFulfilmentTest extends AbstractWebTestBase {

    private static final long FARMA_SHOP_ID = 1774L;

    @Test
    public void checkoutBlueWithoutFulfilmentOrder() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryResponse.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));

        Parameters parameters = setupParameters(deliveryResponse);
        MultiCart cart = doCartBlueWithoutFulfilment(parameters);
        MultiOrder multiOrder = client.checkout(
                orderCreateHelper.mapCartToOrder(cart, parameters),
                cart.getBuyer().getUid()
        );

        assertThat(multiOrder.getOrderFailures(), anyOf(nullValue(), empty()));
        assertThat(multiOrder.getOrders(), hasSize(1));
        assertThat(Iterables.getOnlyElement(multiOrder.getOrders()).isFulfilment(), is(false));
    }

    @Test
    public void cartBlueWithoutFulfilmentOrder() throws Exception {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryResponse.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY));

        Parameters parameters = setupParameters(deliveryResponse);
        MultiCart cart = doCartBlueWithoutFulfilment(parameters);

        Order order = cart.getCarts().iterator().next();
        Assertions.assertTrue(Objects.isNull(order.getChanges()));
        Assertions.assertTrue(cart.getPaymentOptions().contains(PaymentMethod.CASH_ON_DELIVERY));
        Assertions.assertEquals(2, order.getPaymentOptions().size());
    }

    @Test
    public void collectPostapidPaymentsForFarmaTest() throws IOException {
        DeliveryResponse deliveryResponse = DeliveryResponseProvider.buildDeliveryResponse();
        deliveryResponse.setPaymentOptions(Sets.newHashSet(PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.SBP));

        Parameters parameters = setupParameters(deliveryResponse);
        MultiCart cart = doCartBlueWithoutFulfilment(parameters);

        assertFalse(cart.getCarts().get(0).getPaymentOptions()
                .stream().anyMatch(it -> it.getPaymentType() == PaymentType.PREPAID));
    }

    private MultiCart doCartBlueWithoutFulfilment(Parameters parameters) throws IOException {
        orderCreateHelper.initializeMock(parameters);
        CartParameters cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build();
        MultiCart multiCartRequest = parameters.getBuiltMultiCart();
        return client.cart(multiCartRequest, cartParameters);
    }

    private Parameters setupParameters(DeliveryResponse deliveryResponse) {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        parameters.getOrders().forEach(
                order -> order.setShopId(FARMA_SHOP_ID)
        );
        parameters.setMockPushApi(false);
        pushApiConfigurer.mockCart(parameters.getOrder(), Lists.newArrayList(deliveryResponse), false);
        pushApiConfigurer.mockAccept(parameters.getOrder(), true);
        parameters.setMockLoyalty(true);
        parameters.getReportParameters().setDeliveryPartnerTypes(Collections.singletonList("SHOP"));
        return parameters;
    }
}
