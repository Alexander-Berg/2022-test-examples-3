package ru.yandex.market.partner.mvc.controller.supplier.promo;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.common.util.application.EnvironmentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.environment.TestEnvironmentService;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static java.nio.charset.StandardCharsets.UTF_8;
import static ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil.getResult;

@DbUnitDataSet(before = "SupplierPromosControllerFunctionalTest.before.csv")
class SupplierPromosControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private TestEnvironmentService environmentService;

    @BeforeEach
    void before() {
        environmentService.setEnvironmentType(EnvironmentType.PRODUCTION);
    }

    @AfterEach
    void after() {
        System.clearProperty("environment");
    }

    @Test
    void getPromoOrdersInfoByIdTest() throws IOException {
        ResponseEntity<String> response =
                getPromoOrdersInfoById(1001, "{\"ids\": [\"1_TGDYEKD\", \"1_HDNR678DJD5H\", \"1_FAKE\"]}");
        String expected = IOUtils.toString(
                getClass().getResourceAsStream("getPromoOrdersInfoByIdTest_response.json"),
                UTF_8
        );
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    @Test
    @DbUnitDataSet(before = "getPromoValidationsInfoByIdTest.before.csv")
    void getPromoValidationsInfoByIdTest() throws IOException {
        ResponseEntity<String> response =
                getPromoValidationsInfoById(1001, "{\"ids\": [\"1_TGDYEKD\", \"#4211\", \"#1111\", \"1_FAKE\"]}");
        System.out.println(response);
        String expected = IOUtils.toString(
                getClass().getResourceAsStream("getPromoValidationsInfoByIdTest_response.json"),
                UTF_8
        );
        JSONAssert.assertEquals(expected, getResult(response), false);
    }

    private ResponseEntity<String> getPromoOrdersInfoById(
            long campaignId,
            Object body
    ) {
        return FunctionalTestHelper.post(
                baseUrl + "/supplier/promos/orders?campaign_id=" + campaignId,
                body
        );
    }

    private ResponseEntity<String> getPromoValidationsInfoById(
            long campaignId,
            Object body
    ) {
        return FunctionalTestHelper.post(
                baseUrl + "/supplier/promos/last-validations?campaign_id=" + campaignId,
                body
        );
    }
}
