package ru.yandex.market.checkout.checkouter.tasks;

import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.common.tasks.ZooTask;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.checkout.util.stock.StockStorageConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UnfreezeOldStocksTaskTest extends AbstractWebTestBase {

    @Autowired
    private ZooTask unfreezeOldStocksTask;

    @Autowired
    private StockStorageConfigurer stockStorageConfigurer;

    @Test
    public void shouldUnfreezeStocksForOldCancelledOrder() {
        Order oldOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(oldOrder);
        setFixedTime(getClock().instant().plus(696, ChronoUnit.HOURS)); // 29 days

        prepareAndRun();

        assertEquals(1, stockStorageConfigurer.getServeEvents().size());
        oldOrder = orderService.getOrder(oldOrder.getId());
        assertTrue(orderService.getOrder(oldOrder.getId()).getItems().stream()
                .allMatch(item -> item.getFitFreezed() == 0));
    }

    @Test
    public void shouldUnfreezeStocksForOldCancelledDsbsOrder() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setIgnoreStocks(false);
        parameters.getReportParameters().setOffers(parameters.getItems().stream()
                .map(FoundOfferBuilder::createFrom)
                .peek(WhiteParametersProvider.whiteOffer())
                .peek(b -> b.warehouseId(300501))
                .map(FoundOfferBuilder::build)
                .collect(Collectors.toUnmodifiableList()));
        Order oldOrder = orderCreateHelper.createOrder(parameters);
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(oldOrder);
        setFixedTime(getClock().instant().plus(696, ChronoUnit.HOURS)); // 29 days

        prepareAndRun();

        assertEquals(1, stockStorageConfigurer.getServeEvents().size());
        oldOrder = orderService.getOrder(oldOrder.getId());
        assertTrue(orderService.getOrder(oldOrder.getId()).getItems().stream()
                .allMatch(item -> item.getFitFreezed() == 0));
    }

    @Test
    public void shouldNotUnfreezeStocksForNewCancelledOrder() {
        Order newOrder = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderFromUnpaidToCancelled(newOrder);
        setFixedTime(getClock().instant().plus(20, ChronoUnit.DAYS));

        prepareAndRun();

        assertEquals(0, stockStorageConfigurer.getServeEvents().size());
        newOrder = orderService.getOrder(newOrder.getId());
        assertTrue(orderService.getOrder(newOrder.getId()).getItems().stream()
                .noneMatch(item -> item.getFitFreezed() == 0));
    }

    @Test
    public void shouldNotUnfreezeStocksForOldDeliveredOrder() {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.DELIVERED);
        setFixedTime(getClock().instant().plus(732, ChronoUnit.HOURS)); // 30.5 days

        prepareAndRun();

        assertEquals(0, stockStorageConfigurer.getServeEvents().size());
        order = orderService.getOrder(order.getId());
        assertTrue(orderService.getOrder(order.getId()).getItems().stream()
                .noneMatch(item -> item.getFitFreezed() == 0));
    }

    private void prepareAndRun() {
        stockStorageConfigurer.resetRequests();
        stockStorageConfigurer.mockOkForUnfreeze();
        unfreezeOldStocksTask.runOnce();
    }
}
