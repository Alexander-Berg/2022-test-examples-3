package ru.yandex.market.ff.controller.api;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import ru.yandex.market.ff.enums.ExternalOperationType;
import ru.yandex.market.ff.service.implementation.LmsClientCachingServiceImpl;
import ru.yandex.market.ff.util.FileContentUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.BookSlotResponse;
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Функциональный тест для ручки: /requests/{shadowRequestId}/commit-shadow-supply
 */
class CreateSupplyBasedShadowRequestTest extends MvcIntegrationTest {
    private static final long VALID_GATE_ID = 222;
    private static final long VALID_SHADOW_SUPPLY_ID = 0;
    private static final long INVALID_STATUS_SHADOW_SUPPLY_ID = 401;
    private static final long NONE_SHADOW_SUPPLY_ID = 405;
    private static final long NONE_EXISTENT_SHADOW_SUPPLY_ID = -1;

    private static final long NONE_EXISTENT_SUPPLY_ID = -1;
    private static final long MISSING_OPERATION_TYPE_SUPPLY_ID = 401;
    private static final long VALID_SUPPLY_ID = 405;

    private static final long VALID_SERVICE_ID = 333;
    private static final long VALID_XDOC_SERVICE_ID = 47000;
    private static final LocalDate VALID_REQUESTED_DATE = LocalDate.of(2018, 1, 6);
    private static final LocalTime VALID_FROM = LocalTime.of(9, 30);
    private static final LocalTime VALID_FROM_PLUS_60MIN = VALID_FROM.plusMinutes(60);
    private static final LocalTime VALID_TO = VALID_FROM_PLUS_60MIN;
    public static final String TEST_CONSIGNOR = "test consignor";

    @Autowired
    private LmsClientCachingServiceImpl lmsClientCachingService;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @BeforeEach
    void init() {
        reset(lmsClient);
        when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                anyLong(), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                        MockParametersHelper.mockSingleGateAvailableResponse(VALID_GATE_ID, GateTypeResponse.INBOUND),
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

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-supply-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-update-supply.xml")})
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-update-supply.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void updateRequestWithExternalFieldsOk() throws Exception {
        doUpdateSupply(
                VALID_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("externalRequestId", "myExternalRequestId")
                        .put("externalOperationType", ExternalOperationType.SUPPLY_REQUEST.getId())
                        .build())
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-supply-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-update-supply.xml")})
    void updateRequestWithExternalFields4xxOperationTypeDiffers() throws Exception {
        final MvcResult mvcResult = doUpdateSupply(
                MISSING_OPERATION_TYPE_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("externalRequestId", "myExternalRequestId")
                        .put("externalOperationType", ExternalOperationType.SUPPLY_REQUEST.getId())
                        .build())
                .andExpect(status().is4xxClientError())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Actual 'external operation type'" +
                        " differs(required: SUPPLY_REQUEST, but was: null)\"}"));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-supply-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-update-supply.xml")})
    void updateRequestWithExternalFields4xxResourceNotFound() throws Exception {
        final MvcResult mvcResult = doUpdateSupply(
                NONE_EXISTENT_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("externalRequestId", "myExternalRequestId")
                        .put("externalOperationType", ExternalOperationType.SUPPLY_REQUEST.getId())
                        .build())
                .andExpect(status().is4xxClientError())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Failed to find [REQUEST] with id [-1]\"," +
                        "\"resourceType\":\"REQUEST\",\"identifier\":\"-1\"}"));
    }

    @Test
    void updateRequestWithExternalFields4xxInvalidBody() throws Exception {
        final MvcResult mvcResult = doUpdateSupply(
                VALID_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("externalOperationType", ExternalOperationType.SUPPLY_REQUEST.getId())
                        .build())
                .andExpect(status().is4xxClientError())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"externalRequestId must not be empty\"}"));
    }

    /**
     * Проверяем успешное создание транзитной поставки на основе теневой заявки.
     * <p>
     * В БД:
     * - Теневая заявка на поставку с requestId=0 и status=VALIDATED(1)
     * - Под нее НЕ забукано окно.
     * - опционально указан поставщик
     * - опционально указан грузоперевозчик
     * <p>
     * Проверяем:
     * - Теневая заявка с requestId=0 перешла в статус FINISHED(10)
     * - Создана новая заявка на поставку с requestId=1 в статусе CREATED(1)
     * - Скопированы Items и LegalInfo
     * - скопирована запись на документ из которого создана исходная теневая поставка
     * - под неё не зарезервирован временной промежуток
     * - под неё не зарезервированы квоты
     */
    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-api/before-create-xdoc-supply-based-on-shadow.xml")
    })
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-create-xdoc-supply-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldCreateXDocSupplyBasedOnShadowRequest() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                VALID_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("consignor", TEST_CONSIGNOR)
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("xDocServiceId", VALID_XDOC_SERVICE_ID)
                        .put("xDocRequestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build()
        )
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("created-response-for-xdoc-shadow-supply-3p.json", mvcResult);
    }

    /**
     * Проверяем успешное создание обычной транзитной поставки на основе теневой заявки и уточнённого из CommitRequest'a
     * целевого типа поставки.
     * <p>
     * В БД:
     * - Теневая заявка на поставку с requestId=0 и status=VALIDATED(1)
     * - Под нее НЕ забукано окно.
     * - опционально указан поставщик
     * - опционально указан грузоперевозчик
     * <p>
     * Проверяем:
     * - Теневая заявка с requestId=0 перешла в статус FINISHED(10)
     * - Создана новая заявка на поставку с requestId=1 в статусе CREATED(1) и типом SUPPLY(0)
     * - Скопированы Items и LegalInfo
     * - скопирована запись на документ из которого создана исходная теневая поставка
     * - под неё не зарезервирован временной промежуток
     * - под неё не зарезервированы квоты
     */
    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-api/before-create-xdoc-supply-based-on-shadow.xml")})
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-create-xdoc-supply-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldCreateXDocSupplyBasedOnShadowRequestWithRealType() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                VALID_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("consignor", TEST_CONSIGNOR)
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("xDocServiceId", VALID_XDOC_SERVICE_ID)
                        .put("xDocRequestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .put("type", "0")
                        .build()
        )
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("created-response-for-xdoc-shadow-supply-3p.json", mvcResult);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:controller/request-api/before-create-supply-based-on-shadow-with-subtype.xml")
    })
    @ExpectedDatabase(
            value = "classpath:controller/request-api/after-create-supply-based-on-shadow-with-subtype.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldCreateSupplyBasedOnShadowRequestWithSubtype() throws Exception {

        BookSlotResponse bookSlotResponse = new BookSlotResponse(1, 1,
                ZonedDateTime.of(2022, 3, 14, 12, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                ZonedDateTime.of(2022, 3, 14, 13, 0, 0, 0, ZoneId.of("Europe/Moscow"))
        );

        when(calendaringServiceClient.bookSlot(any())).thenReturn(bookSlotResponse);

        final MvcResult mvcResult = doCreateSupply(
                VALID_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("consignor", TEST_CONSIGNOR)
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build()
        )
                .andExpect(status().isOk())
                .andReturn();
        assertJsonResponseCorrect("created-response-for-shadow-supply-3p-with-subtype.json", mvcResult);
    }

    @Test
    void failToCreateBasedOnNonExistShadowSupply() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                NONE_EXISTENT_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build())
                .andExpect(status().isNotFound())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Failed to find [REQUEST] with id [" + NONE_EXISTENT_SHADOW_SUPPLY_ID + "]\"," +
                        "\"resourceType\":\"REQUEST\"," +
                        "\"identifier\":\"" + NONE_EXISTENT_SHADOW_SUPPLY_ID + "\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-supply-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-supply-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowSupplyInInvalidStatus() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                INVALID_STATUS_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"It's prohibited to create real request based on shadow request in status = " +
                        "CREATED. " +
                        "Valid statuses: [VALIDATED]\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-supply-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-supply-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnNotShadowSupplyRequestType() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                NONE_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Given request with id=" + NONE_SHADOW_SUPPLY_ID
                        + " is not shadow supply\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-supply-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-supply-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowSupplyWithNullServiceId() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                VALID_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("requestedDate", VALID_REQUESTED_DATE.toString())
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Service id must be specified\"}"));
    }

    @Test
    @DatabaseSetup("classpath:controller/request-api/before-create-supply-based-on-shadow.xml")
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-supply-based-on-shadow.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void failToCreateBasedOnShadowSupplyWithNullRequestedDate() throws Exception {
        final MvcResult mvcResult = doCreateSupply(
                VALID_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("serviceId", VALID_SERVICE_ID)
                        .put("from", VALID_FROM.toString())
                        .put("to", VALID_TO.toString())
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString(),
                equalTo("{\"message\":\"Requested date is not set but required\"," +
                        "\"type\":\"SUPPLY_DATE_IS_NOT_SET\"}"));
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/check-supply-limits.xml"),
            @DatabaseSetup("classpath:controller/request-api/before-create-supply-based-on-shadow-ekb.xml")})
    @ExpectedDatabase(
            value = "classpath:controller/request-api/before-create-supply-based-on-shadow-ekb.xml",
            assertionMode = NON_STRICT_UNORDERED
    )
    void shouldFailCreateSupplyBasedOnShadowRequestForTimezoneEkbShift() throws Exception {
        when(lmsClient.getWarehousesGatesScheduleByPartnerId(
                anyLong(), any(LocalDate.class), any(LocalDate.class))
        ).thenReturn(
                MockParametersHelper.mockGatesScheduleResponse(
                        MockParametersHelper.mockSingleGateAvailableResponse(VALID_GATE_ID, GateTypeResponse.INBOUND),
                        ImmutableList.of(
                                MockParametersHelper.mockGatesSchedules(LocalDate.of(2018, 1, 1),
                                        LocalTime.of(9, 0), LocalTime.of(13, 0)))
                )
        );
        MvcResult mvcResult = doCreateSupply(
                VALID_SHADOW_SUPPLY_ID,
                ImmutableMap.<String, Object>builder()
                        .put("serviceId", 300)
                        .put("requestedDate", LocalDate.of(2018, 1, 1).toString())
                        .put("from", LocalTime.of(11, 30).toString())
                        .put("to", LocalTime.of(12, 30).toString())
                        .put("consignor", TEST_CONSIGNOR)
                        .build())
                .andExpect(status().isBadRequest())
                .andReturn();
        assertions.assertThat(mvcResult.getResponse().getContentAsString())
                .isEqualTo("{\"message\":\"Request date must be after 2018-01-01T07:10:10Z\"," +
                        "\"type\":\"INVALID_REQUEST_DATE\"}");
    }

    private String getJsonFromFile(String name) throws IOException {
        return FileContentUtils.getFileContent("controller/request-api/response/" + name);
    }

    private ResultActions doCreateSupply(long requestId, Map<String, Object> poorMansJson) throws Exception {
        String serializedJson = objectMapper.writeValueAsString(poorMansJson);
        return mockMvc.perform(
                post("/requests/" + requestId + "/commit-shadow-supply")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serializedJson)
        ).andDo(print());
    }

    private ResultActions doUpdateSupply(long requestId, Map<String, Object> poorMansJson) throws Exception {
        String serializedJson = objectMapper.writeValueAsString(poorMansJson);
        return mockMvc.perform(
                put("/requests/" + requestId + "/update-external-request-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serializedJson)
        ).andDo(print());
    }

    private void assertJsonResponseCorrect(String filename, MvcResult response) throws IOException {
        JSONAssert.assertEquals(getJsonFromFile(filename), response.getResponse().getContentAsString(),
                JSONCompareMode.NON_EXTENSIBLE);
    }
}
