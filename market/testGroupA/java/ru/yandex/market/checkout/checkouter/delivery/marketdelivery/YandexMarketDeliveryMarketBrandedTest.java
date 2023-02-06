package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.hamcrest.Matchers.hasSize;

public class YandexMarketDeliveryMarketBrandedTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    private Parameters parameters;

    @BeforeEach
    void setUp() {
        parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryType(DeliveryType.PICKUP)
                .withActualDelivery(
                        ActualDeliveryProvider.builder()
                                .addPickup(BlueParametersProvider.PICKUP_SERVICE_ID)
                                .build()
                )
                .buildParameters();

        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPickup()
                .forEach(p -> p.setMarketBranded(true));
    }

    @Test
    void shouldPassMarketBrandedToCartResult() {
        MultiCart cart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> deliveryOptions = cart.getCarts().get(0).getDeliveryOptions();
        MatcherAssert.assertThat(deliveryOptions, hasSize(1));
        MatcherAssert.assertThat(deliveryOptions.get(0).isMarketBranded(), CoreMatchers.is(true));
    }

    @Test
    void shouldPassMarketBrandedToCheckoutResult() {
        MultiOrder cart = orderCreateHelper.createMultiOrder(parameters);

        Delivery delivery = cart.getCarts().get(0).getDelivery();
        MatcherAssert.assertThat(delivery.isMarketBranded(), CoreMatchers.is(true));
    }

    @Test
    void shouldPassMarketBrandedToGetOrderResult() throws Exception {
        Order order = orderCreateHelper.createOrder(parameters);

        Long orderId = order.getId();
        order = orderGetHelper.getOrder(orderId, ClientInfo.SYSTEM);

        MatcherAssert.assertThat(order.getDelivery().isMarketBranded(), CoreMatchers.is(true));
    }
}
