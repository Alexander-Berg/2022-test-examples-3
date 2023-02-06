package ru.yandex.market.api.partner.controllers.shipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.ResourceUtilitiesMixin;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logistics.nesu.client.NesuClient;
import ru.yandex.market.logistics.nesu.client.model.page.Page;
import ru.yandex.market.logistics.nesu.client.model.page.PageRequest;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentSearchDto;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatus;
import ru.yandex.market.logistics.nesu.client.model.shipment.PartnerShipmentStatusChange;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.logistics4shops.client.api.ExcludeOrderFromShipmentApi;
import ru.yandex.market.logistics4shops.client.model.ExcludeOrderFromShipmentRequestDto;
import ru.yandex.market.logistics4shops.client.model.ExcludeOrderFromShipmentRequestStatus;
import ru.yandex.market.logistics4shops.client.model.ExcludeOrderRequestListDto;
import ru.yandex.market.logistics4shops.client.model.ExcludeOrderRequestsSearchRequest;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Тестирование взаимодействия с HTTP интерфейсом контроллера поставок {@link ShipmentController}
 * Тест покрывает минимум кейсов ошибок при валидации.
 *
 * @author stani on 25.06.18.
 */
@DbUnitDataSet(before = "ShipmentControllerTest.before.csv")
class ShipmentControllerTest extends FunctionalTest implements ResourceUtilitiesMixin {

    private static final String NEW_WORKFLOW_PARTNERS = "market.papi.new_reception_act_workflow_partners";
    private static final long SHOP_CAMPAIGN_ID = 10668L;
    private static final String LEGAL_NAME = "ООО Рога и Копыта";
    private static final String DELIVERY_NAME = "ООО PickPoint";

    // JSON
    private static final String EXPECTED_ERROR_MESSAGE = "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\"," +
            "\"message\":\"No orders READY_TO_SHIP with shipment date today found for shop 668\"}]}";
    private static final String EXPECTED_ERROR_MESSAGE_EMPTY_BOXES = "{\"status\":\"ERROR\",\"errors\":[{\"code\":" +
            "\"BAD_REQUEST\"," +
            "\"message\":\"Can't proceed with document creation - reason: there are no boxes in order #1111, parcel " +
            "#4236608. Minimum required number of boxes is 1. Please, add boxes to the order #1111, " +
            "parcel #4236608 and try again.\"}]}";
    private static final String EXPECTED_ERROR_MESSAGE_NO_SHIPMENTS = "{\"status\":\"ERROR\",\"errors\":[{\"code\":" +
            "\"NOT_FOUND\",\"message\":\"Closest shipment for reception transfer act generation not found.\"}]}";
    private static final String EXPECTED_ERROR_MESSAGE_ORDERS_NOT_READY = "{\"status\":\"ERROR\",\"errors\":[{\"code\":"
            + "\"BAD_ORDERS\",\"message\":\"Some orders have not been processed yet. " +
            "Please change the status of orders to READY_TO_SHIP and try again. (1111).\"}]}";
    private static final String EXPECTED_ERROR_MESSAGE_ORDERS_EXCLUDING =
            "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"BAD_ORDERS\"," +
                    "\"message\":\"Some orders are in the process of being excluded from shipment (1113). " +
                    "Please wait up to 30 minutes and try again.\"}]}";
    private static final String EXPECTED_ERROR_MESSAGE_NO_ORDERS =
            "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NO_ORDERS\"," +
                    "\"message\":\"No orders for closest shipment found.\"}]}";

    //XML
    private static final String EXPECTED_ERROR_MESSAGE_NO_SHIPMENTS_XML =
            "<response><status>ERROR</status><errors><error code=\"NOT_FOUND\" " +
                    "message=\"Closest shipment for reception transfer act generation not found.\"" +
                    "/></errors></response>";
    private static final String EXPECTED_ERROR_MESSAGE_ORDERS_NOT_READY_XML =
            "<response><status>ERROR</status><errors><error code=\"BAD_ORDERS\" " +
                    "message=\"Some orders have not been processed yet. Please change the status of orders to " +
                    "READY_TO_SHIP and try again. (1111).\"/></errors></response>";
    private static final String EXPECTED_ERROR_MESSAGE_ORDERS_EXCLUDING_XML =
            "<response><status>ERROR</status><errors><error code=\"BAD_ORDERS\" " +
                    "message=\"Some orders are in the process of being excluded from shipment (1113). " +
                    "Please wait up to 30 minutes and try again.\"/></errors></response>";
    private static final String EXPECTED_ERROR_MESSAGE_NO_ORDERS_XML =
            "<response><status>ERROR</status><errors><error code=\"NO_ORDERS\" " +
                    "message=\"No orders for closest shipment found.\"/></errors></response>";

    private static final byte[] PDF_DATA = {1, 2, 3};
    private static final long SHOP_ID = 668L;
    private static final String EMPTY_SHIPMENT_ID = " ";
    private static final long SHIPMENT_ID = 132882L;
    private static final List<Long> ORDER_IDS = List.of(1111L, 1112L);
    private static final int PAGE_SIZE = 2;

    @Autowired
    @Qualifier("checkouterRestTemplate")
    private RestTemplate checkouterRestTemplate;

    @Value("${market.checkouter.client.url}")
    private String checkouterUrl;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private WwClient wwClient;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private NesuClient nesuClient;

    @Autowired
    private ExcludeOrderFromShipmentApi excludeOrderFromShipmentApi;

    @AfterEach
    void checkNoMoreInteractions() {
        verifyNoMoreInteractions(nesuClient);
        verifyNoMoreInteractions(wwClient);
        verifyNoMoreInteractions(excludeOrderFromShipmentApi);
    }

    @Test
    @DisplayName("Корректная генерация акта приемки-передачи")
    void testCorrectPdfActGeneration() {
        // given
        prepareCheckouterMock();
        mockMarketId();
        when(wwClient.generateReceptionTransferAct(any(RtaOrdersData.class), eq(DocumentFormat.PDF)))
                .thenReturn(PDF_DATA);
        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";

        // when
        ResponseEntity<byte[]> response = FunctionalTestHelper.makeRequest(path, HttpMethod.GET, Format.JSON,
                null, byte[].class);

        // then
        assertArrayEquals(PDF_DATA, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertThat(response.getHeaders().getContentType().getSubtype(), Matchers.equalTo("pdf"));

        var ordersDataCaptor = ArgumentCaptor.forClass(RtaOrdersData.class);
        verify(wwClient).generateReceptionTransferAct(ordersDataCaptor.capture(), any());
        RtaOrdersData data = ordersDataCaptor.getValue();
        assertEquals(LEGAL_NAME, data.getSenderLegalName());
        assertEquals(DELIVERY_NAME, data.getPartnerLegalName());
        assertEquals(EMPTY_SHIPMENT_ID, data.getShipmentId());
        List<DocOrder> orders = data.getOrders();
        assertEquals(1, orders.size());
        DocOrder docOrder1 = orders.get(0);
        assertEquals("Shop_order_id", docOrder1.getYandexId());
        assertEquals(String.valueOf(1111L), docOrder1.getPartnerId());
        assertEquals(BigDecimal.valueOf(405), docOrder1.getAssessedCost());
        assertEquals(BigDecimal.valueOf(5.0), docOrder1.getWeight());
        assertEquals(1, docOrder1.getPlacesCount());
    }

    @Test
    @DisplayName("Отсутствие заказов для генерации акта приемки-передачи")
    void testNoOrders() {
        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        prepareEmptyPagedOrdersCheckouterMock();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(path, HttpMethod.GET, Format.JSON,
                        null, byte[].class)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertJsonEquals(EXPECTED_ERROR_MESSAGE, exception.getResponseBodyAsString());
    }

    @Test
    @DisplayName("Ошибка в генерации акта приемки-передачи, когда от чекаутера приходит 0 коробок")
    void testEmptyBoxes() {
        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        prepareCheckouterMockWithEmptyBoxes();
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(path, HttpMethod.GET, Format.JSON,
                        null, byte[].class)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MbiAsserts.assertJsonEquals(EXPECTED_ERROR_MESSAGE_EMPTY_BOXES, exception.getResponseBodyAsString());
    }

    static Stream<Arguments> parameters() {
        return Stream.of(
                arguments(Format.JSON),
                arguments(Format.XML)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @DisplayName("Тест на новый воркфлоу для ручки ААП. Ошибка: отгрузки не найдены.")
    void testNewReceptionActWorkflowNoShipments(Format format) {
        environmentService.setValues(NEW_WORKFLOW_PARTNERS, List.of(String.valueOf(SHOP_CAMPAIGN_ID)));

        mockNesuClientEmpty();

        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(path, HttpMethod.GET, format,
                        null, byte[].class)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        if (format.equals(Format.JSON)) {
            MbiAsserts.assertJsonEquals(EXPECTED_ERROR_MESSAGE_NO_SHIPMENTS, exception.getResponseBodyAsString());
        } else {
            MbiAsserts.assertXmlEquals(EXPECTED_ERROR_MESSAGE_NO_SHIPMENTS_XML, exception.getResponseBodyAsString());
        }

        verifyNesuAndL4s(0, 0);

        environmentService.removeAllValues(NEW_WORKFLOW_PARTNERS);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @DisplayName("Тест на новый воркфлоу для ручки ААП. Ошибка: есть необработанные заказы.")
    void testNewReceptionActWorkflowNonReadyOrders(Format format) {
        environmentService.setValues(NEW_WORKFLOW_PARTNERS, List.of(String.valueOf(SHOP_CAMPAIGN_ID)));

        mockNesuClient(PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION);
        prepareCheckouterMockReceptionTransferAct("checkouter-response-non-ready-orders.json");

        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(path, HttpMethod.GET, format,
                        null, byte[].class)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        if (format.equals(Format.JSON)) {
            MbiAsserts.assertJsonEquals(EXPECTED_ERROR_MESSAGE_ORDERS_NOT_READY, exception.getResponseBodyAsString());
        } else {
            MbiAsserts.assertXmlEquals(EXPECTED_ERROR_MESSAGE_ORDERS_NOT_READY_XML, exception.getResponseBodyAsString());
        }
        verifyNesuAndL4s(1, 0);

        environmentService.removeAllValues(NEW_WORKFLOW_PARTNERS);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @DisplayName("Тест на новый воркфлоу для ручки ААП. Ошибка: есть заказы в процессе переноса.")
    void testNewReceptionActWorkflowOrdersExcluding(Format format) {
        environmentService.setValues(NEW_WORKFLOW_PARTNERS, List.of(String.valueOf(SHOP_CAMPAIGN_ID)));

        mockNesuClient(PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION);
        prepareCheckouterMockReceptionTransferAct("checkouter-response-new.json");
        mockL4sClient();

        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(path, HttpMethod.GET, format,
                        null, byte[].class)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        if (format.equals(Format.JSON)) {
            MbiAsserts.assertJsonEquals(EXPECTED_ERROR_MESSAGE_ORDERS_EXCLUDING, exception.getResponseBodyAsString());
        } else {
            MbiAsserts.assertXmlEquals(EXPECTED_ERROR_MESSAGE_ORDERS_EXCLUDING_XML, exception.getResponseBodyAsString());
        }
        verifyNesuAndL4s(1, 1);
        environmentService.removeAllValues(NEW_WORKFLOW_PARTNERS);
    }

    @ParameterizedTest
    @MethodSource("parameters")
    @DisplayName("Тест на новый воркфлоу для ручки ААП. Ошибка: в отгрузке нет заказов.")
    void testNewReceptionActWorkflowNoOrders(Format format) {
        environmentService.setValues(NEW_WORKFLOW_PARTNERS, List.of(String.valueOf(SHOP_CAMPAIGN_ID)));

        mockNesuClientNoOrders();
        prepareCheckouterMockReceptionTransferAct("checkouter-no-orders-response.json");
        mockL4sClientEmpty();

        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.makeRequest(path, HttpMethod.GET, format,
                        null, byte[].class)
        );
        assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        if (format.equals(Format.JSON)) {
            MbiAsserts.assertJsonEquals(EXPECTED_ERROR_MESSAGE_NO_ORDERS, exception.getResponseBodyAsString());
        } else {
            MbiAsserts.assertXmlEquals(EXPECTED_ERROR_MESSAGE_NO_ORDERS_XML, exception.getResponseBodyAsString());
        }
        verifyNesuAndL4s(1, 0);
        environmentService.removeAllValues(NEW_WORKFLOW_PARTNERS);
    }

    @Test
    @DisplayName("Тест на новый воркфлоу для ручки ААП. Успешное подтверждение и генерация.")
    void testNewReceptionActWorkflowSuccess() {
        environmentService.setValues(NEW_WORKFLOW_PARTNERS, List.of(String.valueOf(SHOP_CAMPAIGN_ID)));

        mockNesuClient(PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION);
        prepareCheckouterMockReceptionTransferAct("checkouter-response-new.json");
        mockL4sClientEmpty();

        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        when(nesuClient.generateAct(anyLong(), anyLong(), anyLong()))
                .thenReturn(PDF_DATA);

        // when
        ResponseEntity<byte[]> response = FunctionalTestHelper.makeRequest(path, HttpMethod.GET, Format.JSON,
                null, byte[].class);

        // then
        assertArrayEquals(PDF_DATA, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verifyNesuAndL4s(1, 1);
        verify(nesuClient, times(1)).confirmShipment(anyLong(), eq(SHOP_ID), eq(SHIPMENT_ID), any());
        verify(nesuClient, times(1)).generateAct(anyLong(), eq(SHOP_ID), eq(SHIPMENT_ID));
        environmentService.removeAllValues(NEW_WORKFLOW_PARTNERS);
    }

    @Test
    @DisplayName("Тест на новый воркфлоу для ручки ААП. Успешная генерация, подтверждение не нужно.")
    void testNewReceptionActWorkflowSuccessNoConfirmation() {
        environmentService.setValues(NEW_WORKFLOW_PARTNERS, List.of(String.valueOf(SHOP_CAMPAIGN_ID)));

        mockNesuClient(PartnerShipmentStatus.FINISHED);
        prepareCheckouterMockReceptionTransferAct("checkouter-response-new.json");
        mockL4sClientEmpty();

        String path = urlBasePrefix + "/campaigns/" + SHOP_CAMPAIGN_ID + "/shipments/reception-transfer-act";
        when(nesuClient.generateAct(anyLong(), anyLong(), anyLong()))
                .thenReturn(PDF_DATA);

        // when
        ResponseEntity<byte[]> response = FunctionalTestHelper.makeRequest(path, HttpMethod.GET, Format.JSON,
                null, byte[].class);

        // then
        assertArrayEquals(PDF_DATA, response.getBody());
        assertEquals(HttpStatus.OK, response.getStatusCode());

        verifyNesuAndL4s(1, 0);
        verify(nesuClient, times(0)).confirmShipment(anyLong(), eq(SHOP_ID), eq(SHIPMENT_ID), any());
        verify(nesuClient, times(1)).generateAct(anyLong(), eq(SHOP_ID), eq(SHIPMENT_ID));
        environmentService.removeAllValues(NEW_WORKFLOW_PARTNERS);
    }

    private void verifyNesuAndL4s(int getShipmentTimes, int searchExcludeOrdersTimes) {
        verify(nesuClient, times(1)).searchPartnerShipments(anyLong(), eq(SHOP_ID), any(), any());
        verify(nesuClient, times(getShipmentTimes)).getShipment(anyLong(), eq(SHOP_ID), eq(SHIPMENT_ID));
        verify(excludeOrderFromShipmentApi, times(searchExcludeOrdersTimes)).searchExcludeOrderRequests(
                eq(new ExcludeOrderRequestsSearchRequest()
                        .shipmentIds(List.of(SHIPMENT_ID))
                        .statuses(List.of(ExcludeOrderFromShipmentRequestStatus.PROCESSING))));
    }

    private void mockNesuClientNoOrders() {
        when(nesuClient.searchPartnerShipments(anyLong(), anyLong(), any(), eq(new PageRequest(0, PAGE_SIZE))))
                .thenReturn(new Page<PartnerShipmentSearchDto>().setPage(0).setSize(1).setData(
                        List.of(PartnerShipmentSearchDto.builder()
                                .id(SHIPMENT_ID)
                                .status(PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION)
                                .build())));
        when(nesuClient.searchPartnerShipments(anyLong(), anyLong(), any(), eq(new PageRequest(1, PAGE_SIZE))))
                .thenReturn(new Page<PartnerShipmentSearchDto>().setPage(0).setSize(0));
        when(nesuClient.getShipment(anyLong(), anyLong(), anyLong()))
                .thenReturn(PartnerShipmentDto.builder()
                        .id(SHIPMENT_ID)
                        .currentStatus(PartnerShipmentStatusChange.builder()
                                .code(PartnerShipmentStatus.OUTBOUND_READY_FOR_CONFIRMATION).build())
                        .orderIds(List.of())
                        .build());
    }

    private void mockNesuClient(PartnerShipmentStatus status) {
        when(nesuClient.searchPartnerShipments(anyLong(), anyLong(), any(), any()))
                .thenReturn(new Page<PartnerShipmentSearchDto>().setPage(0).setSize(2).setData(List.of(
                        PartnerShipmentSearchDto.builder()
                                .id(99999L)
                                .status(status)
                                .planIntervalFrom(LocalDateTime.of(2022, Month.MAY, 1, 12, 0))
                                .build(),
                        PartnerShipmentSearchDto.builder()
                                .id(SHIPMENT_ID)
                                .status(status)
                                .planIntervalFrom(LocalDateTime.of(2022, Month.MAY, 2, 12, 0))
                                .build())));
        when(nesuClient.getShipment(anyLong(), anyLong(), anyLong()))
                .thenReturn(PartnerShipmentDto.builder()
                        .id(SHIPMENT_ID)
                        .currentStatus(PartnerShipmentStatusChange.builder()
                                .code(status).build())
                        .orderIds(ORDER_IDS)
                        .build());
    }

    private void mockNesuClientEmpty() {
        when(nesuClient.searchPartnerShipments(anyLong(), anyLong(), any(), eq(new PageRequest(0, PAGE_SIZE))))
                .thenReturn(new Page<PartnerShipmentSearchDto>().setPage(0).setSize(0).setData(List.of()));
    }

    private void mockL4sClient() {
        when(excludeOrderFromShipmentApi.searchExcludeOrderRequests(any()))
                .thenReturn(new ExcludeOrderRequestListDto()
                        .addExcludeOrderFromShipmentRequestsItem(
                                new ExcludeOrderFromShipmentRequestDto().orderId(1113L)));
    }

    private void mockL4sClientEmpty() {
        when(excludeOrderFromShipmentApi.searchExcludeOrderRequests(any()))
                .thenReturn(new ExcludeOrderRequestListDto());
    }

    private void mockMarketId() {
        MarketIdPartner partner = MarketIdPartner.newBuilder()
                .setPartnerId(668L)
                .setPartnerType(CampaignType.SUPPLIER.getId())
                .build();
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder().setPartner(partner).build();
        // mock ответа MarketID на запрос о ид поставщика
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(LEGAL_NAME)
                            .setType("OOO")
                            .build()
            ).build();
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request), any());

        MarketIdPartner partnerDelivery = MarketIdPartner.newBuilder()
                .setPartnerId(48L)
                .setPartnerType(CampaignType.DELIVERY.getId())
                .build();
        GetByPartnerRequest request1 = GetByPartnerRequest.newBuilder().setPartner(partnerDelivery).build();
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount1 = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(DELIVERY_NAME)
                            .setType("OOO")
                            .build()
            ).build();
            GetByPartnerResponse response1 = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount1).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response1);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request1), any());
    }

    private void prepareCheckouterMock() {
        String responseAsString = resourceAsString("checkouter-response.json");
        LocalDateTime today = LocalDateTime.now();
        String dateToReplace = responseAsString.replace("date_to_replace", today.toString());
        MockRestServiceServer server = MockRestServiceServer.createServer(checkouterRestTemplate);
        setExpectedResponse(server,
                "\"from\":1,\"to\":50,\"pageSize\":50", dateToReplace);
        setExpectedResponse(server, "\"from\":51,\"to\":100,\"pageSize\":50",
                resourceAsString("checkouter-no-orders-response.json"));
    }

    private void prepareCheckouterMockReceptionTransferAct(String response) {
        String responseAsString = resourceAsString(response);
        LocalDateTime today = LocalDateTime.now();
        String dateToReplace = responseAsString.replace("date_to_replace", today.toString());
        MockRestServiceServer server = MockRestServiceServer.createServer(checkouterRestTemplate);
        setExpectedResponseReceptionTransferAct(server, dateToReplace);
        setExpectedResponseReceptionTransferAct(server, resourceAsString("checkouter-no-orders-response.json"));
    }

    private void prepareCheckouterMockWithEmptyBoxes() {
        String responseAsString = resourceAsString("checkouter-response-empty-boxes.json");
        LocalDateTime today = LocalDateTime.now();
        String dateToReplace = responseAsString.replace("date_to_replace", today.toString());
        MockRestServiceServer server = MockRestServiceServer.createServer(checkouterRestTemplate);
        setExpectedResponse(
                server, "\"from\":1,\"to\":50,\"pageSize\":50", dateToReplace);
        setExpectedResponse(server, "\"from\":51,\"to\":100,\"pageSize\":50",
                resourceAsString("checkouter-no-orders-response.json"));
    }

    private void setExpectedResponse(MockRestServiceServer server, String expectedPartOfRequest, String expectedBody) {
        server
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo(checkouterUrl + "/get-orders?clientRole=SHOP&clientId=" + SHOP_ID))
                .andExpect(content().string(Matchers.containsString(expectedPartOfRequest)))
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedBody)
                );
    }

    private void setExpectedResponseReceptionTransferAct(
            MockRestServiceServer server, String expectedBody) {
        server
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo(checkouterUrl
                        + "/get-orders?clientRole=SHOP&clientId="
                        + SHOP_ID + "&shopId=&archived=false"))
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(expectedBody)
                );
    }

    private void prepareEmptyPagedOrdersCheckouterMock() {
        MockRestServiceServer.createServer(checkouterRestTemplate)
                .expect(method(HttpMethod.POST))
                .andExpect(requestTo(checkouterUrl + "/get-orders?clientRole=SHOP&clientId=" + SHOP_ID))
                .andRespond(
                        withSuccess()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(resourceAsString("checkouter-no-orders-response.json"))
                );
    }

}
