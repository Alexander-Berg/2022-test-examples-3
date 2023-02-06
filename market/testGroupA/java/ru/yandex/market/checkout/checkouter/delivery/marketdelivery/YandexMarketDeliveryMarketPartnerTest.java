package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.List;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.hamcrest.Matchers.hasSize;

public class YandexMarketDeliveryMarketPartnerTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    private OrderHistoryEventsTestHelper historyEventsTestHelper;

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
                .forEach(p -> {
                    p.setMarketPartner(true);
                    p.setMarketPostTerm(true);
                });
    }

    @Test
    void shouldPassMarketPartnerToCartResult() {
        MultiCart cart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> deliveryOptions = cart.getCarts().get(0).getDeliveryOptions();
        MatcherAssert.assertThat(deliveryOptions, hasSize(1));
        Assertions.assertTrue(deliveryOptions.get(0).isMarketPartner());
        Assertions.assertTrue(deliveryOptions.get(0).isMarketPostTerm());
    }

    @Test
    void shouldPassMarketPartnerToCheckoutResult() {
        MultiOrder cart = orderCreateHelper.createMultiOrder(parameters);

        Delivery delivery = cart.getCarts().get(0).getDelivery();
        Assertions.assertTrue(delivery.isMarketPartner());
        Assertions.assertTrue(delivery.isMarketPostTerm());
    }

    @Test
    void shouldPassMarketPartnerToGetOrderResult() throws Exception {
        Order order = orderCreateHelper.createOrder(parameters);

        order = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        Assertions.assertTrue(order.getDelivery().isMarketPartner());
        Assertions.assertTrue(order.getDelivery().isMarketPostTerm());
    }

    @Test
    void shouldPassMarketPartnerToHistoryEvent() throws Exception {
        Order order = orderCreateHelper.createOrder(parameters);

        var newOrderEvent = historyEventsTestHelper.getEventsOfType(order.getId(), HistoryEventType.NEW_ORDER);

        Assertions.assertTrue(newOrderEvent.get(0).getOrderAfter().getDelivery().isMarketPartner());
        Assertions.assertTrue(newOrderEvent.get(0).getOrderAfter().getDelivery().isMarketPostTerm());
    }
}
