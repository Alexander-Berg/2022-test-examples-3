package ru.yandex.market.vendor.controllers;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

class RecommendedBusinessControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RecommendedBusinessControllerFunctionalTest/testGetAllRecommendedBusiness/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RecommendedBusinessControllerFunctionalTest/testGetAllRecommendedBusiness/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testGetAllRecommendedBusiness() {
        String response = FunctionalTestHelper.get(resourceUrl("/vendors/101/business/recommended?uid=1"));
        String expected = getStringResource("/testGetAllRecommendedBusiness/expected.json");

        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RecommendedBusinessControllerFunctionalTest/testPutAllRecommendedBusiness/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/RecommendedBusinessControllerFunctionalTest/testPutAllRecommendedBusiness/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/RecommendedBusinessControllerFunctionalTest/testPutAllRecommendedBusiness/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @Test
    void testPutAllRecommendedBusiness() {
        String response = FunctionalTestHelper.put(
                resourceUrl("/vendors/101/business/recommended?uid=1"),
                getStringResource("/testPutAllRecommendedBusiness/requestBody.json")
        );
        String expected = getStringResource("/testPutAllRecommendedBusiness/expected.json");

        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

}
