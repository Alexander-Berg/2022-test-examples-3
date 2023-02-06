package ru.yandex.market.partner.outlet;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Unit тесты для {@link GetOutletInfosCountServantlet}.
 *
 * @author stani on 02.08.18.
 */
@DbUnitDataSet(before = "ShowOutletInfosServantletFunctionalTest.before.csv")
class GetOutletInfosCountServantletFunctionalTest extends FunctionalTest {

    @Test
    void testShopGetOutletInfosCount() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getOutletInfosCount?id={campaignId}&format=json", 101L);
        JsonTestUtil.assertEquals(response, this.getClass(), "/mvc/outlet/outlet_count_by_shop.json");
    }

    @Test
    void testSupplierGetOutletInfosCount() {
        ResponseEntity<String> response = FunctionalTestHelper.get(
                baseUrl + "/getOutletInfosCount?id={campaignId}&format=json", 101L);
        JsonTestUtil.assertEquals(response, this.getClass(), "/mvc/outlet/outlet_count_by_shop.json");
    }

}
