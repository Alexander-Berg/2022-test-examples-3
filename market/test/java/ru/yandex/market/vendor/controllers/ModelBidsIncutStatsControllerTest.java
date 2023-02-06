package ru.yandex.market.vendor.controllers;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;

/**
 * Тесты ручки асинхронной генерации отчетов по врезкам
 * <p>
 * {@link ModelBidsIncutStatsController}
 */
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ModelBidsIncutStatsControllerTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/ModelBidsIncutStatsControllerTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class ModelBidsIncutStatsControllerTest extends AbstractVendorPartnerFunctionalTest {

    @Test
    @DisplayName("Проверка ручки со стабом")
    void testGenerateAsyncTask() {
        String filter = getStringResource("/filter.json");
        String response = FunctionalTestHelper.post(baseUrl +
                        "/vendors/321/modelbids/incuts/statistics/asyncDownload" +
                        "?uid=100500" +
                        "&from=1522530000000" +
                        "&scale=DAY" +
                        "&platform=DESKTOP&platform=TOUCH&platform=APPLICATION" +
                        "&metrics=SHOWS",
                filter);
        String expected = getStringResource("/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }
}
