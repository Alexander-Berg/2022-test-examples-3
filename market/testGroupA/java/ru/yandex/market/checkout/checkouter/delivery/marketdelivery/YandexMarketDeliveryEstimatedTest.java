package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.common.util.SwitchWithWhitelist;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryRouteProvider;
import ru.yandex.market.checkout.util.Constants;

import static java.util.Collections.singleton;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class YandexMarketDeliveryEstimatedTest extends AbstractWebTestBase {

    @Autowired
    private OrderGetHelper orderGetHelper;

    @Test
    void actualDeliveryEstimatedDeliveryTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        checkDeliveryEstimated(parameters, DeliveryType.DELIVERY);
    }

    @Test
    void actualDeliveryEstimatedPickupTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPickup()
                .forEach(p -> p.setEstimated(true));

        checkDeliveryEstimated(parameters, DeliveryType.PICKUP);
    }

    @Test
    void actualDeliveryEstimatedPostTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();

        parameters.setDeliveryType(DeliveryType.POST);
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPost()
                .forEach(p -> p.setEstimated(true));

        checkDeliveryEstimated(parameters, DeliveryType.POST);
    }

    @Test
    void whiteActualDeliveryEstimatedTest() throws Exception {
        Parameters parameters = WhiteParametersProvider.applyTo(WhiteParametersProvider.defaultWhiteParameters());

        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        checkDeliveryEstimated(parameters, DeliveryType.DELIVERY);
    }

    @Test
    void deliveryRouteEstimatedTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        deliveryRouteSetup(parameters);

        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY));

        checkDeliveryEstimated(parameters, DeliveryType.DELIVERY);
    }

    @Test
    void deliveryRouteEstimatedForPickupTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        deliveryRouteSetup(parameters);

        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPickup()
                .forEach(p -> p.setEstimated(true));

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.PICKUP));

        checkDeliveryEstimated(parameters, DeliveryType.PICKUP);
    }

    @Test
    void deliveryRouteEstimatedForPostTest() throws Exception {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        deliveryRouteSetup(parameters);

        parameters.setDeliveryType(DeliveryType.POST);
        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getPost()
                .forEach(p -> p.setEstimated(true));

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.POST));


        checkDeliveryEstimated(parameters, DeliveryType.POST);
    }

    @Test
    void whiteDeliveryRouteEstimatedTest() throws Exception {
        Parameters parameters = WhiteParametersProvider.applyTo(WhiteParametersProvider.defaultWhiteParameters());
        deliveryRouteSetup(parameters);

        parameters.getReportParameters()
                .getActualDelivery()
                .getResults()
                .get(0)
                .getDelivery()
                .forEach(p -> p.setEstimated(true));

        parameters.getReportParameters().setDeliveryRoute(DeliveryRouteProvider.fromActualDelivery(
                parameters.getReportParameters().getActualDelivery(), DeliveryType.DELIVERY));

        checkDeliveryEstimated(parameters, DeliveryType.DELIVERY);
    }

    private void deliveryRouteSetup(Parameters parameters) {
        checkouterFeatureWriter.writeValue(
                ComplexFeatureType.COMBINATOR_FLOW,
                new SwitchWithWhitelist<>(true, singleton(Constants.COMBINATOR_EXPERIMENT)));

        parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.setExperiments(Constants.COMBINATOR_EXPERIMENT);
    }

    private void checkDeliveryEstimated(Parameters parameters, DeliveryType deliveryType) throws Exception {
        MultiCart cart = orderCreateHelper.cart(parameters);

        List<? extends Delivery> deliveryOptions = cart.getCarts().get(0).getDeliveryOptions();

        assertNotEquals(0, deliveryOptions.size());

        List<? extends Delivery> deliveries = deliveryOptions.stream().filter(it -> it.getType() == deliveryType)
                .collect(Collectors.toList());
        assertFalse(deliveries.isEmpty());

        deliveries.forEach(it -> assertTrue(it.getEstimated()));

        deliveryOptions.stream().filter(it -> it.getType() != deliveryType)
                .forEach(it -> assertNull(it.getEstimated()));

        Order order = orderCreateHelper.createOrder(parameters);

        // Из АПИ /checkout
        assertTrue(order.getDelivery().getEstimated());

        Boolean orderProperty = order.getProperty(OrderPropertyType.ESTIMATED_ORDER);
        assertNotNull(orderProperty);
        assertTrue(orderProperty);

        Order dbOrder = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        // Из БД
        assertTrue(dbOrder.getDelivery().getEstimated());
        assertEquals(deliveryType, dbOrder.getDelivery().getType());

        Boolean dbOrderProperty = dbOrder.getProperty(OrderPropertyType.ESTIMATED_ORDER);
        assertNotNull(dbOrderProperty);
        assertTrue(dbOrderProperty);
    }
}
