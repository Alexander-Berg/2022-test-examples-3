package ru.yandex.market.tpl.integration.tests.tests.courier.app;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveringPrepaidMultiOrderTest {
    private final ApiFacade apiFacade;
    private final ManualApiFacade manualApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final PublicApiFacade publicApiFacade;

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
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Успешная доставка")})
    @DisplayName(value = "Тест доставки предоплаченной мульти посылки")
    void test() {
        apiFacade.createAndPickupMultiOrder();
        publicApiFacade.arriveToRoutePoint();
        OrderDeliveryTaskDto task1 = (OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().get(0);
        publicApiFacade.giveParcel(task1);
        publicApiFacade.finishDeliveryTask();
        publicApiFacade.updateDataButton();
        OrderDeliveryTaskDto task2 = (OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().get(1);
        publicApiFacade.giveParcel(task2);
        publicApiFacade.finishLastDeliveryTask();
        apiFacade.finishShiftInSc();
    }
}
