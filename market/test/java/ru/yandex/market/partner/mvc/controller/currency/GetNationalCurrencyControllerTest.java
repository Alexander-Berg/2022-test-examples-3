package ru.yandex.market.partner.mvc.controller.currency;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * @author stani on 03.08.18.
 */
@DbUnitDataSet(before = "GetNationalCurrencyControllerTest.before.csv")
class GetNationalCurrencyControllerTest extends FunctionalTest {

    @Test
    void testShopGetNationalCurrency() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getNationalCurrency?id={campaignId}&format=json", 101L);
        JsonTestUtil.assertEquals(response, "[\"RUR\"]");
    }

    @Test
    void testSupplierGetNationalCurrency() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getNationalCurrency?id={campaignId}&format=json", 201L);
        JsonTestUtil.assertEquals(response, "[\"UAH\"]");
    }
}
