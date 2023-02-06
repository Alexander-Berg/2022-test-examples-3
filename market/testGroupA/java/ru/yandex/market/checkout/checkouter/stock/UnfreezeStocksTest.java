package ru.yandex.market.checkout.checkouter.stock;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.matching.RequestPattern;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.http.HttpStatus;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Features;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.shipment.ParcelStatus;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.FreezeHelper;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.balance.ShopSettingsHelper;
import ru.yandex.market.checkout.util.report.ItemInfo;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static ru.yandex.common.util.date.DateUtil.SIMPLE_DATE_FORMAT;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType.CARRIER;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType.FULFILLMENT;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.CANCELLED;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.DELIVERY;
import static ru.yandex.market.checkout.checkouter.order.OrderStatus.PLACING;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.INTAKE_AVAILABLE_DATE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * Проверить unfreeze при отмене для всех статусов до DELIVERY + been-called=false
 * https://testpalm.yandex-team.ru/testcase/checkouter-196
 * <p>
 * Проверить отсутствие unfreeze при отмене для всех статусов до DELIVERY + been-called=true
 * https://testpalm.yandex-team.ru/testcase/checkouter-197
 *
 * @author Nikolai Iusiumbeli
 * date: 20/12/2017
 */
public class UnfreezeStocksTest extends AbstractWebTestBase {

    private static final long FULFILMENT_SHOP_ID = 100500L;

    @Autowired
    private WireMockServer stockStorageMock;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private ZooTask itemsUnfreezeTask;
    @Autowired
    private OrderGetHelper orderGetHelper;
    @Autowired
    private FreezeHelper freezeHelper;

    private final Order fulfillmentOrder;

    public UnfreezeStocksTest() {

        Order order = OrderProvider.getFulfillmentOrderWithYandexDelivery();
        order.getItems().forEach(oi -> {
            FulfilmentProvider.addFulfilmentFields(
                    oi, FulfilmentProvider.TEST_SKU, FulfilmentProvider.TEST_SHOP_SKU, FULFILMENT_SHOP_ID
            );
            oi.setWarehouseId(1);
            oi.setFulfilmentWarehouseId(1L);
            oi.setFitFreezed(oi.getCount());
        });

        fulfillmentOrder = order;
        fulfillmentOrder.getDelivery().setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        fulfillmentOrder.setFeeTotal(BigDecimal.valueOf(250));
        fulfillmentOrder.setTotal(BigDecimal.valueOf(250));
        fulfillmentOrder.setItemsTotal(BigDecimal.valueOf(250));
        fulfillmentOrder.setBuyerTotal(BigDecimal.valueOf(260));
        fulfillmentOrder.setBuyerItemsTotal(BigDecimal.valueOf(250));
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить unfreeze стока при отмене заказа из статуса PLACING + been-called=false")
    @Test
    public void testUnfreezeFromPlacing() {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        assertFalse(orderService.getOrder(orderId).getBuyer().isBeenCalled());
        cancelAndCheckUnfreezeCalled(orderId);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить unfreeze стока при переходе заказа в PROCESSING.SHIPPED")
    @Test
    public void testUnfreezeOnProcessingShipped() {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        assertFalse(orderService.getOrder(orderId).getBuyer().isBeenCalled());

        checkUnfreezeCalled(orderId, () ->
                orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.SHIPPED,
                        ClientInfo.SYSTEM));
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить, что только один unfreeze стока при переходе заказа в DELIVERY после PROCESSING.SHIPPED")
    @Test
    public void testUnfreezeOnlyOnce() {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.STARTED);
        assertFalse(orderService.getOrder(orderId).getBuyer().isBeenCalled());

        checkUnfreezeCalled(orderId, () ->
                orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING, OrderSubstatus.SHIPPED,
                        ClientInfo.SYSTEM
                ));
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
        orderUpdateService.updateOrderStatus(orderId, DELIVERY, null, ClientInfo.SYSTEM);
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(1));
    }


    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить unfreeze стока при отмене заказа из статуса RESERVED + been-called=false")
    @Test
    public void testUnfreezeFromReserved() {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        assertFalse(orderService.getOrder(orderId).getBuyer().isBeenCalled());
        cancelAndCheckUnfreezeCalled(orderId);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить unfreeze стока при отмене заказа из статуса PENDING + been-called=false")
    @Test
    public void testUnfreezeFromPending() {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PENDING, OrderSubstatus.AWAIT_CONFIRMATION);
        assertFalse(orderService.getOrder(orderId).getBuyer().isBeenCalled());
        cancelAndCheckUnfreezeCalled(orderId, ClientInfo.SYSTEM, OrderSubstatus.SHOP_PENDING_CANCELLED);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить unfreeze стока при отмене заказа из статуса UNPAID + been-called=false")
    @Test
    public void testUnfreezeFromUnpaid() {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.UNPAID, OrderSubstatus.WAITING_USER_INPUT);
        assertFalse(orderService.getOrder(orderId).getBuyer().isBeenCalled());
        cancelAndCheckUnfreezeCalled(orderId);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить unfreeze стока при отмене заказа из статуса PROCESSING " +
            "+ parcel.status=ERROR + нет треков от СЦ")
    @Test
    public void testUnfreezeFromProcessing() throws Exception {
        Long orderId = orderCreateService.createOrder(fulfillmentOrder, ClientInfo.SYSTEM);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.RESERVED);
        orderUpdateService.updateOrderStatus(orderId, OrderStatus.PROCESSING);
        //add parcel with 1 tracks
        Parcel parcel = new Parcel();
        parcel.setStatus(ParcelStatus.ERROR);

        Delivery delivery = new Delivery();
        delivery.setParcels(Collections.singletonList(parcel));
        orderDeliveryHelper.updateOrderDelivery(orderId, delivery);

        Order order = orderService.getOrder(orderId);
        Assertions.assertEquals(1, order.getDelivery().getParcels().size());
        Assertions.assertEquals(ParcelStatus.ERROR, order.getDelivery().getParcels().get(0).getStatus());

        cancelAndCheckUnfreezeCalled(orderId);
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить, что чепоинт 105 (отменено в СЦ) анфризит и отменяет заказ")
    @Test
    public void testUnfreezeSCCancelledCheckpoint() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(FULFILLMENT);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        checkUnfreezeCalled(order.getId(), () -> {
            try {
                notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID,
                        105));
            } catch (Exception e) {
                //won't throw
            }
        });

        order = orderService.getOrder(order.getId());
        assertThat(order, hasProperty("status", is(CANCELLED)));
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить, что чепоинт 130 (отправлено из СЦ) анфризит заказ")
    @Test
    public void testUnfreezeSCSentCheckpoint() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(FULFILLMENT);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 130));

        setFixedTime(INTAKE_AVAILABLE_DATE.plus(5, ChronoUnit.HOURS));
        checkUnfreezeCalled(order.getId(), () -> itemsUnfreezeTask.runOnce());
    }

    @DisplayName("Проверить, что чепоинт 42 анфризит заказ")
    @Test
    public void testUnfreezeSdStorageExtendedCheckpoint() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(CARRIER);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 42));

        setFixedTime(INTAKE_AVAILABLE_DATE.plus(5, ChronoUnit.HOURS));
        checkUnfreezeCalled(order.getId(), () -> itemsUnfreezeTask.runOnce());
    }

    @DisplayName("Проверить, что чепоинт 20 (отправлено из СД) анфризит заказ")
    @Test
    public void testUnfreezeSdStartedCheckpoint() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(CARRIER);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 20));

        setFixedTime(INTAKE_AVAILABLE_DATE.plus(5, ChronoUnit.HOURS));
        checkUnfreezeCalled(order.getId(), () -> itemsUnfreezeTask.runOnce());
    }

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @Feature(Features.FULFILLMENT)
    @DisplayName("Проверить, что чепоинт 120 не анфризит заказ")
    @Test
    public void testNotUnfreeze120Checkpoint() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .build();

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.CANCELLED);

        orderDeliveryHelper.addTrack(order.getId(),
                order.getDelivery().getParcels().get(0).getId(),
                new Track("iddqd", MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID) {{
                    setDeliveryServiceType(FULFILLMENT);
                }},
                ClientInfo.SYSTEM);

        MockTrackerHelper.mockGetDeliveryServices(MOCK_SORTING_CENTER_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 120));

        setFixedTime(INTAKE_AVAILABLE_DATE.plus(5, ChronoUnit.HOURS));

        stockStorageMock.resetAll();
        itemsUnfreezeTask.runOnce();
        assertThat(stockStorageMock.countRequestsMatching(RequestPattern.everything()).getCount(), Is.is(0));
    }

    private void cancelAndCheckUnfreezeCalled(Long orderId) {
        cancelAndCheckUnfreezeCalled(orderId, ClientInfo.SYSTEM, OrderSubstatus.USER_CHANGED_MIND);
    }

    private void cancelAndCheckUnfreezeCalled(Long orderId, ClientInfo clientInfo, OrderSubstatus substatus) {

        checkUnfreezeCalled(orderId, () ->
                orderUpdateService.updateOrderStatus(orderId, OrderStatus.CANCELLED, substatus, clientInfo));
    }

    private void checkUnfreezeCalled(Long orderId, Runnable action) {
        stockStorageMock.resetAll();
        stockStorageMock.stubFor(delete(urlPathMatching("/order/" + orderId))
                .willReturn(aResponse().withStatus(HttpStatus.SC_OK)));
        action.run();
        stockStorageMock.verify(deleteRequestedFor(urlPathMatching("/order/" + orderId)));
    }

    @Test
    public void testFreezeCount0Reserved() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setAcceptOrder(false);
        parameters.turnOffErrorChecks();
        Order order = orderCreateHelper.createOrder(parameters);

        String fromDate = SIMPLE_DATE_FORMAT.format(new Date(getClock().instant().toEpochMilli()));
        String toDate = SIMPLE_DATE_FORMAT.format(new Date(getClock().instant().plus(1, ChronoUnit.DAYS)
                .toEpochMilli()));
        Collection<Order> orders = orderGetHelper.getOrders(ClientInfo.SYSTEM, fromDate, toDate).getItems();

        assertThat(orders, contains(
                hasProperty("status", is(PLACING))
        ));

        freezeHelper.assertFreezeCount(orders.stream().findFirst().get().getId(), 1);
    }

    @Test
    public void testSuccessfulOrderHas1FreezeCount() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        var parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getReportParameters().setIgnoreStocks(false);
        var order = orderCreateHelper.createOrder(parameters);

        freezeHelper.assertFreezeCount(order.getId(), 1);
        cancelAndCheckUnfreezeCalled(order.getId());
        freezeHelper.assertFreezeCount(order.getId(), 0);
    }

    @Test
    public void testSuccessfulFewItemsOrderHasFreezeCount() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);

        Order orderSetup = OrderProvider.getBlueOrder();
        List<OrderItem> items = new ArrayList<>(orderSetup.getItems());
        OrderItem orderItem = OrderItemProvider.getAnotherOrderItem();
        orderItem.setCount(2);
        items.add(orderItem);
        orderSetup.setItems(items);

        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .withOrder(orderSetup)
                .buildParameters();
        parameters.getReportParameters().overrideItemInfo(orderItem.getFeedOfferId())
                .setFulfilment(new ItemInfo.Fulfilment(
                        FulfilmentProvider.ANOTHER_FF_SHOP_ID,
                        FulfilmentProvider.ANOTHER_TEST_SKU,
                        FulfilmentProvider.ANOTHER_TEST_SHOP_SKU
                ));
        parameters.addShopMetaData(FulfilmentProvider.ANOTHER_FF_SHOP_ID, ShopSettingsHelper.getDefaultMeta());
        parameters.getReportParameters().setIgnoreStocks(false);
        Order order = orderCreateHelper.createOrder(parameters);

        freezeHelper.assertFreezeCount(order.getId(), 1, 2);
        cancelAndCheckUnfreezeCalled(order.getId());
        freezeHelper.assertFreezeCount(order.getId(), 0, 0);
    }

    @Test
    public void fakeOrderHas0Freeze() throws Exception {
        setFixedTime(INTAKE_AVAILABLE_DATE);
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.setSandbox(true);
        parameters.setCheckOrderCreateErrors(false);
        parameters.setErrorMatcher(jsonPath("$.checkedOut").value(true));
        Order order = orderCreateHelper.createOrder(parameters);
        assertTrue(order.isFake());
        assertTrue(order.isFulfilment());
        String fromDate = SIMPLE_DATE_FORMAT.format(new Date(getClock().instant().toEpochMilli()));
        String toDate = SIMPLE_DATE_FORMAT.format(new Date(getClock().instant().plus(1, ChronoUnit.DAYS)
                .toEpochMilli()));
        Collection<Order> orders = orderGetHelper.getOrders(ClientInfo.SYSTEM, fromDate, toDate).getItems();
        freezeHelper.assertFreezeCount(orders.stream().findFirst().get().getId(), 0);
    }
}
