package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.util.Collections;
import java.util.HashSet;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import steps.orderSteps.OrderSteps;
import steps.orderSteps.itemSteps.ItemsSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.delivery.mdbapp.integration.payload.ExtendedOrder;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;

import static org.junit.Assert.assertEquals;

public class OrderItemMaskingOrderEnricherTest {

    private static final String MASK_PREFIX = "Hided";

    private final FeatureProperties featureProperties = new FeatureProperties();
    private OrderItemMaskingOrderEnricher orderItemMaskingOrderEnricher;
    private ExtendedOrder extendedOrder;

    @Before
    public void setUp() {
        featureProperties.setDeliveryServicesForNameMasking(new HashSet<>());
        featureProperties.setDisableItemMasking(false);
        orderItemMaskingOrderEnricher = new OrderItemMaskingOrderEnricher(MASK_PREFIX, featureProperties);

        extendedOrder = new ExtendedOrder(OrderSteps.getRedSingleOrder());
    }

    @Test
    public void enrichWithAdultItem() {
        OrderItem adultOrderItem = ItemsSteps.getOrderItem(1L);
        adultOrderItem.setMsku(69L);
        adultOrderItem.setCargoTypes(ImmutableSet.of(
            CargoType.ADULT.getCode(),
            CargoType.WET_CARGO.getCode()
        ));

        extendedOrder.getOrder().setItems(Collections.singletonList(adultOrderItem));

        orderItemMaskingOrderEnricher.enrich(extendedOrder);

        assertEquals(
            MASK_PREFIX + ", 69",
            extendedOrder.getOrder().getItems().iterator().next().getOfferName()
        );
    }

    @Test
    public void enrichWithAdultItemMaskingOff() {
        featureProperties.setDisableItemMasking(true);

        OrderItem adultOrderItem = ItemsSteps.getOrderItem(1L);
        adultOrderItem.setMsku(69L);
        adultOrderItem.setCargoTypes(ImmutableSet.of(
            CargoType.ADULT.getCode(),
            CargoType.WET_CARGO.getCode()
        ));

        extendedOrder.getOrder().setItems(Collections.singletonList(adultOrderItem));

        orderItemMaskingOrderEnricher.enrich(extendedOrder);

        assertEquals(
            "offer name",
            extendedOrder.getOrder().getItems().iterator().next().getOfferName()
        );

        featureProperties.setDisableItemMasking(false);
    }

    @Test
    public void enrichWithoutAdultItem() {
        OrderItem orderItem = ItemsSteps.getOrderItem(1L);

        extendedOrder.getOrder().setItems(Collections.singletonList(orderItem));

        orderItemMaskingOrderEnricher.enrich(extendedOrder);

        assertEquals(
            "offer name",
            extendedOrder.getOrder().getItems().iterator().next().getOfferName()
        );
    }

    @Test
    public void enrichWithDeliveryServiceForMasking() {
        featureProperties.getDeliveryServicesForNameMasking().add(123L);
        featureProperties.getDeliveryServicesForNameMasking().add(321L);
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(123L);
        OrderItem orderItem = ItemsSteps.getOrderItem(1L);
        orderItem.setMsku(69L);
        extendedOrder.getOrder().setItems(Collections.singletonList(orderItem));
        extendedOrder.getOrder().setDelivery(delivery);
        orderItemMaskingOrderEnricher.enrich(extendedOrder);

        assertEquals(
            MASK_PREFIX + ", 69",
            extendedOrder.getOrder().getItems().iterator().next().getOfferName()
        );
    }

    @Test
    public void enrichWithoutDeliveryServiceForMasking() {
        featureProperties.getDeliveryServicesForNameMasking().add(123L);
        featureProperties.getDeliveryServicesForNameMasking().add(321L);
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(111L);
        OrderItem orderItem = ItemsSteps.getOrderItem(1L);
        extendedOrder.getOrder().setItems(Collections.singletonList(orderItem));
        extendedOrder.getOrder().setDelivery(delivery);
        orderItemMaskingOrderEnricher.enrich(extendedOrder);

        assertEquals(
            "offer name",
            extendedOrder.getOrder().getItems().iterator().next().getOfferName()
        );
    }
}
