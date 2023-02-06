package ru.yandex.market.api.partner.controllers.order.v2.labels;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.order.v2.OrderControllerV2TestTemplate;
import ru.yandex.market.api.partner.controllers.util.checkouter.CheckouterMockHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.delivery.label.metrics.LabelGenerationProtoLBEvent;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.report.PdfTestUtil;
import ru.yandex.market.yt.label.event.LabelGenerateInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.matchers.HttpClientErrorMatcher.hasErrorCode;

/**
 * Тесты генерации ярлыков (/delivery/labels)
 */
@DbUnitDataSet(before = "../OrderControllerTest.before.csv")
class OrderLabelsTest extends OrderControllerV2TestTemplate {

    @BeforeEach
    void setUp() {
        checkouterMockHelper = new CheckouterMockHelper(
                checkouterRestTemplate,
                checkouterUrl
        );
        when(logbrokerLabelGenerateEventPublisher.publishEventAsync(any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(invocation.getArgument(0)));
    }

    @Test
    void generateLabel() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock("mocks/checkouter/get_order_boxes.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        doGetPdf(CAMPAIGN_ID, ORDER_ID, 665364L, 336311L, 1, "expected/single_label.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_shipments_parcelId_boxes_boxId_label")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(CAMPAIGN_ID)
                .setUid(67282295L)
                .setPartnerId(PARTNER_ID)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    void generateLabelExpress() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock("mocks/checkouter/get_order_boxes_express.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        doGetPdf(CAMPAIGN_ID, ORDER_ID, 665364L, 336311L, 1, "expected/single_label_express.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_shipments_parcelId_boxes_boxId_label")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(CAMPAIGN_ID)
                .setUid(67282295L)
                .setPartnerId(PARTNER_ID)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    void generateLabelShipmentDateBySupplier() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock("mocks/checkouter/get_order_boxes_shipment_date_by_supplier.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        doGetPdf(CAMPAIGN_ID, ORDER_ID, 665364L, 336311L, 1, "expected/single_label_shipment_date_by_supplier.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_shipments_parcelId_boxes_boxId_label")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(CAMPAIGN_ID)
                .setUid(67282295L)
                .setPartnerId(PARTNER_ID)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    void generateLabelWithFulfilmentWarehouse() throws IOException {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock("mocks/checkouter/get_order_with_fulfilment_warehouse.json", ORDER_ID, CLIENT_ID);
        mockMarketId(4L, "Стриж");
        doGetPdf(CAMPAIGN_ID, ORDER_ID, 11397171L, 18109128L, 1,
                "expected/label_with_fulfilment_warehouse.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_shipments_parcelId_boxes_boxId_label")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(CAMPAIGN_ID)
                .setUid(67282295L)
                .setPartnerId(PARTNER_ID)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @Test
    void generateLabelOrderNotFoundJson() {
        prepareNotFoundCheckouterMock(ORDER_ID, CLIENT_ID);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(CAMPAIGN_ID, ORDER_ID, 1L, 2L, Format.JSON)
        );
        assertThat(
                exception,
                hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertJsonEquals(
                //language=json
                "{\"errors\":[{\"code\":\"NOT_FOUND\"," +
                        "\"message\":\"Order not found: 1\"}]," +
                        "\"status\":\"ERROR\"}",
                exception.getResponseBodyAsString()
        );
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    void generateLabelOrderNotFoundXml() {
        prepareNotFoundCheckouterMock(ORDER_ID, CLIENT_ID);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(CAMPAIGN_ID, ORDER_ID, 1L, 2L, Format.XML)
        );
        assertThat(
                exception,
                hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertXmlEquals(
                //language=xml
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<response><status>ERROR</status><errors><error " +
                        "code=\"NOT_FOUND\" message=\"Order not found: 1\"/></errors></response>",
                exception.getResponseBodyAsString()
        );
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    void generateLabelBoxNotFound() {
        prepareCheckouterMock("mocks/checkouter/get_order_boxes.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "PickPoint");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(CAMPAIGN_ID, ORDER_ID, 2L, 3L, Format.JSON)
        );
        assertThat(
                exception,
                hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertJsonEquals(
                "{\"status\":\"ERROR\",\"errors\":[{\"code\":\"NOT_FOUND\",\"message\":\"Box not found. Parcel: 2. " +
                        "Box: 3\"}]}",
                exception.getResponseBodyAsString());
    }

    @Test
    void generateLabels() throws IOException {
        prepareCheckouterMock("mocks/checkouter/get_order_boxes.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        doGetPdf(CAMPAIGN_ID, ORDER_ID, null, null, 3, "expected/labels_boxes.txt");
    }

    @Test
    void generateLabelsOrderNotFound() {
        prepareNotFoundCheckouterMock(ORDER_ID, CLIENT_ID);
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(CAMPAIGN_ID, ORDER_ID, null, null, Format.JSON)
        );
        assertThat(
                exception,
                hasErrorCode(HttpStatus.NOT_FOUND)
        );
        MbiAsserts.assertJsonEquals(
                //language=json
                "{\"errors\":[{\"code\":\"NOT_FOUND\"," +
                        "\"message\":\"Order not found: 1\"}],\"status\":\"ERROR\"}",
                exception.getResponseBodyAsString()
        );
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    void generateLabelArchivedOrderJson() {
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> generateLabel(CAMPAIGN_ID_WITH_ARCHIVED_ORDER, ARCHIVED_ORDER_ID,
                        1L, 2L, Format.JSON)
        );
        assertThat(
                exception,
                hasErrorCode(HttpStatus.BAD_REQUEST)
        );
        MbiAsserts.assertJsonEquals(
                //language=json
                "{\"errors\":[{\"code\":\"BAD_REQUEST\"," +
                        "\"message\":\"Order id 130 is in terminal status\"}]," +
                        "\"status\":\"ERROR\"}",
                exception.getResponseBodyAsString()
        );
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }


    @Test
    void checkNoBoxesFulfillmentId() {
        prepareCheckouterMock("mocks/checkouter/get_order_boxes_no_fulfillment_id.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> doGetPdf(CAMPAIGN_ID, ORDER_ID, 665364L, 336311L, 1, "expected/single_label.txt")
        );
        //language=json
        JsonTestUtil.assertEquals("" +
                        "{\n" +
                        "  \"status\": \"ERROR\",\n" +
                        "  \"errors\": [\n" +
                        "    {\n" +
                        "      \"code\": \"BAD_REQUEST\",\n" +
                        "      \"message\": \"Boxes 336310, 336311 from order 2827258 don't have fulfillmentId\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                exception.getResponseBodyAsString()
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    @Test
    void checkNoFulfillmentIdOneBox() throws Exception {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock("mocks/checkouter/get_order_box_no_fulfillment_id.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        doGetPdf(CAMPAIGN_ID, ORDER_ID, 665364L, 336310L, 1, "expected/label_with_generated_fulfillment_id.txt");
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_shipments_parcelId_boxes_boxId_label")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(CAMPAIGN_ID)
                .setUid(67282295L)
                .setPartnerId(PARTNER_ID)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @ParameterizedTest
    @MethodSource("argumentsForGetLabelsTest")
    void testGetDataForLabelsGeneration_successful(
            long partnerId, long campaignId, Format format, String expectedContentFilePath, String checkouterOrder) {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock(checkouterOrder, ORDER_ID, partnerId);
        mockMarketId(151, "Стриж");
        var response = getLabelsData(campaignId, ORDER_ID, format);
        assertResponse(response.getBody(), expectedContentFilePath, format);
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_labels_data")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(campaignId)
                .setUid(67282295L)
                .setPartnerId(partnerId)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @ParameterizedTest
    @MethodSource("argumentsForGetLabelsWithoutAddressTest")
    void testGetDataForLabelsGeneration_withoutAddress(Format format, String expectedContentFilePath) {
        clock.setFixed(DateTimes.toInstant(2022, 2, 21), DateTimes.MOSCOW_TIME_ZONE);
        prepareCheckouterMock("mocks/checkouter/get_order_boxes_without_address.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");
        var response = getLabelsData(CAMPAIGN_ID, ORDER_ID, format);
        assertResponse(response.getBody(), expectedContentFilePath, format);
        var event = LabelGenerateInfo.LabelGenerateInfoEvent.newBuilder()
                .setOrderId(ORDER_ID)
                .setMethod("campaigns_campaignId_orders_orderId_delivery_labels_data")
                .setDownloadType(LabelGenerateInfo.DownloadType.API)
                .setDownloadTime("2022-02-21T00:00:00")
                .setCampaignId(CAMPAIGN_ID)
                .setUid(67282295L)
                .setPartnerId(PARTNER_ID)
                .setTimestamp(1645390800000L)
                .build();
        verify(logbrokerLabelGenerateEventPublisher).publishEventAsync(eq(new LabelGenerationProtoLBEvent(event)));
    }

    @ParameterizedTest
    @MethodSource("argumentsForGetLabelsFailureTest")
    void testGetDataForLabelsGeneration_orderNotFound(Format format, String expectedContentFilePath) {
        prepareNotFoundCheckouterMock(ORDER_ID, CLIENT_ID);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getLabelsData(CAMPAIGN_ID, ORDER_ID, format)
        );

        assertThat(exception, hasErrorCode(HttpStatus.NOT_FOUND));

        assertResponse(exception.getResponseBodyAsString(), expectedContentFilePath, format);
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    static Stream<Arguments> argumentsForGetLabelsFailureTest() {
        return Stream.of(
                Arguments.of(Format.JSON, "expected/get-order-labels-order-not-found.json"),
                Arguments.of(Format.XML, "expected/get-order-labels-order-not-found.xml")
        );
    }

    static Stream<Arguments> argumentsForGetLabelsTest() {
        return Stream.of(
                Arguments.of(
                        PARTNER_ID,
                        CAMPAIGN_ID,
                        Format.JSON,
                        "expected/get-order-labels-successful.json",
                        "mocks/checkouter/get_order_boxes.json"
                ),
                Arguments.of(
                        PARTNER_ID,
                        CAMPAIGN_ID,
                        Format.XML,
                        "expected/get-order-labels-successful.xml",
                        "mocks/checkouter/get_order_boxes.json"
                ),
                Arguments.of(
                        DROPSHIP_BY_SELLER_PARTNER_ID,
                        DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                        Format.JSON,
                        "expected/get-dbs-order-labels-successful.json",
                        "mocks/checkouter/get_dbs_order_boxes.json"
                ),
                Arguments.of(
                        DROPSHIP_BY_SELLER_PARTNER_ID,
                        DROPSHIP_BY_SELLER_CAMPAIGN_ID,
                        Format.XML,
                        "expected/get-dbs-order-labels-successful.xml",
                        "mocks/checkouter/get_dbs_order_boxes.json"
                )
        );
    }

    static Stream<Arguments> argumentsForGetLabelsWithoutAddressTest() {
        return Stream.of(
                Arguments.of(Format.JSON, "expected/get-order-labels-without-address.json"),
                Arguments.of(Format.XML, "expected/get-order-labels-without-address.xml")
        );
    }

    @Test
    void testGetDataForLabelsGeneration_noBoxes() {
        prepareCheckouterMock("mocks/checkouter/get_order_box_no_boxes.json", ORDER_ID, CLIENT_ID);
        mockMarketId(151, "Стриж");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> getLabelsData(CAMPAIGN_ID, ORDER_ID, Format.JSON)
        );

        //language=json
        JsonTestUtil.assertEquals("" +
                        "{\n" +
                        "  \"status\": \"ERROR\",\n" +
                        "  \"errors\": [\n" +
                        "    {\n" +
                        "      \"code\": \"BAD_REQUEST\",\n" +
                        "      \"message\": \"Parcel in order 2827258 doesn't have boxes\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}",
                exception.getResponseBodyAsString()
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(logbrokerLabelGenerateEventPublisher, never()).publishEventAsync(any());
    }

    private ResponseEntity<byte[]> generateLabel(long campaignId,
                                                 long orderId,
                                                 Long parcelId,
                                                 Long boxId,
                                                 Format format) {
        String path = urlBasePrefix + "/campaigns/" + campaignId + "/orders/" + orderId +
                "/delivery/";
        if (parcelId != null && boxId != null) {
            path += "shipments/" + parcelId + "/boxes/" + boxId + "/label";
        } else {
            path += "/labels";
        }
        return FunctionalTestHelper.makeRequest(path, HttpMethod.GET, format, null, byte[].class);
    }

    private ResponseEntity<byte[]> doGetPdf(long campaignId,
                                            long orderId,
                                            Long parcelId,
                                            Long boxId,
                                            int numberOfPages,
                                            String fileName) throws IOException {

        ResponseEntity<byte[]> response = generateLabel(campaignId, orderId, parcelId, boxId, Format.JSON);
        assertThat(response.getHeaders().getContentType().getSubtype(), equalTo("pdf"));
        assertThat(response.getBody().length, greaterThan(0));
        PdfTestUtil.assertPdfTextEqualsFile(response.getBody(), resourceAsString(fileName), numberOfPages);
        return response;
    }

    private ResponseEntity<String> getLabelsData(long campaignId, long orderId, Format format) {
        return FunctionalTestHelper.makeRequest(
                urlBasePrefix + "/campaigns/" +
                        campaignId +
                        "/orders/" +
                        orderId +
                        "/delivery/labels/data",
                HttpMethod.GET,
                format);
    }
}
