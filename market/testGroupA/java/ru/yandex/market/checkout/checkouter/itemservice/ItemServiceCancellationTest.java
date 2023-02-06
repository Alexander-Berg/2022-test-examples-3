package ru.yandex.market.checkout.checkouter.itemservice;

import java.util.Collection;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceFetcher;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.edit.OrderEditService;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.ALL_COLORS;
import static ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequest.builder;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICES_CANCELLATION;

/**
 * @author zagidullinri
 * @date 16.11.2021
 */
public class ItemServiceCancellationTest extends AbstractWebTestBase {

    @Autowired
    protected YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    protected OrderEditService orderEditService;
    @Autowired
    protected QueuedCallService queuedCallService;
    @Autowired
    protected ItemServiceFetcher itemServiceFetcher;
    @Autowired
    private WireMockServer yaUslugiMock;

    @DisplayName("Проверяем поведение флага hasCancellationRequest")
    @Test
    public void newOrderCancellationRequestShouldCancelItemServices() throws Exception {
        yaUslugiMock.stubFor(
                post(urlPathMatching("/ydo/api/market_partner_orders/services/.*/cancel"))
                        .willReturn(okJson("{}")));
        Order order = createOrderWithItemService();
        ItemService itemService = order.getItems().iterator().next().getServices().iterator().next();
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERY);
        sendOrderCancellationRequest(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(ITEM_SERVICES_CANCELLATION,
                itemService.getId());
        assertEquals(1, result.size());

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICES_CANCELLATION);
        Order reloadedOrder = orderService.getOrder(order.getId());
        ItemService reloadedItemService = itemServiceFetcher.fetchById(itemService.getId());

        assertTrue(queuedCallService.findQueuedCalls(ITEM_SERVICES_CANCELLATION, itemService.getId()).isEmpty());
        assertNotEquals(OrderStatus.CANCELLED, reloadedOrder.getStatus());
        assertEquals(ItemServiceStatus.CANCELLED, reloadedItemService.getStatus());
    }

    @DisplayName("Если заказ \"протух\", и в Я.Услугах не создавался")
    @Test
    public void testExpiredOrderWithItemServiceCancellation() {
        yaUslugiMock.stubFor(
                post(urlPathMatching("/services/.*/cancel"))
                        .willReturn(notFound()));

        Order order = createOrderWithItemService();
        ItemService itemService = order.getItems().iterator().next().getServices().iterator().next();
        sendOrderCancellationRequest(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(ITEM_SERVICES_CANCELLATION,
                itemService.getId());
        assertEquals(1, result.size());

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICES_CANCELLATION);
        Order reloadedOrder = orderService.getOrder(order.getId());
        ItemService reloadedItemService = itemServiceFetcher.fetchById(itemService.getId());

        assertTrue(queuedCallService.findQueuedCalls(ITEM_SERVICES_CANCELLATION, itemService.getId()).isEmpty());
        assertEquals(OrderStatus.CANCELLED, reloadedOrder.getStatus());
        assertEquals(ItemServiceStatus.CANCELLED, reloadedItemService.getStatus());
    }

    private Order createOrderWithItemService() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        return orderCreateHelper.createOrder(parameters);
    }

    private void sendOrderCancellationRequest(Order order) {
        OrderEditRequest orderEditRequest = new OrderEditRequest();
        orderEditRequest.setCancellationRequest(builder()
                .substatus(OrderSubstatus.USER_CHANGED_MIND)
                .notes("")
                .build());
        ClientInfo clientInfo = order.getUserClientInfo();
        orderEditService.editOrder(order.getId(), clientInfo, ALL_COLORS, orderEditRequest);
    }
}
