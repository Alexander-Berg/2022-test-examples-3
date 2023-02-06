package ru.yandex.market.ff4shops.api.xml.order;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.market.partner.notification.client.PartnerNotificationClient;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.CompatibleCancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.pushapi.client.error.ShopErrorException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.delivery.order.OrderNotificationService;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;
import ru.yandex.market.ff4shops.util.XPathMatcher;
import ru.yandex.market.ff4shops.util.XmlEqualsMatcher;
import ru.yandex.market.partner.notification.client.model.DestinationDTO;
import ru.yandex.market.partner.notification.client.model.SendNotificationRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

@ParametersAreNonnullByDefault
@DbUnitDataSet(before = "environment.before.csv")
class CreateOrderTest extends FunctionalTest {

    private static final long ORDER_ID = 100500L;
    private static final String SHOP_ORDER_ID = "12";
    private static final RequestClientInfo requestClientInfo = new RequestClientInfo(ClientRole.SYSTEM, null);

    @Autowired
    private CheckouterAPI checkouterAPI;

    @Autowired
    private ThreadPoolExecutor ordersThreadPool;

    @Autowired
    @Qualifier("pushApiClient")
    private PushApi apiClient;

    @Autowired
    @Value("${mbi.api.url}")
    protected String mbiApiUrl;

    @Autowired
    @Qualifier("mbiApiRestTemplate")
    private RestTemplate mbiApiRestTemplate;

    @Autowired
    private PartnerNotificationClient partnerNotificationClient;

    private MockRestServiceServer mockRestServiceServer;

    private static final String RESPONSE_XML =/*language=xml*/ "" +
            "<send-notification-response>" +
            "   <notification-group-id>" +
            "       42" +
            "   </notification-group-id>" +
            "</send-notification-response>";

    @BeforeEach
    void initMock() {
        mockRestServiceServer = MockRestServiceServer.createServer(mbiApiRestTemplate);
    }

    void mockNotification(String xml, String notificationData) {
        mockRestServiceServer.expect(requestTo(String.format("%s/send-notification-to-supplier", mbiApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(new XmlEqualsMatcher(xml, Collections.singleton("notification-data"))))
                .andExpect(content().string(new XPathMatcher("send-message-to-supplier-request/notification-data",
                        notificationData, new XmlEqualsMatcher(notificationData,
                        Set.of("hours-for-confirmation", "minutes-for-confirmation"))))
                )
                .andRespond(withSuccess(RESPONSE_XML, MediaType.APPLICATION_XML));
    }

    void mockFailNotification(String xml, String notificationData) {
        mockRestServiceServer.expect(requestTo(String.format("%s/send-notification-to-supplier", mbiApiUrl)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(new XmlEqualsMatcher(xml, Collections.singleton("notification-data"))))
                .andExpect(content().string(new XPathMatcher("send-message-to-supplier-request/notification-data",
                        notificationData, new XmlEqualsMatcher(notificationData,
                        Set.of("hours-for-confirmation", "minutes-for-confirmation"))))
                )
                .andRespond(response -> {
                    throw new RuntimeException("very very bad response");
                });
    }

    void verifyPartnerNotification(SendNotificationRequest sendNotificationRequest, boolean express) {
        if (express) {
            Mockito.verify(partnerNotificationClient).sendNotification(
                    ArgumentMatchers.argThat(req ->
                            new XmlEqualsMatcher(
                                    sendNotificationRequest.getData(),
                                    Set.of(
                                            "minutes-for-confirmation",
                                            "hours-for-confirmation",
                                            "accept-before-time"
                                    )
                            ).matches(req.getData())
                                    && Objects.equals(sendNotificationRequest.getRenderOnly(), req.getRenderOnly())
                                    && Objects.equals(sendNotificationRequest.getTypeId(), req.getTypeId())
                                    && Objects.equals(sendNotificationRequest.getDestination(), req.getDestination())
                    )
            );
        } else {
            Mockito.verifyNoInteractions(partnerNotificationClient);
        }
    }

    @Test
    public void assertCreateOrderWithoutExpressTag() {
        assertCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "create_order_success_use_partner_id_instead_yandex_id.xml"
        );
    }

    private void assertCreateOrder(
            String createOrderRequestPath,
            String expectedResponsePath
    ) {
        long tasksBefore = ordersThreadPool.getCompletedTaskCount();
        Order order = createOrder(OrderStatus.PENDING);

        when(checkouterAPI.getOrder(ORDER_ID, requestClientInfo.getClientRole(), null))
                .thenReturn(order);
        when(checkouterAPI.updateShopOrderId(eq(ORDER_ID), eq(SHOP_ORDER_ID), any()))
                .thenReturn(true);
        when(checkouterAPI.updateOrderStatus(
                ORDER_ID,
                requestClientInfo.getClientRole(),
                null,
                order.getShopId(),
                OrderStatus.PROCESSING,
                null
        )).thenReturn(order);

        OrderResponse orderResponse = new OrderResponse(SHOP_ORDER_ID, true, null);

        doReturn(orderResponse).when(apiClient).orderAccept(order.getShopId(), order, null, "");

        assertXmlCreateOrder(
                createOrderRequestPath,
                "ru/yandex/market/ff4shops/api/xml/order/response/" + expectedResponsePath
        );

        verify(checkouterAPI).updateShopOrderId(eq(ORDER_ID), eq(SHOP_ORDER_ID), any());

        verify(checkouterAPI).updateOrderStatus(
                ORDER_ID,
                requestClientInfo.getClientRole(),
                null,
                order.getShopId(),
                OrderStatus.PROCESSING,
                null
        );

        assertEquals(tasksBefore + 1, ordersThreadPool.getCompletedTaskCount());
    }

    @Test
    @DbUnitDataSet(after = {"deadline_saved.after.csv","notification_order_empty.after.csv"})
    @DisplayName("Проверка заполнения дедлайна сборки заказа: все условия соблюдены")
    void createOrderWithDeadline() {
        String orderXml = /*language=xml*/ "" +
                "<order>\n" +
                "    <id>100500</id>\n" +
                "    <accept-before-time>2.08.2021 11:00</accept-before-time>\n" +
                "    <money-amount>10</money-amount>\n" +
                "    <hours-for-confirmation>-8</hours-for-confirmation>\n" +
                "    <minutes-for-confirmation>-10</minutes-for-confirmation>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>OfferName</title>\n" +
                "            <sku>ShopSku</sku>\n" +
                "            <offer-id>OfferId</offer-id>\n" +
                "            <count>2</count>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "    <status>PENDING</status>\n" +
                "    <is-express>true</is-express>\n" +
                "    <fulfilment-warehouse-id>123456</fulfilment-warehouse-id>\n" +
                "</order>";
        mockNotification(/*language=xml*/ "" +
                        "<send-message-to-supplier-request>" +
                        "  <supplier-id>234</supplier-id>" +
                        "  <notification-type>1643556238</notification-type>" +
                        "  <notification-context-params paramsType=\"ORDER\">" +
                        "    <order-id>100500</order-id>" +
                        "    <order-status-id>8</order-status-id>" +
                        "    <params-type>ORDER</params-type>" +
                        "   </notification-context-params>" +
                        "</send-message-to-supplier-request>",
                orderXml
        );
        assertCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order_with_deadline.xml",
                "create_order_success_use_partner_id_instead_yandex_id.xml"
        );
        verifyPartnerNotification(makePartnerNotificationRequest(234, wrapPNXml(orderXml)), true);
        mockRestServiceServer.verify();
    }

    @Test
    @DbUnitDataSet(after = {"deadline_saved.after.csv", "notification_order.after.csv"})
    @DisplayName("Создание заказа с фейлом нотификации")
    void createOrderWithMessageFail() {
        String orderXml = /*language=xml*/ "" +
                "<order>\n" +
                "    <id>100500</id>\n" +
                "    <accept-before-time>2.08.2021 11:00</accept-before-time>\n" +
                "    <money-amount>10</money-amount>\n" +
                "    <hours-for-confirmation>-8</hours-for-confirmation>\n" +
                "    <minutes-for-confirmation>-10</minutes-for-confirmation>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>OfferName</title>\n" +
                "            <sku>ShopSku</sku>\n" +
                "            <offer-id>OfferId</offer-id>\n" +
                "            <count>2</count>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "    <status>PENDING</status>\n" +
                "    <is-express>true</is-express>\n" +
                "    <fulfilment-warehouse-id>123456</fulfilment-warehouse-id>\n" +
                "</order>";
        mockFailNotification(/*language=xml*/ "" +
                        "<send-message-to-supplier-request>" +
                        "  <supplier-id>234</supplier-id>" +
                        "  <notification-type>1643556238</notification-type>" +
                        "  <notification-context-params paramsType=\"ORDER\">" +
                        "    <order-id>100500</order-id>" +
                        "    <order-status-id>8</order-status-id>" +
                        "    <params-type>ORDER</params-type>" +
                        "   </notification-context-params>" +
                        "</send-message-to-supplier-request>",
                orderXml
        );
        assertCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order_with_deadline.xml",
                "create_order_success_use_partner_id_instead_yandex_id.xml"
        );
        verifyPartnerNotification(makePartnerNotificationRequest(234, wrapPNXml(orderXml)), true);
        mockRestServiceServer.verify();
    }

    @Test
    @DbUnitDataSet(after = "electronic_accept_required_saved.after.csv")
    @DisplayName("Проверка заполнения признака передачи заказа через ЭАПП. Использовать partnerId вместо yandexId.")
    void createOrderWithElectronicAcceptCodeUsePartnerIdInsteadYandexId() {
        createOrderWithElectronicAcceptCodeProcess(
                "create_order_success_use_partner_id_instead_yandex_id.xml"
        );
    }

    private void createOrderWithElectronicAcceptCodeProcess(String expectedResponsePath) {
        String orderXml = /*language=xml*/ "" +
                "<order>\n" +
                "    <id>100500</id>\n" +
                "    <accept-before-time>2.08.2021 11:00</accept-before-time>\n" +
                "    <money-amount>10</money-amount>\n" +
                "    <hours-for-confirmation>-8</hours-for-confirmation>\n" +
                "    <minutes-for-confirmation>-20</minutes-for-confirmation>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>OfferName</title>\n" +
                "            <sku>ShopSku</sku>\n" +
                "            <offer-id>OfferId</offer-id>\n" +
                "            <count>2</count>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "    <status>PENDING</status>\n" +
                "    <is-express>true</is-express>\n" +
                "    <fulfilment-warehouse-id>123456</fulfilment-warehouse-id>\n" +
                "</order>";
        mockNotification(/*language=xml*/ "" +
                        "<send-message-to-supplier-request>" +
                        "  <supplier-id>234</supplier-id>" +
                        "  <notification-type>1643556238</notification-type>" +
                        "  <notification-context-params paramsType=\"ORDER\">" +
                        "    <order-id>100500</order-id>" +
                        "    <order-status-id>8</order-status-id>" +
                        "    <params-type>ORDER</params-type>" +
                        "    </notification-context-params>" +
                        "</send-message-to-supplier-request>",
                orderXml
        );
        assertCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order_with_accept_code.xml",
                expectedResponsePath
        );
        verifyPartnerNotification(makePartnerNotificationRequest(234, wrapPNXml(orderXml)), true);
        mockRestServiceServer.verify();
    }

    @MethodSource("createOrderWithoutDeadline")
    @DbUnitDataSet(after = "no_deadline.csv")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Случаи, когда дедлайн сборки заказа не сохраняется. Использовать partnerId вместо yandexId")
    void createOrderWithoutDeadline(
            String displayName,
            String createOrderRequestPath,
            boolean express
    ) {
        createOrderWithoutDeadlineProcess(
                createOrderRequestPath,
                "create_order_success_use_partner_id_instead_yandex_id.xml",
                express
        );
    }

    private void createOrderWithoutDeadlineProcess(
            String createOrderRequestPath,
            String expectedResponsePath,
            boolean express
    ) {
        String orderXml = /*language=xml*/ "" +
                "<order>\n" +
                "    <id>100500</id>\n" +
                "    <money-amount>10</money-amount>\n" +
                "    <hours-for-confirmation>0</hours-for-confirmation>\n" +
                "    <minutes-for-confirmation>0</minutes-for-confirmation>\n" +
                "    <order-items>\n" +
                "        <item>\n" +
                "            <title>OfferName</title>\n" +
                "            <sku>ShopSku</sku>\n" +
                "            <offer-id>OfferId</offer-id>\n" +
                "            <count>2</count>\n" +
                "        </item>\n" +
                "    </order-items>\n" +
                "    <status>PENDING</status>\n" +
                "    <is-express>true</is-express>\n" +
                "    <fulfilment-warehouse-id>123456</fulfilment-warehouse-id>\n" +
                "</order>\n";
        mockNotification(/*language=xml*/ "" +
                        "<send-message-to-supplier-request>" +
                        "  <supplier-id>234</supplier-id>" +
                        "  <notification-type>1643556238</notification-type>" +
                        "  <notification-context-params paramsType=\"ORDER\">" +
                        "    <order-id>100500</order-id>" +
                        "    <order-status-id>8</order-status-id>" +
                        "    <params-type>ORDER</params-type>" +
                        "    </notification-context-params>" +
                        "</send-message-to-supplier-request>",
                orderXml
        );

        assertCreateOrder(createOrderRequestPath, expectedResponsePath);
        verifyPartnerNotification(makePartnerNotificationRequest(234, wrapPNXml(orderXml)), express);
    }

    @Nonnull
    private static Stream<Arguments> createOrderWithoutDeadline() {
        return Stream.of(
                Arguments.of(
                        "Отсутствует и тег, и shipmentDateTime",
                        "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                        false
                ),
                Arguments.of(
                        "Отсутствует shipmentDateTime",
                        "ru/yandex/market/ff4shops/api/xml/order/request/create_order_without_shipment_date_time.xml",
                        true
                ),
                Arguments.of(
                        "Отсутствует нужный тег",
                        "ru/yandex/market/ff4shops/api/xml/order/request/create_order_without_express_tag.xml",
                        false
                )
        );
    }

    @Test
    public void getOrderFailed() {
        when(checkouterAPI.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
                .thenThrow(new OrderNotFoundException(ORDER_ID));

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_get_order_failed.xml"
        );
    }

    @Test
    @DisplayName("Подтверждение заказа упало по вине pushApi")
    public void orderAcceptFailedWithPushApiError() {
        Order order = createOrder(OrderStatus.PENDING);

        when(checkouterAPI.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
                .thenReturn(order);

        doThrow(new ErrorCodeException("9999", "Exception from pushApi", 9999))
                .when(apiClient).orderAccept(order.getShopId(), order, null, "");

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_accept_failed.xml"
        );
    }

    @Test
    @DisplayName("Подтверждение заказа упало по вине магазина")
    public void orderAcceptFailedWithShopError() {
        Order order = createOrder(OrderStatus.PENDING);

        when(checkouterAPI.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
                .thenReturn(order);

        doThrow(new ShopErrorException(ErrorSubCode.CONNECTION_REFUSED, "Exception on shop interaction", false))
                .when(apiClient).orderAccept(order.getShopId(), order, null, "");

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_accept_failed_by_shop.xml"
        );
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = {"CANCELLED", "CANCELLED_WITHOUT_REFUND"})
    @DisplayName("Пытаемся создать уже отменённый в чекаутере заказ")
    public void orderAllReadyCanceled(OrderStatus orderStatus) {
        Order order = createOrder(orderStatus);

        when(checkouterAPI.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
                .thenReturn(order);

        doThrow(new ShopErrorException(ErrorSubCode.CONNECTION_REFUSED, false))
                .when(apiClient).orderAccept(order.getShopId(), order, null, "");

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_success.xml"
        );
    }

    @Test
    public void notAccepted() {
        Order order = createOrder(OrderStatus.PENDING);

        when(checkouterAPI.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
                .thenReturn(order);
        when(checkouterAPI.createCancellationRequest(
                eq(ORDER_ID),
                any(CompatibleCancellationRequest.class),
                any(ClientRole.class),
                eq(null)
        )).thenReturn(null);

        OrderResponse orderResponse = new OrderResponse(SHOP_ORDER_ID, false, DeclineReason.OTHER);

        doReturn(orderResponse).when(apiClient).orderAccept(order.getShopId(), order, null, "");

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_not_accepted.xml"
        );

        verify(checkouterAPI).createCancellationRequest(
                eq(ORDER_ID),
                any(CompatibleCancellationRequest.class),
                any(ClientRole.class),
                eq(null)
        );
    }

    @Test
    @DisplayName("Автоподтверждение создания заказа")
    public void autoAccept() {
        autoAcceptProcess("create_order_success_use_partner_id_instead_yandex_id.xml");
    }

    private void autoAcceptProcess(String expectedResponsePath) {
        Order order = createOrder(OrderStatus.PROCESSING);

        when(checkouterAPI.getOrder(ORDER_ID, requestClientInfo.getClientRole(), null))
                .thenReturn(order);

        when(checkouterAPI.updateShopOrderId(eq(ORDER_ID), eq(SHOP_ORDER_ID), any()))
                .thenReturn(true);

        OrderResponse orderResponse = new OrderResponse(SHOP_ORDER_ID, true, null);

        doReturn(orderResponse).when(apiClient).orderAccept(order.getShopId(), order, null, "");

        when(checkouterAPI.updateOrderStatus(
                ORDER_ID,
                requestClientInfo.getClientRole(),
                null,
                order.getShopId(),
                OrderStatus.PROCESSING,
                null
        )).thenThrow(new RuntimeException("Should not executed"));

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/" + expectedResponsePath
        );
    }

    @Test
    public void failUpdateOk() {
        failUpdateOkProcess("create_order_success_use_partner_id_instead_yandex_id.xml");
    }

    private void failUpdateOkProcess(String expectedResponsePath) {
        Order order = createOrder(OrderStatus.PENDING);

        when(checkouterAPI.getOrder(ORDER_ID, requestClientInfo.getClientRole(), null))
                .thenReturn(order)
                .thenReturn(createOrder(OrderStatus.PROCESSING));

        updateOrderFlow(order);

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/" + expectedResponsePath
        );
    }

    @Test
    public void failUpdateFail() {
        Order order = createOrder(OrderStatus.PENDING);

        when(checkouterAPI.getOrder(ORDER_ID, requestClientInfo.getClientRole(), null))
                .thenReturn(order);

        updateOrderFlow(order);

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_update_failed.xml"
        );
    }

    @Test
    public void skipWebInterface() {
        Order order = createOrder(OrderStatus.PROCESSING);
        order.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);

        when(checkouterAPI.getOrder(ORDER_ID, requestClientInfo.getClientRole(), null))
                .thenReturn(order);

        when(checkouterAPI.updateShopOrderId(eq(ORDER_ID), eq(SHOP_ORDER_ID), any()))
                .thenThrow(new RuntimeException("Should not executed"));

        when(checkouterAPI.updateOrderStatus(
                ORDER_ID,
                requestClientInfo.getClientRole(),
                null,
                order.getShopId(),
                OrderStatus.PROCESSING,
                null
        )).thenThrow(new RuntimeException("Should not executed"));

        assertXmlCreateOrder(
                "ru/yandex/market/ff4shops/api/xml/order/request/create_order.xml",
                "ru/yandex/market/ff4shops/api/xml/order/response/create_order_success.xml"
        );
    }


    private Order createOrder(OrderStatus status) {
        var order = new Order();
        var delivery = new Delivery();

        order.setDelivery(delivery);
        order.setShopId(234L);
        order.setId(ORDER_ID);
        order.setStatus(status);
        order.setItemsTotal(BigDecimal.TEN);

        var parcel = new Parcel();
        delivery.setParcels(List.of(parcel));

        var parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("100500-1");
        parcelBox.setDepth(20L);
        parcelBox.setWidth(30L);
        parcelBox.setHeight(40L);
        parcelBox.setWeight(1250L);
        parcel.setBoxes(List.of(parcelBox));

        var orderItem = new OrderItem();
        orderItem.setOrderId(ORDER_ID);
        orderItem.setOfferName("OfferName");
        orderItem.setShopSku("ShopSku");
        orderItem.setOfferId("OfferId");
        orderItem.setCount(2);
        orderItem.setFulfilmentWarehouseId(123456L);
        order.setItems(List.of(orderItem));
        return order;
    }

    private void updateOrderFlow(Order order) {
        when(checkouterAPI.updateShopOrderId(eq(ORDER_ID), eq(SHOP_ORDER_ID), any()))
                .thenReturn(true);

        OrderResponse orderResponse = new OrderResponse(SHOP_ORDER_ID, true, null);

        doReturn(orderResponse).when(apiClient).orderAccept(order.getShopId(), order, null, "");

        when(checkouterAPI.updateOrderStatus(
                ORDER_ID,
                requestClientInfo.getClientRole(),
                null,
                order.getShopId(),
                OrderStatus.PROCESSING,
                null
        )).thenThrow(new RuntimeException("Update failed"));
    }

    private SendNotificationRequest makePartnerNotificationRequest(long shopId, String xmlData) {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setTypeId(OrderNotificationService.MARKET_PARTNER_MOBILE_NEW_ORDER_TEMPLATE);
        DestinationDTO destinationDTO = new DestinationDTO();
        destinationDTO.setShopId(shopId);
        request.setDestination(destinationDTO);
        request.setRenderOnly(false);
        request.setData(xmlData);
        return request;
    }

    private String wrapPNXml(String xml){
        return String.format("<data>%s</data>", xml);
    }

    private void assertXmlCreateOrder(String requestPath, String responsePath) {
        assertXmlEquals(
                extractFileContent(responsePath),
                FunctionalTestHelper.postForXml(
                        urlBuilder.url("orders", "createOrder"),
                        extractFileContent(requestPath)
                ).getBody()
        );
    }
}
