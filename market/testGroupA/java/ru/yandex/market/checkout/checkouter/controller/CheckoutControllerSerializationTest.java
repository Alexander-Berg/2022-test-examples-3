package ru.yandex.market.checkout.checkouter.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
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

public class CheckoutControllerSerializationTest extends AbstractWebTestBase {

    private Parameters parameters;

    @BeforeEach
    void configure() {
        parameters = BlueParametersProvider.defaultBlueOrderParameters();
    }

    @Test
    void shouldReturnOutletIdsInsteadObjectsOnCartRequest() {
        parameters.configuration().cart().request().setSimplifyOutlets(true);
        parameters.setDeliveryType(DeliveryType.PICKUP);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order firstOrder = firstOrder(multiCart);

        assertThat(firstOrder.getDeliveryOptions(), not(empty()));
        assertThat(firstOrder.getDeliveryOptions(), hasItem(allOf(
                hasProperty("type", is(DeliveryType.PICKUP)),
                hasProperty("outlets", nullValue()),
                hasProperty("outletIds", notNullValue())
        )));
    }

    @Test
    void shouldReturnOutletsIfFlagNotSetOnCartRequest() {
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        Order firstOrder = firstOrder(multiCart);

        assertThat(firstOrder.getDeliveryOptions(), not(empty()));
        assertThat(firstOrder.getDeliveryOptions(), hasItem(allOf(
                hasProperty("type", is(DeliveryType.PICKUP)),
                hasProperty("outlets", notNullValue()),
                hasProperty("outletIds", nullValue())
        )));
    }

    @Test
    void shouldReturnOutletIdsInsteadObjectsOnCheckoutRequest() {
        parameters.configuration().cart().request().setSimplifyOutlets(true);
        parameters.setDeliveryType(DeliveryType.PICKUP);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order firstOrder = firstOrder(multiOrder);

        assertThat(firstOrder.getDeliveryOptions(), nullValue());
    }

    @Test
    void shouldNotReturnOfferPicturesOnCartRequestWithPictureReducing() {
        parameters.configuration().cart().request().setReducePictures(true);

        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order firstOrder = firstOrder(multiCart);
        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(multiCart.getKnownThumbnails(), notNullValue());
        assertThat(resultItem.getPictures(), nullValue());
        assertThat(resultItem.getPictureUrls(), notNullValue());
    }

    @Test
    void shouldReturnOfferPicturesOnCartRequestWithoutPictureReducing() {
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        Order firstOrder = firstOrder(multiCart);
        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(multiCart.getKnownThumbnails(), nullValue());
        assertThat(resultItem.getPictures(), notNullValue());
        assertThat(resultItem.getPictureUrls(), nullValue());
    }

    @Test
    void shouldNotReturnOfferPicturesOnCheckoutRequest() {
        parameters.configuration().cart().request().setReducePictures(true);

        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order firstOrder = firstOrder(multiOrder);
        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(multiOrder.getKnownThumbnails(), notNullValue());
        assertThat(resultItem.getPictures(), nullValue());
        assertThat(resultItem.getPictureUrls(), notNullValue());
    }

    @Test
    void shouldReturnOfferPicturesOnCheckoutRequestWithoutPictureReducing() {
        MultiOrder multiOrder = orderCreateHelper.createMultiOrder(parameters);
        Order firstOrder = firstOrder(multiOrder);
        OrderItem resultItem = firstOrder.getItems().iterator().next();

        assertThat(multiOrder.getKnownThumbnails(), nullValue());
        assertThat(resultItem.getPictures(), notNullValue());
        assertThat(resultItem.getPictureUrls(), nullValue());
    }
}
