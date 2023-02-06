package ru.yandex.market.vendors.analytics.core.service.sales.hiding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.vendors.analytics.core.FunctionalTest;
import ru.yandex.market.vendors.analytics.core.exception.badrequest.hidden.HiddenDataException;
import ru.yandex.market.vendors.analytics.core.model.common.GeoFilters;
import ru.yandex.market.vendors.analytics.core.model.common.StartEndDate;
import ru.yandex.market.vendors.analytics.core.model.enums.HidingEntityType;
import ru.yandex.market.vendors.analytics.core.model.region.ClickhouseCityType;
import ru.yandex.market.vendors.analytics.core.model.sales.common.RawPeriodicSales;
import ru.yandex.market.vendors.analytics.core.service.SalesTestUtils;
import ru.yandex.market.vendors.analytics.core.service.sales.common.DBDatesInterval;
import ru.yandex.market.vendors.analytics.core.service.strategies.TimeDetailing;
import ru.yandex.market.vendors.analytics.core.utils.dbunit.ClickhouseDbUnitDataSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Тесты методов для скрытий из.
 *
 * @author ogonek
 */
@ClickhouseDbUnitDataSet(before = "filterRawSalesDatesByRegionsHiding.before.csv")
public class SalesHidingServiceTest extends FunctionalTest {

    @Autowired
    private SalesHidingService salesHidingService;

    @Autowired
    private StrictHidingValidationService strictHidingValidationService;

    @Test
    @DisplayName("Проверяет, что строгое категорийное скрытие работает верно")
    void strictValidateCategory() {
        DBDatesInterval dbDatesInterval = new DBDatesInterval(
                new StartEndDate("2018-01-01", "2018-02-01"),
                TimeDetailing.MONTH
        );
        HiddenDataException expected = new HiddenDataException(
                HidingEntityType.CATEGORY,
                Map.of(20L, List.of("2018-01-01", "2018-02-01")),
                TimeDetailing.MONTH
        );

        HiddenDataException actual = Assertions.assertThrows(
                HiddenDataException.class,
                () -> strictHidingValidationService.strictValidateCategoryHidings(
                        20,
                        GeoFilters.empty(),
                        dbDatesInterval
                )
        );

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Проверяет, что фильтрация по категорийным скрытиям работает верно")
    void filterCategoryHiding() {
        var categorySales = List.of(
                getRawPeriodicSales(100, 1, 2, "2018-01-01"),
                getRawPeriodicSales(200, 2, 2, "2018-02-01"),
                getRawPeriodicSales(500, 5, 2, "2018-03-01")
        );

        var geoFilters = getGeoFilters(Set.of(1L), Collections.emptySet());

        Set<String> hiddenDates = salesHidingService.getCategoryHiddenDates(
                2,
                geoFilters,
                TimeDetailing.MONTH,
                categorySales,
                RawPeriodicSales::getDate
        );

        var filtered = SalesHidingService.filterRawSalesByHiddenDates(
                categorySales,
                hiddenDates
        );

        var expected = List.of(
                getRawPeriodicSales(100, 1, 2, "2018-01-01"),
                getRawPeriodicSales(200, 2, 2, "2018-02-01")
        );
        Assertions.assertEquals(expected, filtered);

        var expectedHiddenDates = Set.of("2018-03-01");
        Assertions.assertEquals(expectedHiddenDates, hiddenDates);
    }

    @Test
    @DisplayName("Проверяет, что строгое региональное скрытие работает верно")
    void strictValidateRegion() {
        DBDatesInterval dbDatesInterval = new DBDatesInterval(
                new StartEndDate("2018-01-01", "2018-02-01"),
                TimeDetailing.MONTH
        );

        HiddenDataException actual = Assertions.assertThrows(
                HiddenDataException.class,
                () -> strictHidingValidationService.strictValidateCategoryHidings(
                        3,
                        getGeoFilters(Set.of(11L, 12L), Collections.emptySet()),
                        dbDatesInterval
                )
        );

        HiddenDataException expected = new HiddenDataException(
                HidingEntityType.CATEGORY,
                Map.of(3L, List.of("2018-01-01", "2018-02-01")),
                TimeDetailing.MONTH
        );

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Фильтрация данных по региональным скрытиям")
    void filterRawSalesDatesByRegionsHiding() {
        var rawDataList = new ArrayList<>(Arrays.asList(getDefaultDataList()));
        rawDataList.add(getRawPeriodicSales(1, 1, 1, "2018-03-02"));

        var expectedGeoFilters = getGeoFilters(Set.of(1L), Collections.emptySet());

        Set<String> hiddenDates =
                salesHidingService.getCategoryHiddenDates(
                        1,
                        expectedGeoFilters,
                        TimeDetailing.DAY,
                        rawDataList,
                        RawPeriodicSales::getDate
                );

        var actual = SalesHidingService.filterRawSalesByHiddenDates(
                rawDataList,
                hiddenDates
        );

        RawPeriodicSales[] expected = getDefaultDataList();
        assertThat(actual, containsInAnyOrder(expected));

        var expectedHiddenDates = Set.of("2018-03-02");
        Assertions.assertEquals(expectedHiddenDates, hiddenDates);
    }

    @Test
    @DisplayName("Проверяет, что строгое скрытие по типам городов работает верно")
    void strictValidateCityTypes() {
        DBDatesInterval dbDatesInterval = new DBDatesInterval(
                new StartEndDate("2018-01-01", "2018-02-01"),
                TimeDetailing.MONTH
        );

        HiddenDataException actual = Assertions.assertThrows(
                HiddenDataException.class,
                () -> strictHidingValidationService.strictValidateCategoryHidings(
                        2,
                        getGeoFilters(Set.of(11L, 12L), Set.of(ClickhouseCityType.POPULATION_LESS_500K)),
                        dbDatesInterval
                )
        );

        HiddenDataException expected = new HiddenDataException(
                HidingEntityType.CATEGORY,
                Map.of(2L, List.of("2018-01-01", "2018-02-01")),
                TimeDetailing.MONTH
        );

        Assertions.assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Проверяет, что фильтрация по скрытиям по типам городов работает верно")
    void filterCityTypeHiding() {
        var categorySales = List.of(
                getRawPeriodicSales(100, 1, 3, "2018-01-01"),
                getRawPeriodicSales(200, 2, 3, "2018-02-01"),
                getRawPeriodicSales(500, 5, 3, "2018-05-01")
        );

        var geoFilters = getGeoFilters(Set.of(17L), Set.of(ClickhouseCityType.SAINT_PETERSBURG));
        var dbDatesInterval = new DBDatesInterval(
                new StartEndDate("2018-01-01", "2018-06-01"),
                TimeDetailing.MONTH
        );

        Set<String> hiddenDates = salesHidingService.getCategoryHiddenDates(
                2,
                geoFilters,
                TimeDetailing.MONTH,
                categorySales,
                RawPeriodicSales::getDate
        );

        var filtered = SalesHidingService.filterRawSalesByHiddenDates(
                categorySales,
                hiddenDates
        );

        var expected = List.of(getRawPeriodicSales(200, 2, 3, "2018-02-01"));
        Assertions.assertEquals(expected, filtered);

        var expectedHiddenDates = Set.of("2018-05-01", "2018-01-01");
        Assertions.assertEquals(expectedHiddenDates, hiddenDates);
    }

    private static RawPeriodicSales[] getDefaultDataList() {
        return new RawPeriodicSales[]{
                getRawPeriodicSales(1, 1, 1, "2018-01-01"),
                getRawPeriodicSales(1, 1, 1, "2018-01-02"),
                getRawPeriodicSales(1, 1, 1, "2018-01-21"),
                getRawPeriodicSales(1, 1, 1, "2018-01-31"),
                getRawPeriodicSales(1, 1, 1, "2018-02-02")
        };
    }

    private static RawPeriodicSales getRawPeriodicSales(long money, long count, long group, String date) {
        return SalesTestUtils.getRawSales(date, group, money, count);
    }

    private static GeoFilters getGeoFilters(Set<Long> regions, Set<ClickhouseCityType> cityTypes) {
        return GeoFilters.builder()
                .federalSubjectIds(regions)
                .federalDistrictIds(Set.of())
                .clickhouseCityTypes(cityTypes)
                .build();
    }
}
