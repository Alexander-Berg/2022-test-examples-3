package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.client.dto.RequestAcceptDTO;
import ru.yandex.market.ff.client.dto.RequestRejectDTO;
import ru.yandex.market.ff.configuration.DateTimeTestConfig;
import ru.yandex.market.ff.controller.util.MockParametersHelper;
import ru.yandex.market.ff.dbqueue.producer.SendMbiNotificationQueueProducer;
import ru.yandex.market.ff.model.dbqueue.SendMbiNotificationPayload;
import ru.yandex.market.ff.service.EnvironmentParamService;
import ru.yandex.market.ff.service.LmsClientCachingService;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemFreeze;
import ru.yandex.market.fulfillment.stockstorage.client.entity.enums.SSStockType;
import ru.yandex.market.logistic.api.utils.TimeZoneUtil;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotRequest;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsForDayResponse;
import ru.yandex.market.logistics.calendaring.client.dto.FreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeIntervalReason;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeSlotDTO;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeSlotGateDTO;
import ru.yandex.market.logistics.calendaring.client.dto.NotFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.TimeSlotResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.WarehouseNotFreeSlotsResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType;
import ru.yandex.market.logistics.calendaring.client.dto.enums.NotFreeIntervalReasonType;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;
import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.ff.controller.util.MockParametersHelper.mockGatesSchedules;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.SUPPLY_ACCEPTED_BY_SERVICE;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.SUPPLY_CANCELLATION_REJECTED;
import static ru.yandex.market.ff.service.util.MbiNotificationTypes.WITHDRAW_CANCELLATION_REJECTED;

/**
 * Функциональный тест для {@link RequestController}.
 *
 * @author avetokhin 18/09/17.
 */
class RequestControllerTest extends MvcIntegrationTest {

    public static final LocalDate FIXED_SUPPLY_DATE = LocalDateTime.ofInstant(
            DateTimeTestConfig.FIXED_SUPPLY_FROM, TimeZoneUtil.DEFAULT_OFFSET).toLocalDate();
    private static final long VALID_REQ_ID = 1;
    private static final long REQUEST_ID_WITHOUT_SLOT = 5;
    private static final long NOT_EXISTED_REQ_ID = 66;
    private static final long RETURN_REQ_ID = 4;
    private static final long WITHDRAW_CANCELLED_REQ_ID = 6;
    private static final long WITHDRAW_CALENDARING_CANCELLED_REQ_ID = 666;
    private static final long SUPPLY_CANCELLED_REQ_ID = 66;
    private static final long XDOCK_SUPPLY_CANCELLED_REQ_ID = 77;
    private static final long SUPPLY_CALENDARING_CANCELLED_REQ_ID = 6666;
    private static final long VALID_SUPPLIER_ID = 1;
    private static final long VALID_TRANSFER_REQ_ID = 4;
    private static final long ALIEN_TRANSFER_REQ_ID = 12;

    private static final String REQUEST_NOT_FOUND_ERROR =
            "{\"message\":\"Failed to find [REQUEST] with id [66]\",\"resourceType\":\"REQUEST\"," +
                    "\"identifier\":\"66\"}";

    private static final String ALIEN_TRANSFER_NOT_FOUND_ERROR =
            "{\"message\":\"Failed to find [REQUEST] with id [12]\",\"resourceType\":\"REQUEST\"," +
                    "\"identifier\":\"12\"}";

    private static final String ALREADY_HAD_CANCELLATION_RESULT = "{\"message\":\"Could not cancel request after "
            + "CANCELLATION_REJECTED status\",\"type\":\"INCONSISTENT_REQUEST_MODIFICATION\"}";

    private static final String REQUEST_SLOT = "{\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}";

    @Autowired
    private FulfillmentClient fulfillmentClient;

    @Autowired
    private LmsClientCachingService lmsClientCachingService;

    @Autowired
    private SendMbiNotificationQueueProducer sendMbiNotificationQueueProducer;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @Autowired
    private EnvironmentParamService environmentParamService;

    @AfterEach
    public void initMocks() {
        lmsClientCachingService.invalidateCache();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    void checkRequestCancellationAllowedForCreatedRequest() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/suppliers/1/requests/1/cancel/is-allowed")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-cancellation-allowed-supply-response.json", mvcResult, 1, "true");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    void checkRequestCancellationAllowedForWaitingForConfirmationSupply() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/suppliers/1/requests/77/cancel/is-allowed")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-cancellation-allowed-supply-response.json", mvcResult, 77, "true");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    void checkRequestCancellationNotAllowedForWaitingForSupplyInProgress() throws Exception {
        final MvcResult mvcResult = mockMvc.perform(
                get("/suppliers/1/requests/55/cancel/is-allowed")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-cancellation-allowed-supply-response.json", mvcResult, 55, "false");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-utilization-outbound.xml")
    void checkRequestCancellationAllowedForUtilizationOutbound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/suppliers/1/requests/88/cancel/is-allowed")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertJsonResponseCorrect("check-cancellation-allowed-supply-response.json", mvcResult, 88, "false");
    }

    ////
    //// ТЕСТЫ НА REJECT
    ////


    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-reject-supply.xml", assertionMode = NON_STRICT)
    void rejectSuccessfully() throws Exception {
        performReject(VALID_REQ_ID)
                .andExpect(status().isOk());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any(), anyLong());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-reject-successfully-with-attempt.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-reject-successfully-with-attempt.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testRejectSuccessfullyWithAttempt() throws Exception {
        performReject(VALID_REQ_ID)
                .andExpect(status().isOk());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any(), anyLong());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-reject-return.xml", assertionMode = NON_STRICT)
    void rejectSuccessfullyReturn() throws Exception {
        performReject(RETURN_REQ_ID)
                .andExpect(status().isOk());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any(), anyLong());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-reject-withdraw-cancelled.xml",
            assertionMode = NON_STRICT)
    void rejectSuccessfullyWithdrawWhenCancelled() throws Exception {
        performReject(WITHDRAW_CANCELLED_REQ_ID)
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any(), anyLong());
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-accept-reject-utilization-outbound.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-reject-utilization-outbound.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectSuccessfullyUtilizationOutbound() throws Exception {
        performReject(88L).andExpect(status().isBadRequest());
        performReject(8L).andExpect(status().isOk());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any(), anyLong());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-acceptance.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-acceptance.xml", assertionMode = NON_STRICT)
    void rejectNotFound() throws Exception {
        final MvcResult mvcResult = performReject(NOT_EXISTED_REQ_ID)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andDo(print())
                .andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo(REQUEST_NOT_FOUND_ERROR));
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    ////
    //// ТЕСТЫ НА ПОИСК
    ////

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequests() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("requestDateFrom", "2016-01-01")
                        .param("requestDateTo", "2016-12-06")
                        .param("creationDateFrom", "2016-01-01")
                        .param("types", "1")
                        .param("statuses", "2")
                        .param("shopIds", "1", "2")
                        .param("page", "1")
                        .param("size", "2")
                        .param("sort", "comment,desc")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/search_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-is-registry-field.xml")
    void findRequestsIsRegistryFieldTest() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("types", "0", "1")
                        .param("page", "0")
                        .param("size", "3")
                        .param("sort", "comment,desc")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/search_result-is-registry-field.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findByStatusesRequests() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("statuses", "3", "4")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "id,asc")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/find_by_statuses_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsWithDefects() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("hasDefects", "1")
        ).andDo(print()).andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_has_defects.json");
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-api/requests.xml"),
            @DatabaseSetup("classpath:controller/request-api/zeroify-defect-counts.xml.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsWithDefectsZeroify() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("hasDefects", "1")
        ).andDo(print()).andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_has_defects_zeroified.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsWithSurplus() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("hasSurplus", "true")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_has_surplus.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsWithShortage() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("hasShortage", "true")
        ).andDo(print()).andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_has_shortage.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsWithAnomaly() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("hasAnomaly", "true")
        ).andDo(print()).andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_has_anomaly.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsWithEmptyResults() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("hasShortage", "true")
                        .param("hasSurplus", "true")
                        .param("hasDefects", "true")
                        .param("page", "2")
                        .param("requestIds", "")
        ).andDo(print()).andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_empty_results.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsByStock() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("stockType", "2")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/find_by_stock.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsByService() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests")
                        .param("serviceIds", "101")
                        .param("serviceIds", "102")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/find_by_service.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findRequestsByDetailsLoaded() throws Exception {
        mockMvc.perform(get("/requests").param("detailsLoaded", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("find_by_details_loaded.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findAllRequests() throws Exception {
        mockMvc.perform(get("/requests")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("find_all.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-with-type-not-from-enum.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-with-type-not-from-enum.xml",
            assertionMode = NON_STRICT)
    void findAllRequestsWithTypeNotFromEnum() throws Exception {
        mockMvc.perform(get("/requests")
                .param("types", "1007"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("find_with_type_not_from_enum.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/yard-requests.xml")
    void findAllYardRequests() throws Exception {

        mockMvc.perform(get("/requests/yard-filter")
                .param("types", String.join(",", Arrays.asList("0", "1", "4", "8")))
                .param("statuses", String.join(",", Arrays.asList("2", "3", "4", "240")))
                .param("requestDateFrom", "2016-10-10")
                .param("hasUTDDocuments", "true")
                .param("vetis", "true")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("find_yard_requests.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-with-different-bookings.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-with-different-bookings.xml",
            assertionMode = NON_STRICT)
    void findRequestsWithPartiallyCsBookingsAndPartiallyFfwfBookings() throws Exception {
        when(csClient.getBookingsByIdsV2(Set.of(200L), BookingStatus.ACTIVE))
                .thenReturn(new BookingListResponseV2(List.of(new BookingResponseV2(
                        200,
                        "FFWF",
                        "1",
                        null,
                        10,
                        ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                        ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                        BookingStatus.ACTIVE,
                        LocalDateTime.of(2021, 1, 1, 9, 0, 0), 100L
                ))));
        mockMvc.perform(get("/requests")
                .param("requestIds", "1, 2"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("find_with_different_bookings.json")));
    }

    ////
    //// ТЕСТЫ НА ПОИСК С ОБОГАЩЕНИЕМ
    ////

    /**
     * Проверяем что считаются суммы по всем айтемам и по-заявочно
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/find-and-add-fields/1/db-state.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/find-and-add-fields/1/db-state.xml",
            assertionMode = NON_STRICT)
    void findAndCalculateItemsTotalPrice() throws Exception {
        mockMvc.perform(get("/requests")
                .param("calculateItemsTotalPrice", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/request-api/find-and-add-fields/1/response-expected.json")));
    }

    /**
     * Проверяем что при отсутствии айтемов ничего не падает
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/find-and-add-fields/2/db-state.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/find-and-add-fields/2/db-state.xml",
            assertionMode = NON_STRICT)
    void findAndCalculateItemsTotalPriceButNoItems() throws Exception {
        mockMvc.perform(get("/requests")
                .param("calculateItemsTotalPrice", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/request-api/find-and-add-fields/2/response-expected.json")));
    }

    /**
     * Проверяем что при отсутствии результатов поиска ничего не падает
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/find-and-add-fields/3/db-state.xml")
    void findAndCalculateNoRequest() throws Exception {
        mockMvc.perform(get("/requests")
                .param("calculateItemsTotalPrice", "true"))
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/request-api/find-and-add-fields/3/response-expected.json"), true));
    }

    /**
     * Проверяем что при запросе страницы размером больше 20-ти клиент получит 400-ку
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void findAndCalculateButBadRequest() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/requests")
                .param("calculateItemsTotalPrice", "true")
                .param("size", "21"))
                .andExpect(status().isBadRequest()).andReturn();
        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Page size should not exceed 20 if additional fields are required\"}"));
    }

    /**
     * Тесты на чтение
     */

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void getRequest() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/2")
        ).andDo(print())
                .andReturn();

        String expected = FileContentUtils.getFileContent(
                "controller/request-api/response/single_request_result.json");
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-api/requests.xml"),
            @DatabaseSetup("classpath:controller/request-api/zeroify-defect-counts.xml.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml",
            assertionMode = NON_STRICT)
    void getRequestZeroify() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/2")
        ).andDo(print())
                .andReturn();

        String expected = FileContentUtils.getFileContent("controller/request-api/response/" +
                "single_request_result.json");
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-api/requests-with-anomaly.xml"),
            @DatabaseSetup("classpath:controller/request-api/zeroify-defect-counts.xml.xml")
    })
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-with-anomaly.xml",
            assertionMode = NON_STRICT)
    void getRequestZeroifyAnomaly() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/2")
        ).andDo(print())
                .andReturn();

        String expected = FileContentUtils.getFileContent("controller/request-api/response/" +
                "single_request_result_zeroified.json");
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void getXDocRequest() throws Exception {
        mockMvc.perform(get("/requests/12"))
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("single_xdoc_request_result.json")));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void getRequestWithProblems() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests/6")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/single_request_with_problems_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void getRequestWithUploadingFinishDate() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/4")
        ).andDo(print())
                .andReturn();

        String expected = FileContentUtils.getFileContent(
                "controller/request-api/response/single_request_with_uploading_finish_date_result.json");
        JSONAssert.assertEquals(expected, result.getResponse().getContentAsString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests.xml", assertionMode = NON_STRICT)
    void getWithdrawRequest() throws Exception {
        final MvcResult result = mockMvc.perform(
                get("/requests/10")
        ).andDo(print())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/withdraw_request_result.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-with-different-bookings.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-with-different-bookings.xml",
            assertionMode = NON_STRICT)
    void getRequestWithCsBooking() throws Exception {
        when(csClient.getSlotByExternalIdentifiers(Set.of("1"), "FFWF-test", null))
                .thenReturn(new BookingListResponse(List.of(new BookingResponse(
                                200,
                                "FFWF",
                                "1",
                                null,
                                10,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 11, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 12, 0), ZoneId.of("+04:00")),
                                BookingStatus.ACTIVE,
                                LocalDateTime.of(2021, 1, 1, 9, 0, 0),
                                100L
                        ),
                        new BookingResponse(
                                201,
                                "FFWF",
                                "1",
                                null,
                                11,
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 15, 0), ZoneId.of("+04:00")),
                                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 16, 0), ZoneId.of("+04:00")),
                                BookingStatus.CANCELLED,
                                LocalDateTime.of(2021, 1, 1, 8, 0, 0),
                                100L
                        ))));
        MvcResult result = mockMvc.perform(
                get("/requests/1")
        ).andExpect(status().isOk())
                .andReturn();

        assertResponseEquals(result, "controller/request-api/response/request-with-cs-booking.json");
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/requests-with-type-not-from-enum.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/requests-with-type-not-from-enum.xml",
            assertionMode = NON_STRICT)
    void getRequestWithTypeNotFromEnum() throws Exception {
        MvcResult result = mockMvc.perform(
                get("/requests/2")
        ).andReturn();

        assertResponseEquals(result, "controller/request-api/response/request_with_type_not_from_enum.json");
    }

    ////
    //// ТЕСТЫ НА ОТМЕНУ
    ////

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-supply.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-shadow-supply-created.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelShadowSupplyRequestInCreated() throws Exception {
        MvcResult result = performCancel(1);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-supply.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-cancel-shadow-supply-waiting-for-confirmation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void cancelShadowSupplyRequestInWaitingForConfirmation() throws Exception {
        MvcResult result = performCancel(2);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-supply.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-cancel-shadow-supply-validated.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void cancelShadowSupplyRequestInValidated() throws Exception {
        MvcResult result = performCancel(3);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-supply.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-cancel-shadow-supply.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void cancelShadowSupplyRequestInCancelled() throws Exception {
        MvcResult result = performCancel(4);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));
    }

    ////
    //// ТЕСТЫ НА ОТМЕНУ withdraw
    ////

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-withdraw.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-shadow-withdraw-created.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelShadowWithdrawRequestInCreated() throws Exception {
        MvcResult result = performCancel(1);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-withdraw.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-cancel-shadow-withdraw-validated.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void cancelShadowSWithdrawRequestInValidated() throws Exception {
        MvcResult result = performCancel(3);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-shadow-withdraw.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-cancel-shadow-withdraw.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void cancelShadowWithdrawRequestInCancelled() throws Exception {
        MvcResult result = performCancel(4);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));
    }
    ///

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-success-cancel-transfer.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelTransferInPreparedForCreation() throws Exception {
        MvcResult result = performCancel(1);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-fail-cancel-transfer.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/before-fail-cancel-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelTransferInCreated() throws Exception {
        mockMvc.perform(
                put("/requests/1/cancel")
        ).andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-created.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelRequestCreated() throws Exception {
        when(stockStorageOutboundClient.getFreezes("1")).thenReturn(List.of(
                SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, false)
        ));
        // Изъятие
        MvcResult result = performCancel(1);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));

        // Поставка
        result = performCancel(11);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));

        // Календаризированная поставка
        result = performCancel(111);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));

        verify(stockStorageOutboundClient).getFreezes("1");
        verify(stockStorageOutboundClient).unfreezeStocks(1);
        verifyZeroInteractions(fulfillmentClient);
        verifyNoMoreInteractions(stockStorageOutboundClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-validated.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelRequestValidated() throws Exception {
        // Изъятие
        MvcResult result = performCancel(2);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        // Поставка
        result = performCancel(22);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        // Календаризированная поставка
        result = performCancel(222);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-cancel-waiting-for-confirmation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void cancelRequestWaitingForConfirmation() throws Exception {
        // Поставка
        MvcResult result = performCancel(77);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        // Календаризированная поставка
        result = performCancel(777);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-accepted-by-service.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelRequestAcceptedByService() throws Exception {
        // Изъятие
        MvcResult result = performCancel(4);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        // Поставка
        result = performCancel(44);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        // Календаризированная поставка
        result = performCancel(444);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancel-xdoc-supply-in-accepted-by-xdoc-service.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api" +
            "/after-cancel-xdoc-supply-in-accepted-by-xdoc-service.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelXdocSupplyRequestAcceptedByXdocService() throws Exception {
        MvcResult result = performCancel(3301);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":1}"));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel-utilization-outbound.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancel-utilization-outbound.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelUtilizationOutbound() throws Exception {
        mockMvc.perform(
                put("/requests/88/cancel")
        ).andExpect(status().isBadRequest());

        MvcResult result = performCancel(8);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":0}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/before-cancel.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelRequestInHigherStatusFailed() throws Exception {
        // Изъятие
        MvcResult result = performCancel(5);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":2}"));

        // Поставка
        result = performCancel(55);
        assertThat(result.getResponse().getContentAsString(), equalTo("{\"status\":2}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancel.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/before-cancel.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void cancelRequestAlreadyHadCancellation() throws Exception {
        // Изъятие
        MvcResult result = performCancelBadRequest(6);
        assertThat(result.getResponse().getContentAsString(), equalTo(ALREADY_HAD_CANCELLATION_RESULT));

        // Поставка
        result = performCancelBadRequest(66);
        assertThat(result.getResponse().getContentAsString(), equalTo(ALREADY_HAD_CANCELLATION_RESULT));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-withdraw-cancellation-accepted.xml",
            assertionMode = NON_STRICT)
    void acceptWithdrawCancellation() throws Exception {
        when(stockStorageOutboundClient.getFreezes(String.valueOf(WITHDRAW_CANCELLED_REQ_ID))).thenReturn(List.of(
                SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, true)
        ));
        mockMvc.perform(
                put("/requests/" + WITHDRAW_CANCELLED_REQ_ID + "/accept-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());
        verify(stockStorageOutboundClient).getFreezes(String.valueOf(WITHDRAW_CANCELLED_REQ_ID));
        verifyNoMoreInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-calendaring.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-withdraw-calendaring-cancellation-accepted.xml",
            assertionMode = NON_STRICT)
    void acceptWithdrawCalendaringCancellation() throws Exception {
        when(stockStorageOutboundClient.getFreezes(String.valueOf(WITHDRAW_CALENDARING_CANCELLED_REQ_ID)))
                .thenReturn(List.of(
                        SSItemFreeze.of(null, 1, false, 0, SSStockType.FIT, false)
                ));
        mockMvc.perform(
                put("/requests/" + WITHDRAW_CALENDARING_CANCELLED_REQ_ID + "/accept-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());
        verify(stockStorageOutboundClient).getFreezes(String.valueOf(WITHDRAW_CALENDARING_CANCELLED_REQ_ID));
        verify(stockStorageOutboundClient).unfreezeStocks(WITHDRAW_CALENDARING_CANCELLED_REQ_ID);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-supply-cancellation-accepted.xml",
            assertionMode = NON_STRICT)
    void acceptSupplyCancellation() throws Exception {
        mockMvc.perform(
                put("/requests/" + SUPPLY_CANCELLED_REQ_ID + "/accept-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-xdock-supply.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-xdock-supply-cancellation-accepted.xml",
            assertionMode = NON_STRICT)
    void acceptXdockSupplyCancellation() throws Exception {
        mockMvc.perform(
                put("/requests/" + XDOCK_SUPPLY_CANCELLED_REQ_ID + "/accept-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-cancellation-expired-supplies.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-cancellation-expired-supplies.xml",
            assertionMode = NON_STRICT)
    void acceptExpiredSupplyCancellation() throws Exception {
        performCancellationAcception(1);
        performCancellationAcception(2);
        performCancellationAcception(3);
        performCancellationAcception(4);
        performCancellationAcception(5);
        performCancellationAcception(6);
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-calendaring.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-supply-calendaring-cancellation-accepted.xml",
            assertionMode = NON_STRICT)
    void acceptCalendaringSupplyCancellation() throws Exception {
        mockMvc.perform(
                put("/requests/" + SUPPLY_CALENDARING_CANCELLED_REQ_ID + "/accept-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-withdraw-cancellation-rejected.xml",
            assertionMode = NON_STRICT)
    void rejectWithdrawCancellation() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        mockMvc.perform(
                put("/requests/" + WITHDRAW_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(WITHDRAW_CANCELLATION_REJECTED, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>6</id>" +
                        "<source-warehouse-id>1</source-warehouse-id>" +
                        "<source-warehouse-name>test</source-warehouse-name>" +
                        "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>09:09</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-calendaring.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-withdraw-calendaring-cancellation-rejected.xml",
            assertionMode = NON_STRICT)
    void rejectCalendaringWithdrawCancellation() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        mockMvc.perform(
                put("/requests/" + WITHDRAW_CALENDARING_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer).produceSingle(argumentCaptor.capture());
        Assertions.assertEquals(VALID_SUPPLIER_ID, argumentCaptor.getValue().getSupplierId());
        Assertions.assertEquals(WITHDRAW_CANCELLATION_REJECTED, argumentCaptor.getValue().getNotificationType());
        Assertions.assertEquals("<request-info><id>666</id>" +
                        "<source-warehouse-id>1</source-warehouse-id>" +
                        "<source-warehouse-name>test</source-warehouse-name>" +
                        "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                        "<merchandise-receipt-time>09:09</merchandise-receipt-time></request-info>",
                argumentCaptor.getValue().getData());
        verify(sendMbiNotificationQueueProducer, times(1)).produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-supply-cancellation-rejected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectSupplyCancellation() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        mockMvc.perform(
                put("/requests/" + SUPPLY_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(2)).produceSingle(argumentCaptor.capture());

        assertions.assertThat(argumentCaptor.getAllValues().get(0).getSupplierId()).isEqualTo(VALID_SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getNotificationType())
                .isEqualTo(SUPPLY_CANCELLATION_REJECTED);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getData()).isEqualTo("<request-info><id>66</id>" +
                "<destination-warehouse-id>1</destination-warehouse-id>" +
                "<destination-warehouse-name>test</destination-warehouse-name>" +
                "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                "<merchandise-receipt-time>09:09</merchandise-receipt-time></request-info>");

        assertions.assertThat(argumentCaptor.getAllValues().get(1).getSupplierId()).isEqualTo(VALID_SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getNotificationType())
                .isEqualTo(SUPPLY_ACCEPTED_BY_SERVICE);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getData()).isEqualTo("<request-info><id>66</id>" +
                "<destination-warehouse-id>1</destination-warehouse-id>" +
                "<destination-warehouse-name>test</destination-warehouse-name>" +
                "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                "<merchandise-receipt-time>09:09</merchandise-receipt-time></request-info>");

        verify(sendMbiNotificationQueueProducer, times(2))
                .produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-supply-after-registry-creation.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/" +
                    "after-cancellation-supply-rejected-after-plan-registry-accepted.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void rejectSupplyCancellationAfterRegistryCreation() throws Exception {
        mockMvc.perform(
                put("/requests/" + SUPPLY_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-withdraw-after-registry-creation.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/" +
                    "after-cancellation-withdraw-rejected-after-plan-registry-accepted.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void rejectWithdrawCancellationAfterRegistryCreation() throws Exception {
        mockMvc.perform(
                put("/requests/" + WITHDRAW_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-xdock-supply.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-xdock-supply-cancellation-rejected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectXdockSupplyCancellation() throws Exception {
        mockMvc.perform(
                put("/requests/" + XDOCK_SUPPLY_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-cancellation-calendaring.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-supply-calendaring-cancellation-rejected.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectCalendaringSupplyCancellation() throws Exception {
        ArgumentCaptor<SendMbiNotificationPayload> argumentCaptor =
                ArgumentCaptor.forClass(SendMbiNotificationPayload.class);

        mockMvc.perform(
                put("/requests/" + SUPPLY_CALENDARING_CANCELLED_REQ_ID + "/reject-cancellation")
        ).andDo(print())
                .andExpect(status().isOk());

        verify(sendMbiNotificationQueueProducer, times(2)).produceSingle(argumentCaptor.capture());
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getSupplierId()).isEqualTo(VALID_SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getNotificationType())
                .isEqualTo(SUPPLY_CANCELLATION_REJECTED);
        assertions.assertThat(argumentCaptor.getAllValues().get(0).getData()).isEqualTo("<request-info><id>6666</id>" +
                "<destination-warehouse-id>1</destination-warehouse-id>" +
                "<destination-warehouse-name>test</destination-warehouse-name>" +
                "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                "<merchandise-receipt-time>09:09</merchandise-receipt-time></request-info>");

        assertions.assertThat(argumentCaptor.getAllValues().get(1).getSupplierId()).isEqualTo(VALID_SUPPLIER_ID);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getNotificationType())
                .isEqualTo(SUPPLY_ACCEPTED_BY_SERVICE);
        assertions.assertThat(argumentCaptor.getAllValues().get(1).getData()).isEqualTo("<request-info><id>6666</id>" +
                "<destination-warehouse-id>1</destination-warehouse-id>" +
                "<destination-warehouse-name>test</destination-warehouse-name>" +
                "<merchandise-receipt-date>09 сентября</merchandise-receipt-date>" +
                "<merchandise-receipt-time>09:09</merchandise-receipt-time></request-info>");

        verify(sendMbiNotificationQueueProducer, times(2))
                .produceSingle(any(SendMbiNotificationPayload.class));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
    }

    /**
     * Проверяет работу метода {@link RequestController#rejectByService(long)} для правильного набора данных
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/on-reject-transfer.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/after-reject-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectTransferSuccessfully() throws Exception {
        performReject(VALID_TRANSFER_REQ_ID).andExpect(status().isOk());
        verify(stockStorageOutboundClient).unfreezeStocks(VALID_TRANSFER_REQ_ID);
        verifyNoMoreInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any());
        verify(sendMbiNotificationQueueProducer, never()).produceSingle(any(), anyLong());
    }

    /**
     * Проверяет ошибку метода {@link RequestController#rejectByService(long)}  для отсутствующего трансфера
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/on-reject-transfer.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-reject-transfer.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectTransferNotFound() throws Exception {
        final MvcResult mvcResult = performReject(ALIEN_TRANSFER_REQ_ID)
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(APPLICATION_JSON_UTF8))
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo(ALIEN_TRANSFER_NOT_FOUND_ERROR));
        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    /**
     * Проверяет, что заявка, которая находилась в статусе {@code WAITING_FOR_CONFIRMATION},
     * успешно перешла в статус {@code VALIDATED} после подтверждения.
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/before-confirmation.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-confirmation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void confirmWaitingForConfirmationRequest() throws Exception {
        final long requestIdWithWaitingForConfirmationStatus = 2;

        performConfirmation(requestIdWithWaitingForConfirmationStatus)
                .andExpect(status().isOk())
                .andReturn();

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    /**
     * Проверяет, что запрос на подверждение заявки, которая находилась
     * в статусе предшествующем {@code WAITING_FOR_CONFIRMATION}, вернул 400 код ответа.
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/before-confirmation.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-confirmation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void confirmBeforeWaitingForConfirmationRequestStatus() throws Exception {
        final long requestIdWithCreatedStatus = 1;

        final MvcResult mvcResult = performConfirmation(requestIdWithCreatedStatus)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Request 1 has invalid status for confirmation: 0 that is not equal to 12\"}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    /**
     * Проверяет, что запрос на подверждение заявки, которая находилась
     * в статусе после {@code WAITING_FOR_CONFIRMATION}, вернул 400 код ответа.
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/before-confirmation.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-confirmation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void confirmAfterWaitingForConfirmationRequestStatus() throws Exception {
        final long requestIdWithValidatedStatus = 3;

        final MvcResult mvcResult = performConfirmation(requestIdWithValidatedStatus)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Request 3 has invalid status for confirmation: 1 that is not equal to 12\"}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    /**
     * Упасть на подтверждении заявки, если для неё есть активные UPDATING_REQUEST, вернуть 400 код ответа.
     */
    @Test
    @DatabaseSetup("classpath:controller/request-api/before-confirmation.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-confirmation.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failOnConfirmIfActiveUpdaitingRequestsExist() throws Exception {
        final long requestIdWithActiveUptatedRequest = 4;

        final MvcResult mvcResult = performConfirmation(requestIdWithActiveUptatedRequest)
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Request 4 has active updating request(s): 4001\"}"));

        verifyZeroInteractions(stockStorageOutboundClient);
        verifyZeroInteractions(fulfillmentClient);
        verifyZeroInteractions(sendMbiNotificationQueueProducer);
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-get-free-slots-for-marschroute-not-validated.xml")
    void getSlotsForWithdrawOnMarschrouteReturnEmpty() throws Exception {
        setupLmsGateSchedule(9, 12, GateTypeResponse.OUTBOUND, FIXED_SUPPLY_DATE);
        MvcResult mvcResult = performGetSlots(REQUEST_ID_WITHOUT_SLOT);
        assertThat(mvcResult.getResponse().getContentAsString(), equalToIgnoringWhiteSpace(
                getJsonResponseNoFile("get_slots_empty.json")
        ));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-get-slots-for-xdoc-use-calendaring.xml")
    void getSlotsForXDocUseCalendaring() throws Exception {
        setupLmsGateSchedule(9, 12, FIXED_SUPPLY_DATE);
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(1, Collections.emptyList()))));
        long xDocServiceId = 2L;

        mockMvc.perform(
                get("/requests/5/getFreeTimeSlots?xDocServiceId=" + xDocServiceId)
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        ArgumentCaptor<GetFreeSlotsRequest> argumentCaptor = ArgumentCaptor.forClass(GetFreeSlotsRequest.class);
        verify(calendaringServiceClient).getFreeSlots(argumentCaptor.capture());
        GetFreeSlotsRequest request = argumentCaptor.getValue();

        Assertions.assertEquals(30, request.getSlotDurationMinutes());
        Assertions.assertTrue(request.getWarehouseIds().contains(xDocServiceId));
        Assertions.assertEquals(BookingType.X_DOC_PARTNER_SUPPLY_TO_FF, request.getBookingType());
        Assertions.assertEquals(50, request.getTakenItems());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/consist-in-updating-consolidated-shipping.xml")
    void failConsistInConsolidatedShipping() throws Exception {
        setupLmsGateSchedule(9, 12, FIXED_SUPPLY_DATE);
        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(1, Collections.emptyList()))));
        long xDocServiceId = 2L;
        String result = mockMvc.perform(
                get("/requests/5/getFreeTimeSlots?xDocServiceId=" + xDocServiceId)
        ).andDo(print())
                .andExpect(status().is4xxClientError())
                .andReturn().getResponse().getContentAsString();
        JSONAssert.assertEquals(
                result,
                "{\"message\" : \"Consist in consolidated shipping with update status\"}",
                JSONCompareMode.NON_EXTENSIBLE
        );
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-get-slots-for-xdoc-big-inbound.xml")
    void getNotFreeSlots() throws Exception {

        List<NotFreeIntervalReason> reasons =
                List.of(new NotFreeIntervalReason(NotFreeIntervalReasonType.NOT_IN_SCHEDULE, null));

        List<NotFreeSlotGateDTO> notFreeSlotGateDTOS = List.of(new NotFreeSlotGateDTO(1L, "1", reasons));

        List<NotFreeSlotDTO> notFreeSlotDTOS = List.of(new NotFreeSlotDTO(
                LocalDateTime.of(2021, 1, 10, 10, 0),
                LocalDateTime.of(2021, 1, 10, 10, 30),
                notFreeSlotGateDTOS
        ));

        NotFreeSlotsResponse notFreeSlotsResponse =
                new NotFreeSlotsResponse(List.of(new WarehouseNotFreeSlotsResponse(1L, notFreeSlotDTOS)));

        when(calendaringServiceClient.getNotFreeSlotsReasons(any())).thenReturn(notFreeSlotsResponse);

        mockMvc.perform(
                get("/requests/5/getNotFreeTimeSlots")
        ).andDo(print())
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().json(getJsonResponseNoFile("get_not_free_slots.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-get-free-slots-for-import-supply-wth-calendaring-service.xml")
    void getSlotsForImportWithCalendaringService() throws Exception {
        LocalDate date1 = LocalDate.of(2018, 1, 5);
        List<TimeSlotResponse> slots = List.of(
                new TimeSlotResponse(LocalTime.of(9, 0), LocalTime.of(10, 0)),
                new TimeSlotResponse(LocalTime.of(9, 30), LocalTime.of(10, 30)),
                new TimeSlotResponse(LocalTime.of(10, 0), LocalTime.of(11, 0)),
                new TimeSlotResponse(LocalTime.of(10, 30), LocalTime.of(11, 30)),
                new TimeSlotResponse(LocalTime.of(11, 0), LocalTime.of(12, 0))
        );
        List<FreeSlotsForDayResponse> freeSlotsForDayResponses =
                List.of(new FreeSlotsForDayResponse(date1, ZoneOffset.of("+03:00"), slots));

        when(calendaringServiceClient.getFreeSlots(any())).thenReturn(new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(1, freeSlotsForDayResponses))));
        setupLmsGateSchedule(9, 12, FIXED_SUPPLY_DATE);

        mockMvc.perform(
                get("/requests/5/getFreeTimeSlots?slotSize=180")
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content()
                        .json(getJsonResponseNoFile("get-free-slots-for-import-suply-without-slot-size.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-select-xdoc-slot-before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/on-select-xdoc-slot-after.xml",
            assertionMode = NON_STRICT)
    void selectSlotForXDoc() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        mockMvc.perform(
                post("/requests/1/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-select-xdoc-slot-using-calendaring-before.xml")
    void selectSlotForXDocUsingCalendaring() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6));
        when(calendaringServiceClient.bookSlot(any())).thenReturn(new BookSlotResponse(1L, 1L,
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 9, 0), ZoneId.of("+03:00")),
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 9, 30), ZoneId.of("+03:00"))));
        mockMvc.perform(
                post("/requests/1/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isOk());

        ArgumentCaptor<BookSlotRequest> argumentCaptor = ArgumentCaptor.forClass(BookSlotRequest.class);
        verify(calendaringServiceClient).bookSlot(argumentCaptor.capture());
        BookSlotRequest request = argumentCaptor.getValue();

        Assertions.assertEquals(2, request.getWarehouseId());
        Assertions.assertEquals(BookingType.X_DOC_PARTNER_SUPPLY_TO_FF, request.getType());
        Assertions.assertEquals(LocalDateTime.of(2018, 1, 6, 9, 0), request.getFrom());
        Assertions.assertEquals(LocalDateTime.of(2018, 1, 6, 9, 30), request.getTo());
        Assertions.assertEquals(3, request.getTakenItems());
        Assertions.assertEquals(1, request.getTakenPallets());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-select-slot-ok-before-using-calendaring.xml")
    void selectSlotHappyPathMidnight() throws Exception {
        setupLmsGateSchedule(0, 0, LocalDate.of(2018, 1, 6));
        when(calendaringServiceClient.bookSlot(any())).thenReturn(new BookSlotResponse(1L, 1L,
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 22, 30), ZoneId.of("+03:00")),
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 7, 23, 0), ZoneId.of("+03:00"))));
        mockMvc.perform(
                post("/requests/1/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, LocalDate.of(2018, 1, 6),
                                LocalTime.of(23, 0),
                                LocalTime.of(0, 0)))
        )
                .andExpect(status().isOk());

        ArgumentCaptor<BookSlotRequest> argumentCaptor = ArgumentCaptor.forClass(BookSlotRequest.class);
        verify(calendaringServiceClient).bookSlot(argumentCaptor.capture());
        assertThat(argumentCaptor.getValue().getTo(), equalTo(LocalDateTime.of(2018, 1, 7, 0, 0)));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/fail-on-select-xdoc-slot-before.xml")
    void failOnSelectSlotForXDoc() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6));
        mockMvc.perform(
                post("/requests/1/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/fail-on-select-xdoc-slot-using-calendaring-before.xml")
    void failOnSelectSlotForXDocUsingCalendaring() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6));
        when(calendaringServiceClient.bookSlot(any())).thenReturn(new BookSlotResponse(1L, 1L,
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 9, 0), ZoneId.of("+03:00")),
                ZonedDateTime.of(LocalDateTime.of(2018, 1, 6, 9, 30), ZoneId.of("+03:00"))));
        mockMvc.perform(
                post("/requests/1/selectSlot")
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(String.format(REQUEST_SLOT, LocalDate.of(2018, 1, 6), LocalTime.of(9, 0),
                                LocalTime.of(9, 30)))
        )
                .andExpect(status().isBadRequest());

        verify(calendaringServiceClient, never()).bookSlot(any());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/on-get-slots-with-destination-id.xml")
    void getFreeTimeSlotsForDestinationService() throws Exception {
        setupLmsGateSchedule(9, 12, LocalDate.of(2018, 1, 6), LocalDate.of(2018, 1, 7));

        FreeSlotsResponse response = new FreeSlotsResponse(List.of(
                new WarehouseFreeSlotsResponse(147L, List.of())));

        when(calendaringServiceClient.getFreeSlots(argThat(param -> param.getDestinationWarehouseId().equals(1L))))
                .thenReturn(response);

        performGetSlots(1L);
        verify(calendaringServiceClient).getFreeSlots(argThat(param -> param.getDestinationWarehouseId().equals(1L)));
    }

    @Test
    @DatabaseSetup("classpath:controller/additional-supply/items.xml")
    void getAdditionalSuppliesStatus() throws Exception {
        mockMvc.perform(
                get("/requests/1/additional-supplies/status")
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("additional-supplies/get-status.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/additional-supply/items.xml")
    void getAdditionalSuppliesStatuses() throws Exception {
        mockMvc.perform(
                get("/requests/additional-supplies/statuses?requestIds=1,2")
        ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(getJsonResponseNoFile("additional-supplies/get-statuses.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/" +
            "accept-by-service-with-send-to-service-status.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-accept-by-service-with-send-to-service-status.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceOutboundWithSendToServiceStatus() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());
        verify(fulfillmentClient, never()).putInbound(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/inbound/before-supply-put-inbound-supply.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/inbound/" +
            "after-supply-put-inbound-supply.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void acceptByServiceSupplyPutInbound() throws Exception {
        environmentParamService.setParam("supply-put-inbound-date-param",
                List.of(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:" +
            "controller/request-api/updateRequests/inbound/before-xdock-partner-supply-put-inbound-supply.xml")
    @ExpectedDatabase(value = "classpath:" +
            "controller/request-api/updateRequests/inbound/after-xdock-partner-supply-put-inbound-supply.xml",
            assertionMode = NON_STRICT)
    void acceptByServiceXDockPartnerSupplyPutInbound() throws Exception {
        environmentParamService.setParam("supply-put-inbound-date-param",
                List.of(LocalDateTime.MIN.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/accept-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/" +
            "accept-by-service-with-send-to-service-status.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-accept-by-service-with-send-to-service-status.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectByServiceOutboundWithSendToServiceStatus() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new RequestAcceptDTO("0001")))
        ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/updateRequests/outbound/" +
            "before-reject-by-service-with-message.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/updateRequests/outbound/" +
            "after-reject-by-service-with-message.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void rejectByServiceWithMessage() throws Exception {
        String content = toJson(new RequestRejectDTO("Error"));
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reject-by-service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        ).andExpect(status().isOk());
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/reopen-request/wrong-type/before.xml")
    void reopenRequestWrongTypeTest() throws Exception {

        String responsePath = "controller/request-api/reopen-request/wrong-type/response.json";

        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(FileContentUtils.getFileContent(responsePath)));

    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/reopen-request/wrong-status/before.xml")
    void reopenRequestWrongStatusTest() throws Exception {

        String responsePath = "controller/request-api/reopen-request/wrong-status/response.json";


        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().json(FileContentUtils.getFileContent(responsePath)));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/reopen-request/successfully/before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/reopen-request/successfully/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void reopenRequestSuccessfullyTest() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/reopen-request/successfully-invalid/before.xml")
    @ExpectedDatabase(value = "classpath:controller/request-api/reopen-request/successfully-invalid/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void reopenRequestSuccessfullyInInvalidStatusTest() throws Exception {
        mockMvc.perform(
                put("/requests/" + VALID_REQ_ID + "/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
        ).andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/yard-requests.xml")
    void getRequestTypeStatusMapTest() throws Exception {
        mockMvc.perform(get("/requests/request-type-status-map"))
                .andExpect(status().isOk());
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/request-rejections/before.xml")
    void testGetRequestRejections() throws Exception {
        mockMvc.perform(get("/requests/1/rejections"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().json(FileContentUtils.getFileContent(
                        "controller/request-api/request-rejections/response.json")));
    }

    private void setupLmsGateSchedule(int fromHour, int toHour, LocalDate... workingDays) {
        setupLmsGateSchedule(fromHour, toHour, GateTypeResponse.INBOUND, workingDays);
    }

    private void setupLmsGateSchedule(int fromHour, int toHour, GateTypeResponse gateType, LocalDate... workingDays) {
        when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                anyLong(), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                        MockParametersHelper.mockSingleGateAvailableResponse(1L, gateType),
                        Arrays.stream(workingDays)
                                .map(day -> mockGatesSchedules(day, LocalTime.of(fromHour, 0), LocalTime.of(toHour, 0)))
                                .collect(Collectors.toList())
                )
        );
    }

    private MvcResult performCancel(long id) throws Exception {
        return mockMvc.perform(
                put("/requests/" + id + "/cancel")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult performCancelBadRequest(long id) throws Exception {
        return mockMvc.perform(
                put("/requests/" + id + "/cancel")
        ).andDo(print())
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    private MvcResult performGetSlots(long id) throws Exception {
        return mockMvc.perform(
                get("/requests/" + id + "/getFreeTimeSlots")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private MvcResult performCancellationAcception(long id) throws Exception {
        return mockMvc.perform(
                put("/requests/" + id + "/accept-cancellation")
        ).andDo(print())
                .andExpect(status().isOk())
                .andReturn();
    }

    private ResultActions performReject(long requestId) throws Exception {
        return perform(null, requestId, "reject-by-service", "");
    }

    private ResultActions performRejectRegisterByService(long requestId) throws Exception {
        return perform(null, requestId, "reject-register-by-service", "");
    }

    private ResultActions performConfirmation(long requestId) throws Exception {
        return perform(null, requestId, "confirm", "");
    }

    private ResultActions perform(Long supplierId, long requestId, String method, String content) throws Exception {
        return mockMvc.perform(
                put((supplierId == null ? "" : "/suppliers/" + supplierId) + "/requests/" + requestId + "/" + method)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
        ).andDo(print());
    }

    private String getJsonResponseNoFile(String name) throws IOException {
        return getJsonNoFile("response/", name);
    }

    private String getJsonNoFile(String prefix, String name) throws IOException {
        return FileContentUtils.getFileContent("controller/request-api/" + prefix + name);
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response,
                                           Integer requestId,
                                           String isAllowed) throws IOException {
        JSONAssert.assertEquals(getJsonResponseNoFile(filename)
                        .replace("\"{requestId}\"", requestId.toString())
                        .replace("\"{allowed}\"", isAllowed),
                response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
