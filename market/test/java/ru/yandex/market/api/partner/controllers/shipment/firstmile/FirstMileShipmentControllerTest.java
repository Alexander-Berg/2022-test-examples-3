package ru.yandex.market.api.partner.controllers.shipment.firstmile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.partner.apisupport.ErrorRestModelCode;
import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.api.partner.controllers.shipment.firstmile.dto.FirstMileShipmentConfirmRequestDTO;
import ru.yandex.market.api.partner.controllers.shipment.firstmile.dto.FirstMileShipmentSearchRequestDTO;
import ru.yandex.market.api.partner.controllers.shipment.firstmile.dto.ShipmentStatus;
import ru.yandex.market.api.partner.controllers.util.checkouter.CheckouterMockHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.enums.ShipmentType;
import ru.yandex.market.logistics.nesu.client.model.NamedEntity;
import ru.yandex.market.logistics.nesu.client.model.error.ClientError;
import ru.yandex.market.logistics.nesu.client.model.error.ErrorType;
import ru.yandex.market.logistics.nesu.client.model.error.PartnerShipmentConfirmationError;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceNotFoundError;
import ru.yandex.market.logistics.nesu.client.model.error.ResourceType;
import ru.yandex.market.logistics.nesu.client.model.error.ValidationError;
import ru.yandex.market.logistics.nesu.client.model.page.Page;
import ru.yandex.market.logistics.nesu.client.model.page.PageRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentActions;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentConfirmRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentFilter;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentOrdersCount;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentSearchDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatusChange;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentWarehouseDto;
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.mbi.web.converter.MbiWebXmlMapperFactory;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.core.shipment.FirstMileShipmentErrorUtil.getNesuErrorObjectMapper;

/**
 * Функциональные тесты для {@link FirstMileShipmentController}.
 *
 * @author serenitas
 */
@DbUnitDataSet(before = "FirstMileShipmentControllerTest.before.csv")
class FirstMileShipmentControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final long SUPPLIER_ID = 100;
    private static final long CAMPAIGN_ID = 1000500100;

    @Autowired
    @Qualifier("nesuClient")
    private NesuClient nesuClient;

    @Autowired
    private ZoneId conversationTimeZone;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    private CheckouterMockHelper checkouterMockHelper;

    @BeforeEach
    void setUp() {
        checkouterMockHelper = new CheckouterMockHelper(
                checkouterRestTemplate,
                checkouterUrl
        );
    }

    @ParameterizedTest(name = "get: {0}")
    @MethodSource("getGetArgs")
    void testGetShipment(Format format, String filename) throws Exception {
        Mockito.when(nesuClient.getShipment(Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID),
                Mockito.eq(12321L))).thenReturn(getPartnerShipmentDto());

        final ResponseEntity<String> stringResponseEntity = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321",
                HttpMethod.GET, format);

        assertResponseEquals(filename, stringResponseEntity, format);
    }

    private static Stream<Arguments> getGetArgs() {
        return Stream.of(
                Arguments.of(Format.XML, "get-shipment-response.xml"),
                Arguments.of(Format.JSON, "get-shipment-response.json")
        );
    }

    @Test
    @DbUnitDataSet(before = "FirstMileShipmentControllerTest.mapping.before.csv")
    void testShipmentMapping() throws Exception  {
        Mockito.when(nesuClient.getShipment(Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(Set.of(100L, 1001L)),
                        Mockito.eq(12321L))).thenReturn(getPartnerShipmentDto());

        final ResponseEntity<String> response = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321",
                HttpMethod.GET, Format.JSON);

        JSONAssert.assertEquals(readResource("get-shipment-mapping-response.json"), response.getBody(), JSONCompareMode.LENIENT);
    }

    @ParameterizedTest(name = "get: {0} {2}")
    @MethodSource("getSearchArgs")
    void testSearchShipments(Format format, ObjectMapper mapper, String requestFilename, String responseFilename) throws Exception {
        final PartnerShipmentFilter filter = PartnerShipmentFilter.builder()
                .dateFrom(LocalDate.of(2020, 6, 6))
                .dateTo(LocalDate.of(2020, 6, 26))
                .orderIds(List.of(5L, 6L, 7L))
                .statuses(List.of(PartnerShipmentStatus.INBOUND_ACCEPTED, PartnerShipmentStatus.MOVEMENT_COURIER_FOUND))
                .build();

        Mockito.when(nesuClient.searchPartnerShipments(Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID),
                Mockito.eq(filter), Mockito.eq(new PageRequest(2, 30))))
                .thenReturn(new Page<PartnerShipmentSearchDto>()
                        .setData(List.of(PartnerShipmentSearchDto.builder()
                                .id(21L)
                                .partner(NamedEntity.builder().id(360L).name("Доставка единорогами").build())
                                .shipmentType(ShipmentType.WITHDRAW)
                                .number("89gtyriuei")
                                .ordersCount(PartnerShipmentOrdersCount.builder().draft(10).planned(9).fact(5).build())
                                .planIntervalFrom(LocalDateTime.parse("2020-06-06T00:30:00"))
                                .planIntervalTo(LocalDateTime.parse("2020-06-06T10:20:30"))
                                .status(PartnerShipmentStatus.INBOUND_SHIPPED)
                                .statusDescription("Ваши товары уже летят со склада")
                                .build()))
                        .setPage(2)
                        .setTotalPages(5));

        final String body = readResource(requestFilename);
        final ResponseEntity<String> stringResponseEntity = FunctionalTestHelper.makeRequest(
                urlBasePrefix +
                        "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments?page_token" +
                        "=eyJvcCI6Ij4iLCJrZXkiOnsicGFnZU51bWJlciI6MiwibGltaXQiOjMwfSwic2tpcCI6MH0",
                HttpMethod.PUT, format, body);

        assertResponseEquals(responseFilename, stringResponseEntity, format);
    }

    private static Stream<Arguments> getSearchArgs() {
        return Stream.of(
                Arguments.of(Format.XML, new MbiWebXmlMapperFactory(new ApiObjectMapperFactory()).getObject(),
                        "search-shipments-request.xml", "search-shipments-response.xml"),
                Arguments.of(Format.JSON, new ApiObjectMapperFactory().createJsonMapper(),
                        "search-shipments-request.json", "search-shipments-response.json"),
                Arguments.of(Format.XML, new MbiWebXmlMapperFactory(new ApiObjectMapperFactory()).getObject(),
                        "search-shipments-request-old-date-format.xml", "search-shipments-response.xml"),
                Arguments.of(Format.JSON, new ApiObjectMapperFactory().createJsonMapper(),
                        "search-shipments-request-old-date-format.json", "search-shipments-response.json")
        );
    }

    @ParameterizedTest(name = "confirm: {0}")
    @MethodSource("getConfirmArgs")
    void testConfirmShipment(Format format, ObjectMapper objectMapper) throws JsonProcessingException {
        final String externalId = "external-id-12321";
        final List<Long> orderIds = List.of(1L, 4L, 987654321L);

        final FirstMileShipmentConfirmRequestDTO requestDTO =
                new FirstMileShipmentConfirmRequestDTO(externalId, orderIds);

        String request = objectMapper.writeValueAsString(requestDTO);

        FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321/confirm",
                HttpMethod.POST, format, request
        );

        ArgumentCaptor<PartnerShipmentConfirmRequest> captor =
                ArgumentCaptor.forClass(PartnerShipmentConfirmRequest.class);

        Mockito.verify(nesuClient).confirmShipment(
                Mockito.eq(FunctionalTestHelper.DEFAULT_UID),
                Mockito.eq(SUPPLIER_ID),
                Mockito.eq(12321L),
                captor.capture());

        PartnerShipmentConfirmRequest expected = PartnerShipmentConfirmRequest.builder().externalId(externalId)
                .orderIds(orderIds).build();
        Assertions.assertEquals(expected, captor.getValue());
    }

    private static Stream<Arguments> getConfirmArgs() {
        return Stream.of(
                Arguments.of(Format.XML, new MbiWebXmlMapperFactory(new ApiObjectMapperFactory()).getObject()),
                Arguments.of(Format.JSON, new ApiObjectMapperFactory().createJsonMapper())
        );
    }

    @ParameterizedTest
    @MethodSource("testDownloadActArgs")
    void testDownloadAct(Function<NesuClient, byte[]> mockNesuClient, String url, String mimeType) {
        Mockito.when(mockNesuClient.apply(nesuClient)).thenReturn(new byte[10]);

        final ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + url,
                HttpMethod.GET, Format.JSON, null, byte[].class
        );

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals(mimeType,
                Objects.requireNonNull(responseEntity.getHeaders().getContentType()).getSubtype());
        Assertions.assertEquals(10, Objects.requireNonNull(responseEntity.getBody()).length);
    }

    @Nonnull
    private static Stream<Arguments> testDownloadActArgs() {
        return Stream.of(
            Arguments.of((Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateAct(
                    Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID), Mockito.eq(12321L)),
                    "/first-mile/shipments/12321/act", "pdf"),
            Arguments.of((Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateInboundAct(
                    Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID), Mockito.eq(12321L)),
                    "/first-mile/shipments/12321/inbound-act", "pdf"),
            Arguments.of((Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateDiscrepancyAct(
                    Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(Set.of(SUPPLIER_ID)), Mockito.eq(12321L)),
                    "/first-mile/shipments/12321/discrepancy-act",
                    "vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        );
    }

    @Test
    void testDownloadTransportationWaybill() {
        Mockito.when(nesuClient.generateTransportationWaybill(
            Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID),
            Mockito.eq(12321L)
        ))
            .thenReturn(new byte[10]);

        final ResponseEntity<byte[]> responseEntity = FunctionalTestHelper.makeRequest(
            urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321/transportation-waybill",
            HttpMethod.GET, Format.JSON, null, byte[].class
        );

        Assertions.assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        Assertions.assertEquals("pdf",
            Objects.requireNonNull(responseEntity.getHeaders().getContentType()).getSubtype());
        Assertions.assertEquals(10, Objects.requireNonNull(responseEntity.getBody()).length);
    }

    @ParameterizedTest(name = "getOrdersInfo: {0}")
    @MethodSource("getGetShipmentOrdersInfoArgs")
    void testGetOrdersInfo(Format format, String filename) throws Exception {
        Mockito.when(nesuClient.getShipment(Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID),
                Mockito.eq(12321L)))
                .thenReturn(
                        PartnerShipmentDto.builder()
                                .id(999L)
                                .planIntervalFrom(LocalDateTime.parse("2021-05-08T10:15:00"))
                                .planIntervalTo(LocalDateTime.parse("2021-05-26T10:15:00"))
                                .shipmentType(ShipmentType.WITHDRAW)
                                .warehouseFrom(
                                        PartnerShipmentWarehouseDto.builder()
                                                .id(48123L)
                                                .name("DropshipWH")
                                                .address("DropshipWH address")
                                                .build()
                                )
                                .partner(NamedEntity.builder().id(12348L).name("Del Service").build())
                                .currentStatus(PartnerShipmentStatusChange.builder()
                                        .code(PartnerShipmentStatus.MOVEMENT_COURIER_FOUND)
                                        .datetime(LocalDateTime.parse("2021-05-16T10:15:00").toInstant(ZoneOffset.UTC))
                                        .description("Some text")
                                        .build())
                                .orderIds(List.of(5L, 6L, 7L))
                                .confirmedOrderIds(Collections.emptyList())
                                .build());
        prepareCheckouterMock();

        final ResponseEntity<String> stringResponseEntity = FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321/orders/info",
                HttpMethod.GET, format
        );

        assertResponseEquals(filename, stringResponseEntity, format);
    }

    private static Stream<Arguments> getGetShipmentOrdersInfoArgs() {
        return Stream.of(
                Arguments.of(Format.XML, "get-shipment-orders-info-response.xml"),
                Arguments.of(Format.JSON, "get-shipment-orders-info-response.json")
        );
    }

    @ParameterizedTest(name = "get errors: {0}")
    @MethodSource("getShipmentErrorsArgs")
    void testGetShipmentErrors(@SuppressWarnings("unused") String testName,
                               HttpTemplateException throwable, HttpStatus httpStatus, String expectedBody) {

        Mockito.when(nesuClient.getShipment(Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID),
                Mockito.eq(12321L)))
                .thenThrow(throwable);

        HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321",
                        HttpMethod.GET, Format.JSON
                ));

        Assertions.assertEquals(httpStatus, e.getStatusCode());
        Assertions.assertEquals(expectedBody, e.getResponseBodyAsString());
    }

    @Nonnull
    private static Stream<Arguments> getShipmentErrorsArgs() throws JsonProcessingException {
        final ResourceNotFoundError notFoundError = new ResourceNotFoundError();
        notFoundError.setType(ErrorType.RESOURCE_NOT_FOUND);
        notFoundError.setResourceType(ResourceType.PARTNER_SHIPMENT);

        final ResourceNotFoundError otherNotFoundError = new ResourceNotFoundError();
        otherNotFoundError.setType(ErrorType.RESOURCE_NOT_FOUND);
        otherNotFoundError.setResourceType(ResourceType.DELIVERY_SERVICE);

        final ClientError anyError = new ClientError();
        anyError.setType(ErrorType.SENDER_SETTINGS_VALIDATION);

        return Stream.of(Arguments.of("Shipment is not found", buildHttpException(notFoundError), HttpStatus.NOT_FOUND,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Shipment with ID 12321 not " +
                        "found\"}]}"),
                Arguments.of("Something else is not found", buildHttpException(otherNotFoundError),
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"Cannot get " +
                                "information about shipment with ID 12321\"}]}"),
                Arguments.of("Other client error", buildHttpException(anyError), HttpStatus.INTERNAL_SERVER_ERROR,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"Cannot get " +
                                "information about shipment with ID 12321\"}]}"));
    }

    @ParameterizedTest(name = "search errors: {0}")
    @MethodSource("searchShipmentErrorsArgs")
    void testSearchShipmentsErrors(@SuppressWarnings("unused") String testName,
                                   HttpTemplateException throwable, HttpStatus httpStatus,
                                   String expectedBody) throws JsonProcessingException {
        Mockito.when(nesuClient.searchPartnerShipments(Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID), Mockito.any(), Mockito.any()))
                .thenThrow(throwable);

        final String body =
                new ApiObjectMapperFactory().createJsonMapper().writeValueAsString(new FirstMileShipmentSearchRequestDTO(LocalDate.of(2020, 6, 6),
                        LocalDate.of(2020, 6, 26), List.of(ShipmentStatus.INBOUND_ACCEPTED,
                        ShipmentStatus.MOVEMENT_COURIER_FOUND), List.of(5L, 6L, 7L)));
        HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequest(urlBasePrefix +
                                "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments?page_token" +
                                "=eyJvcCI6Ij4iLCJrZXkiOnsicGFnZU51bWJlciI6MiwibGltaXQiOjMwfSwic2tpcCI6MH0",
                        HttpMethod.PUT, Format.JSON, body));

        Assertions.assertEquals(httpStatus, e.getStatusCode());
        Assertions.assertEquals(expectedBody, e.getResponseBodyAsString());
    }

    @Nonnull
    private static Stream<Arguments> searchShipmentErrorsArgs() throws JsonProcessingException {
        final ValidationError withErrors = new ValidationError();

        final ValidationError.ValidationViolation validationViolation = new ValidationError.ValidationViolation();
        validationViolation.setErrorCode(ValidationError.ValidationErrorCode.FIELD_NOT_VALID);
        validationViolation.setField("number");
        validationViolation.setMessage("must not be empty");

        final ValidationError.ValidationViolation validationViolation1 = new ValidationError.ValidationViolation();
        validationViolation1.setErrorCode(ValidationError.ValidationErrorCode.FIELD_NOT_VALID);
        validationViolation1.setField("statuses");
        validationViolation1.setMessage("must not contain nulls");

        withErrors.setType(ErrorType.VALIDATION_ERROR);
        withErrors.setErrors(List.of(validationViolation, validationViolation1));

        final ValidationError withoutErrors = new ValidationError();
        withoutErrors.setType(ErrorType.VALIDATION_ERROR);

        final ClientError anyError = new ClientError();
        anyError.setType(ErrorType.SENDER_SETTINGS_VALIDATION);

        return Stream.of(Arguments.of("Validation error with concrete errors", buildHttpException(withErrors),
                HttpStatus.BAD_REQUEST,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot search shipments: " +
                        "bad request. There are some errors: number must not be empty; statuses must not " +
                        "contain nulls\"}]}"),
                Arguments.of("Validation error only", buildHttpException(withoutErrors), HttpStatus.BAD_REQUEST,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot search " +
                                "shipments: bad request\"}]}"),
                Arguments.of("Other client error", buildHttpException(anyError), HttpStatus.INTERNAL_SERVER_ERROR,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"Cannot search " +
                                "shipments\"}]}"));
    }

    @ParameterizedTest(name = "confirm errors: {0}")
    @MethodSource("confirmShipmentErrorsArgs")
    void testConfirmShipmentsErrors(@SuppressWarnings("unused") String testName,
                                    HttpTemplateException throwable, HttpStatus httpStatus,
                                    String expectedBody) throws JsonProcessingException {
        Mockito.doThrow(throwable).when(nesuClient).confirmShipment(Mockito.eq(FunctionalTestHelper.DEFAULT_UID),
                Mockito.eq(SUPPLIER_ID), Mockito.anyLong(), Mockito.any());

        final String externalId = "external-id-12321";
        final List<Long> orderIds = List.of(1L, 4L, 987654321L);

        final FirstMileShipmentConfirmRequestDTO requestDTO =
                new FirstMileShipmentConfirmRequestDTO(externalId, orderIds);

        String request = new ApiObjectMapperFactory().createJsonMapper().writeValueAsString(requestDTO);
        HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321/confirm",
                        HttpMethod.POST, Format.JSON, request
                ));

        Assertions.assertEquals(httpStatus, e.getStatusCode());
        Assertions.assertEquals(expectedBody, e.getResponseBodyAsString());
    }

    @ParameterizedTest
    @EnumSource(value = PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType.class)
    void testMessagesForConfirmationError(PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType type) {
        final Map<PartnerShipmentConfirmationError.ExcludedOrderReason, List<Long>> ordersMap =
                Map.of(PartnerShipmentConfirmationError.ExcludedOrderReason.CANCELLED, List.of(1L, 2L),
                        PartnerShipmentConfirmationError.ExcludedOrderReason.NO_PLACES, List.of(3L));

        final PartnerShipmentConfirmationError error = new PartnerShipmentConfirmationError();
        error.setError(type);
        error.setInvalidOrders(ordersMap);

        final Pair<ErrorRestModelCode, String> data = Assertions.assertDoesNotThrow(
                () -> NesuClientShipmentErrorWrapper.getConfirmError(1L, error),
                "Every confirmation error must have readable message"
        );
        Assertions.assertNotNull(data.getKey());
        Assertions.assertNotNull(data.getValue());
    }

    @Nonnull
    private static Stream<Arguments> confirmShipmentErrorsArgs() throws JsonProcessingException {
        final PartnerShipmentConfirmationError notCreated = new PartnerShipmentConfirmationError();
        notCreated.setType(ErrorType.PARTNER_SHIPMENT_CONFIRMATION_VALIDATION);
        notCreated.setError(PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType.NOT_CREATED);

        final PartnerShipmentConfirmationError alreadyConfirmed = new PartnerShipmentConfirmationError();
        alreadyConfirmed.setType(ErrorType.PARTNER_SHIPMENT_CONFIRMATION_VALIDATION);
        alreadyConfirmed.setError(PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType.ALREADY_CONFIRMED);

        final PartnerShipmentConfirmationError cutoffNotReached = new PartnerShipmentConfirmationError();
        cutoffNotReached.setType(ErrorType.PARTNER_SHIPMENT_CONFIRMATION_VALIDATION);
        cutoffNotReached.setError(PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType.CUTOFF_NOT_REACHED);

        final PartnerShipmentConfirmationError noOrders = new PartnerShipmentConfirmationError();
        noOrders.setType(ErrorType.PARTNER_SHIPMENT_CONFIRMATION_VALIDATION);
        noOrders.setError(PartnerShipmentConfirmationError.PartnerShipmentConfirmationErrorType.NO_ORDERS);

        final ClientError anyError = new ClientError();
        anyError.setType(ErrorType.SENDER_SETTINGS_VALIDATION);

        return Stream.of(Arguments.of("notCreated", buildHttpException(notCreated), HttpStatus.BAD_REQUEST,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Shipment with ID 12321 is " +
                        "not created yet\"}]}"),
                Arguments.of("alreadyConfirmed", buildHttpException(alreadyConfirmed), HttpStatus.BAD_REQUEST,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"ALREADY_CONFIRMED\",\"message\":\"Shipment " +
                                "with ID 12321 has been confirmed already\"}]}"),
                Arguments.of("cutoffNotReached", buildHttpException(cutoffNotReached), HttpStatus.BAD_REQUEST,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"CUTOFF_NOT_REACHED\",\"message\":\"Cutoff time" +
                                " for shipments has not been reached yet\"}]}"),
                Arguments.of("noOrders", buildHttpException(noOrders), HttpStatus.BAD_REQUEST,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NO_ORDERS\",\"message\":\"Shipment with ID " +
                                "12321 does not contain any orders\"}]}"),
                Arguments.of("Other client error", buildHttpException(anyError), HttpStatus.INTERNAL_SERVER_ERROR,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"Shipment with " +
                                "ID 12321 cannot be confirmed\"}]}"));
    }

    @ParameterizedTest(name = "download act errors: {0}")
    @MethodSource({"downloadDiscrepancyActErrorsArgs", "downloadActErrorsArgs"})
    void testDownloadActErrors(@SuppressWarnings("unused") String testName,
                               HttpTemplateException throwable, HttpStatus httpStatus, String expectedBody,
                               Function<NesuClient, byte[]> mockNesuClient, String url) {
        Mockito.when(mockNesuClient.apply(nesuClient)).thenThrow(throwable);

        HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + url,
                        HttpMethod.GET, Format.JSON, null, byte[].class
                ));

        Assertions.assertEquals(httpStatus, e.getStatusCode());
        Assertions.assertEquals(expectedBody, e.getResponseBodyAsString());
    }

    @Nonnull
    private static Stream<Arguments> downloadActErrorsArgs() throws JsonProcessingException {
        final ValidationError withErrors = new ValidationError();
        final ValidationError.ValidationViolation validationViolation = new ValidationError.ValidationViolation();
        validationViolation.setConditionCode("ValidShipmentStatus");
        validationViolation.setObjectName("shipment");
        validationViolation.setMessage("must be confirmed");
        withErrors.setErrors(List.of(validationViolation));
        withErrors.setType(ErrorType.VALIDATION_ERROR);

        final ValidationError withoutErrors = new ValidationError();
        final ValidationError.ValidationViolation emptyValidationViolation = new ValidationError.ValidationViolation();
        emptyValidationViolation.setConditionCode("OtherConditionCode");
        withoutErrors.setErrors(List.of(emptyValidationViolation));
        withoutErrors.setType(ErrorType.VALIDATION_ERROR);

        final ClientError anyError = new ClientError();
        anyError.setType(ErrorType.SENDER_SETTINGS_VALIDATION);

        List<Arguments> errors = List.of(
                Arguments.of("Not confirmed", buildHttpException(withErrors), HttpStatus.BAD_REQUEST,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot download" +
                                " act for shipment with ID 12321: bad request. There are some errors: shipment must " +
                                "be confirmed\"}]}"),
                Arguments.of("Other validation error", buildHttpException(withoutErrors), HttpStatus.BAD_REQUEST,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot download" +
                                " act for shipment with ID 12321: bad request\"}]}"),
                Arguments.of("Other client error", buildHttpException(anyError), HttpStatus.INTERNAL_SERVER_ERROR,
                        "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"Cannot download" +
                                " act for shipment with ID 12321\"}]}"));

        return Stream.of(
                Arguments.of((Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateAct(
                        Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID), Mockito.eq(12321L)),
                        "/first-mile/shipments/12321/act"),
                Arguments.of((Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateInboundAct(
                        Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID), Mockito.eq(12321L)),
                        "/first-mile/shipments/12321/inbound-act")
            ).flatMap( arguments -> {
                    Object[] clientArgs =  arguments.get();
                    return errors.stream().map(Arguments::get).map(errorArgs -> Arguments.of(errorArgs[0],
                            errorArgs[1], errorArgs[2], errorArgs[3], clientArgs[0], clientArgs[1]));
                }
            );
    }

    @Nonnull
    private static Stream<Arguments> downloadDiscrepancyActErrorsArgs() throws JsonProcessingException {
        final ValidationError withErrors = new ValidationError();
        final ValidationError.ValidationViolation validationViolation = new ValidationError.ValidationViolation();
        validationViolation.setConditionCode("ValidShipmentStatus");
        validationViolation.setMessage("Could not process discrepancy act file");
        withErrors.setErrors(List.of(validationViolation));
        withErrors.setType(ErrorType.VALIDATION_ERROR);

        final ValidationError withoutErrors = new ValidationError();
        final ValidationError.ValidationViolation emptyValidationViolation = new ValidationError.ValidationViolation();
        emptyValidationViolation.setConditionCode("OtherConditionCode");
        withoutErrors.setErrors(List.of(emptyValidationViolation));
        withoutErrors.setType(ErrorType.VALIDATION_ERROR);

        final ClientError anyError = new ClientError();
        anyError.setType(ErrorType.SENDER_SETTINGS_VALIDATION);

        return Stream.of(
            Arguments.of("Discrepancy act not confirmed", buildHttpException(withErrors), HttpStatus.BAD_REQUEST,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot download" +
                    " discrepancy act for shipment with ID 12321: bad request. There are some errors: shipment must " +
                    "be confirmed\"}]}"),
            Arguments.of("Discrepancy act other validation error", buildHttpException(withoutErrors),
                HttpStatus.BAD_REQUEST, "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":" +
                "\"Cannot download discrepancy act for shipment with ID 12321: bad request\"}]}"),
            Arguments.of("Discrepancy act other client error", buildHttpException(anyError),
                HttpStatus.INTERNAL_SERVER_ERROR, "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\"," +
                "\"message\":\"Cannot download discrepancy act for shipment with ID 12321\"}]}"))
            .map(Arguments::get)
            .map(errorArgs -> Arguments.of(errorArgs[0], errorArgs[1], errorArgs[2], errorArgs[3],
                (Function<NesuClient, byte[]>) nesuClient -> nesuClient.generateDiscrepancyAct(
                    Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(Set.of(SUPPLIER_ID)),
                    Mockito.eq(12321L)), "/first-mile/shipments/12321/discrepancy-act"));
    }


    @ParameterizedTest(name = "download transportation waybill errors: {0}")
    @MethodSource("downloadTransportationWaybillErrorsArgs")
    void testDownloadTransportationWaybillErrors(@SuppressWarnings("unused") String testName,
                                                 HttpTemplateException throwable, HttpStatus httpStatus,
                                                 String expectedBody) {
        Mockito.when(nesuClient.generateTransportationWaybill(
            Mockito.eq(FunctionalTestHelper.DEFAULT_UID), Mockito.eq(SUPPLIER_ID),
            Mockito.eq(12321L)
        ))
            .thenThrow(throwable);

        HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class,
            () -> FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" + CAMPAIGN_ID + "/first-mile/shipments/12321/transportation-waybill",
                HttpMethod.GET, Format.JSON, null, byte[].class
            ));

        Assertions.assertEquals(httpStatus, e.getStatusCode());
        Assertions.assertEquals(expectedBody, e.getResponseBodyAsString());
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"DBS,1000500200","FBY,1000500300"})
    void testBadRequestForInvalidCampaigns(String type, long campaignId) {
        HttpStatusCodeException actualException = Assertions.assertThrows(HttpStatusCodeException.class,
                () -> FunctionalTestHelper.makeRequest(
                        urlBasePrefix + "/campaigns/" + campaignId + "/first-mile/shipments/12321/act",
                        HttpMethod.GET, Format.JSON, null, byte[].class
                ));
        Assertions.assertEquals(HttpStatus.BAD_REQUEST, actualException.getStatusCode());
        Assertions.assertEquals("{\"status\":\"ERROR\",\"errors\":[{\"code\":\"CAMPAIGN_TYPE_NOT_SUPPORTED\"," +
                "\"message\":\"Campaign type is not allowed\"}]}", actualException.getResponseBodyAsString());
    }

    @Nonnull
    private static Stream<Arguments> downloadTransportationWaybillErrorsArgs() throws JsonProcessingException {
        final ValidationError withStatusErrors = new ValidationError();
        final ValidationError.ValidationViolation statusValidationViolation = new ValidationError.ValidationViolation();
        statusValidationViolation.setConditionCode("ValidShipmentStatus");
        statusValidationViolation.setObjectName("shipment");
        statusValidationViolation.setMessage("must be confirmed");
        withStatusErrors.setErrors(List.of(statusValidationViolation));
        withStatusErrors.setType(ErrorType.VALIDATION_ERROR);

        final ValidationError withTypeErrors = new ValidationError();
        final ValidationError.ValidationViolation typeValidationViolation = new ValidationError.ValidationViolation();
        typeValidationViolation.setConditionCode("ValidShipmentType");
        typeValidationViolation.setObjectName("shipment");
        typeValidationViolation.setMessage("must be withdraw");
        withTypeErrors.setErrors(List.of(typeValidationViolation));
        withTypeErrors.setType(ErrorType.VALIDATION_ERROR);

        final ValidationError withoutErrors = new ValidationError();
        final ValidationError.ValidationViolation emptyValidationViolation = new ValidationError.ValidationViolation();
        emptyValidationViolation.setConditionCode("OtherConditionCode");
        withoutErrors.setErrors(List.of(emptyValidationViolation));
        withoutErrors.setType(ErrorType.VALIDATION_ERROR);

        final ClientError anyError = new ClientError();
        anyError.setType(ErrorType.SENDER_SETTINGS_VALIDATION);

        return Stream.of(Arguments.of("Not confirmed", buildHttpException(withStatusErrors), HttpStatus.BAD_REQUEST,
            "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot download"
                + " transportation waybill for shipment with ID 12321: bad request. There are some errors: " +
                    "shipment must be confirmed\"}]}"),
            Arguments.of("Not withdraw", buildHttpException(withTypeErrors), HttpStatus.BAD_REQUEST,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot download"
                    + " transportation waybill for shipment with ID 12321: bad request. There are some errors: " +
                        "shipment must be withdraw\"}]}"),
            Arguments.of("Other validation error", buildHttpException(withoutErrors), HttpStatus.BAD_REQUEST,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_REQUEST\",\"message\":\"Cannot download" +
                    " transportation waybill for shipment with ID 12321: bad request\"}]}"),
            Arguments.of("Other client error", buildHttpException(anyError), HttpStatus.INTERNAL_SERVER_ERROR,
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"INTERNAL_ERROR\",\"message\":\"Cannot download" +
                    " transportation waybill for shipment with ID 12321\"}]}"));
    }


    @Nonnull
    private static HttpTemplateException buildHttpException(ClientError error) throws JsonProcessingException {
        final String body = getNesuErrorObjectMapper().writeValueAsString(error);
        return new HttpTemplateException(HttpStatus.NOT_FOUND.value(), body);
    }

    private void assertResponseEquals(String filename, ResponseEntity<String> response, Format format) throws Exception {
        final String expected = readResource(filename);
        final String body = response.getBody();
        if (format == Format.XML) {
            MbiAsserts.assertXmlEquals(expected, body);
        } else {
            MbiAsserts.assertJsonEquals(expected, body);
        }

    }

    private String readResource(String filename) throws Exception {
        try (final InputStream resource = getClass().getResourceAsStream(filename)) {
            return IOUtils.toString(
                    Preconditions.checkNotNull(resource, String.format("Resource %s is not found", filename)),
                    StandardCharsets.UTF_8
            );
        }
    }

    private void prepareCheckouterMock() {
        String responseAsString = resourceAsString("validate-orders-checkouter-response.json");
        checkouterMockHelper.mockGetOrders(SUPPLIER_ID)
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(responseAsString)
                );
    }

    private PartnerShipmentDto getPartnerShipmentDto() {
        return PartnerShipmentDto.builder()
                .id(999L)
                .planIntervalFromInstant(Instant.parse("2021-01-08T07:15:00Z"))
                .planIntervalToInstant(Instant.parse("2021-01-26T07:15:00Z"))
                .shipmentType(ShipmentType.WITHDRAW)
                .warehouseFrom(
                        PartnerShipmentWarehouseDto.builder()
                                .id(48123L)
                                .name("Мой лучший склад")
                                .address("Мой лучший адрес")
                                .build()
                )
                .warehouseTo(
                        PartnerShipmentWarehouseDto.builder()
                                .id(48124L)
                                .name("Другой лучший склад")
                                .address("Другой лучший адрес")
                                .build()
                )
                .partner(NamedEntity.builder().id(12348L).name("Мой лучший перевозчик").build())
                .currentStatus(PartnerShipmentStatusChange.builder()
                        .code(PartnerShipmentStatus.MOVEMENT_COURIER_FOUND)
                        .datetime(LocalDateTime.parse("2021-01-16T10:15:00").atZone(conversationTimeZone).toInstant())
                        .description("Для отгрузки найден лучший перевозчик в Галактике")
                        .build())
                .orderIds(List.of(5L, 6L, 7L))
                .availableActions(
                    PartnerShipmentActions.builder()
                        .confirm(true)
                        .downloadAct(true)
                        .downloadInboundAct(true)
                        .downloadDiscrepancyAct(true)
                        .build()
                )
                .build();
    }
}
