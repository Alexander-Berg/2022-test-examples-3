package ru.yandex.market.tpl.integration.tests.tests.courier.app;

import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.order.DetailedOrderDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.client.PublicApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PvzDeliveryComplexTest {
    private final ApiFacade apiFacade;
    private final ManualApiFacade manualApiFacade;
    private final PublicApiFacade publicApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final ManualApiClient manualApiClient;
    private final PublicApiClient publicApiClient;
    private String notPickupedOrder;

    @BeforeEach
    void before() {
        courierApiFacade.createCourierWithSchedule();
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteCourier();
        AutoTestContextHolder.clearContext();
    }

    @Test
    @Feature("Доставка")
    @DisplayName(value = "Тест задания в PVZ, когда забрали из СЦ только 2 из 3, потом cancel, reopen")
    void test() {
        //создаём 3 заказа
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
        manualApiFacade.createLockerDeliveryRoutePoint();
        manualApiFacade.createLockerDeliveryRoutePoint();
        manualApiFacade.createLockerDeliveryRoutePoint();
        var orderIds = retrieveExtOrderIds();

        //забираем из СЦ 2 заказа из трёх
        publicApiFacade.startUserShift();
        publicApiFacade.updateDataButton();
        publicApiFacade.updateCurrentRoutePoint();
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.startOrderPickupTask();
        notPickupedOrder = orderIds.get(2);
        publicApiFacade.scanItems(orderIds.subList(0, 2), orderIds.subList(2, 3), "Забрал не все заказы");
        checkOrderStatus();
        publicApiFacade.updateCurrentRoutePoint();

        //прибываем на точку и сразу отменяем таску, потом переоткрываем
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.cancelLockerTask();
        checkOrderStatus();
        publicApiFacade.reopenLockerTask();
        checkOrderStatus();

        //успешно загружаем и заканчиваем день
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.loadToLocker();
        publicApiFacade.unloadRandomOrderFromLockerSuccessfully();
        apiFacade.finishShiftInSc();
    }

    private List<String> retrieveExtOrderIds() {
        RoutePointDto routePoint = AutoTestContextHolder.getContext().getRoutePoint();
        LockerDeliveryTaskDto task = (LockerDeliveryTaskDto) routePoint.getTasks().iterator().next();
        List<String> ids = task.getOrders().stream().map(OrderDto::getExternalOrderId).collect(Collectors.toList());
        assertThat(ids).hasSize(3);
        return ids;
    }

    private void checkOrderStatus() {
        DetailedOrderDto detailedOrder =
                manualApiClient.getDetailedOrderInfoByExtId(notPickupedOrder);
        assertThat(detailedOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.SORTING_CENTER_PREPARED);
        assertThat(detailedOrder.getOrderDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
    }

    private void checkOneFailedTask() {
        List<OrderSummaryDto> tasks = publicApiClient.getTasks().getTasks();
        assertThat(tasks.size()).isEqualTo(1);
        OrderSummaryDto task = tasks.iterator().next();
        assertThat(task.getTaskStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
    }
}
