package ru.yandex.market.ff.util;

import org.mockito.verification.VerificationMode;

import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Sku;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.Stock;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.StockType;

import static java.util.Collections.singletonList;


public class TestUtils {
    static DontCareMode dontCareMode = new DontCareMode();

    private TestUtils() {

    }

    public static Sku createSkuWithSingleFitStock(String sku, int availableAmount) {
        return Sku.of(SSItem.of(sku, 1L, 1),
                singletonList(Stock.of(availableAmount, 0, availableAmount, String.valueOf(StockType.FIT))),
                null, true, true, null, null);
    }

    public static VerificationMode dontCare() {
        return dontCareMode;
    }

}
