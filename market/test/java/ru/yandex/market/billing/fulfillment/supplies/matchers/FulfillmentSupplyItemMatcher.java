package ru.yandex.market.billing.fulfillment.supplies.matchers;

import java.time.Instant;

import org.hamcrest.Matcher;

import ru.yandex.market.core.billing.fulfillment.supplies.model.FulfillmentSupplyItem;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчеры для {@link FulfillmentSupplyItem}.
 *
 * @author vbudnev
 */
public class FulfillmentSupplyItemMatcher {

    public static Matcher<FulfillmentSupplyItem> hasSupplyId(long expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getSupplyId, expectedValue, "supplyId")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasCategoryId(Long expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getCategoryId, expectedValue, "categoryId")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasFactCount(long expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getFactCount, expectedValue, "factCount")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasCount(long expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getCount, expectedValue, "Count")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasMappingUpdateTime(Instant expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getMappingUpdateTime, expectedValue, "mappingUpdateTime")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasShopSku(String expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasSupplierId(long expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static Matcher<FulfillmentSupplyItem> hasSurplusCount(long expectedValue) {
        return MbiMatchers.<FulfillmentSupplyItem>newAllOfBuilder()
                .add(FulfillmentSupplyItem::getSurplusCount, expectedValue, "surplusCount")
                .build();
    }

}
