package ru.yandex.market.checkout.experements;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.client.CheckoutParametersBuilder;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.HitRateGroup;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;

public class OrderItemReducedPicturesTest extends AbstractWebTestBase {

    private Parameters parameters;

    @BeforeEach
    void configure() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
    }

    @Test
    void shouldNotReturnOfferPicturesOnCartRequest() throws IOException {
        orderCreateHelper.initializeMock(parameters);

        MultiCart multiCart = client.cart(parameters.getBuiltMultiCart(), CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withReducePictures(true)
                .withContext(Context.MARKET)
                .withHitRateGroup(HitRateGroup.UNLIMIT)
                .withApiSettings(ApiSettings.PRODUCTION)
                .withRgb(Color.BLUE)
                .build());
        Order firstOrder = firstOrder(multiCart);
        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(multiCart.getKnownThumbnails(), notNullValue());
        assertThat(resultItem.getPictures(), nullValue());
        assertThat(resultItem.getPictureUrls(), notNullValue());
    }

    @Test
    void shouldNotReturnOfferPicturesOnCheckoutRequest() {
        parameters.configuration().cart().request().setReducePictures(true);

        MultiCart cart = orderCreateHelper.cart(parameters);
        MultiOrder order = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(order.getCarts().get(0), true);
        MultiOrder multiOrder = client.checkout(order,
                CheckoutParametersBuilder.aCheckoutParameters()
                        .withUid(parameters.getBuyer().getUid())
                        .withReducePictures(true)
                        .withContext(Context.MARKET)
                        .withHitRateGroup(HitRateGroup.UNLIMIT)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withRgb(Color.BLUE)
                        .build());


        Order firstOrder = firstOrder(multiOrder);
        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(multiOrder.getKnownThumbnails(), notNullValue());
        assertThat(resultItem.getPictures(), nullValue());
        assertThat(resultItem.getPictureUrls(), notNullValue());
    }

}
