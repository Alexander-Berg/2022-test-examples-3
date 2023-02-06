package ru.yandex.market.partner.mvc.controller.delivery;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.grpc.stub.StreamObserver;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.delivery.RecipientPerson;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.communication.proxy.client.CommunicationProxyClient;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectRequest;
import ru.yandex.market.communication.proxy.client.model.CreateRedirectResponse;
import ru.yandex.market.communication.proxy.exception.CommunicationProxyClientException;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.delivery.label.metrics.LabelGenerationProtoLBEvent;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.id.GetByPartnerRequest;
import ru.yandex.market.id.GetByPartnerResponse;
import ru.yandex.market.id.LegalInfo;
import ru.yandex.market.id.MarketAccount;
import ru.yandex.market.id.MarketIdPartner;
import ru.yandex.market.id.MarketIdServiceGrpc;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.personal_market.PersonalMarketService;
import ru.yandex.market.personal_market.PersonalRetrieveResponse;
import ru.yandex.market.personal_market.client.model.CommonType;
import ru.yandex.market.personal_market.client.model.CommonTypeEnum;
import ru.yandex.market.personal_market.client.model.FullName;
import ru.yandex.market.personal_market.client.model.MultiTypeRetrieveResponseItem;
import ru.yandex.market.report.PdfTestUtil;
import ru.yandex.market.yt.label.event.LabelGenerateInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.partner.mvc.controller.util.ResponseJsonUtil.getResult;

/**
 * Тесты для {@link OrderDeliveryController}.
 */
class OrderDeliveryControllerTest extends FunctionalTest {

    @Autowired
    private CheckouterClient checkouterClient;

    @Autowired
    private MarketIdServiceGrpc.MarketIdServiceImplBase marketIdServiceImplBase;

    @Autowired
    private CommunicationProxyClient communicationProxyClient;

    @Autowired
    private LogbrokerEventPublisher<LabelGenerationProtoLBEvent> logbrokerLabelGenerateEventPublisher;

    @Autowired
    private TestableClock clock;

    @Autowired
    private PersonalMarketService personalMarketService;

    @BeforeEach
    void prepareMocks() {
        mockMarketId(48L, "ООО PickPoint");
        when(logbrokerLabelGenerateEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
        mockPersonal();
    }

    private void mockPersonal() {

        var item1 = new MultiTypeRetrieveResponseItem();
        item1.setId("personalFullNameId");
        item1.setType(CommonTypeEnum.FULL_NAME);

        {
            var fullNameType = new CommonType();
            var fullName = new FullName();
            fullName.setForename("Имя");
            fullName.setSurname("Фамилия");
            fullName.setPatronymic("Отчество");
            fullNameType.setFullName(fullName);
            item1.setValue(fullNameType);
        }

        var item2 = new MultiTypeRetrieveResponseItem();
        item2.setId("personalFullname22");
        item2.setType(CommonTypeEnum.FULL_NAME);

        {
            var fullNameType = new CommonType();
            var fullName = new FullName();
            fullName.setForename("Сидр");
            fullName.setSurname("Сидоров");
            fullName.setPatronymic("Сидорович");
            fullNameType.setFullName(fullName);
            item2.setValue(fullNameType);
        }

        when(personalMarketService.retrieve(any()))
                .thenReturn(CompletableFuture.completedFuture(new PersonalRetrieveResponse(
                        List.of(item1, item2)
                )));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void testGetProxyPhoneSuccess() {
        Long orderId = 12345943L;
        Long partnerId = 101L;
        String proxyNumber = "+79011231212";

        when(communicationProxyClient.createRedirect(new CreateRedirectRequest().orderId(orderId).partnerId(partnerId)))
                .thenReturn(new CreateRedirectResponse().proxyNumber(proxyNumber));

        ResponseEntity<String> response = getPhone(201, orderId);
        String expectedJson = "{\"phone\":" + proxyNumber + "}}";
        MbiAsserts.assertJsonEquals(expectedJson, getResult(response));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void testGetProxyPhoneOrderNotFound() {
        Long orderId = 12345943L;
        Long partnerId = 101L;

        when(communicationProxyClient.createRedirect(new CreateRedirectRequest().orderId(orderId).partnerId(partnerId)))
                .thenThrow(new CommunicationProxyClientException("Order not found", HttpStatus.NOT_FOUND.value()));

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getPhone(201, orderId)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void testGetRealPhone() {
        Long orderId = 12345943L;
        Long partnerId = 101L;
        String realNumberId = "phone_09hn9de123hqec2665622e70falf4mll";
        String realNumber = "+79998887711";

        when(communicationProxyClient.createRedirect(new CreateRedirectRequest().orderId(orderId).partnerId(partnerId)))
                .thenReturn(new CreateRedirectResponse().proxyNumber(null).realNumberId(realNumberId));

        when(personalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(
                        PersonalRetrieveResponse.builder().phone(realNumberId, realNumber).build()
                )
        );

        ResponseEntity<String> response = getPhone(201, orderId);
        String expectedJson = "{\"phone\":" + realNumber + "}}";
        MbiAsserts.assertJsonEquals(expectedJson, getResult(response));
    }


    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void testFailedToGetRealPhone() {
        Long orderId = 12345943L;
        Long partnerId = 101L;
        String realNumberId = "phone_09hn9de123hqec2665622e70falf4mll";

        when(communicationProxyClient.createRedirect(new CreateRedirectRequest().orderId(orderId).partnerId(partnerId)))
                .thenReturn(new CreateRedirectResponse().proxyNumber(null).realNumberId(realNumberId));

        when(personalMarketService.retrieve(any())).thenReturn(
                CompletableFuture.completedFuture(
                        PersonalRetrieveResponse.builder().build()
                )
        );

        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> getPhone(201, orderId)
        );
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void testNoRealPhoneId() {
        Long orderId = 12345943L;
        Long partnerId = 101L;

        when(communicationProxyClient.createRedirect(new CreateRedirectRequest().orderId(orderId).partnerId(partnerId)))
                .thenReturn(new CreateRedirectResponse().proxyNumber(null).realNumberId(null));

        HttpServerErrorException exception = Assertions.assertThrows(
                HttpServerErrorException.class,
                () -> getPhone(201, orderId)
        );
        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatusCode());
        verify(personalMarketService, never()).retrieve(any());
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabel() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock(12345943, "shop_12345943");
        doGetPdf(201, 12345943, "EXT123454152");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(12345943L)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_box_fulfilmentId_generateLabel")
                .setDownloadType(LabelGenerateInfo.DownloadType.PI)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(201L)
                .setUid(0L)
                .setPartnerId(101L)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabel_shopOrderIdWithIncorrectSymbols() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock(12345943, "Д123212");
        doGetPdf(201, 12345943,
                "EXT123454152", 1,
                "OrderDeliveryControllerTest.invalidSymbols.labels.pdf.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(12345943L)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_box_fulfilmentId_generateLabel")
                .setDownloadType(LabelGenerateInfo.DownloadType.PI)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(201L)
                .setUid(0L)
                .setPartnerId(101L)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabelWithFulfillment() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterFulfilmentWarehouseMock(12345943, "shop_12345943");
        doGetPdf(201, 12345943, "EXT123454152", 1,
                "OrderDeliveryControllerTest_withFulfilmentWarehouse.pdf.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(12345943L)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_box_fulfilmentId_generateLabel")
                .setDownloadType(LabelGenerateInfo.DownloadType.PI)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(201L)
                .setUid(0L)
                .setPartnerId(101L)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.shop.csv")
    void generateShopLabelWithFulfillment() throws IOException {
        prepareCheckouterFulfilmentWarehouseMock(12345943, "shop_12345943");
        doGetPdf(201, 12345943, "EXT123454152", 1,
                "OrderDeliveryControllerTest_withFulfilmentWarehouse.pdf.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(12345943L)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_box_fulfilmentId_generateLabel")
                .setDownloadType(LabelGenerateInfo.DownloadType.PI)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(201L)
                .setUid(0L)
                .setPartnerId(101L)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabelOrderIsArchived() {
        when(checkouterClient.getOrder(anyLong(), ArgumentMatchers.any(ClientRole.class), anyLong()))
                .thenThrow(new OrderNotFoundException(12345943));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(201, 12345943, "EXT123454152")
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(
                "[{\"code\":\"BAD_PARAM\",\"details\":" +
                        "{\"reason\":\"Order id 12345943 is in terminal status\"," +
                        "\"field\":\"orderId\",\"subcode\":\"INVALID\"}}]",
                exception.getResponseBodyAsString());
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabelFulfilmentIdNotFound() {
        prepareCheckouterMock(12345943, null);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(201, 12345943, "EXT123454153")
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.NOT_FOUND)
        );
        JsonTestUtil.assertResponseErrorMessage(
                "[{\"code\":\"BAD_PARAM\",\"details\":{\"reason\":\"Fulfilment id EXT123454153 not found\"," +
                        "\"field\":\"fulfilmentId\",\"subcode\":\"INVALID\"}}]",
                exception.getResponseBodyAsString());
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabels() throws IOException {
        prepareCheckouterMock(12345943, null);
        doGetPdf(201, 12345943, null, 3, "OrderDeliveryControllerTest.labels.pdf.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(12345943L)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_box_generateLabel")
                .setDownloadType(LabelGenerateInfo.DownloadType.PI)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(201L)
                .setUid(0L)
                .setPartnerId(101L)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabelsOrderIsArchived() {
        when(checkouterClient.getOrder(anyLong(), ArgumentMatchers.any(ClientRole.class), anyLong()))
                .thenThrow(new OrderNotFoundException(12345943));
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(201, 12345943, null)
        );
        MatcherAssert.assertThat(
                exception,
                HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        JsonTestUtil.assertResponseErrorMessage(
                "[{\"code\":\"BAD_PARAM\",\"details\":" +
                        "{\"reason\":\"Order id 12345943 is in terminal status\"," +
                        "\"field\":\"orderId\",\"subcode\":\"INVALID\"}}]",
                exception.getResponseBodyAsString());
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    @DbUnitDataSet(before = "OrderDeliveryControllerTest.csv")
    void generateLabelWithErrorSymbols() throws IOException {
        prepareCheckouterWithErrorSymbolsMock(12345943, "shop_12345943");
        doGetPdf(201, 12345943, "EXT123454152");

        prepareCheckouterWithErrorSymbolsMock(12345943, "shop_12345943");
        doGetPdf(201, 12345943, "EXT\u00A0123454152");
    }

    private ResponseEntity<String> getPhone(long campaignId, long orderId) {
        return FunctionalTestHelper.get(
                baseUrl + "/campaigns/" +
                        campaignId +
                        "/orders/" +
                        orderId +
                        "/delivery/phone");
    }

    private ResponseEntity<byte[]> generateLabel(long campaignId, long orderId, String fulfilmentId) {
        String path = baseUrl + "/campaigns/" + campaignId + "/orders/" + orderId + "/delivery/box";
        if (fulfilmentId != null) {
            path += "/" + fulfilmentId;
        }
        path += "/generateLabel";
        return FunctionalTestHelper.get(path, byte[].class);
    }

    private ResponseEntity<byte[]> generateLabel(long campaignId, long orderId, long boxId) {
        String path = baseUrl + "/campaigns/" + campaignId + "/orders/" + orderId +
                "/delivery/shipments/1/box/" + boxId + "/label";
        return FunctionalTestHelper.get(path, byte[].class);
    }

    private ResponseEntity<byte[]> doGetPdf(long campaignId, long orderId, String fulfilmentId) throws IOException {
        return doGetPdf(campaignId, orderId, fulfilmentId, 1, "OrderDeliveryControllerTest.pdf.txt");
    }

    private ResponseEntity<byte[]> doGetPdf(long campaignId,
                                            long orderId,
                                            String fulfilmentId,
                                            int numberOfPages,
                                            String fileName) throws IOException {

        ResponseEntity<byte[]> response = generateLabel(campaignId, orderId, fulfilmentId);
        assertThat(response.getHeaders().getContentType().getSubtype(), equalTo("pdf"));
        assertThat(response.getBody().length, greaterThan(0));
        PdfTestUtil.assertPdfTextEqualsFile(
                response.getBody(), IOUtils.toString(
                        getClass().getResourceAsStream(fileName),
                        Charset.defaultCharset()
                ),
                numberOfPages);
        return response;
    }

    private ResponseEntity<byte[]> doGetPdf(long campaignId,
                                            long orderId,
                                            long boxId,
                                            int numberOfPages,
                                            String fileName) throws IOException {

        ResponseEntity<byte[]> response = generateLabel(campaignId, orderId, boxId);
        assertThat(response.getHeaders().getContentType().getSubtype(), equalTo("pdf"));
        assertThat(response.getBody().length, greaterThan(0));
        PdfTestUtil.assertPdfTextEqualsFile(
                response.getBody(), IOUtils.toString(
                        getClass().getResourceAsStream(fileName),
                        Charset.defaultCharset()
                ),
                numberOfPages);
        return response;
    }

    private void prepareCheckouterMock(long orderId, String shopId) {
        Order order = createOrder(orderId, shopId, false);

        when(checkouterClient.getOrder(eq(orderId), eq(ClientRole.SHOP), eq(101L))).thenReturn(order);
    }

    private static Order createOrder(long orderId, String shopId, boolean fake) {
        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setId(1L);
        parcelBox1.setWeight(500L);
        parcelBox1.setFulfilmentId("FF100");

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setId(2L);
        parcelBox2.setWeight(10500L);
        parcelBox2.setFulfilmentId("EXT123454152");

        ParcelBox parcelBox3 = new ParcelBox();
        parcelBox3.setId(3L);
        parcelBox3.setWeight(100000L);
        parcelBox3.setFulfilmentId("100");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");

        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setBoxes(Arrays.asList(parcelBox2, parcelBox3, parcelBox1));

        parcel.setShipmentDate(LocalDate.parse("2022-01-11"));

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(48L);
        delivery.setParcels(Collections.singletonList(parcel));
        delivery.setRecipient(createRecipient());

        delivery.setDeliveryDates(getDeliveryDates(dateFormat));


        Buyer buyer = new Buyer();
        buyer.setPhone("+78005553535");

        Order order = new Order();
        order.setId(orderId);
        order.setShopOrderId(shopId);
        order.setBuyer(buyer);
        order.setDelivery(delivery);
        order.setFake(fake);
        return order;
    }

    private void prepareCheckouterFulfilmentWarehouseMock(long orderId, String shopId) {
        OrderItem item = new OrderItem();
        item.setFulfilmentWarehouseId(4L);

        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setId(1L);
        parcelBox1.setWeight(500L);
        parcelBox1.setFulfilmentId("FF100");

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setId(2L);
        parcelBox2.setWeight(10500L);
        parcelBox2.setFulfilmentId("EXT123454152");

        ParcelBox parcelBox3 = new ParcelBox();
        parcelBox3.setId(3L);
        parcelBox3.setWeight(100000L);
        parcelBox3.setFulfilmentId("100");

        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setBoxes(Arrays.asList(parcelBox2, parcelBox3, parcelBox1));
        parcel.setShipmentDate(LocalDate.parse("2022-01-11"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(48L);
        delivery.setParcels(Collections.singletonList(parcel));
        delivery.setRecipient(createRecipient());

        delivery.setDeliveryDates(getDeliveryDates(dateFormat));

        Order order = new Order();
        order.setId(orderId);
        order.setShopOrderId(shopId);
        order.setItems(List.of(item));
        order.setDelivery(delivery);

        when(checkouterClient.getOrder(eq(orderId), eq(ClientRole.SHOP), eq(101L))).thenReturn(order);
        mockMarketId(4L, "ООО Сдэк");
    }

    private void prepareCheckouterWithErrorSymbolsMock(long orderId, String shopId) {
        ParcelBox parcelBox1 = new ParcelBox();
        parcelBox1.setId(1L);
        parcelBox1.setWeight(500L);
        parcelBox1.setFulfilmentId("FF\u00A0100");

        ParcelBox parcelBox2 = new ParcelBox();
        parcelBox2.setId(2L);
        parcelBox2.setWeight(10500L);
        parcelBox2.setFulfilmentId("EXT\u00A0123454152");

        ParcelBox parcelBox3 = new ParcelBox();
        parcelBox3.setId(3L);
        parcelBox3.setWeight(100000L);
        parcelBox3.setFulfilmentId("1\u00A000");

        Parcel parcel = new Parcel();
        parcel.setId(1L);
        parcel.setBoxes(Arrays.asList(parcelBox2, parcelBox3, parcelBox1));
        parcel.setShipmentDate(LocalDate.parse("2022-01-11"));

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");

        Delivery delivery = new Delivery();
        delivery.setDeliveryServiceId(48L);
        delivery.setParcels(Collections.singletonList(parcel));
        delivery.setRecipient(createRecipient());

        delivery.setDeliveryDates(getDeliveryDates(dateFormat));

        Order order = new Order();
        order.setId(orderId);
        order.setShopOrderId(shopId);
        order.setDelivery(delivery);

        when(checkouterClient.getOrder(eq(orderId), eq(ClientRole.SHOP), eq(101L))).thenReturn(order);
        mockMarketId(48L, "ООО PickPoint");
    }

    private static DeliveryDates getDeliveryDates(SimpleDateFormat dateFormat) throws RuntimeException {
        try {
            return new DeliveryDates(
                    dateFormat.parse("2022.01.11"),
                    dateFormat.parse("2022.01.12")
            );
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private static Recipient createRecipient() {
        return new Recipient(
                new RecipientPerson("Сидр", "Сидорович", "Сидоров"),
                "personalFullname22",
                "+7 666-555-4433", null, "sidorov@ya.ru", null
        );
    }

    private void mockMarketId(long partnerId, String legalName) {
        MarketIdPartner partner = MarketIdPartner.newBuilder()
                .setPartnerId(partnerId)
                .setPartnerType(CampaignType.YADELIVERY.getId())
                .build();
        GetByPartnerRequest request = GetByPartnerRequest.newBuilder().setPartner(partner).build();
        // mock ответа MarketID на запрос о ид поставщика
        doAnswer(invocation -> {
            StreamObserver<GetByPartnerResponse> marketAccountStreamObserver = invocation.getArgument(1);
            MarketAccount marketAccount = MarketAccount.newBuilder().setLegalInfo(
                    LegalInfo.newBuilder()
                            .setLegalName(legalName)
                            .build()
            ).build();
            GetByPartnerResponse response = GetByPartnerResponse.newBuilder()
                    .setMarketId(marketAccount).setSuccess(true).build();

            marketAccountStreamObserver.onNext(response);
            marketAccountStreamObserver.onCompleted();
            return null;
        }).when(marketIdServiceImplBase).getByPartner(eq(request), any());
    }
}
