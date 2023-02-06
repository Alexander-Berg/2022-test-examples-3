package ru.yandex.market.providers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.shopadminstub.model.CartRequest;
import ru.yandex.market.shopadminstub.model.inventory.Inventory;
import ru.yandex.market.shopadminstub.model.inventory.InventoryDelivery;
import ru.yandex.market.shopadminstub.model.inventory.InventoryItem;

public abstract class InventoryProvider {
    public static Inventory buildInventory(CartRequest cartRequest) {
        Inventory inventory = new Inventory();

        inventory.setInventory(cartRequest.getItems().values()
                .stream()
                .map(item -> {
                    InventoryItem inventoryItem = new InventoryItem();
                    inventoryItem.setFeedId(item.getFeedId());
                    inventoryItem.setOfferId(item.getOfferId());
                    inventoryItem.setCount(item.getCount());
                    inventoryItem.setPrice(item.getPrice().longValue());
                    return inventoryItem;
                }).collect(Collectors.toMap(ii -> new FeedOfferId(ii.getOfferId(), ii.getFeedId()), Function.identity())));
        inventory.setDelivery(buildInventoryDeliveries());
        inventory.setPaymentMethods(buildPaymentMethods());

        return inventory;
    }

    public static List<InventoryDelivery> buildInventoryDeliveries() {
        InventoryDelivery delivery = new InventoryDelivery();
        delivery.setId(1L);
        delivery.setType(DeliveryType.DELIVERY);
        delivery.setName("Шнырь-курьер");
        delivery.setPrice(250L);
        delivery.setFromDateOffset(0);
        delivery.setToDateOffset(0);

        InventoryDelivery deliveryPremium = new InventoryDelivery();
        deliveryPremium.setId(2L);
        deliveryPremium.setType(DeliveryType.DELIVERY);
        deliveryPremium.setName("Premium курьер");
        deliveryPremium.setPrice(200L);
        deliveryPremium.setFromDateOffset(1);
        deliveryPremium.setToDateOffset(1);

        InventoryDelivery deliveryCourier = new InventoryDelivery();
        deliveryCourier.setId(3L);
        deliveryCourier.setType(DeliveryType.DELIVERY);
        deliveryCourier.setName("Курьер");
        deliveryCourier.setPrice(150L);
        deliveryCourier.setFromDateOffset(0);
        deliveryCourier.setToDateOffset(0);

        InventoryDelivery deliveryPost = new InventoryDelivery();
        deliveryPost.setId(4L);
        deliveryPost.setType(DeliveryType.POST);
        deliveryPost.setName("Почта");
        deliveryPost.setPrice(100L);
        deliveryPost.setFromDateOffset(0);
        deliveryPost.setToDateOffset(0);

        return Arrays.asList(delivery, deliveryPremium, deliveryCourier, deliveryPost);
    }

    public static List<PaymentMethod> buildPaymentMethods() {
        return Arrays.asList(PaymentMethod.YANDEX, PaymentMethod.CASH_ON_DELIVERY, PaymentMethod.CARD_ON_DELIVERY);
    }
}
