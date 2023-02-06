package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Month;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.Mockito.when;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
public class VendorProductPlacementControllerTest extends AbstractVendorPartnerFunctionalTest {
    @Autowired
    private Clock clock;

    @Test
    @DisplayName("Запрос статуса услуги без катофов")
    public void getPlacementWithoutCutoffs() {
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 0, 0)));

        String body = getStringResource("/getPlacementWithoutCutoffs/body.json");
        String expected = getStringResource("/getPlacementWithoutCutoffs/expected.json");

        String response = FunctionalTestHelper.put(baseUrl + "/vendors/101/recommended/placement?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Запрос статуса услуги с клиентским CLIENT катофом, и накладываение FINANCE катофа")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/getPlacementWithClientCutoff/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/getPlacementWithClientCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    public void getPlacementWithClientCutoff() {
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 0, 0)));

        String body = getStringResource("/getPlacementWithClientCutoff/body.json");
        String expected = getStringResource("/getPlacementWithClientCutoff/expected.json");

        String response = FunctionalTestHelper.put(baseUrl + "/vendors/101/recommended/placement?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Запрос на закрытие услуги с клиентским CLIENT и INACTIVE_SHOP_BID катофами, и накладываение FINANCE катофа")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/getSuspendPlacementWithClientCustomCutoff/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/getSuspendPlacementWithClientCustomCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    public void getSuspendPlacementWithClientCustomCutoff() {
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 0, 0)));

        String body = getStringResource("/getSuspendPlacementWithClientCustomCutoff/body.json");
        String expected = getStringResource("/getSuspendPlacementWithClientCustomCutoff/expected.json");
        String response = FunctionalTestHelper.put(baseUrl + "/vendors/101/recommended/placement?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Запрос статуса услуги с существующим FINANCE катофом")
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/getPlacementWithTwoFinanceCutoff/before.csv",
            after = "/ru/yandex/market/vendor/controllers/VendorProductPlacementControllerTest/getPlacementWithTwoFinanceCutoff/after.csv",
            dataSource = "csBillingDataSource"
    )
    public void getPlacementWithTwoFinanceCutoff() {
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 0, 0)));

        String body = getStringResource("/getPlacementWithTwoFinanceCutoff/body.json");
        String expected = getStringResource("/getPlacementWithTwoFinanceCutoff/expected.json");

        String response = FunctionalTestHelper.put(baseUrl + "/vendors/101/recommended/placement?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DisplayName("Изменение статуса услуги Платые отзывы")
    public void putPaidOpinionsPlacement() {
        when(clock.instant()).thenReturn(TimeUtil.toInstant(LocalDateTime.of(2020, Month.MARCH, 23, 0, 0)));

        String body = getStringResource("/putPaidOpinionsPlacement/body.json");
        String expected = getStringResource("/putPaidOpinionsPlacement/expected.json");

        String response = FunctionalTestHelper.put(baseUrl + "/vendors/101/paidOpinions/placement?uid=1", body);
        JsonAssert.assertJsonEquals(expected, response, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }

}
