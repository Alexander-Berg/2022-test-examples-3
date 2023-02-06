package ru.yandex.market.billing.imports.globalorder.dao;

import java.time.OffsetDateTime;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.billing.imports.globalorder.model.GlobalOrder;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderDeliveryStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderItem;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderPaymentStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderShopStatus;
import ru.yandex.market.billing.imports.globalorder.model.GlobalOrderStatus;
import ru.yandex.market.core.currency.Currency;

@ParametersAreNonnullByDefault
public class GlobalTestObjects {
    private GlobalTestObjects() {
    }

    public static GlobalOrder.Builder defaultGlobalOrder(long orderId) {
        return GlobalOrder.builder()
                .setId(orderId)
                .setShopId(-7777777L)
                .setStatus(GlobalOrderStatus.FINISHED)
                .setDeliveryStatus(GlobalOrderDeliveryStatus.ORDER_DELIVERED)
                .setPaymentStatus(GlobalOrderPaymentStatus.CLEARED)
                .setShopStatus(GlobalOrderShopStatus.READY)
                .setCurrency(Currency.ILS)
                .setItemsTotal(10000L)
                .setSubsidyTotal(1000L)
                .setProcessInBillingAt(OffsetDateTime.parse("2021-11-02T03:00:00+03").toInstant())
                .setCreatedAt(OffsetDateTime.parse("2021-11-02T03:00:00+03").toInstant())
                .setPaymentId(0L);
    }

    public static GlobalOrderItem.Builder defaultGlobalOrderItem(long itemId, long orderId) {
        return GlobalOrderItem.builder()
                .setId(itemId)
                .setOrderId(orderId)
                .setOfferId("test-offer-" + itemId)
                .setOfferName("אבקת חלב רזה")
                .setPrice(1000)
                .setSubsidy(10)
                .setCount(1)
                .setMarketCategoryId(234);
    }

}
