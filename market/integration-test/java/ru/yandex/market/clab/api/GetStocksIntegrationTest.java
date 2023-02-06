package ru.yandex.market.clab.api;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.clab.api.stock.StocksService;
import ru.yandex.market.clab.common.service.good.GoodFilter;
import ru.yandex.market.clab.common.service.good.GoodService;
import ru.yandex.market.clab.db.jooq.generated.tables.pojos.Good;
import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.ItemExpiration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 23.04.2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class GetStocksIntegrationTest extends BaseApiIntegrationTest {
    private static final Long SUPPLIER_ID = 75785410L;
    private static final String SHOP_SKU_1 = "SHOP_SKU_1";
    private static final String SHOP_SKU_2 = "SHOP_SKU_2";
    private static final String SHOP_SKU_3 = "SHOP_SKU_3";

    @Autowired
    private GoodService goodService;

    @Autowired
    private StocksService stocksService;

    @Test
    public void stocksPagingWithShopSkuIdCollision() {
        createNGoods(2, SHOP_SKU_1);
        createNGoods(3, SHOP_SKU_2);
        createNGoods(5, SHOP_SKU_3);

        assertThat(goodService.getGoodsNoData(GoodFilter.any())).hasSize(2 + 3 + 5);

        final int pageSize = 2;
        List<ItemStocks> stocks = stocksService.getStocks(pageSize, 0, null);
        assertThat(stocks)
            .extracting(ItemStocks::getUnitId)
            .containsExactlyInAnyOrder(
                new UnitId(null, SUPPLIER_ID, SHOP_SKU_1),
                new UnitId(null, SUPPLIER_ID, SHOP_SKU_2)
            );

        stocks = stocksService.getStocks(pageSize, pageSize, null);
        assertThat(stocks)
            .extracting(ItemStocks::getUnitId)
            .containsExactlyInAnyOrder(
                new UnitId(null, SUPPLIER_ID, SHOP_SKU_3)
            );
    }

    @Test
    public void expirationItemsPagingWithShopSkuIdCollision() {
        LocalDateTime manufacturedDate = LocalDate.of(2018, Month.AUGUST, 20).atStartOfDay();
        createNExpiringGoods(2, SHOP_SKU_1, manufacturedDate);
        createNExpiringGoods(3, SHOP_SKU_2, manufacturedDate);
        createNExpiringGoods(5, SHOP_SKU_3, manufacturedDate);

        assertThat(goodService.getGoodsNoData(GoodFilter.any())).hasSize(2 + 3 + 5);

        final int pageSize = 2;
        List<ItemExpiration> expirations = stocksService.getExpirationItems(pageSize, 0, null);
        assertThat(expirations)
            .extracting(ItemExpiration::getUnitId)
            .containsExactlyInAnyOrder(
                new UnitId(null, SUPPLIER_ID, SHOP_SKU_1),
                new UnitId(null, SUPPLIER_ID, SHOP_SKU_2)
            );

        expirations = stocksService.getExpirationItems(pageSize, pageSize, null);
        assertThat(expirations)
            .extracting(ItemExpiration::getUnitId)
            .containsExactlyInAnyOrder(
                new UnitId(null, SUPPLIER_ID, SHOP_SKU_3)
            );
    }

    private void createNExpiringGoods(int n, String shopSkuId, LocalDateTime manufacturedDate) {
        for (int i = 0; i < n; i++) {
            Good good = new Good();
            good.setSupplierId(SUPPLIER_ID);
            good.setSupplierSkuId(shopSkuId);
            good.setManufacturedDate(manufacturedDate);
            goodService.createGood(good);
        }
    }

    private void createNGoods(int n, String shopSkuId) {
        createNExpiringGoods(n, shopSkuId, null);
    }
}
