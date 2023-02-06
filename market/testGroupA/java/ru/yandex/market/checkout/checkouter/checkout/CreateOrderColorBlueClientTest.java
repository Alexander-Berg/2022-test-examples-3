package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CheckoutParameters;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.service.business.OrderFinancialService;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;

public class CreateOrderColorBlueClientTest extends AbstractWebTestBase {

    @Autowired
    private OrderFinancialService financialService;

    @Test
    public void testCreateOrderWithBlueColor() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();

        financialService.calculateAndSetOrderTotals(parameters.getOrder());
        orderCreateHelper.setupShopsMetadata(parameters);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0), true);

        MultiOrder checkoutResult = client.checkout(multiOrder, CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.BLUE)
                .build());
        assertThat(checkoutResult, notNullValue());
        Order order = Iterables.getOnlyElement(checkoutResult.getOrders());

        Assertions.assertEquals(Color.BLUE, order.getRgb());
        Assertions.assertEquals(Boolean.FALSE, order.isFulfilment());

        Order fromGet = orderService.getOrder(order.getId());

        Assertions.assertEquals(Color.BLUE, fromGet.getRgb());
        Assertions.assertEquals(Boolean.FALSE, order.isFulfilment());
    }

    @Test
    public void testCreateOrderWithWhiteColor() throws IOException {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();

        financialService.calculateAndSetOrderTotals(parameters.getOrder());
        orderCreateHelper.setupShopsMetadata(parameters);
        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0), true);

        MultiOrder checkoutResult = client.checkout(multiOrder, CheckoutParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.WHITE)
                .build());
        assertThat(checkoutResult, notNullValue());
        Order order = Iterables.getOnlyElement(checkoutResult.getOrders());

        Assertions.assertEquals(Color.WHITE, order.getRgb());
        Assertions.assertEquals(Boolean.FALSE, order.isFulfilment());

        pushApiConfigurer.assertAcceptOrder(
                allOf(
                        hasProperty("shopId", equalTo(order.getShopId())),
                        hasProperty("rgb", equalTo(order.getRgb()))
                )
        );

        Order fromGet = orderService.getOrder(order.getId());

        Assertions.assertEquals(Color.WHITE, fromGet.getRgb());
        Assertions.assertEquals(Boolean.FALSE, order.isFulfilment());
    }
}
