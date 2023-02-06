package ru.yandex.market.checkout.checkouter.checkout;

import java.io.IOException;
import java.util.List;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actual.ActualItem;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ActualizeItemParameters;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParametersBuilder;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class CheckouterClientPriceDropAttributesTest extends AbstractWebTestBase {

    private Parameters parameters;
    private OrderItem orderItem;

    @BeforeEach
    public void setUp() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        orderItem = Iterables.getOnlyElement(
                Iterables.getOnlyElement(parameters.getBuiltMultiCart().getCarts()).getItems());

        checkouterFeatureWriter.writeValue(BooleanFeatureType.USE_REPORT_DISCOUNT_VALUE, true);
    }

    @Test
    public void shouldTransferPriceDropAttributesToCart() throws IOException {
        parameters.configuration()
                .cart()
                .request()
                .addPriceDropMsku(orderItem.getMsku());

        orderCreateHelper.initializeMock(parameters);

        MultiCart multiCart = client.cart(
                parameters.getBuiltMultiCart(),
                CartParameters.builder()
                        .withRgb(Color.BLUE)
                        .withUid(parameters.getBuyer().getUid())
                        .withPriceDropMskuList(List.of(orderItem.getMsku()))
                        .build()
        );

        assertThat(multiCart, notNullValue());
        assertThat(multiCart.isValid(), is(true));
    }

    @Test
    public void shouldTransferPriceDropAttributesToCheckout() {
        parameters.configuration()
                .cart()
                .request()
                .addPriceDropMsku(orderItem.getMsku());

        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0), true);

        MultiOrder result = client.checkout(
                multiOrder,
                CheckoutParametersBuilder.aCheckoutParameters()
                        .withRgb(Color.BLUE)
                        .withUid(parameters.getBuyer().getUid())
                        .withPriceDropMskuList(List.of(orderItem.getMsku()))
                        .build()
        );

        assertThat(result, notNullValue());
        assertThat(result.isValid(), is(true));
    }

    @Test
    public void shouldTransferPriceDropAttributesToActualize() throws IOException {
        parameters.configuration()
                .cart()
                .request()
                .addPriceDropMsku(orderItem.getMsku());

        orderCreateHelper.initializeMock(parameters);

        ActualItem toSend = new ActualItem(orderItem);
        toSend.setShopId(parameters.getShopId());
        toSend.setBuyerRegionId(parameters.getBuyer().getRegionId());
        toSend.setRgb(Color.BLUE);

        ActualItem actualItem = client.actualizeItem(
                toSend,
                ActualizeItemParameters.builder()
                        .withUid(parameters.getBuyer().getUid())
                        .withPriceDropItemMsku(orderItem.getMsku())
                        .build()
        );

        assertThat(actualItem, notNullValue());
    }
}
