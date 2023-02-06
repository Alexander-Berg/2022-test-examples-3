package ru.yandex.market.wms.radiator.service.stocks.infordb;

import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.fulfillment.ItemStocks;
import ru.yandex.market.logistic.api.model.fulfillment.Stock;
import ru.yandex.market.logistic.api.model.fulfillment.StockType;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wms.radiator.service.config.Dispatcher;
import ru.yandex.market.wms.radiator.test.IntegrationTestBackend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.wms.radiator.test.TestStocksData.mSku100_02;
import static ru.yandex.market.wms.radiator.test.TestStocksData.mSku100_03;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_1_ID;
import static ru.yandex.market.wms.radiator.test.IntegrationTestConstants.WH_2_ID;

class InforDbStocksServiceTest extends IntegrationTestBackend {

    @Autowired
    private InforDbStocksService stocksService;
    @Autowired
    private Dispatcher dispatcher;


    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-1.xml", connection = "wh2Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh2Connection")
    })
    @Test
    void getByRange() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    List<ItemStocks> result = stocksService.getByRange(1, 2);
                    assertThat(result.size(), is(equalTo(2)));
                    assertThat(result.get(0).getUnitId(), is(equalTo(mSku100_02(WH_1_ID).getUnitId())));
                    assertThat(result.get(1).getUnitId(), is(equalTo(mSku100_03(WH_1_ID).getUnitId())));
                }
        );
        dispatcher.withWarehouseId(
                WH_2_ID, () -> {
                    List<ItemStocks> result = stocksService.getByRange(1, 2);
                    assertThat(result.size(), is(equalTo(2)));
                    assertThat(result.get(0).getUnitId(), is(equalTo(mSku100_02(WH_2_ID).getUnitId())));
                    assertThat(result.get(1).getUnitId(), is(equalTo(mSku100_03(WH_2_ID).getUnitId())));
                }
        );
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-2.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection")
    })
    @Test
    void testFitQty() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    UnitId unitId = new UnitId.UnitIdBuilder(100L, "M_SKU_100_03").build();
                    List<ItemStocks> result = stocksService.getByIds(Collections.singletonList(unitId));
                    assertThat(result.size(), is(equalTo(1)));
                    ItemStocks itemStocks = result.get(0);
                    assertThat(getStockType(itemStocks, StockType.FIT), is(equalTo(5)));
                    assertThat(getStockType(itemStocks, StockType.QUARANTINE), is(equalTo(0)));
                    assertThat(getStockType(itemStocks, StockType.EXPIRED), is(equalTo(0)));
                    assertThat(getStockType(itemStocks, StockType.DEFECT), is(equalTo(0)));
                    assertThat(getStockType(itemStocks, StockType.SURPLUS), is(equalTo(7)));
                }
        );
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-3.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection")
    })
    @Test
    void testQuarantineQty() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    UnitId unitId = new UnitId.UnitIdBuilder(100L, "M_SKU_100_03").build();
                    List<ItemStocks> result = stocksService.getByIds(Collections.singletonList(unitId));
                    assertThat(result.size(), is(equalTo(1)));
                    ItemStocks itemStocks = result.get(0);
                    assertThat("fit", getStockType(itemStocks, StockType.FIT), is(equalTo(5)));
                    assertThat("qrt", getStockType(itemStocks, StockType.QUARANTINE), is(equalTo(2 + 7)));
                    assertThat("exp", getStockType(itemStocks, StockType.EXPIRED), is(equalTo(0)));
                    assertThat("def", getStockType(itemStocks, StockType.DEFECT), is(equalTo(0)));
                    assertThat("sur", getStockType(itemStocks, StockType.SURPLUS), is(equalTo(0)));
                }
        );
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-4.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection")
    })
    @Test
    void testDefectQty() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    UnitId unitId = new UnitId.UnitIdBuilder(100L, "M_SKU_100_03").build();
                    List<ItemStocks> result = stocksService.getByIds(Collections.singletonList(unitId));
                    assertThat(result.size(), is(equalTo(1)));
                    ItemStocks itemStocks = result.get(0);
                    assertThat("fit", getStockType(itemStocks, StockType.FIT), is(equalTo(5)));
                    assertThat("qrt", getStockType(itemStocks, StockType.QUARANTINE), is(equalTo(0)));
                    assertThat("exp", getStockType(itemStocks, StockType.EXPIRED), is(equalTo(0)));
                    assertThat("def", getStockType(itemStocks, StockType.DEFECT), is(equalTo(3)));
                    assertThat("sur", getStockType(itemStocks, StockType.SURPLUS), is(equalTo(0)));
                }
        );
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-5.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection")
    })
    @Test
    void testExpiredQty() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    UnitId unitId = new UnitId.UnitIdBuilder(100L, "M_SKU_100_03").build();
                    List<ItemStocks> result = stocksService.getByIds(Collections.singletonList(unitId));
                    assertThat(result.size(), is(equalTo(1)));
                    ItemStocks itemStocks = result.get(0);
                    assertThat("fit", getStockType(itemStocks, StockType.FIT), is(equalTo(5)));
                    assertThat("qrt", getStockType(itemStocks, StockType.QUARANTINE), is(equalTo(0)));
                    assertThat("exp", getStockType(itemStocks, StockType.EXPIRED), is(equalTo(3)));
                    assertThat("def", getStockType(itemStocks, StockType.DEFECT), is(equalTo(0)));
                    assertThat("sur", getStockType(itemStocks, StockType.SURPLUS), is(equalTo(0)));
                }
        );
    }

    @DatabaseSetups({
            @DatabaseSetup(value = "/fixtures/dbStocks/sku-6.xml", connection = "wh1Connection"),
            @DatabaseSetup(value = "/fixtures/dbSqlConfig/fallback-enabled.xml", connection = "wh1Connection")
    })
    @Test
    void testIntransitQuarantineQty() {
        dispatcher.withWarehouseId(
                WH_1_ID, () -> {
                    UnitId unitId = new UnitId.UnitIdBuilder(100L, "M_SKU_100_03").build();
                    List<ItemStocks> result = stocksService.getByIds(Collections.singletonList(unitId));
                    assertThat(result.size(), is(equalTo(1)));
                    ItemStocks itemStocks = result.get(0);
                    assertThat("fit", getStockType(itemStocks, StockType.FIT), is(equalTo(5)));
                    assertThat("qrt", getStockType(itemStocks, StockType.QUARANTINE),
                            is(equalTo(2 + 7 + 3)));
                    assertThat("exp", getStockType(itemStocks, StockType.EXPIRED), is(equalTo(0)));
                    assertThat("def", getStockType(itemStocks, StockType.DEFECT), is(equalTo(0)));
                    assertThat("sur", getStockType(itemStocks, StockType.SURPLUS), is(equalTo(0)));
                }
        );
    }

    private Integer getStockType(ItemStocks itemStocks, StockType type) {
        return itemStocks.getStocks().stream()
                .filter(s -> s.getType().equals(type))
                .findFirst()
                .map(Stock::getCount)
                .orElse(0);
    }
}
