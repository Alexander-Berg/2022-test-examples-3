package ru.yandex.market.tpl.integration.tests.tests.courier.app;


import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.order.DetailedOrderDto;
import ru.yandex.market.tpl.api.model.order.OrderDeliveryStatus;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderSummaryDto;
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
public class PvzDeliveryReopenSuccessfullyLoadedTest {
    private final ApiFacade apiFacade;
    private final ManualApiFacade manualApiFacade;
    private final PublicApiFacade publicApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final ManualApiClient manualApiClient;
    private final PublicApiClient publicApiClient;

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
    @DisplayName(value = "Тест доставки отмены и переоткрытия задания в PVZ")
    void test() {
        apiFacade.createLockerOrder();
        apiFacade.pickupOrders();

        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.loadToLocker();
        publicApiFacade.unloadRandomOrderFromLockerSuccessfully();
        reopen();
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.loadToLocker();
        publicApiFacade.unloadRandomOrderFromLockerSuccessfully();
        apiFacade.finishShiftInSc();
    }

    @Step("Возобновляем задание")
    private void reopen() {
        publicApiFacade.reopenLockerTask();
        checkOrderStatus();
        checkUnloadOrdersIsEmpty();
    }

    private void checkOrderStatus() {
        AutoTestContextHolder.getContext().getRoutePoint().getTasks().stream()
                .map(LockerDeliveryTaskDto.class::cast)
                .flatMap(x -> x.getOrders().stream())
                .forEach(order -> {
                    DetailedOrderDto detailedOrder =
                            manualApiClient.getDetailedOrderInfoByExtId(order.getExternalOrderId());
                    assertThat(detailedOrder.getOrderFlowStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
                    assertThat(detailedOrder.getOrderDeliveryStatus()).isEqualTo(OrderDeliveryStatus.NOT_DELIVERED);
                });
    }

    private void checkUnloadOrdersIsEmpty() {
        List<OrderSummaryDto> tasks = publicApiClient.getTasks().getTasks();
        assertThat(tasks.size()).isEqualTo(1);
    }
}
