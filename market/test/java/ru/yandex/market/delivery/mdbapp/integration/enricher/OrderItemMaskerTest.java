package ru.yandex.market.delivery.mdbapp.integration.enricher;

import java.util.HashSet;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import steps.orderSteps.OrderSteps;
import steps.orderSteps.itemSteps.ItemsSteps;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.delivery.mdbapp.configuration.FeatureProperties;
import ru.yandex.market.logistic.gateway.common.model.delivery.CargoType;

import static org.assertj.core.api.Assertions.assertThat;

public class OrderItemMaskerTest {

    private static final String MASK_PREFIX = "Hided";
    private final FeatureProperties featureProperties = new FeatureProperties();
    private OrderItemMasker orderItemMasker;

    @Before
    public void setUp() {
        featureProperties.setDeliveryServicesForNameMasking(new HashSet<>());
        featureProperties.setDisableItemMasking(false);
        orderItemMasker = new OrderItemMasker(MASK_PREFIX, featureProperties);
    }

    @DisplayName("Проверка что название товара скрывается, если это интим товар.")
    @Test
    public void enrichWithAdultItem() {
        Order order = OrderSteps.getFilledOrder(1L);
        OrderItem adultOrderItem = ItemsSteps.getOrderItem(1L);
        adultOrderItem.setMsku(69L);
        adultOrderItem.setCargoTypes(ImmutableSet.of(
            CargoType.ADULT.getCode(),
            CargoType.WET_CARGO.getCode()
        ));

        assertThat(orderItemMasker.mask(order, adultOrderItem))
            .as("Masked adult offer")
            .extracting(OrderItem::getOfferName)
            .isEqualTo(MASK_PREFIX + ", 69");
    }

    @DisplayName("Проверка что название товара не скрывается, если карго тип 910, но скрытие выключено.")
    @Test
    public void enrichWithAdultItemMaskingOff() {
        featureProperties.setDisableItemMasking(true);

        Order order = OrderSteps.getFilledOrder(1L);
        OrderItem adultOrderItem = ItemsSteps.getOrderItem(1L);
        adultOrderItem.setMsku(69L);
        adultOrderItem.setCargoTypes(ImmutableSet.of(
            CargoType.ADULT.getCode(),
            CargoType.WET_CARGO.getCode()
        ));

        assertThat(orderItemMasker.mask(order, adultOrderItem))
            .as("Masked adult offer")
            .extracting(OrderItem::getOfferName)
            .isEqualTo("offer name");

        featureProperties.setDisableItemMasking(false);
    }

    @DisplayName("Проверка что название не скрывается, когда это не интим товар.")
    @Test
    public void enrichWithoutAdultItem() {
        Order order = OrderSteps.getFilledOrder(1L);
        OrderItem orderItem = ItemsSteps.getOrderItem(1L);

        assertThat(orderItemMasker.mask(order, orderItem))
            .as("Masked adult offer")
            .extracting(OrderItem::getOfferName)
            .isEqualTo("offer name");

    }

    @DisplayName("Проверка что название товара скрывается, если служба доставки из списка для скрытия.")
    @Test
    public void enrichWithDeliveryServiceForMasking() {
        featureProperties.getDeliveryServicesForNameMasking().add(123L);
        featureProperties.getDeliveryServicesForNameMasking().add(321L);

        Order order = OrderSteps.getFilledOrder(1L);
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(123L);
        order.setDelivery(delivery);
        OrderItem orderItem = ItemsSteps.getOrderItem(1L);
        orderItem.setMsku(69L);

        assertThat(orderItemMasker.mask(order, orderItem))
            .as("Masked item")
            .extracting(OrderItem::getOfferName)
            .isEqualTo(MASK_PREFIX + ", 69");
    }

    @DisplayName("Проверка что название товара не скрывается, если служба доставки не из списка для скрытия.")
    @Test
    public void enrichWithoutDeliveryServiceForMasking() {
        featureProperties.getDeliveryServicesForNameMasking().add(123L);
        featureProperties.getDeliveryServicesForNameMasking().add(321L);

        Order order = OrderSteps.getFilledOrder(1L);
        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(111L);
        order.setDelivery(delivery);
        OrderItem orderItem = ItemsSteps.getOrderItem(1L);

        assertThat(orderItemMasker.mask(order, orderItem))
            .as("Masked item")
            .extracting(OrderItem::getOfferName)
            .isEqualTo("offer name");
    }
}
