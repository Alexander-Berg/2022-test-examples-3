package ru.yandex.market.core.fulfillment.matchers;

import org.hamcrest.Matcher;

import ru.yandex.market.core.fulfillment.SkuStockInfo;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчер на класс {@link SkuStockInfo}.
 */
public class SkuStockInfoMatchers {
    public static Matcher<SkuStockInfo> hasShopSku(String expectedValue) {
        return MbiMatchers.<SkuStockInfo>newAllOfBuilder()
                .add(SkuStockInfo::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<SkuStockInfo> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<SkuStockInfo>newAllOfBuilder()
                .add(SkuStockInfo::getSupplierId, expectedValue, "supplierId")
                .build();
    }
}
