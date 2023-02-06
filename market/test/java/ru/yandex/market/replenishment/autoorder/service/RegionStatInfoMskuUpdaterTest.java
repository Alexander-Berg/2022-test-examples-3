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
import ru.yandex.market.replenishment.autoorder.service.yt.loader.sku_transition.RegionStatInfoMskuUpdater;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@ActiveProfiles("unittest")
public class RegionStatInfoMskuUpdaterTest extends FunctionalTest {

    private static final Exception ERROR_ON_NOW = new RuntimeException("Expected time get from event path");

    private static final LocalDate MOCKED_DATE = LocalDate.of(2021, 5, 20);
    private static final LocalDateTime MOCKED_DATETIME = LocalDateTime.of(MOCKED_DATE, LocalTime.MIN);

    @Autowired
    private RegionStatInfoMskuUpdater regionStatInfoMskuUpdater;

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/latest",
                    "//home/market/production/mstat/dictionaries/mbo/sku_transitions/2021-05-18"
            },
            csv = "RegionStatInfoMskuUpdaterTest_import.yql.csv",
            yqlMock = "RegionStatInfoMskuUpdaterTest.yql.mock"
    )
    @DbUnitDataSet(
            before = "RegionStatInfoMskuUpdaterTest.before.csv",
            after = "RegionStatInfoMskuUpdaterTest.after.csv"
    )
    public void testLoading() {
        TimeService brokenTimeService = mock(TimeService.class);
        when(brokenTimeService.getNowDate()).thenThrow(ERROR_ON_NOW);
        when(brokenTimeService.getNowDateTime()).thenThrow(ERROR_ON_NOW);
        ReflectionTestUtils.setField(regionStatInfoMskuUpdater, "timeService", brokenTimeService);

        TimeService workingTimeService = mock(TimeService.class);
        when(workingTimeService.getNowDate()).thenReturn(MOCKED_DATE);
        when(workingTimeService.getNowDateTime()).thenReturn(MOCKED_DATETIME);
        ReflectionTestUtils.setField(
                regionStatInfoMskuUpdater, EventTriggeredLoader.class,
                "timeService", workingTimeService, TimeService.class
        );

        regionStatInfoMskuUpdater.load();
    }
}
