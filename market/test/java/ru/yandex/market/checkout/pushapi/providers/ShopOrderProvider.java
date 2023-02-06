package ru.yandex.market.checkout.pushapi.providers;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.order.Courier;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeliveryWithRegion;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrderItem;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collection;

import static ru.yandex.market.checkout.pushapi.client.util.test.PushApiTestHelper.createLongDate;

public final class ShopOrderProvider {

    private ShopOrderProvider() {
        throw new UnsupportedOperationException();
    }

    public static ShopOrder prepareOrder(@Nonnull final DeliveryWithRegion deliveryWithRegion,
                                         @Nullable final Collection<ShopOrderItem> items) {
        return prepareOrder(prepareBuyer(), deliveryWithRegion, items);
    }

    public static ShopOrder prepareOrderWithSberId(@Nonnull final DeliveryWithRegion deliveryWithRegion,
                                                   @Nullable final Collection<ShopOrderItem> items) {
        return prepareOrder(prepareSberIdBuyer(), deliveryWithRegion, items);
    }

    private static ShopOrder prepareOrder(@Nonnull final Buyer buyer,
                                          @Nonnull final DeliveryWithRegion deliveryWithRegion,
                                          @Nullable final Collection<ShopOrderItem> items) {
        return new ShopOrder() {{
            setBusinessId(1L);
            setId(1234L);
            setStatus(OrderStatus.CANCELLED);
            setSubstatus(OrderSubstatus.USER_CHANGED_MIND);
            setCreationDate(createLongDate("2013-07-06 15:30:40"));
            setCurrency(Currency.RUR);
            setItemsTotal(new BigDecimal("10.75"));
            setTotal(new BigDecimal("11.43"));
            setPaymentType(PaymentType.PREPAID);
            setPaymentMethod(PaymentMethod.CARD_ON_DELIVERY);
            setFake(true);
            setContext(Context.SANDBOX);
            setItems(items);
            setDeliveryWithRegion(deliveryWithRegion);
            setBuyer(buyer);
        }};
    }

    public static Buyer prepareBuyer() {
        final Buyer buyer = BuyerProvider.getBuyer();
        buyer.setId(String.valueOf(1234567890L));
        buyer.setMiddleName("Nikolaevich");
        return buyer;
    }

    public static Buyer prepareSberIdBuyer() {
        final Buyer buyer = BuyerProvider.getSberIdBuyer();
        buyer.setId(String.valueOf(1234567890L));
        buyer.setMiddleName("Nikolaevich");
        return buyer;
    }
}
