package ru.yandex.market.vendor.controllers;

import java.time.LocalDate;
import java.util.Set;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.stats.StatisticsScaleType;
import ru.yandex.vendor.stats.brandzone.BrandzoneMetricType;
import ru.yandex.vendor.stats.brandzone.model.TrafficGroup;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * {@link BrandzoneStatsController}
 */
public class BrandzoneStatsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testAllMetricDayAdStatsEmptyData/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testAllMetricDayAdStatsEmptyData/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testAllMetricDayAdStatsEmptyData() {
        long from = TimeUtil.toMillis(LocalDate.of(2020, 9,1));
        long to = TimeUtil.toMillis(LocalDate.of(2020, 9,7));
        String url = createStatsUrl("",321, from, to,
                null,
                Set.of(TrafficGroup.AD),
                StatisticsScaleType.DAY);
        String actual = FunctionalTestHelper.get(url);
        String expected = getStringResource("/testAllMetricDayAdStatsEmptyData/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testAllMetricDayAdStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testAllMetricDayAdStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testAllMetricDayAdStats() {
        long from = TimeUtil.toMillis(LocalDate.of(2020, 9,1));
        long to = TimeUtil.toMillis(LocalDate.of(2020, 9,7));
        String url = createStatsUrl("",321, from, to,
                BrandzoneMetricType.getMetricsAsSet(),
                Set.of(TrafficGroup.AD),
                StatisticsScaleType.DAY);
        String actual = FunctionalTestHelper.get(url);
        String expected = getStringResource("/testAllMetricDayAdStats/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testShowsDayAdStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testShowsDayAdStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testShowsDayAdStats() {
        long from = TimeUtil.toMillis(LocalDate.of(2020, 9,1));
        long to = TimeUtil.toMillis(LocalDate.of(2020, 9,7));
        String url = createStatsUrl("",321, from, to,
                Set.of(BrandzoneMetricType.SHOWS), Set.of(TrafficGroup.AD), StatisticsScaleType.DAY);
        String actual = FunctionalTestHelper.get(url);
        String expected = getStringResource("/testShowsDayAdStats/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testShowsAndClickoutsWeekStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testShowsAndClickoutsWeekStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testShowsAndClickoutsWeekStats() {
        long from = TimeUtil.toMillis(LocalDate.of(2020, 12,7));
        long to = TimeUtil.toMillis(LocalDate.of(2020, 12,20));
        String url = createStatsUrl("",
                321, from, to,
                Set.of(BrandzoneMetricType.SHOWS, BrandzoneMetricType.CLICKOUTS),
                Set.of(),
                StatisticsScaleType.WEEK
        );
        String actual = FunctionalTestHelper.get(url);
        String expected = getStringResource("/testShowsAndClickoutsWeekStats/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testAllMetricsMonthOrganic/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testAllMetricsMonthOrganic/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testAllMetricsMonthOrganic() {
        long from = TimeUtil.toMillis(LocalDate.of(2020, 9,1));
        String url = createStatsUrl("",
                321, from, null,
                Set.of(BrandzoneMetricType.values()),
                Set.of(TrafficGroup.ORGANIC),
                StatisticsScaleType.MONTH
        );
        String actual = FunctionalTestHelper.get(url);
        System.out.println(actual);
        String expected = getStringResource("/testAllMetricsMonthOrganic/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testPostAsyncReport/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandzoneStatsControllerFunctionalTest/testPostAsyncReport/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPostAsyncReport() {
        long from = TimeUtil.toMillis(LocalDate.of(2020, 12,1));
        String url = createStatsUrl(
                "/asyncDownload",
                321,
                from,
                null,
                Set.of(BrandzoneMetricType.SHOWS),
                Set.of(TrafficGroup.ORGANIC),
                StatisticsScaleType.DAY);
        String actual = FunctionalTestHelper.get(url);
        System.out.println(actual);
        String expected = getStringResource("/testPostAsyncReport/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    private String createStatsUrl(String place,
                                  long vendorId,
                                  long from,
                                  Long to,
                                  Set<BrandzoneMetricType> metrics,
                                  Set<TrafficGroup> trafficGroups,
                                  StatisticsScaleType scaleType) {
        StringBuilder url = new StringBuilder(baseUrl + "/vendors/");
        url.append(vendorId).append("/brandzone/statistics");
        url.append(place);
        url.append("?uid=1&from=");
        url.append(from);
        if (to != null) {
            url.append("&to=").append(to);
        }
        if (metrics != null) {
            for (BrandzoneMetricType metric : metrics) {
                url.append("&metrics=").append(metric.name());
            }
        }
        if (trafficGroups != null) {
            for (TrafficGroup trafficGroup : trafficGroups) {
                url.append("&trafficGroups=").append(trafficGroup.name());
            }
        }
        url.append("&scale=").append(scaleType.name());
        return url.toString();
    }

}
