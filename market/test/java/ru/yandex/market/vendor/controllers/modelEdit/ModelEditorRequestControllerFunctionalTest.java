package ru.yandex.market.vendor.controllers.modelEdit;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

@Disabled
@DbUnitDataSet(before = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/before.csv")
class ModelEditorRequestControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/testPutResetAutorejectedTicketsForVendor/before.csv",
            after = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/testPutResetAutorejectedTicketsForVendor/after.csv"
    )
    void testPutResetAutorejectedTicketsForVendor() {
        int vendorId = 401;
        String string = FunctionalTestHelper.put(baseUrl + "/vendors/" + vendorId + "/modelEdit/resetTickets?uid=100500&requestId=187784", null);
        String resource = getStringResource("/testPutResetAutorejectedTicketsForVendor/response.json");
        JsonAssert.assertJsonEquals(resource, string, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/testPutResetAutorejectedTicketsForAgencies/before.csv",
            after = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/testPutResetAutorejectedTicketsForAgencies/after.csv"
    )
    void testPutResetAutorejectedTicketsForAgencies() {
        int agencyId = 400;
        String string = FunctionalTestHelper.put(baseUrl + "/agencies/" + agencyId + "/modelEdit/resetTickets?uid=100500&requestId=187781", null);
        String resource = getStringResource("/testPutResetAutorejectedTicketsForAgencies/response.json");
        JsonAssert.assertJsonEquals(resource, string, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/testPutResetAutorejectedTicketsForShops/before.csv",
            after = "/ru/yandex/market/vendor/controllers/modelEdit/ModelEditorRequestControllerFunctionalTest/testPutResetAutorejectedTicketsForShops/after.csv"
    )
    void testPutResetAutorejectedTicketsForShops() {
        int shopId = 400;
        String string = FunctionalTestHelper.put(baseUrl + "/shops/" + shopId + "/modelEdit/resetTickets?uid=100500&requestId=187781", null);
        String resource = getStringResource("/testPutResetAutorejectedTicketsForShops/response.json");
        JsonAssert.assertJsonEquals(resource, string, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
