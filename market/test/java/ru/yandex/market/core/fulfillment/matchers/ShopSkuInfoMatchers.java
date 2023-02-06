package ru.yandex.market.core.fulfillment.matchers;

import java.math.BigDecimal;

import org.hamcrest.Matcher;

import ru.yandex.market.core.fulfillment.model.ShopSkuInfo;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * @author vbudnev
 */
public class ShopSkuInfoMatchers {

    public static Matcher<ShopSkuInfo> hasWeight(BigDecimal expectedValue) {
        return MbiMatchers.<ShopSkuInfo>newAllOfBuilder()
                .add(ShopSkuInfo::getWeight, expectedValue, "weight")
                .build();
    }

    public static Matcher<ShopSkuInfo> hasLength(Integer expectedValue) {
        return MbiMatchers.<ShopSkuInfo>newAllOfBuilder()
                .add(ShopSkuInfo::getLength, expectedValue, "length")
                .build();
    }

    public static Matcher<ShopSkuInfo> hasWidth(Integer expectedValue) {
        return MbiMatchers.<ShopSkuInfo>newAllOfBuilder()
                .add(ShopSkuInfo::getWidth, expectedValue, "width")
                .build();
    }

    public static Matcher<ShopSkuInfo> hasHeight(Integer expectedValue) {
        return MbiMatchers.<ShopSkuInfo>newAllOfBuilder()
                .add(ShopSkuInfo::getHeight, expectedValue, "height")
                .build();
    }

    public static Matcher<ShopSkuInfo> hasShopSku(String expectedValue) {
        return MbiMatchers.<ShopSkuInfo>newAllOfBuilder()
                .add(ShopSkuInfo::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static Matcher<ShopSkuInfo> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<ShopSkuInfo>newAllOfBuilder()
                .add(ShopSkuInfo::getSupplierId, expectedValue, "supplierId")
                .build();
    }

}
