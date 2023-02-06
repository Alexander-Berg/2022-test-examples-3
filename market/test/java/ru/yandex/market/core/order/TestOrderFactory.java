package ru.yandex.market.core.order;

import java.math.BigDecimal;
import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.checkout.checkouter.order.promo.PromoType;
import ru.yandex.market.core.order.model.MbiOrderItem;
import ru.yandex.market.core.order.model.MbiOrderItemPromo;
import ru.yandex.market.core.order.model.MbiOrderPromo;

@ParametersAreNonnullByDefault
public class TestOrderFactory {
    private static final long DEFAULT_FEED_ID = 0L;
    private static final int DEFAULT_CATEGORY_ID = 0;
    private static final String DEFAULT_OFFER_ID = "test-offer";
    private static final String DEFAULT_OFFER_NAME = "test-offer-name";
    private static final String DEFAULT_WARE_MD5 = "test-ware-md5";
    private static final String DEFAULT_SHOW_UID = "test-show-uid-";
    private static final BigDecimal DEFAULT_PRICE = BigDecimal.TEN;
    private static final BigDecimal DEFAULT_FEE = BigDecimal.ZERO;
    private static final int DEFAULT_COUNT = 1;

    static public MbiOrderPromo.Builder defaultOrderPromo(PromoType cashback, String marketPromoId) {
        return new MbiOrderPromo.Builder().setPromoType(cashback).setMarketPromoId(marketPromoId);
    }

    static public MbiOrderItem.Builder defaultOrderItem(
            long orderId, long itemId, Collection<MbiOrderItemPromo> itemPromos
    ) {
        return MbiOrderItem.builder()
                .setOrderId(orderId)
                .setId(itemId)
                .setFeedId(DEFAULT_FEED_ID)
                .setOfferId(DEFAULT_OFFER_ID)
                .setOfferName(DEFAULT_OFFER_NAME)
                .setWareMd5(DEFAULT_WARE_MD5)
                .setCategoryId(DEFAULT_CATEGORY_ID)
                .setShowUid(DEFAULT_SHOW_UID)
                .setPrice(DEFAULT_PRICE)
                .setBuyerPrice(DEFAULT_PRICE)
                .setCount(DEFAULT_COUNT)
                .setIntFee(DEFAULT_FEE.intValue())
                .setNormFee(DEFAULT_FEE)
                .setShopFee(DEFAULT_FEE)
                .setNetFeeUE(DEFAULT_FEE)
                .setPromos(itemPromos);
    }

    static public MbiOrderItemPromo.Builder defaultItemPromo(MbiOrderPromo orderPromo) {
        return new MbiOrderItemPromo.Builder()
                .fromOrderPromo(orderPromo);
    }
}
