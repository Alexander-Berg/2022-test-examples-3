package ru.yandex.market.checkout.checkouter.delivery;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.ActualDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeliveryPreselectionTest extends AbstractWebTestBase {

    @Test
    void shouldPreselectDeliveryForCart() {
        var blueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        Delivery deliveryPrefillParameters = new Delivery();
        DeliveryType expectedDeliveryType = DeliveryType.DELIVERY;
        deliveryPrefillParameters.setType(expectedDeliveryType);
        blueOrderParameters.getOrder().setDeliveryPrefillParameters(deliveryPrefillParameters);
        MultiCart multiCart = orderCreateHelper.cart(blueOrderParameters);

        Order cart = multiCart.getCarts().get(0);
        assertNotNull(cart);
        assertNotNull(cart.getDelivery());
        String deliveryOptionId = cart.getDelivery().getDeliveryOptionId();
        assertNotNull(deliveryOptionId);
        var deliveryOptions = cart.getDeliveryOptions()
                .stream()
                .filter(delivery -> delivery.getDeliveryOptionId().equals(deliveryOptionId))
                .collect(Collectors.toList());
        assertEquals(1, deliveryOptions.size());
        assertEquals(expectedDeliveryType, deliveryOptions.get(0).getType());
    }

    @Test
    void shouldPreselectDeliveryForCartWithFeatures() throws CloneNotSupportedException {
        var blueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        List<ActualDeliveryOption> deliveries =
                blueOrderParameters.getReportParameters().getActualDelivery().getResults().get(0).getDelivery();

        ActualDeliveryOption opt = deliveries.get(0);
        ActualDeliveryOption opt2 = (ActualDeliveryOption) opt.clone();
        opt2.setIsOnDemand(true);
        opt2.setDeliveryServiceId(53L);
        deliveries.add(opt2);

        Delivery deliveryPrefillParameters = new Delivery();
        DeliveryType expectedDeliveryType = DeliveryType.DELIVERY;
        deliveryPrefillParameters.setType(expectedDeliveryType);
        DeliveryFeature expectedDeliveryFeature = DeliveryFeature.ON_DEMAND;
        deliveryPrefillParameters.setFeatures(Set.of(expectedDeliveryFeature));
        blueOrderParameters.getOrder().setDeliveryPrefillParameters(deliveryPrefillParameters);

        MultiCart multiCart = orderCreateHelper.cart(blueOrderParameters);

        Order cart = multiCart.getCarts().get(0);
        assertNotNull(cart);
        assertNotNull(cart.getDelivery());
        String deliveryOptionId = cart.getDelivery().getDeliveryOptionId();
        assertNotNull(deliveryOptionId);
        var deliveryOptions = cart.getDeliveryOptions()
                .stream()
                .filter(delivery -> delivery.getDeliveryOptionId().equals(deliveryOptionId))
                .collect(Collectors.toList());
        assertEquals(1, deliveryOptions.size());
        assertEquals(expectedDeliveryType, deliveryOptions.get(0).getType());
        Assertions.assertTrue(deliveryOptions.get(0).getFeatures().contains(expectedDeliveryFeature));
    }
}
