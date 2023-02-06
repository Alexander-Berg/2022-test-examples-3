package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.service.yt.loader.EventTriggeredLoader;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ActiveProfiles("unittest")
public class RegionStatInfoLoaderTest extends FunctionalTest {

    private static final Exception ERROR_ON_NOW = new RuntimeException("Expected time get from event path");

    private static final LocalDate MOCKED_DATE = LocalDate.of(2020, 12, 12);
    private static final LocalDateTime MOCKED_DATETIME = LocalDateTime.of(MOCKED_DATE, LocalTime.MIN);

    @Autowired
    RegionStatInfoLoader regionStatInfoLoader;

    @Test
    @YqlTest(
        schemasDir = "/yt/schemas",
        schemas = {
            "//home/market/production/mbo/export/recent/models/sku",
            "//home/market/production/replenishment/order_planning/2020-12-12/intermediate/warehouses",
            "//home/market/production/replenishment/order_planning/2020-12-12/intermediate/regions",
            "//home/market/production/replenishment/order_planning/2020-12-05/intermediate/forecast_region",
            "//home/market/production/mstat/dictionaries/stock_sku/1d/2020-12-12",
            "//home/market/production/ir/ultra-controller/supplier_to_market_sku",
            "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
            "//home/market/production/mbi/dictionaries/partner_biz_snapshot/latest",
            "//home/market/production/replenishment/order_planning/2020-12-12/intermediate/suppliers_demand"
        },
        csv = "RegionStatInfoLoaderTest_import.yql.csv",
        yqlMock = "RegionStatInfoLoaderTest.yql.mock"
    )
    @DbUnitDataSet(
        before = "RegionStatInfoLoaderTest.before.csv",
        after = "RegionStatInfoLoaderTest.after.csv"
    )
    public void testLoading() {
        TimeService brokenTimeService = mock(TimeService.class);
        when(brokenTimeService.getNowDate()).thenThrow(ERROR_ON_NOW);
        when(brokenTimeService.getNowDateTime()).thenThrow(ERROR_ON_NOW);
        ReflectionTestUtils.setField(regionStatInfoLoader, "timeService", brokenTimeService);

        TimeService workingTimeService = mock(TimeService.class);
        when(workingTimeService.getNowDate()).thenReturn(MOCKED_DATE);
        when(workingTimeService.getNowDateTime()).thenReturn(MOCKED_DATETIME);
        ReflectionTestUtils.setField(
            regionStatInfoLoader, EventTriggeredLoader.class,
            "timeService", workingTimeService, TimeService.class
        );

        regionStatInfoLoader.load();
    }
}
