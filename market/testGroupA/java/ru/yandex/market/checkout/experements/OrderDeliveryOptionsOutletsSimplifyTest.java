package ru.yandex.market.checkout.experements;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParametersBuilder;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;

public class OrderDeliveryOptionsOutletsSimplifyTest extends AbstractWebTestBase {

    private Parameters parameters;

    @BeforeEach
    void configure() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setDeliveryType(DeliveryType.PICKUP);
    }

    @Test
    void shouldNotReturnOutletsAsObjectsOnCartRequest() throws IOException {
        orderCreateHelper.initializeMock(parameters);

        MultiCart multiCart = client.cart(parameters.getBuiltMultiCart(), CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withSimplifyOutlets(true)
                .withContext(Context.MARKET)
                .withHitRateGroup(HitRateGroup.UNLIMIT)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withRgb(Color.BLUE)
                .build());
        Order firstOrder = firstOrder(multiCart);
        assertThat(firstOrder.getDeliveryOptions(), not(empty()));
        assertThat(firstOrder.getDeliveryOptions(), hasItem(allOf(
                hasProperty("type", is(DeliveryType.PICKUP)),
                hasProperty("outlets", nullValue()),
                hasProperty("outletIds", notNullValue())
        )));
    }

    @Test
    void shouldNotReturnOutletsAsObjectsOnCheckoutRequest() {
        parameters.configuration().cart().request().setSimplifyOutlets(true);

        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder order = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0), true);
        MultiOrder multiOrder = client.checkout(order,
                CheckoutParametersBuilder.aCheckoutParameters()
                        .withUid(parameters.getBuyer().getUid())
                        .withSimplifyOutlets(true)
                        .withContext(Context.MARKET)
                        .withHitRateGroup(HitRateGroup.UNLIMIT)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withRgb(Color.BLUE)
                        .build());


        Order firstOrder = firstOrder(multiOrder);
        assertThat(firstOrder.getDeliveryOptions(), nullValue());
    }

}
