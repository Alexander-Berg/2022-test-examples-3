package ru.yandex.market.checkout.pushapi.providers;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import static ru.yandex.market.checkout.checkouter.order.VatType.VAT_18;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.FEED_ID;

public abstract class PushApiCartProvider {

    private PushApiCartProvider() {
        throw new UnsupportedOperationException();
    }

    public static Cart buildCartRequest() {
        Cart cart = new Cart();

        cart.setCurrency(Currency.RUR);
        cart.setItems(Collections.singletonList(defaultOrderItem()));
        cart.setDelivery(DeliveryProvider.getEmptyDelivery(DeliveryProvider.REGION_ID));
        cart.setBuyer(BuyerProvider.getBuyer());

        return cart;
    }

    @Nonnull
    private static CartItem defaultOrderItem() {
        CartItem cartItem = new CartItem();
        cartItem.setOfferId(String.valueOf(ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE)));
        cartItem.setCategoryId(ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE));
        cartItem.setFeedCategoryId(String.valueOf(ThreadLocalRandom.current().nextLong()));
        cartItem.setOfferName("OfferName");
        cartItem.setCount(1);
        cartItem.setFeedId(FEED_ID);
        cartItem.setCategoryId(123);
        cartItem.setModelId(13334267L);
        cartItem.setFeedCategoryId("123");
        cartItem.setSupplierId(123L);
        cartItem.setShopSku("shop_sku_test");
        cartItem.setVat(VAT_18);
        cartItem.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue());
        cartItem.setPrice(BigDecimal.valueOf(250L));
        cartItem.setBuyerPrice(BigDecimal.valueOf(250L));
        return cartItem;
    }
}
