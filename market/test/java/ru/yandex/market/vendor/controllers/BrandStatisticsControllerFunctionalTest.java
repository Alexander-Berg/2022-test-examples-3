package ru.yandex.market.vendor.controllers;

import java.time.Clock;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

class BrandStatisticsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final Clock clock;

    @Autowired
    public BrandStatisticsControllerFunctionalTest(Clock clock) {
        this.clock = clock;
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTop/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTop/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitAmountCurrentBrandInTop() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitAmountCurrentBrandInTop/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandNotInTopBrandChosen/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandNotInTopBrandChosen/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitAmountCurrentBrandNotInTopBrandChosen() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitAmountCurrentBrandNotInTopBrandChosen/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosen/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosen/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosen() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosen/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenHaveUnknownBrands/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenHaveUnknownBrands/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenHaveUnknownBrands() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenHaveUnknownBrands/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosen/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosen/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosen() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=PERCENT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosen/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosenYoY/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosenYoY/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosenYoY() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=true" +
                "&metric=CLICKS" +
                "&yUnit=PERCENT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitPercentCurrentBrandInTopBrandChosenYoY/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=true" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksWeekScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksWeekScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksWeekScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&scale=WEEK" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=true" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksWeekScaleUnitAbsoluteCurrentBrandInTopBrandChosenYoY/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenNoData/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenNoData/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenNoData() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/series" +
                "?uid=1" +
                "&from=1627765200000" +
                "&to=1628283600000" +
                "&scale=DAY" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testSeriesClicksDayScaleUnitAmountCurrentBrandInTopBrandChosenNoData/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieClicksUnitAmountCurrentBrandInTopBrandChosen/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieClicksUnitAmountCurrentBrandInTopBrandChosen/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPieClicksUnitAmountCurrentBrandInTopBrandChosen() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/pie" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testPieClicksUnitAmountCurrentBrandInTopBrandChosen/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieOrdersUnitPercentCurrentBrandInTopBrandChosenYoy/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieOrdersUnitPercentCurrentBrandInTopBrandChosenYoy/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPieOrdersUnitPercentCurrentBrandInTopBrandChosenYoy() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/pie" +
                "?uid=1" +
                "&from=1630443600000" +
                "&to=1630962000000" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=true" +
                "&metric=ORDERS" +
                "&yUnit=PERCENT"
        );

        String expected = getStringResource("/testPieOrdersUnitPercentCurrentBrandInTopBrandChosenYoy/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieClicksUnitAmountCurrentBrandInTopBrandChosenNoData/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieClicksUnitAmountCurrentBrandInTopBrandChosenNoData/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPieClicksUnitAmountCurrentBrandInTopBrandChosenNoData() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/pie" +
                "?uid=1" +
                "&from=1627765200000" +
                "&to=1628283600000" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=false" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testPieClicksUnitAmountCurrentBrandInTopBrandChosenNoData/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieClicksUnitAmountYoyCurrentBrandInTopBrandChosenNoData/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/BrandStatisticsControllerFunctionalTest/testPieClicksUnitAmountYoyCurrentBrandInTopBrandChosenNoData/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testPieClicksUnitAmountYoyCurrentBrandInTopBrandChosenNoData() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/brands/statistics/pie" +
                "?uid=1" +
                "&from=1627765200000" +
                "&to=1628283600000" +
                "&categoryId=666" +
                "&brandId=110326" +
                "&top5=true" +
                "&yoy=true" +
                "&metric=CLICKS" +
                "&yUnit=AMOUNT"
        );

        String expected = getStringResource("/testPieClicksUnitAmountYoyCurrentBrandInTopBrandChosenNoData/expected.json");
        JsonAssert.assertJsonEquals(expected, response);
    }
}
