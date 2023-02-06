package ru.yandex.market.ff4shops.api.xml.order;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

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
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrdersEventsRequest;
import ru.yandex.market.ff4shops.api.OrderHistoryUtil;
import ru.yandex.market.ff4shops.config.FunctionalTest;
import ru.yandex.market.ff4shops.util.FunctionalTestHelper;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff4shops.util.FfAsserts.assertXmlEquals;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class GetOrderTest extends FunctionalTest {
    private static final long YANDEX_ID = 1L;
    private static final long ORDER_ID = 100500L;

    @Autowired
    private CheckouterAPI checkouterAPI;

    private CheckouterOrderHistoryEventsApi historyEventsApi;

    @BeforeEach
    void setup() {
        historyEventsApi = OrderHistoryUtil.prepareMock(checkouterAPI);
    }

    @Test
    void success() {
        mockGetOrders(YANDEX_ID);
        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_success.xml"
        );
    }

    private void mockGetOrders(long orderId) {
        Parcel parcel = new Parcel();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("100500-1");
        parcelBox.setDepth(20L);
        parcelBox.setWidth(30L);
        parcelBox.setHeight(40L);
        parcelBox.setWeight(1250L);

        parcel.setBoxes(List.of(parcelBox));

        Order order = getOrder(parcel);

        when(checkouterAPI.getOrder(orderId, ClientRole.SYSTEM, null))
            .thenReturn(order);
        mockHistoryEvents(List.of(), orderId);
    }

    @Test
    void instancesWithCIS() {
        Parcel parcel = new Parcel();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("100500-1");
        parcelBox.setDepth(20L);
        parcelBox.setWidth(30L);
        parcelBox.setHeight(40L);
        parcelBox.setWeight(1250L);

        parcel.setBoxes(List.of(parcelBox));

        OrderItem orderItem = getOrderItem(1, 2);

        Order order = getOrder(parcel);
        order.addItem(orderItem);

        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenReturn(order);
        mockHistoryEvents(List.of(orderItem), YANDEX_ID);

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_with_cis.xml"
        );
    }

    @Test
    void notFound() {
        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenThrow(new OrderNotFoundException(YANDEX_ID));

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_not_found.xml"
        );
    }

    @Test
    void emptyOrder() {
        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenReturn(new Order());
        mockHistoryEvents(List.of(), YANDEX_ID);

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_empty.xml"
        );
    }

    @Test
    void emptyParcelBoxes() {
        Order order = getOrder(new Parcel());

        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenReturn(order);
        mockHistoryEvents(List.of(), YANDEX_ID);

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_empty.xml"
        );
    }

    @Test
    void emptyParcelBoxDimensions() {
        Parcel parcel = new Parcel();

        ParcelBox parcelBox = new ParcelBox();
        parcelBox.setId(98765L);
        parcelBox.setFulfilmentId("100500-1");

        parcel.setBoxes(List.of(parcelBox));

        Order order = getOrder(parcel);

        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenReturn(order);
        mockHistoryEvents(List.of(), YANDEX_ID);

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_empty_parcel_box_dimensions.xml"
        );
    }

    @Test
    void itemsDeleted() {
        Order order = new Order();
        order.setItems(List.of(getOrderItem(1, 1)));

        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenReturn(order);
        mockHistoryEvents(
            List.of(
                getOrderItem(1, 2),
                getOrderItem(2, 3)
            ),
            YANDEX_ID
        );

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_with_deleted_items.xml"
        );
    }

    @Test
    void itemsAdded() {
        Order order = new Order();
        order.setItems(List.of(
            getOrderItem(2, 3)
        ));

        when(checkouterAPI.getOrder(YANDEX_ID, ClientRole.SYSTEM, null))
            .thenReturn(order);
        mockHistoryEvents(List.of(getOrderItem(1, 2)), YANDEX_ID);

        assertGetOrder(
            "ru/yandex/market/ff4shops/api/xml/order/request/get_order.xml",
            "ru/yandex/market/ff4shops/api/xml/order/response/get_order_negative_undefined_count.xml"
        );
    }

    private void assertGetOrder(String requestPath, String responsePath) {
        String result = FunctionalTestHelper.postForXml(
            urlBuilder.url("orders", "getOrder"),
            extractFileContent(requestPath)
        ).getBody();

        assertXmlEquals(
            extractFileContent(responsePath),
            result
        );
    }

    private Order getOrder(Parcel parcel) {
        Order order = new Order();
        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);
        return order;
    }

    private void mockHistoryEvents(Collection<OrderItem> items, long orderId) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setItems(items);

        OrderHistoryEvent event = new OrderHistoryEvent();
        event.setType(HistoryEventType.NEW_ORDER);
        event.setOrderAfter(order);

        doReturn(new OrderHistoryEvents(List.of(event))).when(historyEventsApi)
            .getOrdersHistoryEvents(refEq(
                OrdersEventsRequest.builder(new long[]{orderId})
                    .withEventTypes(new HistoryEventType[]{HistoryEventType.NEW_ORDER})
                    .withPartials(Set.of(OptionalOrderPart.ITEMS))
                    .build()
            ));
    }

    private OrderItem getOrderItem(long id, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setOfferItemKey(new OfferItemKey("offerId", id, null));
        orderItem.setOfferName("offer name");
        orderItem.setCount(count);
        orderItem.setPrice(BigDecimal.TEN);
        orderItem.setSupplierId(1L);
        orderItem.setShopSku("shop-sku-1");

        ArrayNode instances = JsonNodeFactory.instance.arrayNode()
            .add(JsonNodeFactory.instance.objectNode().put("CIS", "\u001d010460427800144621d;r:IcdEu&pVN\u001d91EE06"))
            .add(JsonNodeFactory.instance.objectNode().put("cis", "CIS-2"))
            .add(JsonNodeFactory.instance.objectNode().put("cisFull", "CIS-FULL-3"))
            .add(JsonNodeFactory.instance.objectNode().put("UIT", "UIT-4"));

        orderItem.setInstances(instances);

        return orderItem;
    }
}
