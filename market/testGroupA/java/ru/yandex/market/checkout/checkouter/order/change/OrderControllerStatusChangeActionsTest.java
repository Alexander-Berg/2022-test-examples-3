package ru.yandex.market.checkout.checkouter.order.change;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.event.OrderEventPublishService;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.status.actions.InstantUnfreezeStocksAction;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.service.StockStorageService;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.FulfilmentProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItem;
import ru.yandex.market.fulfillment.stockstorage.client.entity.dto.SSItemAmount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Created by asafev on 30/10/2017.
 */
public class OrderControllerStatusChangeActionsTest extends AbstractWebTestBase {

    @Autowired
    private InstantUnfreezeStocksAction instantUnfreezeStocksAction;
    @Autowired
    private StockStorageService stockStorage;
    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;
    @Autowired
    private ZooTask itemsUnfreezeTask;
    @Autowired
    private OrderEventPublishService orderEventPublishService;

    @Epic(Epics.CHANGE_ORDER)
    @Story(Stories.ORDERS_STATUS)
    @DisplayName("При смене статуса fulfilment-заказа на CANCELLED снимается фриз стоков в StockStorage.")
    @Test
    // checkouter-164
    public void setInstantUnfreezeStocksWhenOrderCancelled() throws Exception {
        List<Order> unfreezedOrders = new ArrayList<>();

        instantUnfreezeStocksAction.setStockStorage(createSsMock(unfreezedOrders));
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().setIgnoreStocks(false);
        Order order = orderCreateHelper.createOrder(parameters);
        assertEquals(OrderStatus.UNPAID, order.getStatus());
        order = orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus
                .SHOP_PENDING_CANCELLED);

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertThat("Не сняты фризы с айтемов заказов!", unfreezedOrders, not(empty()));
        instantUnfreezeStocksAction.setStockStorage(stockStorage);
    }

    @Test
    public void shouldCancelPreorderStockIfCancelledBeforeProcessPreorder() {
        Order orderRequest = OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        });

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(orderRequest);
        parameters.setWeight(BigDecimal.ONE);
        parameters.setDimensions("10", "10", "10");
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setDeliveryServiceId(100501L);
        parameters.getReportParameters().setIgnoreStocks(false);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);

        Instant time = ZonedDateTime.of(2018, 9, 26, 19, 0, 0, 0, ZoneId.systemDefault()).toInstant();

        setFixedTime(time);

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        stockStorageConfigurer.mockOkForUnfreezePreorder();
        setFixedTime(time.plusSeconds(1).plus(4, ChronoUnit.HOURS));
        itemsUnfreezeTask.runOnce();

        assertThat(stockStorageConfigurer.getServeEvents().stream()
                        .filter(se -> se.getResponse().getStatus() == 200)
                        .collect(Collectors.toList()), hasItem(new BaseMatcher<ServeEvent>() {
                    @Override
                    public boolean matches(Object item) {
                        if (!(item instanceof ServeEvent)) {
                            return false;
                        }

                        LoggedRequest request = ((ServeEvent) item).getRequest();
                        return RequestMethod.DELETE.equals(request.getMethod()) &&
                                request.getUrl().startsWith("/preorder/" + order.getId());

                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("has method 'DELETE' and url = '/preorder/" + order.getId() + "'");
                    }

                    @Override
                    public void describeMismatch(Object item, Description description) {
                        if (!(item instanceof ServeEvent)) {
                            return;
                        }

                        LoggedRequest request = ((ServeEvent) item).getRequest();
                        description.appendText("was method '" + request.getMethod() + "' and url = '" + request
                                .getUrl() + "'");
                    }
                })
        );
    }

    @Test
    public void shouldCancelFitStockIfCancelledAfterProcessPreorder() {
        Order orderRequest = OrderProvider.getBlueOrder(o -> {
            o.getItems().forEach(oi -> oi.setPreorder(true));
        });

        SSItem item = SSItem.of(FulfilmentProvider.TEST_SHOP_SKU, FulfilmentProvider.FF_SHOP_ID, 300501);

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(orderRequest);
        parameters.setWeight(BigDecimal.ONE);
        parameters.setDimensions("10", "10", "10");
        parameters.setPaymentMethod(PaymentMethod.YANDEX);
        parameters.setDeliveryType(DeliveryType.PICKUP);
        parameters.setDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET);
        parameters.setDeliveryServiceId(100501L);
        parameters.getReportParameters().setIgnoreStocks(false);

        Order order = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PENDING);

        stockStorageConfigurer.mockGetAvailableCount(SSItemAmount.of(item, 1));
        stockStorageConfigurer.mockOkForUnfreezePreorder();
        stockStorageConfigurer.mockOkForRefreeze();

        tmsTaskHelper.runProcessPreorderTaskV2();

        Order processing = orderService.getOrder(order.getId());

        assertThat(processing.getStatus(), is(OrderStatus.PENDING));
        assertThat(processing.getSubstatus(), is(OrderSubstatus.AWAIT_CONFIRMATION));

        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.of(0, 10));
        orderEventPublishService.publishEventsBatch(Integer.MAX_VALUE, Partition.NULL);
        Instant time = ZonedDateTime.of(2018, 9, 26, 19, 0, 0, 0, ZoneId.systemDefault()).toInstant();
        setFixedTime(time);

        orderStatusHelper.updateOrderStatus(order.getId(), OrderStatus.CANCELLED, OrderSubstatus.USER_CHANGED_MIND);

        stockStorageConfigurer.mockOkForUnfreeze();
        setFixedTime(time.plusSeconds(1).plus(4, ChronoUnit.HOURS));
        itemsUnfreezeTask.runOnce();

        Assertions.assertTrue(stockStorageConfigurer.getServeEvents().stream()
                .filter(se -> se.getResponse().getStatus() == 200)
                .anyMatch(se ->
                        RequestMethod.DELETE.equals(se.getRequest().getMethod()) &&
                                se.getRequest().getUrl().startsWith("/order/" + order.getId()))
        );

    }

    private StockStorageService createSsMock(List<Order> unfreezedOrders) {
        StockStorageService service = mock(StockStorageService.class);
        doAnswer(invocation -> {
            unfreezedOrders.add(invocation.getArgument(0));
            return true;
        }).when(service).tryUnfreezeStocksWithCancellationOrScheduleOnFail(any(Order.class), anyBoolean(),
                anyBoolean(), anyBoolean());
        doAnswer(invocation -> {
            unfreezedOrders.add(invocation.getArgument(0));
            return true;
        }).when(service).tryUnfreezeStocksWithCancellationOrScheduleOnFail(any(Order.class), anyBoolean(),
                anyBoolean());
        return service;
    }

}
