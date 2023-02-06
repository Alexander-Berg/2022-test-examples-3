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

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.shift.UserShiftPayStatisticsDto;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatisticsDto;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContext;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ChequeCancellationTest {
    private final ApiFacade apiFacade;
    private final PublicApiFacade publicApiFacade;
    private final ManualApiFacade manualApiFacade;
    private final CourierApiFacade courierApiFacade;

    @BeforeEach
    void before() {
        AutoTestContext context = AutoTestContextHolder.getContext();
        TestConfiguration configuration = context.getTestConfiguration();
        configuration.setPaymentStatus(OrderPaymentStatus.UNPAID);
        configuration.setPaymentType(OrderPaymentType.CASH);
        courierApiFacade.createCourierWithSchedule();
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteCourier();
        AutoTestContextHolder.clearContext();
    }

    @Test
    @Feature("Доставка")
    @Stories({@Story("Забрать посылку из СЦ"), @Story("Оплата наличными"),
            @Story("Отмена чека"), @Story("Оплата картой")})
    @DisplayName(value = "Тест отмены чека курьером")
    void test() {
        apiFacade.createAndPickupOrder();
        publicApiFacade.arriveToRoutePoint();
        apiFacade.giveOrderWithPayment();
        publicApiFacade.cancelCheque();
        AutoTestContextHolder.getContext().getTestConfiguration().setPaymentType(OrderPaymentType.CARD);
        apiFacade.giveOrderWithPayment();
        apiFacade.finishShiftInScWithCash();
        checkStatistic();
    }

    private void checkStatistic() {
        UserShiftStatisticsDto shiftStatistics = publicApiFacade.getShiftStatistics();
        assertThat(shiftStatistics.getNumberOfAllTasks()).isEqualTo(1);
        assertThat(shiftStatistics.getNumberOfFinishedTasks()).isEqualTo(1);
        UserShiftPayStatisticsDto shiftPayStatistics = publicApiFacade.getShiftPayStatistics();
        assertThat(shiftPayStatistics.getPayments().size()).isEqualTo(1);
        UserShiftPayStatisticsDto.PayDtoEntry pay = shiftPayStatistics.getPayments().iterator().next();
        assertThat(pay.getPaymentType()).isEqualTo(OrderPaymentType.CARD);
    }

}
