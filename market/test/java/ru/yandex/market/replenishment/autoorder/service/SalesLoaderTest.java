package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.EventTriggeredLoader;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.SalesLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.Mockito.when;

public class SalesLoaderTest extends FunctionalTest {

    @Autowired
    SalesLoader salesLoader;

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-11",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2021-11-16T04--colon--00--colon--00",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2021-11-16T12--colon--00--colon--00",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest"
        },
        csv = "SalesLoaderTest_testLoading.yql.csv",
        yqlMock = "SalesLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(before = "SalesLoaderTest_testLoading.before.csv",
        after = "SalesLoaderTest_testLoading.after.csv")
    public void testLoading() {
        TimeService timeService = Mockito.mock(TimeService.class);
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2021, 12, 12));
        when(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2021, 12, 12, 0, 0));
        ReflectionTestUtils.setField(salesLoader, "timeService", timeService);
        ReflectionTestUtils.setField(salesLoader, EventTriggeredLoader.class, "timeService", timeService,
            TimeService.class);
        salesLoader.load();
    }

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_item_dict/2021-11",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2021-11-16T04--colon--00--colon--00",
            "//home/market/production/monetize/dynamic_pricing/expiring_goods/expiring_prices/2021-11-16T12--colon--00--colon--00",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_dict",
            "//home/market/production/mstat/analyst/regular/cubes_vertica/fact_new_order_delivery",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mstat/dictionaries/shop_real_supplier/latest"
        },
        csv = "SalesLoaderTest_testLoading.yql.csv",
        yqlMock = "SalesLoaderTest_testLoading.yql.mock.json"
    )
    @DbUnitDataSet(before = "SalesLoaderTest_testExpanding.before.csv",
        after = "SalesLoaderTest_testExpanding.after.csv")
    public void testExpanding() {
        TimeService timeService = Mockito.mock(TimeService.class);
        when(timeService.getNowDate()).thenReturn(LocalDate.of(2021, 12, 12));
        when(timeService.getNowDateTime()).thenReturn(LocalDateTime.of(2021, 12, 12, 0, 0));
        ReflectionTestUtils.setField(salesLoader, "timeService", timeService);
        ReflectionTestUtils.setField(salesLoader, EventTriggeredLoader.class, "timeService", timeService,
            TimeService.class);
        salesLoader.load();
    }
}
