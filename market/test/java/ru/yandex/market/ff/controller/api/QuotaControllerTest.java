package ru.yandex.market.ff.controller.api;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.enums.DailyLimitsType;
import ru.yandex.market.ff.util.FileContentUtils;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.util.FileContentUtils.getFileContent;

public class QuotaControllerTest extends MvcIntegrationTest {

    @Test
    @DatabaseSetup("classpath:controller/quota/get/success/before.xml")
    void getSuccess() throws Exception {
        String response = FileContentUtils.getFileContent("controller/quota/get/success/response.json");
        mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "MOVEMENT_SUPPLY")
                .param("warehouses", "300", "400", "500")
                .param("supplierType", "FIRST_PARTY")
                .param("dates", "2020-11-11", "2020-11-12")
                .param("exceptBookings", "101", "102")
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/success/before.xml")
    void getSuccessv2() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/get/success/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/get/success/response.json");
        mockMvc.perform(
            post("/quota/v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isOk())
            .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/success-negative/before.xml")
    void getSuccessWhenNegativeValues() throws Exception {
        String response = FileContentUtils.getFileContent("controller/quota/get/success-negative/response.json");
        mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "MOVEMENT_SUPPLY")
                .param("warehouses", "300", "400", "500")
                .param("supplierType", "FIRST_PARTY")
                .param("dates", "2020-11-11", "2020-11-12")
                .param("exceptBookings", "101", "102")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/3p-success/before.xml")
    void get3pSuccess() throws Exception {
        String response = FileContentUtils.getFileContent("controller/quota/get/3p-success/response.json");
        mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "MOVEMENT_SUPPLY")
                .param("warehouses", "300", "400", "500")
                .param("supplierType", "THIRD_PARTY")
                .param("dates", "2020-11-11", "2020-11-12")
                .param("exceptBookings", "101", "102")
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/ignore-items/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/get/ignore-items/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void ignoreItemsQuotaForXDockSuccess() throws Exception {
        String response = FileContentUtils.getFileContent("controller/quota/get/ignore-items/response.json");
        MvcResult mvcResult = mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "XDOCK_TRANSPORT_SUPPLY")
                .param("warehouses", "300")
                .param("supplierType", "THIRD_PARTY")
                .param("dates", "2020-11-11", "2020-11-12")
        )
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        JSONAssert.assertEquals(response, mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/with-destinations/success/before.xml")
    void getSuccessWithDifferentDestinations() throws Exception {
        String response =
                FileContentUtils.getFileContent("controller/quota/get/with-destinations/success/response.json");
        MvcResult mvcResult = mockMvc.perform(get("/quota/with-destinations")
                .param("dailyLimitsType", "SUPPLY")
                .param("warehouses", "172")
                .param("destinationWarehouseIds", "147", "171")
                .param("supplierType", "FIRST_PARTY")
                .param("dates", "2020-11-11", "2020-11-12")
                .param("exceptBookings", "10", "11", "12")
        )
                .andExpect(status().isOk())
                .andReturn();

        JSONAssert.assertEquals(response, mvcResult.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/success/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take/success/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/success/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take-zero/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-zero/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeZeroSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-zero/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-zero/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/3p-success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/3p-success/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void take3pSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take/3p-success/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/3p-success/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/retry/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/retry/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeRetry() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take/retry/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/retry/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/not-enough-quota/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/not-enough-quota/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeNotEnoughQuota() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take/not-enough-quota/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/not-enough-quota/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isConflict())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/x-dock-ignore-items/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/x-dock-ignore-items/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void ignoreItemsQuotaForXDockAndTakeQuota() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take/x-dock-ignore-items/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/x-dock-ignore-items/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/decrease/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/decrease/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void decreaseQuotaSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/decrease/request-success.json");
        String response = FileContentUtils.getFileContent("controller/quota/decrease/response.json");
        mockMvc.perform(
                post("/quota/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/decrease/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/decrease/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void decreaseQuotaFailByItems() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/decrease/request-fail-items.json");
        mockMvc.perform(
                post("/quota/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("{\"message\":\"New items/pallets count for bookingId = 55" +
                        " can't be greater than existing: old items = 100, new items = 101;" +
                        " old pallets = 10, new pallets = 10\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/decrease/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/decrease/before.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void decreaseQuotaFailByPallets() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/decrease/request-fail-pallets.json");
        mockMvc.perform(
                post("/quota/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().is5xxServerError())
                .andExpect(content().string("{\"message\":\"New items/pallets count for bookingId = 55" +
                        " can't be greater than existing: old items = 100, new items = 95;" +
                        " old pallets = 10, new pallets = 12\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/update/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/update/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void updateQuotaSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/update/request-increase-success.json");
        mockMvc.perform(
                post("/quota/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/update-x-dock/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/update-x-dock/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void updateQuotaForXDockWhenNoItemsSuccess() throws Exception {
        String request = FileContentUtils
                .getFileContent("controller/quota/update-x-dock/request-increase-success.json");
        mockMvc.perform(
                post("/quota/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/update-booking-id/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/update-booking-id/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void updateBookingIdSuccess() throws Exception {
        String response = FileContentUtils.getFileContent("controller/quota/update-booking-id/response.json");
        mockMvc.perform(
                post("/quota/updateBookingId/55/100")
                        .contentType(MediaType.APPLICATION_JSON)

        )
                .andExpect(content().json(response))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/release/success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/release/success/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void releaseSuccess() throws Exception {
        mockMvc.perform(
                delete("/quota")
                        .param("bookingId", "101", "102")
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/release/success/after.xml")
    void releaseEmpty() throws Exception {
        mockMvc.perform(
                delete("/quota")
                        .param("bookingId", "101", "102")
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/interval/before-get-quota-info.xml")
    @ExpectedDatabase(
            value = "classpath:controller/quota/get/interval/before-get-quota-info.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getQuotaInfoWithEmptyTypes() throws Exception {
        mockMvc.perform(get("/quota/100/2019-10-28/2019-10-30"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"message\":\"Required List parameter 'limitType' is not present\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/interval/before-get-quota-info.xml")
    @ExpectedDatabase(
            value = "classpath:controller/quota/get/interval/before-get-quota-info.xml",
            assertionMode = NON_STRICT_UNORDERED)
    public void getQuotaInfoForTwoTypes() throws Exception {
        mockMvc.perform(get("/quota/100/2019-10-28/2019-10-30")
                .param("limitType", DailyLimitsType.WITHDRAW.getId())
                .param("limitType", DailyLimitsType.MOVEMENT_SUPPLY.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/quota/get/interval/response-quota-info.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/bybookingid/before.xml")
    public void testFindByRequestId() throws Exception {
        mockMvc.perform(get("/quota/by-bookingid/1"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent("controller/quota/bybookingid/response.json")));
    }


    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/1/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/1/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeConsolidatedQuotasSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/1/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-many/1/response.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/7/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/7/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeConsolidatedQuotasWithDestinationIdSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/7/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-many/7/response.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/2/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/2/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeConsolidatedQuotasDeleteOldSuccess() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/2/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-many/2/response.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }


    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/3/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/3/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void takeConsolidatedQuotasDifferentDates() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/3/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-many/3/response.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }


    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/4/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/4/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeConsolidatedQuotasNoFreeQuota() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/4/request.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().is4xxClientError())
                .andExpect(content().json("{\"message\":\"Limits for FIRST_PARTY were exceeded by bookings [1,2]\"}"));


    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/5/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/5/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeConsolidatedQuotasStayTheSame() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/5/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-many/5/response.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take-many/6/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take-many/6/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void decreaseConsolidatedQuotas() throws Exception {
        String request = FileContentUtils.getFileContent("controller/quota/take-many/6/request.json");
        String response = FileContentUtils.getFileContent("controller/quota/take-many/6/response.json");
        mockMvc.perform(
                post("/quota/take-or-update-consolidated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/success-with-destination-service/before.xml")
    void getSuccessWithDestinationWarehouseId() throws Exception {
        String response = FileContentUtils.getFileContent("controller/quota/get/" +
                "success-with-destination-service/response.json");
        mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "SUPPLY")
                .param("warehouses", "300")
                .param("destinationWarehouseId", "172")
                .param("supplierType", "FIRST_PARTY")
                .param("dates", "2020-11-11", "2020-11-12", "2020-11-13", "2020-11-14")
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/success/after-destination-service-id.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeQuotaWithDestinationServiceIdSuccess() throws Exception {
        String request = FileContentUtils
                .getFileContent("controller/quota/take/success/request-with-destination-service-id.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/success/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/success/before-infinity-destination-service-id.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/success/after-infinity-destination-service-id.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeQuotaInfinityWithDestinationServiceIdSuccess() throws Exception {
        String request = FileContentUtils
                .getFileContent("controller/quota/take/success/request-with-destination-service-id.json");
        String response = FileContentUtils.getFileContent("controller/quota/take/success/response.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isOk())
                .andExpect(content().json(response));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/info/before.xml")
    public void getQuotaInfoForFulfilmentServiceType() throws Exception {
        mockMvc.perform(get("/quota/100/2019-10-27/2019-10-28")
                .param("limitType", DailyLimitsType.SUPPLY.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/quota/get/info/response.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/info/before.xml")
    public void getQuotaForDistributionServiceType() throws Exception {
        mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "SUPPLY")
                .param("warehouses", "100")
                .param("supplierType", "FIRST_PARTY")
                .param("dates", "2019-10-26", "2019-10-27", "2019-10-28")
                .param("destinationWarehouseId", "200"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/quota/get/destination-service-id/response.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/get/infinity-limit/before.xml")
    public void getQuotaWithInfinityLimits() throws Exception {
        mockMvc.perform(get("/quota")
                .param("dailyLimitsType", "SUPPLY")
                .param("warehouses", "100")
                .param("supplierType", "FIRST_PARTY")
                .param("dates", "2019-10-27", "2019-10-28")
                .param("destinationWarehouseId", "200"))
                .andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/quota/get/infinity-limit/response.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/quota/take/success/before.xml")
    @ExpectedDatabase(value = "classpath:controller/quota/take/quota_exceeded_exception/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    void takeQuotaWithDestinationServiceIdQuotaExceededException() throws Exception {
        String request = FileContentUtils
                .getFileContent("controller/quota/take/quota_exceeded_exception/request.json");
        mockMvc.perform(
                post("/quota")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isConflict())
                .andExpect(content().json("{\"message\": \"Limits for FIRST_PARTY were exceeded by booking 55\"}\n"));
    }
}
