package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

class MarketModelBidBannerControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;
    private final Clock clock;

    @Autowired
    MarketModelBidBannerControllerFunctionalTest(NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate,
                                                 Clock clock) {
        this.vendorNamedParameterJdbcTemplate = vendorNamedParameterJdbcTemplate;
        this.clock = clock;
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testCreateVendorOwnBanner/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testCreateVendorOwnBanner/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testCreateVendorOwnBanner/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateVendorOwnBanner() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 1, 20)));

        String actual = FunctionalTestHelper.post(
                baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                getStringResource("/testCreateVendorOwnBanner/requestBody.json")
        );
        String expected = getStringResource("/testCreateVendorOwnBanner/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testCreateVendorMarketBannerValidationError/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testCreateVendorMarketBannerValidationError/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testCreateVendorMarketBannerValidationError() {

        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 1, 20)));
        String packageName = "/testCreateVendorMarketBannerValidationError/";

        String overLength = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource(packageName + "requestBodyTitleOverLength.json")
                )).getResponseBodyAsString();

        String expectedOverLength = getStringResource(packageName + "expectedOverLength.json");
        JsonAssert.assertJsonEquals(expectedOverLength, overLength, JSON_ASSERT_CONFIG);

        String wrongEmail = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource(packageName + "requestBodyNotEmail.json")
                )).getResponseBodyAsString();

        String expectedWrongEmail = getStringResource(packageName + "expectedWrongEmail.json");
        JsonAssert.assertJsonEquals(expectedWrongEmail, wrongEmail, JSON_ASSERT_CONFIG);

        String wrongLink= Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource(packageName + "requestWrongLink.json")
                )).getResponseBodyAsString();

        String expectedWrongLink = getStringResource(packageName + "expectedWrongUrl.json");
        JsonAssert.assertJsonEquals(expectedWrongLink, wrongLink, JSON_ASSERT_CONFIG);

    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testGetBanner/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testGetBanner/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetBanner() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/321/modelbids/banner?uid=100500&bannerId=1");
        String expected = getStringResource("/testGetBanner/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testGetBanner/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testGetBanner/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testBannerNotFoundException() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(baseUrl + "/vendors/321/modelbids/banner?uid=100500&bannerId=2"))
                .getResponseBodyAsString();
        String expected = getStringResource("/testEntityNotFoundException/expected.json");
        JsonAssert.assertJsonEquals(expected, exception, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateVendorBanner/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateVendorBanner/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateVendorBanner/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testUpdateVendorBanner() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                getStringResource("/testUpdateVendorBanner/requestBody.json")
        );
        String expected = getStringResource("/testUpdateVendorBanner/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testEntityNotFoundException/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testEntityNotFoundException/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testEntityNotFoundException() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource("/testEntityNotFoundException/requestBody.json")
                )).getResponseBodyAsString();
        String expected = getStringResource("/testEntityNotFoundException/expected.json");
        JsonAssert.assertJsonEquals(expected, exception, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionNewToDraft/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionNewToDraft/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionNewToDraft/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testTransitionNewToDraft() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.post(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource("/testTransitionNewToDraft/requestBody.json")
                );
        String expected = getStringResource("/testTransitionNewToDraft/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testFindBannersByCategoryIdAndBid/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testFindBannersByCategoryIdAndBid/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testFindBannersByCategoryIdAndBid() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/banner/list?uid=100500" +
                        "&categoryId=4&bidFrom=10&bidTo=30");
        String expected = getStringResource("/testFindBannersByCategoryIdAndBid/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testFindBannersByNameAndState/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testFindBannersByNameAndState/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testFindBannersByNameAndState() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/banner/list?uid=100500" +
                        "&status=ACTIVE&bannerName=стулья");
        String expected = getStringResource("/testFindBannersByNameAndState/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testAllVendorBannersPagination/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testAllVendorBannersPagination/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testAllVendorBannersPagination() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/banner/list?uid=100500" +
                        "&page=2&pageSize=3");
        String expected = getStringResource("/testAllVendorBannersPagination/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testAllVendorBannersWithPriority/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testAllVendorBannersWithPriority/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testAllVendorBannersWithPriority() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/banner/list?uid=100500" +
                        "&page=1&pageSize=5&priorityBanners=8&priorityBanners=9&priorityBanners=10");
        String expected = getStringResource("/testAllVendorBannersWithPriority/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testGetCategories/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testGetCategories/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetCategories() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.get(
                baseUrl + "/vendors/321/modelbids/banner/categories?uid=100500" +
                        "&textForSearch=ст&page=1&pageSize=3");
        String expected = getStringResource("/testGetCategories/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationSuccess/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationSuccess/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationSuccess/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testTransitionAwaitingActivationToActiveValidationSuccess() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                getStringResource("/testTransitionAwaitingActivationToActiveValidationSuccess/requestBody.json")
        );
        String expected = getStringResource("/testTransitionAwaitingActivationToActiveValidationSuccess/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationFailure/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationFailure/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationFailure/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testTransitionAwaitingActivationToActiveValidationFailure() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource("/testTransitionAwaitingActivationToActiveValidationFailure/requestBody.json")
                )).getResponseBodyAsString();

        String expected = getStringResource("/testTransitionAwaitingActivationToActiveValidationFailure/expected.json");
        JsonAssert.assertJsonEquals(expected, exception, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationFailureHasActiveBanner/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationFailureHasActiveBanner/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testTransitionAwaitingActivationToActiveValidationFailureHasActiveBanner/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testTransitionAwaitingActivationToActiveValidationFailureHasActiveBanner() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource("/testTransitionAwaitingActivationToActiveValidationFailureHasActiveBanner/requestBody.json")
                )).getResponseBodyAsString();

        String expected = getStringResource("/testTransitionAwaitingActivationToActiveValidationFailureHasActiveBanner/expected.json");
        JsonAssert.assertJsonEquals(expected, exception, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateBannerStatus/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateBannerStatus/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateBannerStatus/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testUpdateBannerStatus() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));
        String actual = FunctionalTestHelper.put(
                baseUrl + "/vendors/321/modelbids/banner/1?uid=100500" +
                        "&newState=AWAITING_MODERATION", "{}");
        String expected = getStringResource("/testUpdateBannerStatus/expected.json");
        JsonAssert.assertJsonEquals(expected, actual, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateBannerStatusLifeCycleException/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateBannerStatusLifeCycleException/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testUpdateBannerStatusLifeCycleException/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testUpdateBannerStatusLifeCycleException() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String actually = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/vendors/321/modelbids/banner/1?uid=100500" +
                                "&newState=ACTIVE", "{}")
                ).getResponseBodyAsString();
        String expected = getStringResource("/testUpdateBannerStatusLifeCycleException/expected.json");
        JsonAssert.assertJsonEquals(expected, actually, JSON_ASSERT_CONFIG);
    }

    @Disabled
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testBidValidationException/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testBidValidationException/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testBidValidationException/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testBidValidationException() {
        Mockito.when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDate.of(2021, 2, 20)));

        String exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.put(
                        baseUrl + "/vendors/321/modelbids/banner?uid=100500",
                        getStringResource("/testBidValidationException/requestBody.json")
                )).getResponseBodyAsString();
        System.out.println(exception);
        String expected = getStringResource("/testBidValidationException/expected.json");
        JsonAssert.assertJsonEquals(expected, exception, JSON_ASSERT_CONFIG);
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testBannerCategoryRecommendation/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/MarketModelBidBannerControllerFunctionalTest/testBannerCategoryRecommendation/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testBannerCategoryRecommendation() {
        String actual = FunctionalTestHelper.get(baseUrl + "/vendors/321/modelbids/banner/categories/recommendation?uid=100500");
        String expected = getStringResource("/testBannerCategoryRecommendation/expected.json");
        JsonAssert.assertJsonEquals(expected, actual);
    }
}
