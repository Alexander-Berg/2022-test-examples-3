package ru.yandex.market.test.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.core.order.model.MbiOrder;
import ru.yandex.market.core.order.model.MbiOrderBuilder;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderItemPromo;
import ru.yandex.market.core.order.model.MbiOrderPromo;
import ru.yandex.market.core.order.model.MbiOrderStatus;

public class OrderTestData {
    private static final long DEFAULT_FEED_ID = 0L;
    private static final int DEFAULT_CATEGORY_ID = 0;
    private static final String DEFAULT_OFFER_ID = "test-offer";
    private static final String DEFAULT_OFFER_NAME = "test-offer-name";
    private static final String DEFAULT_SHOP_SKU = "test-shop-sku";
    private static final Long DEFAULT_WIDTH = 333L;
    private static final Long DEFAULT_HEIGHT = 444L;
    private static final Long DEFAULT_DEPTH = 555L;
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_FEE = BigDecimal.ZERO;
    private static final int DEFAULT_COUNT = 1;

    private OrderTestData() {
    }

    public static MbiOrderBuilder orderBuilder(Long orderId) {
        return new MbiOrderBuilder()
                .setId(orderId)
                .setShopId(123L)
                .setMultiOrderId("mult_order_id")
                .setStatus(MbiOrderStatus.DELIVERY)
                .setColor(Color.BLUE)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setTotal(BigDecimal.TEN)
                .setCreationDate(Date.from(Instant.now()));
    }

    public static MbiOrderPromo.Builder orderPromoBuilder(
            Long orderId,
            PromoType promoType,
            String marketPromoId) {
        return new MbiOrderPromo.Builder()
                .setOrderId(orderId)
                .setPromoType(promoType)
                .setMarketPromoId(marketPromoId);
    }

    public static MbiOrderItem.Builder orderItemBuilder(Long orderId, Long itemId) {
        return MbiOrderItem.builder()
                .setId(itemId)
                .setOrderId(orderId)
                .setFeedId(DEFAULT_FEED_ID)
                .setOfferId(DEFAULT_OFFER_ID)
                .setOfferName(DEFAULT_OFFER_NAME)
                .setShopSku(DEFAULT_SHOP_SKU)
                .setCount(DEFAULT_COUNT)
                .setCategoryId(DEFAULT_CATEGORY_ID)
                .setWidth(DEFAULT_WIDTH)
                .setHeight(DEFAULT_HEIGHT)
                .setDepth(DEFAULT_DEPTH)
                .setPrice(DEFAULT_PRICE)
                .setShopFee(DEFAULT_FEE)
                .setIntFee(DEFAULT_FEE.intValue())
                .setSubsidy(BigDecimal.ONE);
    }

    public static MbiOrderItemPromo.Builder orderItemPromoBuilder(PromoType promoType, String marketPromoId) {
        return new MbiOrderItemPromo.Builder()
                .setPromoType(promoType)
                .setMarketPromoId(marketPromoId)
                .setSubsidy(BigDecimal.ONE);
    }

    public static MbiOrderItemPromo.Builder orderItemPromoBuilder(MbiOrderPromo orderPromo) {
        return new MbiOrderItemPromo.Builder()
                .fromOrderPromo(orderPromo);
    }

    public static MbiOrder order(Long orderId, List<MbiOrderPromo> promos, List<MbiOrderItem> items) {
        return orderBuilder(orderId)
                .setPromos(promos)
                .setItems(items)
                .build();
    }

    public static MbiOrderPromo orderPromo(Long orderId, PromoType promoType, String marketPromoId) {
        return orderPromoBuilder(orderId, promoType, marketPromoId).build();
    }

    public static MbiOrderItem orderItem(Long orderId, Long itemId, List<MbiOrderItemPromo> promos) {
        return orderItemBuilder(orderId, itemId).setPromos(promos).build();
    }

    public static MbiOrderItemPromo orderItemPromo(PromoType promoType, String marketPromoId) {
        return orderItemPromoBuilder(promoType, marketPromoId).build();
    }
}
