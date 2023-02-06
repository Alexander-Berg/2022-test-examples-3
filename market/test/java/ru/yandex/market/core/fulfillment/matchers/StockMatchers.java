package ru.yandex.market.core.fulfillment.matchers;

import java.time.LocalDateTime;

import org.hamcrest.Matcher;

import ru.yandex.market.core.fulfillment.model.Stock;
import ru.yandex.market.core.order.SupplierShopSkuKey;
import ru.yandex.market.core.order.SupplierWarehouseShopSkuKey;
import ru.yandex.market.mbi.util.MbiMatchers;

/**
 * Матчер на класс {@link Stock}.
 *
 * @author vbudnev
 */
public class StockMatchers {

    public static Matcher<Stock<SupplierWarehouseShopSkuKey>> hasWarehouseName(String expectedValue) {
        return MbiMatchers.<Stock<SupplierWarehouseShopSkuKey>>newAllOfBuilder()
                .add(stock -> stock.getKey().getWarehouseName(), expectedValue, "warehouseName")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasWarehouseId(Long expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getWarehouseId, expectedValue, "warehouseId")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasShopSku(String expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getShopSku, expectedValue, "shopSku")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasSupplierId(Long expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getSupplierId, expectedValue, "supplierId")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasAvailable(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getAvailable, expectedValue, "available")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasExpired(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getExpired, expectedValue, "expired")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasFreeze(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getFreeze, expectedValue, "freeze")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasDefect(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getDefect, expectedValue, "defect")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasQuarantine(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getQuarantine, expectedValue, "quarantine")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasUtilization(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getUtilization, expectedValue, "utilization")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasDateTime(LocalDateTime expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getDateTime, expectedValue, "dateTime")
                .build();
    }

    public static <T extends SupplierShopSkuKey> Matcher<Stock<T>> hasLifetime(Integer expectedValue) {
        return MbiMatchers.<Stock<T>>newAllOfBuilder()
                .add(Stock::getLifetime, expectedValue, "lifetime")
                .build();
    }
}
