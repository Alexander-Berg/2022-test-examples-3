package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.vendor.stats.modelbidbanner.model.MarketModelBidsBannersContext;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.vendor.stats.StatisticsScaleType.DAY;
import static ru.yandex.vendor.stats.StatisticsScaleType.MONTH;
import static ru.yandex.vendor.stats.StatisticsScaleType.WEEK;
import static ru.yandex.vendor.stats.modelbidbanner.model.MarketModelBidsBannerMetric.SHOWS;

public class MarketModelBidsBannersStatsControllerTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private Clock clock;

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testSummaryStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testSummaryStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSummaryStats() {
        var to = TimeUtil.toMillis(LocalDate.of(2021, 4, 1));
        doReturn(to).when(clock).millis();
        var from = TimeUtil.toMillis(LocalDate.of(2021, 3, 1));
        var actually = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/statistics/summary?from=" + from + "&uid=1");
        var expected = getStringResource("/testSummaryStats/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testSummaryMultiBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testSummaryMultiBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSummaryMultiBrand() {
        var to = TimeUtil.toMillis(LocalDate.of(2021, 4, 1));
        doReturn(to).when(clock).millis();
        var from = TimeUtil.toMillis(LocalDate.of(2021, 3, 1));
        var actually = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/statistics/summary?from=" + from + "&uid=1");
        var expected = getStringResource("/testSummaryMultiBrand/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, when(IGNORING_ARRAY_ORDER));
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testCategoryBanners/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testCategoryBanners/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testCategoryBanners() {
        var actually = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/statistics/banners/categories?textForSearch=стулья&uid=1");
        var expected = getStringResource("/testCategoryBanners/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, when(IGNORING_ARRAY_ORDER));
    }


    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testListBannersStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testListBannersStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testListBannersStats() {
        var to = TimeUtil.toMillis(LocalDate.of(2021, 7, 1));
        doReturn(to).when(clock).millis();
        var from = TimeUtil.toMillis(LocalDate.of(2021, 2, 1));
        var actually = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/statistics/banners/list" +
                        "?to=" + to + "&from=" + from + "&uid=1&page=2&pageSize=3");
        var expected = getStringResource("/testListBannersStats/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testListMultiBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testListMultiBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testListMultiBrand() {
        var to = TimeUtil.toMillis(LocalDate.of(2021, 7, 1));
        doReturn(to).when(clock).millis();
        var from = TimeUtil.toMillis(LocalDate.of(2021, 2, 1));
        var actually = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/statistics/banners/list" +
                        "?to=" + to + "&from=" + from + "&uid=1&page=1&pageSize=30");
        var expected = getStringResource("/testListMultiBrand/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testListBannersStats/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testListBannersStats/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testListBannersStatsWithTextForSearch() {
        var to = TimeUtil.toMillis(LocalDate.of(2021, 7, 1));
        doReturn(to).when(clock).millis();
        var from = TimeUtil.toMillis(LocalDate.of(2021, 2, 1));
        var actually = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/statistics/banners/list" +
                        "?to=" + to + "&from=" + from + "&uid=1&textForSearch=стул");
        var expected = getStringResource("/testListBannersStatsWithTextForSearch/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testGetChartsAllMetricsDay/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testGetChartsAllMetricsDay/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetChartsAllMetricsDay() {
        doReturn(TimeUtil.toInstant(LocalDate.of(2021, 4, 20))).when(clock).instant();
        var context = MarketModelBidsBannersContext.builder()
                .withVendorId(321)
                .withFrom(LocalDate.of(2021, 1, 1))
                .withScaleType(DAY)
                .withMetrics(new TreeSet<>())
                .withUid(1L)
                .build();

        var url = "/vendors/321/modelbids/statistics/banners/charts?";
        assertHttpGet(
                url + getRequestParamString(context),
                "/testGetChartsAllMetricsDay/expected.json"
        );

    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/getChartsMultiBrand/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/getChartsMultiBrand/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void getChartsMultiBrand() {
        doReturn(TimeUtil.toInstant(LocalDate.of(2021, 4, 20))).when(clock).instant();
        var context = MarketModelBidsBannersContext.builder()
                .withVendorId(321)
                .withFrom(LocalDate.of(2021, 1, 1))
                .withScaleType(MONTH)
                .withMetrics(new TreeSet<>())
                .withUid(1L)
                .build();

        var url = "/vendors/321/modelbids/statistics/banners/charts?";
        assertHttpGet(
                url + getRequestParamString(context),
                "/getChartsMultiBrand/expected.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testGetChartsAllMetricsDay/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testGetChartsAllMetricsDay/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetChartsWeekWithBannersAndCategoriesShows() {
        doReturn(TimeUtil.toInstant(LocalDate.of(2021, 4, 20))).when(clock).instant();
        var context = MarketModelBidsBannersContext.builder()
                .withVendorId(321)
                .withFrom(LocalDate.of(2021, 3, 1))
                .withScaleType(WEEK)
                .withMetrics(new TreeSet<>(Set.of(SHOWS)))
                .withCategoryIds(Set.of(2L))
                .withBannerIds(Set.of(1L))
                .withUid(1L)
                .build();

        var url = "/vendors/321/modelbids/statistics/banners/charts?";
        assertHttpGet(
                url + getRequestParamString(context),
                "/testGetChartsWeekWithBannersAndCategoriesShows/expected.json"
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testGetChartsAllMetricsDay/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidsBannersStatsControllerTest/testGetChartsAllMetricsDay/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetAllMonth() {
        doReturn(TimeUtil.toInstant(LocalDate.of(2021, 4, 20))).when(clock).instant();
        var context = MarketModelBidsBannersContext.builder()
                .withVendorId(321)
                .withFrom(LocalDate.of(2021, 1, 1))
                .withScaleType(MONTH)
                .withMetrics(new TreeSet<>())
                .withUid(1L)
                .build();

        var url = "/vendors/321/modelbids/statistics/banners/charts?";
        assertHttpGet(
                url + getRequestParamString(context),
                "/testGetAllMonth/expected.json"
        );
    }


    private static String getRequestParamString(MarketModelBidsBannersContext context) {
        var params = new StringBuilder();
        params.append("vendorId=").append(context.getVendorId());
        for (var categoryId : context.getCategoryIds()) {
            params.append("&categoryIds=").append(categoryId);
        }
        for (var bannerId : context.getBannerIds()) {
            params.append("&bannerIds=").append(bannerId);
        }
        for (var metric : context.getMetrics()) {
            params.append("&metrics=").append(metric.name());
        }
        for (var platform : context.getPlatforms()) {
            params.append("&platforms=").append(platform.name());
        }
        params.append("&scaleType=").append(context.getScaleType());
        params.append("&from=").append(TimeUtil.toMillis(context.getFrom()));
        if (context.getTo() != null) {
            params.append("&to=").append(TimeUtil.toMillis(context.getTo()));
        }
        params.append("&uid=").append(context.getUid());
        return params.toString();
    }

}
