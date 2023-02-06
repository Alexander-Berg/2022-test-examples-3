package ru.yandex.market.checkout.common.rest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.JsonExpectationsHelper;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelItem;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.MultiOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderStatusNotAllowedException;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.receipt.Receipt;
import ru.yandex.market.checkout.checkouter.receipt.Receipts;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.ping.CheckResult;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.checkout.common.web.CheckoutHttpParameters.CHECKOUTER_CLIENT_VERSION_HEADER;

/**
 * Created by disproper on 17/06/21.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:WEB-INF/checkouter-client.xml")
public class MockServerClientTest {

    private static final String MOCK_SERVER_URL = "http://fake.server.com";

    @Autowired
    private CheckouterClient checkouterAPI;
    @Autowired
    private RestTemplate checkouterRestTemplate;

    private MockRestServiceServer server;

    private String clientVersion;

    @BeforeEach
    public void setUp() throws IOException {
        checkouterAPI.setServiceURL(MOCK_SERVER_URL);
        server = MockRestServiceServer.createServer(checkouterRestTemplate);

        Properties properties = new Properties();
        properties.load(MockServerClientTest.class.getResourceAsStream("/checkouter-client-version.properties"));
        clientVersion = properties.getProperty("checkouter.client.version", "");
    }

    @Test
    public void testGetOrdersCount() throws Exception {
        long shopId = 774;
        server.expect(
                requestTo(
                        allOf(
                                containsString(MOCK_SERVER_URL + "/orders/count?"),
                                containsString("clientRole=SHOP"),
                                containsString("clientId=" + shopId)
                        )
                )
        ).andExpect(method(HttpMethod.GET)).andRespond(withSuccess("{\"value\": 5}", MediaType.APPLICATION_JSON));

        int count = checkouterAPI.getOrdersCount(new OrderSearchRequest(), ClientRole.SHOP, shopId);
        assertEquals(5, count);
    }

    @Test
    public void testFailedToUpdateOrderStatus() {
        Assertions.assertThrows(OrderStatusNotAllowedException.class, () -> {
            long orderId = 1724648;
            long shopId = 349113;
            OrderStatus targetStatus = OrderStatus.DELIVERY;
            server.expect(requestTo(allOf(
                    containsString(MOCK_SERVER_URL + "/orders/" + orderId + "/status"),
                    containsString("clientRole=SHOP_USER"),
                    containsString("clientId=" + shopId),
                    containsString("shopId=" + shopId),
                    containsString("status=" + targetStatus)
            ))).andExpect(method(HttpMethod.POST))
                    .andRespond(withBadRequest().body("{\n" +
                            "    \"status\": 400,\n" +
                            "    \"code\": \"STATUS_NOT_ALLOWED\",\n" +
                            "    \"message\": \"Order 1677568 with status CANCELLED is not allowed for status " +
                            "DELIVERY\"\n" +
                            "}"));

            checkouterAPI.updateOrderStatus(orderId,
                    ClientRole.SHOP_USER, shopId, shopId, targetStatus, null, null);
        });
    }

    @Test
    public void testOutOfDateCheckout() throws Exception {
        long uid = 454826726L;
        server.expect(requestTo(allOf(
                containsString(MOCK_SERVER_URL + "/checkout"),
                containsString("uid=" + uid)
        )))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(getResource("order/checkout-out-of-date.json"), MediaType.APPLICATION_JSON));

        MultiOrder multiOrder = new MultiOrder();
        multiOrder.addOrder(OrderProvider.getBlueOrder());
        multiOrder.setBuyer(BuyerProvider.getBuyer());
        multiOrder.setBuyerRegionId(2L);
        multiOrder.setPromoCode("3196-0438-1580-8356");
        multiOrder.setBuyerCurrency(Currency.RUR);
        multiOrder.setPaymentType(PaymentType.PREPAID);
        multiOrder.setPaymentMethod(PaymentMethod.YANDEX);

        MultiOrder result = checkouterAPI.checkout(multiOrder, uid);

        assertEquals(1, (long) result.getCartFailuresCount());
        assertEquals(OrderFailure.Code.OUT_OF_DATE, result.getOrderFailures().get(0).getErrorCode());
    }

    @Test
    public void testPingSuccess() throws Exception {
        server.expect(requestTo(MOCK_SERVER_URL + "/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("0;OK", MediaType.TEXT_HTML));

        CheckResult checkResult = checkouterAPI.ping();
        assertEquals(CheckResult.Level.OK, checkResult.getLevel());
    }

    @Test
    public void testPingFailed() throws Exception {
        server.expect(requestTo(MOCK_SERVER_URL + "/ping"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("2;Some serious error", MediaType.TEXT_HTML));

        CheckResult checkResult = checkouterAPI.ping();
        assertEquals(CheckResult.Level.CRITICAL, checkResult.getLevel());
    }

    @Test
    public void testGetOrderReceipts() throws Exception {
        server.expect(requestTo(MOCK_SERVER_URL + "/orders/123/receipts?clientRole=SYSTEM&" +
                "clientId=&shopId=&archived=false"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(getResource("order-receipts.json"), MediaType.APPLICATION_JSON));

        Receipts orderReceipts = checkouterAPI.getOrderReceipts(123, ClientRole.SYSTEM, null, null);
        Assertions.assertEquals(2, orderReceipts.getContent().size(), "Should return 2 receipts");
        for (Receipt receipt : orderReceipts.getContent()) {
            assertNotNull(receipt.getCreatedAt());
            assertNotNull(receipt.getType());
            assertNotNull(receipt.getRefundId());
            assertNotNull(receipt.getStatus());
            assertNotNull(receipt.getUpdatedAt());
            assertNotNull(receipt.getStatusUpdatedAt());
        }
    }

    @Test
    public void testPushTracksWithOldBodyFormat() throws IOException {
        mockPostDelivery("delivery-old-format.json");

        Delivery delivery = new Delivery();
        delivery.addTrack(new Track("iddqd", 123L));

        checkouterAPI.updateOrderDelivery(123, ClientRole.SYSTEM, 111L, delivery);

        server.verify();
    }


    @Test
    public void testSaveMultiShipmentDelivery() throws Exception {
        mockPostDelivery("delivery.json");

        Parcel shipment1 = new Parcel();
        shipment1.setId(11L);
        shipment1.addTrack(new Track("iddqd-1", 123L));
        shipment1.addParcelItem(new ParcelItem(1L, 5));

        Parcel shipment2 = new Parcel();
        shipment2.setId(22L);
        shipment2.addTrack(new Track("iddqd-2", 123L));
        shipment2.addTrack(new Track("iddqd-3", 123L));
        shipment2.addParcelItem(new ParcelItem(1L, 10));

        Delivery delivery = new Delivery();
        delivery.setParcels(Arrays.asList(shipment1, shipment2));

        Order order = checkouterAPI.updateOrderDelivery(123, ClientRole.SYSTEM, 111L, delivery);
        delivery = order.getDelivery();

        assertThat(delivery.getParcels(), hasSize(2));

        shipment1 = delivery.getParcels().get(0);
        assertEquals(11, (long) shipment1.getId());

        assertThat(shipment1.getParcelItems(), hasSize(1));
        assertItem(5, shipment1.getParcelItems().get(0));

        assertThat(shipment1.getTracks(), hasSize(1));
        assertTrack("iddqd-1", shipment1.getTracks().get(0));

        shipment2 = delivery.getParcels().get(1);
        assertEquals(22, (long) shipment2.getId());

        assertThat(shipment2.getParcelItems(), hasSize(1));
        assertItem(10, shipment2.getParcelItems().get(0));

        assertThat(shipment2.getTracks(), hasSize(2));
        assertTrack("iddqd-2", shipment2.getTracks().get(0));
        assertTrack("iddqd-3", shipment2.getTracks().get(1));

        assertNotNull(delivery.getShipment());
        assertEquals(11, (long) delivery.getShipment().getId());

        assertThat(delivery.getTracks(), hasSize(1));
        assertTrack("iddqd-1", delivery.getTracks().get(0));

        server.verify();
    }

    @Test
    public void testSendShipmentProperties() throws IOException {
        mockPostDelivery("delivery-with-shipment-old-format.json");

        Parcel shipment = new Parcel();
        shipment.setWidth(10L);
        shipment.setHeight(20L);
        shipment.setDepth(30L);
        shipment.setWeight(40L);

        Delivery delivery = new Delivery();
        delivery.setShipment(shipment);

        checkouterAPI.updateOrderDelivery(123, ClientRole.SYSTEM, 111L, delivery);

        server.verify();
    }

    @Test
    public void shouldWriteVersionHeader() {
        server.expect(requestTo(MOCK_SERVER_URL + "/ping")
        ).andExpect(
                header(CHECKOUTER_CLIENT_VERSION_HEADER, clientVersion)
        ).andRespond(withSuccess("0;OK", MediaType.TEXT_PLAIN));

        checkouterAPI.ping();

        server.verify();
    }

    private void mockPostDelivery(String requestBodyPath) throws IOException {
        server.expect(requestTo(MOCK_SERVER_URL + "/orders/123/delivery?clientRole=SYSTEM&clientId=111"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(new JsonMatcher(getResource(requestBodyPath))))
                .andRespond(withSuccess(
                        getResource("order-with-delivery.json"),
                        MediaType.APPLICATION_JSON)
                );
    }

    private void assertItem(int count, ParcelItem item) {
        assertEquals(1, (long) item.getItemId());
        assertEquals(count, (int) item.getCount());
    }

    private void assertTrack(String trackCode, Track track) {
        assertEquals(trackCode, track.getTrackCode());
        assertEquals(123, (long) track.getDeliveryServiceId());
        assertEquals(
                new GregorianCalendar(2017, 7, 8).getTime(),
                track.getCreationDate()
        );
    }

    private String getResource(String path) throws IOException {
        return IOUtils.toString(
                getClass().getResourceAsStream(path),
                StandardCharsets.UTF_8);
    }

    private static class JsonMatcher extends BaseMatcher<String> {

        private final JsonExpectationsHelper jsonExpectationsHelper = new JsonExpectationsHelper();
        private final String expectedJson;

        JsonMatcher(String expectedJson) {
            this.expectedJson = expectedJson;
        }

        @Override
        public boolean matches(Object item) {
            try {
                jsonExpectationsHelper.assertJsonEqual(expectedJson, (String) item);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            return true;
        }

        @Override
        public void describeTo(Description description) {

        }
    }
}
