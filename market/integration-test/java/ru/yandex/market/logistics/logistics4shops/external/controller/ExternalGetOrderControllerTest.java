package ru.yandex.market.logistics.logistics4shops.external.controller;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.CheckouterOrderHistoryEventsApi;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.logistics.logistics4shops.AbstractIntegrationTest;
import ru.yandex.market.logistics.logistics4shops.utils.RestAssuredFactory;
import ru.yandex.market.order_service.client.api.OrdersLogisticsApi;
import ru.yandex.market.order_service.client.model.GetOrderItemsLogisticsDto;
import ru.yandex.market.order_service.client.model.GetOrderLogisticsDto;
import ru.yandex.market.order_service.client.model.GetOrderLogisticsResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.safeRefEq;

@DisplayName("Получение заказа")
@DatabaseSetup("/external/controller/getOrder/prepare.xml")
@ParametersAreNonnullByDefault
class ExternalGetOrderControllerTest extends AbstractIntegrationTest {

    private static final String URL = "external/orders/getOrder";
    private static final Long ORDER_ID = 100104L;

    @Autowired
    private OrdersLogisticsApi ordersLogisticsApi;

    @Autowired
    private CheckouterAPI checkouterAPI;

    private CheckouterOrderHistoryEventsApi historyEventsApi;

    @BeforeEach
    void setup() {
        when(ordersLogisticsApi.getPartnerOrderLogistics(anyLong(), anyLong())).thenReturn(logisticsResponse());
        historyEventsApi = mock(CheckouterOrderHistoryEventsApi.class);
        when(checkouterAPI.orderHistoryEvents()).thenReturn(historyEventsApi);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(ordersLogisticsApi, checkouterAPI, historyEventsApi);
    }

    @Test
    @DisplayName("Заказ есть в базе и в order-service, коробки есть в базе")
    void getOrder() {
        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/l4s_order.xml",
            "external/controller/getOrder/response/l4s_order.xml"
        );
        verify(ordersLogisticsApi).getPartnerOrderLogistics(200100L, 100100L);
    }

    @Test
    @DisplayName("Заказ есть в базе и в order-service, коробки есть в базе, товар удалили")
    void getOrderItemsDeleted() {
        GetOrderLogisticsResponse mockResponse = logisticsResponse();
        GetOrderItemsLogisticsDto secondItem = mockResponse.getOrder().getItems().get(1);
        secondItem.setCount(3);

        when(ordersLogisticsApi.getPartnerOrderLogistics(anyLong(), anyLong()))
            .thenReturn(mockResponse);


        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/l4s_order.xml",
            "external/controller/getOrder/response/l4s_order_items_deleted.xml"
        );
        verify(ordersLogisticsApi).getPartnerOrderLogistics(200100L, 100100L);
    }

    @Test
    @DisplayName("Заказ есть в базе и в order-service, коробки есть в базе, товара стало больше")
    void getOrderItemsAdded() {
        GetOrderLogisticsResponse mockResponse = logisticsResponse();
        GetOrderItemsLogisticsDto firstItem = mockResponse.getOrder().getItems().get(0);
        firstItem.setCount(3);
        firstItem.setItemId(2L); // Для переиспользования файлика с ошибкой

        when(ordersLogisticsApi.getPartnerOrderLogistics(anyLong(), anyLong()))
            .thenReturn(mockResponse);


        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/l4s_order.xml",
            "external/controller/getOrder/response/items_added.xml"
        );
        verify(ordersLogisticsApi).getPartnerOrderLogistics(200100L, 100100L);
    }

    @Test
    @DisplayName("Заказ есть в базе и в order-service, коробок нет в базе")
    void getOrderWithoutBoxesInDB() {
        when(ordersLogisticsApi.getPartnerOrderLogistics(200100L, 100103L))
            .thenReturn(logisticsResponse());
        stubGetCheckouterOrder(100103L).thenReturn(order());

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_items.xml",
            "external/controller/getOrder/response/checkouter_items.xml"
        );

        verify(ordersLogisticsApi).getPartnerOrderLogistics(200100L, 100103L);
        verify(checkouterAPI).getOrder(
            safeRefEq(RequestClientInfo.builder(ClientRole.SYSTEM).build()),
            safeRefEq(OrderRequest.builder(100103L).build())
        );
    }

    @Test
    @DisplayName("Заказ есть везде, но нигде нет коробок")
    void getOrderWithoutBoxes() {
        when(ordersLogisticsApi.getPartnerOrderLogistics(200100L, 100103L))
            .thenReturn(logisticsResponse());
        Order order = new Order();
        order.setItems(List.of(orderItem(1, 1)));
        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(new Parcel()));
        order.setDelivery(delivery);

        stubGetCheckouterOrder(100103L).thenReturn(order);


        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_items.xml",
            "external/controller/getOrder/response/no_boxes_at_all.xml"
        );

        verify(ordersLogisticsApi).getPartnerOrderLogistics(200100L, 100103L);
        verify(checkouterAPI).getOrder(
            safeRefEq(RequestClientInfo.builder(ClientRole.SYSTEM).build()),
            safeRefEq(OrderRequest.builder(100103L).build())
        );
    }

    @Test
    @DisplayName("Заказа нигде нет")
    void notFound() {
        stubGetCheckouterOrder(ORDER_ID)
            .thenThrow(new OrderNotFoundException(ORDER_ID));

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_order.xml",
            "external/controller/getOrder/response/order_not_found.xml"
        );

        verifyGetCheckouterOrder(ORDER_ID);
    }

    @Test
    @DisplayName("Заказа нет в order-service")
    void emptyOrder() {
        stubGetCheckouterOrder(100100L).thenReturn(order());
        mockHistoryEvents(List.of());

        when(ordersLogisticsApi.getPartnerOrderLogistics(200100L, 100100L))
            .thenThrow(new RestClientException("error"));
        mockHistoryEvents(List.of());

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/l4s_order.xml",
            "external/controller/getOrder/response/items_exist_checkouter_order.xml"
        );

        verify(ordersLogisticsApi).getPartnerOrderLogistics(200100L, 100100L);
        verifyGetCheckouterOrder(100100L);
        verify(checkouterAPI).orderHistoryEvents();
        verify(historyEventsApi).getOrdersHistoryEvents(any());
    }

    @Test
    @DisplayName("Заказ из чекаутера: пустые коробки")
    void emptyParcelBoxes() {
        stubGetCheckouterOrder(ORDER_ID).thenReturn(order(new Parcel()));
        mockHistoryEvents(List.of());

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_order.xml",
            "external/controller/getOrder/response/checkouter_empty_parcel.xml"
        );

        verifyGetCheckouterOrder(ORDER_ID);
        verify(checkouterAPI).orderHistoryEvents();
        verify(historyEventsApi).getOrdersHistoryEvents(any());
    }

    @Test
    @DisplayName("Заказ из чекаутера: пустые габариты")
    void emptyParcelBoxDimensions() {
        Parcel parcel = new Parcel();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("100500-1");

        parcel.setBoxes(List.of(parcelBox));

        Order order = order(parcel);

        stubGetCheckouterOrder(ORDER_ID).thenReturn(order);
        mockHistoryEvents(List.of());

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_order.xml",
            "external/controller/getOrder/response/checkoter_empty_dimensions.xml"
        );

        verifyGetCheckouterOrder(ORDER_ID);
        verify(checkouterAPI).orderHistoryEvents();
        verify(historyEventsApi).getOrdersHistoryEvents(any());
    }

    @Test
    @DisplayName("Заказ из чекаутера: удалены товары")
    void itemsDeleted() {
        Order order = new Order();
        order.setItems(List.of(orderItem(1, 1)));

        stubGetCheckouterOrder(ORDER_ID).thenReturn(order);
        mockHistoryEvents(List.of(orderItem(1, 2), orderItem(2, 3)));

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_order.xml",
            "external/controller/getOrder/response/checkouter_items_deleted.xml"
        );

        verifyGetCheckouterOrder(ORDER_ID);
        verify(checkouterAPI).orderHistoryEvents();
        verify(historyEventsApi).getOrdersHistoryEvents(any());
    }

    @Test
    @DisplayName("Заказ из чекаутера: добавлены товары")
    void itemsAdded() {
        Order order = new Order();
        order.setItems(List.of(orderItem(2, 3)));

        stubGetCheckouterOrder(ORDER_ID).thenReturn(order);
        mockHistoryEvents(List.of(orderItem(1, 2)));

        RestAssuredFactory.assertPostXml(
            URL,
            "external/controller/getOrder/request/checkouter_order.xml",
            "external/controller/getOrder/response/items_added.xml"
        );

        verifyGetCheckouterOrder(ORDER_ID);
        verify(checkouterAPI).orderHistoryEvents();
        verify(historyEventsApi).getOrdersHistoryEvents(any());
    }

    @Nonnull
    private Order order(Parcel parcel) {
        Order order = new Order();
        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);
        return order;
    }

    private void mockHistoryEvents(Collection<OrderItem> items) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setItems(items);

        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.NEW_ORDER);
        event.setOrderAfter(order);

        doReturn(new OrderHistoryEvents(List.of(event))).when(historyEventsApi).getOrdersHistoryEvents(any());
    }

    @Nonnull
    private OrderItem orderItem(long id, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setOfferItemKey(new OfferItemKey("offerId", id, null));
        orderItem.setOfferName("offer name");
        orderItem.setCount(count);
        orderItem.setPrice(BigDecimal.TEN);
        orderItem.setSupplierId(1L);
        orderItem.setShopSku("shop-sku-1");

        ArrayNode instances = JsonNodeFactory.instance.arrayNode()
            .add(JsonNodeFactory.instance.objectNode().put("cis", "\u001d010460427800144621d;r:IcdEu&pVN\u001d91EE06"))
            .add(JsonNodeFactory.instance.objectNode().put("cis", "CIS-2"));

        orderItem.setInstances(instances);

        return orderItem;
    }

    @Nonnull
    private GetOrderLogisticsResponse logisticsResponse() {
        return new GetOrderLogisticsResponse().order(
            new GetOrderLogisticsDto()
                .items(List.of(orderItemsLogisticsDto(1), orderItemsLogisticsDto(2)))
                .orderId(1L)
        );
    }

    @Nonnull
    private GetOrderItemsLogisticsDto orderItemsLogisticsDto(int seed) {
        GetOrderItemsLogisticsDto item = new GetOrderItemsLogisticsDto()
            .itemId(1000L + seed)
            .offerName("offer" + seed)
            .shopSku("sku" + seed)
            .price(BigDecimal.TEN.add(BigDecimal.valueOf(seed)))
            .count(2 * seed)
            .initialCount(2 * seed)
            .partnerId(1L);

        if (seed % 2 == 0) {
            item.cisFull(List.of(
                "\u001d010460427800144621d;r:IcdEu&pVN\u001d91EE06",
                "cis " + seed
            ));
        } else {
            item.cis(List.of(
                "cis" + seed,
                "cis" + seed + " " + seed
            ));
        }

        return item;
    }

    @Nonnull
    private Order order() {
        Parcel parcel = new Parcel();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("CHECKOUTER-FF");
        parcelBox.setDepth(20L);
        parcelBox.setWidth(30L);
        parcelBox.setHeight(40L);
        parcelBox.setWeight(1250L);

        parcel.setBoxes(List.of(parcelBox));

        return order(parcel);
    }

    @Nonnull
    private OngoingStubbing<Order> stubGetCheckouterOrder(long orderId) {
        return when(checkouterAPI.getOrder(
            safeRefEq(RequestClientInfo.builder(ClientRole.SYSTEM).build()),
            safeRefEq(OrderRequest.builder(orderId).build())
        ));
    }

    private void verifyGetCheckouterOrder(long orderId) {
        verify(checkouterAPI).getOrder(
            safeRefEq(RequestClientInfo.builder(ClientRole.SYSTEM).build()),
            safeRefEq(OrderRequest.builder(orderId).build())
        );
    }
}
