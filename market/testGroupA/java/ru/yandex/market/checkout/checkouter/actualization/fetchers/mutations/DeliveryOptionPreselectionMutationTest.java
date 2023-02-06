package ru.yandex.market.checkout.checkouter.actualization.fetchers.mutations;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.actualization.flow.context.CartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ActualizationContext;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryFeature;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;

class DeliveryOptionPreselectionMutationTest {

    private DeliveryOptionPreselectionMutation target;
    private CartFetchingContext context;

    @BeforeEach
    public void setUp() {
        target = new DeliveryOptionPreselectionMutation();
        Order order = new Order();
        Delivery delivery = new Delivery();
        delivery.setRegionId(213L);
        order.setDelivery(delivery);
        CheckoutContextHolder.setCheckoutOperation(false);
        context = CartFetchingContext.of(Mockito.mock(MultiCartFetchingContext.class),
                Mockito.mock(ActualizationContext.ActualizationContextBuilder.class),
                order);
    }

    @Test
    public void preselectionByType() {
        Order order = context.getOrder();
        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        order.setDeliveryPrefillParameters(delivery);

        Delivery deliveryOptionDelivery = new Delivery();
        deliveryOptionDelivery.setType(DeliveryType.DELIVERY);
        Delivery deliveryOptionDigital = new Delivery();
        deliveryOptionDigital.setType(DeliveryType.DIGITAL);
        Delivery deliveryOptionPickup = new Delivery();
        deliveryOptionPickup.setType(DeliveryType.PICKUP);

        order.setDeliveryOptions(List.of(deliveryOptionDelivery, deliveryOptionDigital, deliveryOptionPickup));

        target.onSuccess(context);

        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
    }

    @Test
    public void preselectionByOutletId() {
        Order order = context.getOrder();
        long expectedOutletId = 931L;

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setOutletId(expectedOutletId);
        order.setDeliveryPrefillParameters(delivery);

        Delivery deliveryOptionPickup1 = new Delivery();
        deliveryOptionPickup1.setType(DeliveryType.PICKUP);
        deliveryOptionPickup1.setOutletId(123L);
        Delivery deliveryOptionPickup2 = new Delivery();
        deliveryOptionPickup2.setType(DeliveryType.PICKUP);
        deliveryOptionPickup2.setOutletId(expectedOutletId);

        order.setDeliveryOptions(List.of(deliveryOptionPickup1, deliveryOptionPickup2));

        target.onSuccess(context);

        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertEquals(expectedOutletId, order.getDelivery().getOutletId());
    }

    @Test
    public void preselectionByDates() {
        Order order = context.getOrder();
        DeliveryDates expectedDeliveryDates = new DeliveryDates(new Date(), new Date());
        expectedDeliveryDates.setFromTime(LocalTime.now().minusMinutes(2));
        expectedDeliveryDates.setToTime(LocalTime.now().plusMinutes(2));

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setDeliveryDates(expectedDeliveryDates);
        order.setDeliveryPrefillParameters(delivery);

        Delivery deliveryOptionPickup1 = new Delivery();
        deliveryOptionPickup1.setType(DeliveryType.DELIVERY);
        deliveryOptionPickup1.setDeliveryDates(
                new DeliveryDates(new Date(), new Date(), LocalTime.now(), LocalTime.now()));
        Delivery deliveryOptionPickup2 = new Delivery();
        deliveryOptionPickup2.setType(DeliveryType.DELIVERY);
        deliveryOptionPickup2.setDeliveryDates(expectedDeliveryDates);

        order.setDeliveryOptions(List.of(deliveryOptionPickup1, deliveryOptionPickup2));

        target.onSuccess(context);

        Assertions.assertEquals(DeliveryType.DELIVERY, order.getDelivery().getType());
        Assertions.assertEquals(expectedDeliveryDates, order.getDelivery().getDeliveryDates());
    }

    @Test
    public void preselectionByFeatures() {
        Order order = context.getOrder();
        Set<DeliveryFeature> expectedDeliveryFeatures = Set.of(DeliveryFeature.ON_DEMAND,
                DeliveryFeature.ON_DEMAND_COMBINATOR);

        Delivery delivery = new Delivery();
        delivery.setType(DeliveryType.PICKUP);
        delivery.setFeatures(expectedDeliveryFeatures);
        order.setDeliveryPrefillParameters(delivery);

        Delivery deliveryOptionPickup1 = new Delivery();
        deliveryOptionPickup1.setFeatures(Set.of(DeliveryFeature.ON_DEMAND));
        Delivery deliveryOptionPickup2 = new Delivery();
        deliveryOptionPickup2.setFeatures(expectedDeliveryFeatures);

        order.setDeliveryOptions(List.of(deliveryOptionPickup1, deliveryOptionPickup2));

        target.onSuccess(context);

        Assertions.assertEquals(expectedDeliveryFeatures, order.getDelivery().getFeatures());
    }
}
