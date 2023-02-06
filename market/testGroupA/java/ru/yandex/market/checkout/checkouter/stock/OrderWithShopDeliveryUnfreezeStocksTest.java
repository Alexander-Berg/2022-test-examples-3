package ru.yandex.market.checkout.checkouter.stock;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import org.apache.http.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.StatusAndSubstatus;
import ru.yandex.market.checkout.checkouter.shop.pushapi.SettingsService;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;

public class OrderWithShopDeliveryUnfreezeStocksTest extends AbstractWebTestBase {

    private static final Settings DEFAULT_SETTINGS_WITH_PARTNER_INTERFACE = Settings.builder()
            .urlPrefix("prefix")
            .authType(AuthType.URL)
            .authToken("token")
            .dataType(DataType.XML)
            .partnerInterface(true)
            .build();

    @Autowired
    private WireMockServer stockStorageMock;
    @Autowired
    private SettingsService settingsService;
    private Order orderWithShopDelivery;

    private Order orderWithIgnoreStocks;

    public OrderWithShopDeliveryUnfreezeStocksTest() {
        orderWithShopDelivery = createOrderWithShopDelivery();

        orderWithIgnoreStocks = createOrderWithShopDelivery();
        orderWithIgnoreStocks.setIgnoreStocks(true);
    }

    private Order createOrderWithShopDelivery() {
        Order order = OrderProvider.getBlueOrder();
        order.setFulfilment(false);
        order.setDelivery(DeliveryProvider.getShopDeliveryWithPickupType());
        order.getItems().forEach(oi -> {
            oi.setFitFreezed(oi.getCount());
            oi.setAtSupplierWarehouse(true);
        });
        return order;
    }

    private Order createDsbsOrder() {
        Order order = OrderProvider.getColorOrder(Color.WHITE);
        order.setFulfilment(false);
        order.setDelivery(DeliveryProvider.getShopDelivery());
        order.getItems().forEach(oi -> {
            oi.setFitFreezed(oi.getCount());
        });
        return order;
    }

    @Test
    public void testUnfreezeCancelFromPlacing() {
        Long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        settingsService.updateSettings(orderWithShopDelivery.getShopId(),
                DEFAULT_SETTINGS_WITH_PARTNER_INTERFACE, false);
        cancelAndCheckUnfreezeCalled(orderId, true);
    }

    @Test
    public void testUnfreezeCancelFromReserved() {
        Long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        settingsService.updateSettings(orderWithShopDelivery.getShopId(),
                DEFAULT_SETTINGS_WITH_PARTNER_INTERFACE, false);

        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        cancelAndCheckUnfreezeCalled(orderId, true);
    }

    @Test
    public void testUnfreezeCancelFromUnpaid() {
        Long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        settingsService.updateSettings(orderWithShopDelivery.getShopId(),
                DEFAULT_SETTINGS_WITH_PARTNER_INTERFACE, false);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.UNPAID, OrderSubstatus.WAITING_USER_INPUT);
        cancelAndCheckUnfreezeCalled(orderId, true);
    }

    @Test
    public void testUnfreezeCancelFromPending() {
        Long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        settingsService.updateSettings(orderWithShopDelivery.getShopId(),
                DEFAULT_SETTINGS_WITH_PARTNER_INTERFACE, false);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);
        cancelAndCheckUnfreezeCalled(orderId, true);
    }

    @Test
    public void testUnfreezeCancelFromProcessing() {
        Long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(0));
    }

    @Test
    public void testUnfreezeToDelivery() {
        Long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        checkUnfreezeCalled(orderId, () ->
                orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY, null,
                        ClientInfo.SYSTEM));
    }

    @Test
    public void testUnfreezeToDelivered() {
        Order dsbsOrder = createDsbsOrder();
        long orderId = orderCreateService.createOrder(dsbsOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY);
        checkUnfreezeCalled(orderId, () ->
                orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERED, null,
                        ClientInfo.SYSTEM));
    }

    @Test
    public void testUnfreezeToPickup() {
        long orderId = orderCreateService.createOrder(orderWithShopDelivery, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY);
        checkUnfreezeCalled(orderId, () ->
                orderUpdateService.updateOrderStatus(orderId, OrderStatus.PICKUP, null,
                        ClientInfo.SYSTEM));
    }

    @Test
    public void testIgnoreStocksToCancelled() {
        Long orderId = orderCreateService.createOrder(orderWithIgnoreStocks, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND,
                ClientInfo.SYSTEM);
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(0));
    }

    @Test
    public void testIgnoreStocksToDelivery() {
        Long orderId = orderCreateService.createOrder(orderWithIgnoreStocks, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY, null,
                ClientInfo.SYSTEM);
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(0));
    }

    @Test
    public void testIgnoreStocksToDelivered() {
        Order dsbsOrderWithIgnoreStocks = createDsbsOrder();
        dsbsOrderWithIgnoreStocks.setIgnoreStocks(true);
        long orderId = orderCreateService.createOrder(dsbsOrderWithIgnoreStocks, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERED, null, ClientInfo.SYSTEM);
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(0));
    }

    @Test
    public void testIgnoreStocksToPickup() {
        long orderId = orderCreateService.createOrder(orderWithIgnoreStocks, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.DELIVERY);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PICKUP, null, ClientInfo.SYSTEM);
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(0));
    }

    private void cancelAndCheckUnfreezeCalled(Long orderId, boolean withForceApi) {
        cancelAndCheckUnfreezeCalled(orderId, ClientInfo.SYSTEM, OrderSubstatus.USER_CHANGED_MIND, withForceApi);
    }

    private void cancelAndCheckUnfreezeCalled(Long orderId, ClientInfo clientInfo, OrderSubstatus substatus,
                                              boolean useForceApi) {

        if (useForceApi) {
            checkForceUnfreezeCalled(() ->
                    orderUpdateService.updateOrderStatus(orderId,
                            StatusAndSubstatus.of(OrderStatus.CANCELLED, substatus), clientInfo));
        } else {
            checkUnfreezeCalled(orderId, () ->
                    orderUpdateService.updateOrderStatus(orderId,
                            StatusAndSubstatus.of(OrderStatus.CANCELLED, substatus), clientInfo)
            );
        }
    }

    private void checkUnfreezeCalled(Long orderId, Runnable action) {
        stockStorageMock.resetAll();
        stockStorageMock.stubFor(delete(urlPathMatching("/order/" + orderId))
                .willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
        action.run();
        stockStorageMock.verify(deleteRequestedFor(urlPathMatching("/order/" + orderId)));
    }

    private void checkForceUnfreezeCalled(Runnable action) {
        stockStorageMock.resetAll();
        stockStorageMock.stubFor(post(urlPathMatching("/stocks/force-unfreeze"))
                .willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
        action.run();
        stockStorageMock.verify(postRequestedFor(urlPathMatching("/stocks/force-unfreeze")));
    }
}
