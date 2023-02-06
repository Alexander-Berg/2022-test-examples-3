package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static org.mockito.Mockito.when;

public class CutoffsControllerTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private Clock clock;

    @BeforeEach
    public void beforeEach() {
        when(clock.instant())
                .thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, 1, 10, 0, 0, 0)));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/getCutoffsTest/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/getCutoffsTest/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void getCutoffsTest() {
        var response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/cutoffs?uid=1");
        var expected = getStringResource("/getCutoffsTest/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/getSameTypeCutoffsTest/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/getSameTypeCutoffsTest/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DisplayName("Если есть открытые катоффы одного типа, возвращаем только первый")
    void getSameTypeCutoffsTest() {
        var response = FunctionalTestHelper.get(baseUrl + "/vendors/101/modelbids/cutoffs?uid=1");
        var expected = getStringResource("/getSameTypeCutoffsTest/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/postCloseCutoffsTest/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/postCloseCutoffsTest/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/postCloseCutoffsTest/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void postCloseCutoffsTest() {
        FunctionalTestHelper.post(baseUrl + "/vendors/101/recommended/cutoffs/close?uid=1&cutoffId=9");
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/postOpenCutoffsTest/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/postOpenCutoffsTest/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/postOpenCutoffsTest/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void postOpenCutoffsTest() {
        var response = FunctionalTestHelper.post(
                baseUrl + "/vendors/101/recommended/cutoffs/open?uid=1&cutoffType=CLIENT");
        var expected = getStringResource("/postOpenCutoffsTest/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @DisplayName("Пытаемся создать кастомный катофф с тегом test_tag")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testCreateCustomCutoffWithTag/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testCreateCustomCutoffWithTag/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testCreateCustomCutoffWithTag/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testCreateCustomCutoffWithTag() {
        var ex = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.post(baseUrl + "/vendors/101/recommended/cutoffs/open?uid=1&cutoffType=CUSTOM&cutoffTag=INACTIVE_SHOP_BID")
        );
        var expected = getStringResource("/testCreateCustomCutoffWithTag/expected.json");
        Assertions.assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        JsonAssert.assertJsonEquals(expected, ex.getResponseBodyAsString(), JSON_ASSERT_CONFIG);
    }

    @DisplayName("Создаем катоф с тегом CHANGE_TARIFF_LIMIT, временная возможность")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testCreateCustomCutoffWithTagChangeTariffLimit/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testCreateCustomCutoffWithTagChangeTariffLimit/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testCreateCustomCutoffWithTagChangeTariffLimit/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testCreateCustomCutoffWithTagChangeTariffLimit() {
        var response = FunctionalTestHelper.post(
                baseUrl + "/vendors/101/recommended/cutoffs/open?uid=1&cutoffType=CUSTOM&cutoffTag=CHANGE_TARIFF_LIMIT");
        var expected = getStringResource("/testCreateCustomCutoffWithTagChangeTariffLimit/expected.json");
        JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }

    @DisplayName("Уже есть INACTIVE_SHOP_BID катофф, не накладываем снова")
    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testDontCreateCutoffInactiveShopBidCutoff/before.cs_billing.csv",
            after = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testDontCreateCutoffInactiveShopBidCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/CutoffsControllerTest/testDontCreateCutoffInactiveShopBidCutoff/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    void testDontCreateCutoffInactiveShopBidCutoff() {
        var response = FunctionalTestHelper.post(
                baseUrl + "/vendors/101/recommended/cutoffs/open?uid=1&cutoffType=CLIENT");
        var expected = getStringResource("/testDontCreateCutoffInactiveShopBidCutoff/expected.json");
            JsonAssert.assertJsonEquals(expected, response, JSON_ASSERT_CONFIG);
    }
}
