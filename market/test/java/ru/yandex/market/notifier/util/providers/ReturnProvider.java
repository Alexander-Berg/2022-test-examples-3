package ru.yandex.market.notifier.util.providers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.ShopOutlet;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.checkout.checkouter.returns.ReturnItem;
import ru.yandex.market.checkout.checkouter.returns.ReturnReasonType;

public class ReturnProvider {

    public static final String RETURN_REASON = "Вернул и все тут!";

    public static Return makeReturnWithReasonType(Order order, ReturnReasonType reasonType) {
        Return ret = makeReturn(order);
        ret.getItems().forEach(ri -> ri.setReasonType(reasonType));
        return ret;
    }

    public static Return makeReturn(Order order) {
        return makeReturn(
                order.getId(),
                order.getItems().stream()
                        .map(i -> new ReturnItem(i.getId(), i.getCount(), false, BigDecimal.ZERO))
                        .peek(ri -> ri.setReturnReason(RETURN_REASON))
                        .collect(Collectors.toList())
        );
    }

    public static Return makeReturn(long orderId, List<ReturnItem> items) {
        return makeReturn(orderId, items, makeDeliveryOption());
    }

    public static Return makeReturn(long orderId, List<ReturnItem> items, ReturnDelivery deliveryOption) {
        Return ret = new Return();
        ret.setOrderId(orderId);
        ret.setItems(items);
        ret.setDelivery(deliveryOption);
        return ret;
    }

    public static ReturnDelivery makeDeliveryOption() {
        return makeDeliveryOption(22345L);
    }

    public static ReturnDelivery makeDeliveryOption(long deliveryServiceId) {
        ReturnDelivery returnDelivery = new ReturnDelivery();
        returnDelivery.setDeliveryServiceId(deliveryServiceId);
        returnDelivery.setOutletId(1234L);
        returnDelivery.setType(DeliveryType.PICKUP);
        ShopOutlet shopOutlet = ShopOutletProvider.getShopOutlet();
        shopOutlet.setNotes("Дойти до Дикси, а там разберетесь.");
        returnDelivery.setOutlet(shopOutlet);
        return returnDelivery;
    }

    public static ReturnItem toReturnItemWithReasonAndCount(OrderItem orderItem, ReturnReasonType reasonType,
                                                            int count) {
        ReturnItem item = new ReturnItem(orderItem.getId(), count, false, BigDecimal.ZERO);
        item.setReasonType(reasonType);
        item.setReturnReason(reasonType.name());
        return item;
    }
}
