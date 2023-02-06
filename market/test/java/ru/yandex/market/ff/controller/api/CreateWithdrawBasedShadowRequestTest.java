package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.ff.base.MvcIntegrationTest;
import ru.yandex.market.ff.controller.util.MockParametersHelper;
import ru.yandex.market.ff.service.implementation.LmsClientCachingServiceImpl;
import ru.yandex.market.ff.util.CalendaringServiceUtils;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для ручки: /requests/{shadowRequestId}/commit-shadow-withdraw
 */
public class CreateWithdrawBasedShadowRequestTest extends MvcIntegrationTest {
    private static final long VALID_GATE_ID = 222;
    private static final long VALID_SHADOW_WITHDRAW_ID = 0;
    private static final long INVALID_STATUS_SHADOW_WITHDRAW_ID = 401;
    private static final long NONE_SHADOW_WITHDRAW_ID = 405;
    private static final long NONE_EXISTENT_SHADOW_WITHDRAW_ID = -1;
    private static final long VALID_SHADOW_WITHDRAW_ID_NEEDED_MIN_TIMESLOT = 408;
    private static final long QUOTA_EXCEEDED_SHADOW_WITHDRAW_ID = 409;

    private static final LocalDate VALID_REQUESTED_DATE = LocalDate.of(2018, 1, 6);
    private static final LocalDate REQUESTED_DATE_AFTER_NEAREST = LocalDate.of(2018, 1, 16);
    private static final LocalDate REQUESTED_DATE_BEFORE_NEAREST = LocalDate.of(2018, 1, 2);
    private static final LocalTime VALID_FROM = LocalTime.of(9, 30);
    private static final LocalTime VALID_FROM_PLUS_30MIN = VALID_FROM.plusMinutes(30);
    private static final LocalTime VALID_FROM_PLUS_60MIN = VALID_FROM.plusMinutes(60);
    private static final LocalTime VALID_TO = VALID_FROM_PLUS_60MIN;

    @Autowired
    private LmsClientCachingServiceImpl lmsClientCachingService;

    @BeforeEach
    void init() {
        reset(lmsClient);
        when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                anyLong(), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                        MockParametersHelper.mockSingleGateAvailableResponse(VALID_GATE_ID, GateTypeResponse.OUTBOUND),
                        ImmutableList.of(
                                MockParametersHelper.mockGatesSchedules(VALID_REQUESTED_DATE,
                                        LocalTime.of(9, 0), LocalTime.of(13, 0)))
                )
        );
    }

    @AfterEach
    void invalidateCache() {
        lmsClientCachingService.invalidateCache();
    }

    /**
     * Проверяем успешное создание изъятия на основе теневой заявки.
     * <p>
     * В БД:
     * - Теневая заявка на изъятие с requestId=0 и status=VALIDATED(1)
     * - Под нее НЕ забукано окно.
     * <p>
     * Проверяем:
     * - Теневая заявка с requestId=0 перешла в статус FINISHED(10)
     * - Создана новая заявка на поставку с requestId=1 в статусе VALIDATED(1)
     * - Скопированы Items, LegalInfo, StatusHistoryList
     * - скопирована запись на документ из которого создана исходная теневая поставка
     * - под неё зарезервирован временной промежуток
     * - под неё зарезервированы квоты
     */
    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-withdraw-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")})
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldCreateWithdrawBasedOnShadowRequest() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        final MvcResult mvcResult = doCreateWithdraw(
                VALID_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("comment", "some comment")
                        .put("consignee", "ООО НЕФТЬ VAPE")
                        .put("phoneNumber", "79234568797")
                        .put("contactPersonName", "Имя1")
                        .put("contactPersonSurname", "Фамилия1")
                        .put("externalOperationType", "2")
                        .put("externalRequestId", "ext333")
                        .put("requestCreator", "Creator")
                        .build())
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("created-response-for-shadow-withdraw-3p.json", mvcResult);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-withdraw-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-auction-based-on-shadow.xml")})
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-create-withdraw-auction-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldCreateWithdrawAuctionRequest() throws Exception {
        when(csClient.bookSlot(any())).thenReturn(CalendaringServiceUtils.createBookSlotResponse(1));
        final MvcResult mvcResult = doCreateWithdraw(
                VALID_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("comment", "some comment")
                        .put("consignee", "ООО НЕФТЬ VAPE")
                        .put("phoneNumber", "79234568797")
                        .put("contactPersonName", "Имя1")
                        .put("contactPersonSurname", "Фамилия1")
                        .put("externalOperationType", "2")
                        .put("externalRequestId", "ext333")
                        .put("requestCreator", "Creator")
                        .build())
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("created-response-for-shadow-withdraw-auction.json", mvcResult);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-withdraw-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")})
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateWithdrawWithIncorrectTimeslotDuration() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                VALID_SHADOW_WITHDRAW_ID_NEEDED_MIN_TIMESLOT,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_FROM_PLUS_30MIN.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Slot is out of limits: RequestedSlotDTO{date=2018-01-06, from=09:30, " +
                        "to=10:00}\",\"type\":\"CANNOT_BOOK_TIMESLOT\"}"));
    }


    @Test
    void failToCreateBasedOnNonExistWithdrawRequest() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                NONE_EXISTENT_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Failed to find [REQUEST] with id [" + NONE_EXISTENT_SHADOW_WITHDRAW_ID +
                        "]\"," +
                        "\"resourceType\":\"REQUEST\"," +
                        "\"identifier\":\"" + NONE_EXISTENT_SHADOW_WITHDRAW_ID + "\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowWithdrawInInvalidStatus() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                INVALID_STATUS_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"It's prohibited to create real request based on shadow request in status = " +
                        "CREATED. " +
                        "Valid statuses: [VALIDATED]\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnNotShadowWithdrawRequestType() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                NONE_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Given request with id=" + NONE_SHADOW_WITHDRAW_ID
                        + " is not shadow withdraw\"}"));
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowWithdrawWithNullRequestedDate() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                VALID_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Requested date is not set but required\"," +
                        "\"type\":\"SUPPLY_DATE_IS_NOT_SET\"}"));
    }


    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowWithdrawWithRequestedDateAfterNearestPeriod() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                VALID_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", REQUESTED_DATE_AFTER_NEAREST.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Requested slot outside the permitted period\"," +
                        "\"type\":\"INVALID_REQUEST_DATE\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-withdraw-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowWithdrawWithRequestedDateBeforeNearestPeriod() throws Exception {
        final MvcResult mvcResult = doCreateWithdraw(
                VALID_SHADOW_WITHDRAW_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", REQUESTED_DATE_BEFORE_NEAREST.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("consignee", "cons")
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Requested slot outside the permitted period\"," +
                        "\"type\":\"INVALID_REQUEST_DATE\"}"));
    }

    private String getJsonFromFile(String name) throws IOException {
        return FileContentUtils.getFileContent("controller/request-api/response/" + name);
    }

    private ResultActions doCreateWithdraw(long requestId, Map<String, Object> poorMansJson) throws Exception {
        String serializedJson = objectMapper.writeValueAsString(poorMansJson);
        return mockMvc.perform(
                post("/requests/" + requestId + "/commit-shadow-withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serializedJson)
        ).andDo(print());
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
