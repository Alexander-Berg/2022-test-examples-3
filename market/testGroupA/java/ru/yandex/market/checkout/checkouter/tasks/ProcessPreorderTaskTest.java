package ru.yandex.market.checkout.checkouter.tasks;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.ControllerUtils;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.WrongDeliveryOption;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentStatus;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

public class ProcessPreorderTaskTest extends AbstractWebTestBase {

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldProcessPreorderTask(boolean enablePreorderMultipleItemsSupport) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PREORDER_MULTIPLE_ITEMS_SUPPORT,
                enablePreorderMultipleItemsSupport);

        int orderCount = 11;
        List<Order> orders = IntStream.range(0, orderCount).mapToObj(i ->
                createPreorder()).collect(Collectors.toList());

        SSItem item = SSItem.of(FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.FF_SHOP_ID, 300501);

        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(item, 1));
        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();
        tmsTaskHelper.runProcessPreorderTaskV2();

        for (Order order : orders) {
            order = orderService.getOrder(order.getId());

            assertThat(order.getStatus(), is(OrderStatus.PENDING));
            assertThat("order " + order.getId() + " substatus", order.getSubstatus(), is(OrderSubstatus
                    .AWAIT_CONFIRMATION));
        }

        List<ServeEvent> serveEvents = stockStorageConfigurer.getServeEvents();

        for (Order order : orders) {
            long orderId = order.getId();

            ServeEvent checkStocksEvent = serveEvents.stream()
                    .filter(se -> ("/order/getAvailableAmounts").equals(se.getRequest().getUrl()))
                    .findAny()
                    .orElse(null);
            assertThat(checkStocksEvent, notNullValue());

            ServeEvent unfreezeEvent = serveEvents.stream()
                    .filter(se -> ("/preorder/" + orderId + "?cancel=false").equals(se.getRequest().getUrl())
                            && RequestMethod.DELETE.equals(se.getRequest().getMethod()))
                    .findAny()
                    .orElse(null);
            assertThat(unfreezeEvent, notNullValue());

            ServeEvent freezeEvent = serveEvents.stream()
                    .filter(se -> "/order".equals(se.getRequest().getUrl())
                            && RequestMethod.POST.equals(se.getRequest().getMethod()))
                    .findAny()
                    .orElse(null);
            assertThat(freezeEvent, notNullValue());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldClearPreorderOnDeliveryIfWasNotClearedInPending(boolean enablePreorderMultipleItemsSupport) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PREORDER_MULTIPLE_ITEMS_SUPPORT,
                enablePreorderMultipleItemsSupport);
        Order preorder = createPreorder();

        SSItem item = SSItem.of(FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.FF_SHOP_ID, 1);

        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(item, 1));
        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        Order processed = orderService.getOrder(preorder.getId());

        assertThat(preorder.getStatus(), is(OrderStatus.PENDING));
        assertThat(preorder.getPayment().getStatus(), is(PaymentStatus.HOLD));

        orderStatusHelper.proceedOrderToStatus(processed, OrderStatus.DELIVERY);

        Order delivery = orderService.getOrder(preorder.getId());

        assertThat(delivery.getPayment().getStatus(), is(PaymentStatus.CLEARED));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldNotMovePreorderOrderToAwaitConfirmationIfNotEnoughStock(
            boolean enablePreorderMultipleItemsSupport) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PREORDER_MULTIPLE_ITEMS_SUPPORT,
                enablePreorderMultipleItemsSupport);
        Order order = createPreorder();

        SSItem ssItem = SSItem.of(FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.FF_SHOP_ID, 1);
        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(ssItem, 0));
        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(OrderStatus.PENDING));
        assertThat(order.getSubstatus(), is(OrderSubstatus.PREORDER));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldHandleShipmentChangeCorrectly(boolean enablePreorderMultipleItemsSupport) throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PREORDER_MULTIPLE_ITEMS_SUPPORT,
                enablePreorderMultipleItemsSupport);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 15);
        setFixedTime(calendar.toInstant());
        Order order = createPreorder();
        Parcel parcel = order.getDelivery().getParcels().stream().findFirst().get();

        assertThat(parcel.getShipmentDate(), is(DeliveryDates.daysOffsetToLocalDate(getClock(), 2)));

        setFixedTime(getClock().instant().plus(60, ChronoUnit.DAYS));

        SSItem item = SSItem.of(FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.FF_SHOP_ID, 1);
        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(item, 1));
        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        Parameters parameters = createPreorderParameters(order);
        parameters.setContext(Context.SANDBOX);
        parameters.setSandbox(true);
        parameters.setApiSettings(ApiSettings.PRODUCTION);
        MultiCart crmCart = orderCreateHelper.cart(parameters);
        Delivery editDelivery = crmCart.getCarts().get(0).getDeliveryOptions().stream()
                .filter(d -> d.getDeliveryPartnerType() == DeliveryPartnerType.YANDEX_MARKET)
                .findFirst().get();

        orderDeliveryHelper.updateOrderDelivery(order.getId(), editDelivery);

        orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        order = orderService.getOrder(order.getId());

        parcel = order.getDelivery().getParcels().stream().findFirst().get();
        assertThat(parcel.getShipmentDate(), is(DeliveryDates.daysOffsetToLocalDate(getClock(), 2)));

        PagedEvents events = eventService.getPagedOrderHistoryEvents(order.getId(),
                ControllerUtils.getDefaultLimitedPager(),
                null, null, EnumSet.of(HistoryEventType.ORDER_STATUS_UPDATED),
                false, ClientInfo.SYSTEM, null);

        // Проверяем дату в эвенте
        OrderHistoryEvent event = events.getItems()
                .stream()
                .filter(e -> e.getType() == HistoryEventType.ORDER_STATUS_UPDATED && e.getOrderAfter().getStatus() ==
                        OrderStatus.PROCESSING)
                .findFirst().get();

        order = orderService.getOrder(order.getId());

        parcel = order.getDelivery().getParcels().stream().findFirst().get();
        assertThat(parcel.getShipmentDate(), is(DeliveryDates.daysOffsetToLocalDate(getClock(), 2)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void shouldFailOnChangeStatusToProcessing(boolean enablePreorderMultipleItemsSupport) {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.ENABLE_PREORDER_MULTIPLE_ITEMS_SUPPORT,
                enablePreorderMultipleItemsSupport);
        Assertions.assertThrows(WrongDeliveryOption.class, () -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 19);
            setFixedTime(calendar.toInstant());
            Order order = createPreorder();

            Parcel parcel = order.getDelivery().getParcels().stream().findFirst().get();

            assertThat(parcel.getShipmentDate(), is(DeliveryDates.daysOffsetToLocalDate(getClock(), 2)));


            setFixedTime(getClock().instant().plus(60, ChronoUnit.DAYS));

            SSItem item = SSItem.of(FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.FF_SHOP_ID, 1);
            stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(item, 1));
            stockStorageConfigurer.mockOkForUnfreezePreorder();
            stockStorageConfigurer.mockOkForRefreeze();

            tmsTaskHelper.runProcessPreorderTaskV2();

            orderUpdateService.updateOrderStatus(order.getId(), OrderStatus.PROCESSING);
        });
    }

    private Order createPreorder() {
        Order orderRequest = OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        });

        Parameters parameters = createPreorderParameters(orderRequest);

        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.isFulfilment(), is(true));
        assertThat(order.isPreorder(), is(true));
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);

        order = orderService.getOrder(order.getId());

        assertThat(order.getStatus(), is(OrderStatus.PENDING));
        assertThat(order.getSubstatus(), is(OrderSubstatus.PREORDER));
        return order;
    }

    private Parameters createPreorderParameters(Order orderRequest) {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(orderRequest);
        parameters.setWeight(BigDecimal.ONE);
        parameters.setDimensions("10", "10", "10");
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setDeliveryServiceId(100501L);
        return parameters;
    }
}
