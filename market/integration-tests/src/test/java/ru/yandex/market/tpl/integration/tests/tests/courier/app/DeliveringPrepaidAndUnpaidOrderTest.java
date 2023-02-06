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
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.configuration.TestConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.RecipientApiFacade;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DeliveringPrepaidAndUnpaidOrderTest {
    private final ApiFacade apiFacade;
    private final ManualApiFacade manualApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final PublicApiFacade publicApiFacade;
    private final RecipientApiFacade recipientApiFacade;

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
    @DisplayName(value = "Тест доставки предоплаченной и неоплаченной посылки")
    void test() {
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();

        TestConfiguration configuration = AutoTestContextHolder.getContext().getTestConfiguration();
        configuration.setPaymentStatus(OrderPaymentStatus.PAID);
        configuration.setPaymentType(OrderPaymentType.PREPAID);
        manualApiFacade.createEmptyRoutePoint();
        manualApiFacade.createDeliveryTask();
        long firstRoutePoint = AutoTestContextHolder.getContext().getRoutePointId();

        configuration.setPaymentStatus(OrderPaymentStatus.UNPAID);
        configuration.setPaymentType(OrderPaymentType.CASH);
        manualApiFacade.createEmptyRoutePoint();
        manualApiFacade.createDeliveryTask();
        long secondRoutePoint = AutoTestContextHolder.getContext().getRoutePointId();

        apiFacade.pickupOrders();
        publicApiFacade.successCallToRecipient();

        assertThat(recipientApiFacade.getTrackingByRoutePointId(firstRoutePoint).getCourier().getDeliveriesLeft()).isEqualTo(0);
        assertThat(recipientApiFacade.getTrackingByRoutePointId(secondRoutePoint).getCourier().getDeliveriesLeft()).isEqualTo(1);

        configuration.setPaymentStatus(OrderPaymentStatus.PAID);
        configuration.setPaymentType(OrderPaymentType.PREPAID);
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.giveParcel();
        publicApiFacade.finishDeliveryTask();
        publicApiFacade.updateDataButton();

        configuration.setPaymentStatus(OrderPaymentStatus.UNPAID);
        configuration.setPaymentType(OrderPaymentType.CASH);
        publicApiFacade.arriveToRoutePoint();
        apiFacade.giveOrderWithPayment();

        apiFacade.finishShiftInScWithCash();
    }
}
