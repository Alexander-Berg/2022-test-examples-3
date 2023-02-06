package ru.yandex.market.billing.imports.shopsku;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;

public class ShopSkuInfoTestData {

    private ShopSkuInfoTestData() {
    }

    public static ShopSkuInfo smallBox(Long supplierId, String shopSku) {
        return new ShopSkuInfo()
                .withSupplierId(supplierId)
                .withShopSku(shopSku)
                .withLength(70)
                .withWidth(50)
                .withHeight(40)
                .withWeight(BigDecimal.ONE.movePointLeft(1).setScale(3, RoundingMode.DOWN));
    }

    public static ShopSkuInfo mediumBox(Long supplierId, String shopSku) {
        return new ShopSkuInfo()
                .withSupplierId(supplierId)
                .withShopSku(shopSku)
                .withLength(300)
                .withWidth(100)
                .withHeight(80)
                .withWeight(BigDecimal.ONE.setScale(3, RoundingMode.DOWN));
    }

    public static ShopSkuInfo largeBox(Long supplierId, String shopSku) {
        return new ShopSkuInfo()
                .withSupplierId(supplierId)
                .withShopSku(shopSku)
                .withLength(2000)
                .withWidth(700)
                .withHeight(500)
                .withWeight(BigDecimal.TEN.setScale(3, RoundingMode.DOWN));
    }

    public static void assertEquals(ShopSkuInfo actual, ShopSkuInfo expected) {
        assertThat(actual.getSupplierId()).as("supplierId").isEqualTo(expected.getSupplierId());
        assertThat(actual.getShopSku()).as("shopSku").isEqualTo(expected.getShopSku());
        assertThat(actual.getLength()).as("length").isEqualTo(expected.getLength());
        assertThat(actual.getWidth()).as("width").isEqualTo(expected.getWidth());
        assertThat(actual.getHeight()).as("height").isEqualTo(expected.getHeight());
        assertThat(actual.getWeight()).as("weight").isEqualTo(expected.getWeight());
    }
}
