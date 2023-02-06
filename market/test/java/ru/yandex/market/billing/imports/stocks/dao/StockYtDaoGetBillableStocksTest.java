package ru.yandex.market.billing.imports.stocks.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.fulfillment.billing.storage.StorageBillingService;
import ru.yandex.market.billing.imports.shopsku.SupplierShopSkuKey;
import ru.yandex.market.billing.imports.stocks.Stock;
import ru.yandex.market.billing.imports.stocks.StockYtDao;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.util.FunctionalUtils.mapToSet;

@SuppressWarnings("rawtypes")
public class StockYtDaoGetBillableStocksTest extends FunctionalTest {

    private static final LocalDate DATE = LocalDate.of(2021, 12, 21);
    private static final String DATE_STR = "2021-12-21";

    @Autowired
    private StockYtDao stockYtDao;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/partner_types/" + DATE_STR,
                    "//home/market/production/mstat/dwh/staging/stock_sku/" + DATE_STR
            },
            csv = "StockYtDaoGetBillableStocksTest.fetchesOnlySupplierPartners.yt.csv",
            yqlMock = "StockYtDaoGetBillableStocksTest.fetchesOnlySupplierPartners.yt.mock"
    )
    public void fetchesOnlySupplierPartners() {
        List<Stock<SupplierShopSkuKey>> stocks = getBillableStocks();
        assertThat(stocks).hasSize(4);

        Set<Long> supplierIds = mapToSet(stocks, Stock::getSupplierId);
        assertThat(supplierIds).containsOnly(101L, 103L);
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/partner_types/" + DATE_STR,
                    "//home/market/production/mstat/dwh/staging/stock_sku/" + DATE_STR
            },
            csv = "StockYtDaoGetBillableStocksTest.fetchesOnlyBillableWarehouses.yt.csv",
            yqlMock = "StockYtDaoGetBillableStocksTest.fetchesOnlyBillableWarehouses.yt.mock"
    )
    public void fetchesOnlyBillableWarehouses() {
        List<Stock<SupplierShopSkuKey>> stocks = getBillableStocks();
        assertThat(stocks).hasSize(11);

        Set<Long> warehouseIds = mapToSet(stocks, Stock::getWarehouseId);
        Set<Long> billableWarehouses = StorageBillingService.BILLABLE_WAREHOUSES;
        assertThat(warehouseIds).containsOnly(billableWarehouses.toArray(new Long[]{}));
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/partner_types/" + DATE_STR,
                    "//home/market/production/mstat/dwh/staging/stock_sku/" + DATE_STR
            },
            csv = "StockYtDaoGetBillableStocksTest.fetchesOnlyStocksWithBillableAmount.yt.csv",
            yqlMock = "StockYtDaoGetBillableStocksTest.fetchesOnlyStocksWithBillableAmount.yt.mock"
    )
    public void fetchesOnlyStocksWithBillableAmount() {
        List<Stock<SupplierShopSkuKey>> stocks = getBillableStocks();
        Set<String> shopSkuSet = mapToSet(stocks, Stock::getShopSku);
        assertThat(shopSkuSet).containsOnly("101-1", "101-2", "101-3", "101-4");
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/partner_types/" + DATE_STR,
                    "//home/market/production/mstat/dwh/staging/stock_sku/" + DATE_STR
            },
            csv = "StockYtDaoGetBillableStocksTest.fillsDataObjectCorrectly.yt.csv",
            yqlMock = "StockYtDaoGetBillableStocksTest.fillsDataObjectCorrectly.yt.mock"
    )
    public void fillsDataObjectCorrectly() {
        List<Stock<SupplierShopSkuKey>> stocks = getBillableStocks();
        assertThat(stocks).hasSize(1);

        Stock<SupplierShopSkuKey> stock = stocks.get(0);
        assertThat(stock.getWarehouseId()).isEqualTo(147L);
        assertThat(stock.getSupplierId()).isEqualTo(101L);
        assertThat(stock.getShopSku()).isEqualTo("101-1-xxx");
        assertThat(stock.getAvailable()).isEqualTo(7L);
        assertThat(stock.getQuarantine()).isEqualTo(1L);
        assertThat(stock.getDefect()).isEqualTo(2L);
        assertThat(stock.getFreeze()).isEqualTo(5L);
        assertThat(stock.getExpired()).isEqualTo(4L);
        assertThat(stock.getLifetime()).isEqualTo(30L);
    }

    private List<Stock<SupplierShopSkuKey>> getBillableStocks() {
        List<Stock<SupplierShopSkuKey>> stocks = new ArrayList<>();
        stockYtDao.getBillableStocks(DATE, stocks::add);
        return stocks;
    }
}
