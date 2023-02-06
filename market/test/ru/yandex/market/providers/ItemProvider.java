package ru.yandex.market.providers;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.shopadminstub.model.Item;
import ru.yandex.market.shopadminstub.model.ItemDeliveryOption;

import static ru.yandex.market.providers.ItemDeliveryOptionProvider.buildAverage;
import static ru.yandex.market.providers.ItemDeliveryOptionProvider.buildFastest;
import static ru.yandex.market.providers.ItemDeliveryOptionProvider.buildFree;
import static ru.yandex.market.providers.ItemDeliveryOptionProvider.buildGlobal;

public abstract class ItemProvider {

    public static final String DEFAULT_OFFER_ID = "1";
    public static final String ANOTHER_OFFER_ID = "2";
    public static final long DEFAULT_FEED_ID = 383182L;
    public static final FeedOfferId DEFAULT_FEED_OFFER_ID = new FeedOfferId(DEFAULT_OFFER_ID, DEFAULT_FEED_ID);

    public static final String DEFAULT_GLOBAL_OFFER_ID = "6";
    public static final long DEFAULT_GLOBAL_FEED_ID = 200312607L;

    public static final String DEFAULT_FULFILMENT_OFFER_ID = "333";
    public static final long DEFAULT_FULFILMENT_FEED_ID = 444L;

    public static final int DEFAULT_COUNT = 1;
    public static final Long DEFAULT_FULFILMENT_SHOP_ID = null;
    public static final Long FULFILMENT_SHOP_ID = 123456L;
    public static final String DEFAULT_SKU = null;
    public static final String FULFILMENT_SKU = "sku";
    public static final String DEFAULT_SHOP_SKU = null;
    public static final int DEFAULT_WAREHOUSE_ID = 1;
    public static final String FULFILMENT_SHOP_SKU = "shopSku";
    public static final String DEFAULT_WARE_MD5 = "4wTSrqUBspf3hkJrw6Peww";
    public static final String ANOTHER_WARE_MD5 = "4wTSrqUBspf3hkJrw6Pewe";
    public static final String DEFAULT_GLOBAL_WARE_MD5 = "cpmh964BKB-kuNv5gRKccg";
    public static final List<ItemDeliveryOption> DEFAULT_DELIVERY_OPTIONS;
    public static final List<ItemDeliveryOption> GLOBAL_DELIVERY_OPTIONS;

    static {
        DEFAULT_DELIVERY_OPTIONS = Arrays.asList(buildFree(), buildAverage(), buildFastest());
        GLOBAL_DELIVERY_OPTIONS = Collections.singletonList(buildGlobal());
    }

    public static Item buildDefaultItem() {
        return buildItem(DEFAULT_FEED_ID, DEFAULT_OFFER_ID);
    }

    public static Item buildGlobalItem() {
        Item item = buildItem(DEFAULT_GLOBAL_FEED_ID, DEFAULT_GLOBAL_OFFER_ID);
        item.setGlobal(true);
        item.setDeliveryOptions(GLOBAL_DELIVERY_OPTIONS);
        item.setPickupPossible(false);
        item.setWareMd5(DEFAULT_GLOBAL_WARE_MD5);
        return item;
    }

    public static Item buildItem(long feedId, String offerId) {
        return buildItem(feedId, offerId, DEFAULT_DELIVERY_OPTIONS);
    }

    public static Item buildItem(long feedId, String offerId, ItemDeliveryOption... itemDeliveryOptions) {
        return buildItem(feedId, offerId, Arrays.asList(itemDeliveryOptions));
    }

    public static Item buildItem(long feedId, String offerId, List<ItemDeliveryOption> itemDeliveryOptions) {
        Item item = new Item();
        item.setOfferId(offerId);
        item.setFeedId(feedId);
        item.setCount(DEFAULT_COUNT);
        item.setFulfilmentShopId(DEFAULT_FULFILMENT_SHOP_ID);
        item.setSku(DEFAULT_SKU);
        item.setShopSku(DEFAULT_SHOP_SKU);
        item.setWarehouseId(DEFAULT_WAREHOUSE_ID);
        // иначе будет NPE при сериализации OrderAcceptRequest
        item.setPrice(new BigDecimal("250"));
        item.setCurrency(Currency.RUR.name());
        item.setPickupPossible(true);
        item.setDeliveryPossible(true);
        item.setGlobal(false);
        item.setWareMd5(DEFAULT_WARE_MD5);
        item.setDeliveryOptions(itemDeliveryOptions);
        return item;
    }

    public static Item buildFulfilmentItem() {
        return makeFulfilment(buildItem(DEFAULT_FULFILMENT_FEED_ID, DEFAULT_FULFILMENT_OFFER_ID));
    }

    public static Item makeFulfilment(Item item) {
        item.setFulfilmentShopId(FULFILMENT_SHOP_ID);
        item.setSku(FULFILMENT_SKU);
        item.setShopSku(FULFILMENT_SHOP_SKU);
        return item;
    }
}
