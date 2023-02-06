package ru.yandex.market.vendors.analytics.core.service;

import ru.yandex.market.vendors.analytics.core.model.sales.common.RawPeriodicSales;
import ru.yandex.market.vendors.analytics.core.model.sales.shops.share.RawMarketShare;

/**
 * Util class for tests of widget controller methods.
 *
 * @author ogonek
 */
public class SalesTestUtils {

    private SalesTestUtils() {
    }

    public static RawPeriodicSales getRawSales(String date, long id, long moneySum, long countSum) {
        return RawPeriodicSales.builder()
                .date(date)
                .groupId(id)
                .moneySum(moneySum)
                .countSum(countSum)
                .build();
    }

    public static RawMarketShare createRawMarketShare(
            int shopsWithSales, int totalShops, String date, long groupId
    ) {
        return RawMarketShare.builder()
                .shopsWithSales(shopsWithSales)
                .totalShops(totalShops)
                .date(date)
                .groupId(groupId)
                .build();
    }
}
