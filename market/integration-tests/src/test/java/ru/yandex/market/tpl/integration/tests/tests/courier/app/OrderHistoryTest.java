package ru.yandex.market.tpl.integration.tests.tests.courier.app;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.task.call.CallTaskDto;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContext;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PartnerApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderHistoryTest {
    private final ApiFacade apiFacade;
    private final PublicApiFacade publicApiFacade;
    private final ManualApiFacade manualApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final PartnerApiFacade partnerApiFacade;

    @BeforeEach
    void before() {
        AutoTestContext context = AutoTestContextHolder.getContext();
        TestConfiguration configuration = context.getTestConfiguration();
        configuration.setRecipientCallEnabled(true);
        courierApiFacade.createCourierWithSchedule();
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteCourier();
        AutoTestContextHolder.clearContext();
    }

    @Test
    @Feature("История заказа")
    @Stories({@Story("Обновить данные клиента"), @Story("Проверить наличие истории")})
    @DisplayName(value = "Тест отображения истории после обновления данных клиента")
    void test() {
        apiFacade.createOrder();
        apiFacade.pickupOrders();

        partnerApiFacade.updateRecipientData();
        publicApiFacade.updateDataButton();
        publicApiFacade.updateCurrentRoutePoint();

        checkHistoryInCallTask();
        checkHistoryInDeliveryTask();
    }

    private void checkHistoryInCallTask() {
        List<CallTaskDto> callTasks = AutoTestContextHolder.getContext().getRoutePoint().getCallTasks();
        Assertions.assertThat(callTasks.size()).isEqualTo(1);
        var orderHistory = callTasks.iterator().next().getOrders().iterator().next().getOrderHistory();
        Assertions.assertThat(orderHistory).isNotNull();
        Assertions.assertThat(orderHistory.getName()).isNotEmpty();
        Assertions.assertThat(orderHistory.getPhone()).isNotEmpty();
    }

    private void checkHistoryInDeliveryTask() {
        var deliveryTaskDto = publicApiFacade.getDeliveryTask();
        var orderHistory = deliveryTaskDto.getOrder().getOrderHistory();
        Assertions.assertThat(orderHistory).isNotNull();
        Assertions.assertThat(orderHistory.getName()).isNotEmpty();
        Assertions.assertThat(orderHistory.getPhone()).isNotEmpty();
    }
}
