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

import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.IN_TRANSIT;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.UNFINISHED;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PostponeMultiOrderTest {
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
    @Feature("Отложить заказ на N минут")
    @Stories({@Story("Забрать заказ из СЦ"), @Story("Успешно отложить заказ")})
    @DisplayName(value = "Тест, чтобы была возможность отложить задание")
    void test() {
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();

        manualApiFacade.createEmptyRoutePoint();
        manualApiFacade.createDeliveryTask();
        long firstRoutePoint = AutoTestContextHolder.getContext().getRoutePointId();

        //secondRoutePoint
        manualApiFacade.createEmptyRoutePoint();
        manualApiFacade.createDeliveryTask();
        long secondRoutePoint = AutoTestContextHolder.getContext().getRoutePointId();

        apiFacade.pickupOrders();
        publicApiFacade.successCallToRecipient();
        publicApiFacade.arriveToRoutePoint();

        postponeFirstOrderOnFirstRoutePoint(firstRoutePoint, secondRoutePoint);
        finishSecondRoutePoint();
        checkThatRevertPostponeFirstRoutePoint(firstRoutePoint);
    }

    private void postponeFirstOrderOnFirstRoutePoint(long firstRoutePoint, long secondRoutePoint) {
        publicApiFacade.postpone();

        PublicApiFacade.UpdateDataResponse updateDataResponse = publicApiFacade.updateDataButton();

        updateDataResponse.getRoutePoints().getRoutePoints().stream()
                .filter(routePointSummaryDto -> routePointSummaryDto.getId() == firstRoutePoint)
                .forEach(routePointSummaryDto -> assertThat(routePointSummaryDto.getStatus()).isEqualTo(UNFINISHED));

        assertThat(updateDataResponse.getUserShift().getCurrentRoutePointId()).isEqualTo(secondRoutePoint);
    }

    private void finishSecondRoutePoint() {
        publicApiFacade.updateCurrentRoutePoint();
        publicApiFacade.arriveToRoutePoint();
        publicApiFacade.giveParcel();
        publicApiFacade.finishDeliveryTask();
    }

    private void checkThatRevertPostponeFirstRoutePoint(long firstRoutePoint) {
        PublicApiFacade.UpdateDataResponse updateDataResponse = publicApiFacade.updateDataButton();

        assertThat(updateDataResponse.getUserShift().getCurrentRoutePointId()).isEqualTo(firstRoutePoint);

        updateDataResponse.getRoutePoints().getRoutePoints().stream()
                .filter(routePointSummaryDto -> routePointSummaryDto.getId() == firstRoutePoint)
                .forEach(routePointSummaryDto -> assertThat(routePointSummaryDto.getStatus()).isEqualTo(IN_TRANSIT));
    }
}
