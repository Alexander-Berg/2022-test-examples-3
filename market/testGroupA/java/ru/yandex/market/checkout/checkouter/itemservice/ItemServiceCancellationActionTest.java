package ru.yandex.market.checkout.checkouter.itemservice;

import java.util.Collection;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceFetcher;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICES_CANCELLATION;

public class ItemServiceCancellationActionTest extends AbstractWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;
    @Autowired
    private WireMockServer yaUslugiMock;
    @Autowired
    private ItemServiceFetcher itemServiceFetcher;

    private static Collection<OrderStatus> args() {
        return List.of(OrderStatus.UNPAID, OrderStatus.DELIVERY);
    }

    @ParameterizedTest(name = "should add ITEM_SERVICES_CANCELLATION QC when cancelling order with status {0}")
    @MethodSource("args")
    public void shouldAddItemServiceCancellationQueuedCall(OrderStatus status) {
        yaUslugiMock.stubFor(
                post(urlPathMatching("/ydo/api/market_partner_orders/services/.*/cancel"))
                        .willReturn(okJson("{}")));
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);
        ItemService itemService = order.getItems().iterator().next().getServices().iterator().next();
        orderStatusHelper.proceedOrderToStatus(order, status);

        orderStatusHelper.proceedOrderToStatus(orderService.getOrder(order.getId()), OrderStatus.CANCELLED);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(ITEM_SERVICES_CANCELLATION,
                itemService.getId());
        assertEquals(1, result.size());
        queuedCallService.executeQueuedCallBatch(ITEM_SERVICES_CANCELLATION);
        assertTrue(queuedCallService.findQueuedCalls(ITEM_SERVICES_CANCELLATION, itemService.getId()).isEmpty());
        Order updatedOrder = orderService.getOrder(order.getId());
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus());
        ItemService updatedItemService = itemServiceFetcher.fetchById(itemService.getId());
        assertEquals(ItemServiceStatus.CANCELLED, updatedItemService.getStatus());

    }

    @Test
    public void shouldNotAddItemServiceCancellationQueueCall() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);

        orderStatusHelper.proceedOrderFromUnpaidToCancelled(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCallsByOrderId(order.getId());
        assertTrue(result.isEmpty());
    }
}
