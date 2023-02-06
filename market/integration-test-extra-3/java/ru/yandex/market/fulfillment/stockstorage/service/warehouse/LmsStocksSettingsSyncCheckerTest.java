package ru.yandex.market.fulfillment.stockstorage.service.warehouse;

import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.common.ping.CheckResult;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.Warehouse;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.FFInterval;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.FFIntervalService;
import ru.yandex.market.fulfillment.stockstorage.service.lms.LmsPartnerType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LmsStocksSettingsSyncCheckerTest {

    LmsStocksSettingsSyncChecker checker;
    SoftAssertions assertions;
    private FFIntervalService ffIntervalService;
    private WarehouseSyncService warehouseSyncService;

    private static Stream<Arguments> bothCases() {
        return Stream.of(
                Arguments.of(
                        ImmutableMap.of(
                                100, new Warehouse(100, false, null, LmsPartnerType.DROPSHIP, true),
                                200, new Warehouse(200, true, null, LmsPartnerType.FULFILLMENT, true)),
                        ImmutableList.of(
                                new FFInterval().setWarehouseId(100).setActive(true),
                                new FFInterval().setWarehouseId(200).setActive(false)),
                        new CheckResult(CheckResult.Level.WARNING,
                                "SS does not sync stocks for warehouses switched on in LMS: [200]")),
                Arguments.of(
                        ImmutableMap.of(
                                100, new Warehouse(100, true, null, LmsPartnerType.DROPSHIP, true),
                                200, new Warehouse(200, false, null, LmsPartnerType.FULFILLMENT, true)),
                        ImmutableList.of(
                                new FFInterval().setWarehouseId(100).setActive(false),
                                new FFInterval().setWarehouseId(200).setActive(true)),
                        new CheckResult(CheckResult.Level.CRITICAL,
                                "SS syncs stocks for warehouses switched off in LMS: [200]" +
                                "SS does not sync stocks for warehouses switched on in LMS: [100]"))
        );
    }

    @BeforeEach
    void setUp() {
        ffIntervalService = mock(FFIntervalService.class);
        warehouseSyncService = mock(WarehouseSyncService.class);
        checker = new LmsStocksSettingsSyncChecker(warehouseSyncService, ffIntervalService);
        assertions = new SoftAssertions();
    }

    @AfterEach
    void tearDown() {
        assertions.assertAll();
    }

    @ParameterizedTest
    @MethodSource("bothCases")
    void excludeNotFulfillmentFromCritical(ImmutableMap<Integer, Warehouse> fromLms, ImmutableList<FFInterval> stored,
                             CheckResult expectedResult) {
        when(warehouseSyncService.getAllWarehousesMap()).thenReturn(fromLms);
        when(ffIntervalService.findAllByJobNames(any())).thenReturn(stored);
        CheckResult checkResult = checker.check();
        assertions.assertThat(checkResult).isEqualToComparingFieldByField(expectedResult);
    }
}
