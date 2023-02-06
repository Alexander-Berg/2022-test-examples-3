package ru.yandex.market.vendor.controllers;

import java.time.Clock;
import java.time.LocalDateTime;

import net.javacrumbs.jsonunit.JsonAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.cs.billing.util.TimeUtil;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.vendor.AbstractVendorPartnerFunctionalTest;
import ru.yandex.market.vendor.util.FunctionalTestHelper;

import static net.javacrumbs.jsonunit.JsonAssert.when;
import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/before.vendors.csv",
        dataSource = "vendorDataSource"
)
@DbUnitDataSet(
        before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/before.cs_billing.csv",
        dataSource = "csBillingDataSource"
)
public class AsyncReportsControllerFunctionalTest extends AbstractVendorPartnerFunctionalTest {

    private final Clock clock;

    @Autowired
    public AsyncReportsControllerFunctionalTest(Clock clock) {
        this.clock = clock;
    }

    @BeforeEach
    void setUp() {
        // to make ReportWorkers feel better
        doReturn(TimeUtil.toInstant(LocalDateTime.now())).when(clock).instant();
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testGetTaskById/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testGetTaskById/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetTaskById() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/asyncReports/1?uid=100500");

        String expected = getStringResource("/testGetTaskById/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testGetAllTasks/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testGetAllTasks/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetAllTasks() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/asyncReports?uid=100500");

        String expected = getStringResource("/testGetAllTasks/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testGetAllTasksByType/before.vendors.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testGetAllTasksByType/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testGetAllTasksByType() {
        String response = FunctionalTestHelper.get(baseUrl + "/vendors/321/asyncReports?uid=100500" +
                "&reportType=MODELBIDS_STATS");

        String expected = getStringResource("/testGetAllTasksByType/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }

    @Test
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testDeleteTaskById/before.vendors.csv",
            after = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testDeleteTaskById/after.csv",
            dataSource = "vendorDataSource"
    )
    @DbUnitDataSet(
            before = "/ru/yandex/market/vendor/controllers/AsyncReportsControllerFunctionalTest/testDeleteTaskById/before.cs_billing.csv",
            dataSource = "csBillingDataSource"
    )
    void testDeleteTaskById() {
        String response = FunctionalTestHelper.delete(baseUrl + "/vendors/321/asyncReports/1?uid=100500");

        String expected = getStringResource("/testDeleteTaskById/expected.json");
        JsonAssert.assertJsonEquals(expected, response, when(IGNORING_ARRAY_ORDER));
    }
}
