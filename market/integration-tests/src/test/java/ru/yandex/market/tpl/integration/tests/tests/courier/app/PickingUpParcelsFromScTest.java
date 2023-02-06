package ru.yandex.market.tpl.integration.tests.tests.courier.app;

import java.util.List;
import java.util.Map;

import io.qameta.allure.Epic;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderPickupTaskStatus;
import ru.yandex.market.tpl.api.model.task.TaskType;
import ru.yandex.market.tpl.integration.tests.client.LmsScApiClient;
import ru.yandex.market.tpl.integration.tests.client.LmsTplApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;
import ru.yandex.market.tpl.integration.tests.service.LmsSortingCenterPropertyDetailDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.tpl.api.model.shift.UserShiftStatus.NO_SHIFT;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Курьерское приложение")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PickingUpParcelsFromScTest {
    public final static String TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED = "transfer_act_for_order_pickup_enabled";
    private final static String NEW_PICKUP_FLOW_ENABLED = "new_pickup_flow_enabled";
    private final static String ONLINE_TRANSFER_ACT = "ONLINE_TRANSFER_ACT";
    private final static Long DEFAULT_SC_ID = 1L;

    private final PublicApiFacade publicApiFacade;
    private final ManualApiFacade manualApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final LmsTplApiClient lmsTplApiClient;
    private final LmsScApiClient lmsScApiClient;
    private List<Long> courierPropertiesIds;
    private List<Long> sortingCenterPropertiesIds;

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
    @Story("Забрать посылку из СЦ")
    @DisplayName(value = "Тест забора посылок")
    void pickingUpTest() {
        runApp();
        createUserShift();
        updateDataAndCheck();
        startUserShift();
        arriveToSc();
        scanItems();
        arriveToRoutePoint();
    }

    @Test
    @Story("Забрать посылку из СЦ с подписанием ЭАПП")
    @DisplayName(value = "Тест забора посылок с подписанием ЭАПП")
    void pickingUpWithTransferActTest() {
        enableTransferAct();
        runApp();
        createUserShift();
        updateDataAndCheck();
        startUserShift();
        arriveToSc();
        scanItems();
        arriveToRoutePoint();
        deleteFlagTransferAct();
    }

    private void scanItems() {
        publicApiFacade.scanItems();
    }

    @Step("Нажимаем [Я на месте] возле СЦ")
    private void arriveToSc() {
        RoutePointDto currentRoutePoint = publicApiFacade.arriveToRoutePoint();
        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(currentRoutePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
    }

    private void arriveToRoutePoint() {
        var currentRoutePoint = publicApiFacade.arriveToRoutePoint();
        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
    }

    private void startUserShift() {
        var currentRoutePoint = publicApiFacade.startUserShift();
        assertThat(currentRoutePoint.getType()).isEqualTo(RoutePointType.ORDER_PICKUP);
        assertThat(currentRoutePoint.getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(currentRoutePoint.getTasks()).hasSize(1);
        var currentTaskDto = currentRoutePoint.getTasks().iterator().next();
        assertThat(currentTaskDto.getType()).isEqualTo(TaskType.ORDER_PICKUP);
        assertThat(((OrderPickupTaskDto) currentTaskDto).getStatus()).isEqualTo(OrderPickupTaskStatus.NOT_STARTED);
    }

    @Step("Запуск приложения")
    private void runApp() {
        var response = publicApiFacade.updateDataButton();
        assertTrue(response.getVersion().isLatest());
        assertEquals(NO_SHIFT, response.getUserShift().getStatus());
        assertEquals(List.of(), response.getRoutePoints().getRoutePoints());
    }

    private void createUserShift() {
        var currentUserShift = manualApiFacade.createDefaultShiftWithTasks();
        assertThat(currentUserShift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(currentUserShift.isActive()).isTrue();
    }

    @Step("Обновить данные и проверить, что смена создалась")
    private void updateDataAndCheck() {
        var response = publicApiFacade.updateDataButton();
        assertThat(response.getUserShift().getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(response.getUserShift().isActive()).isTrue();
        assertThat(response.getRoutePoints().getRoutePoints()).isNotEmpty();
        assertThat(response.getRoutePoints().getRoutePoints()).allMatch(rp -> rp.getStatus() == RoutePointStatus.NOT_STARTED);
        assertThat(response.getRoutePoints().getRoutePoints()).anyMatch(rp -> rp.getType() == RoutePointType.ORDER_PICKUP);
        assertThat(response.getRoutePoints().getRoutePoints()).anyMatch(rp -> rp.getType() == RoutePointType.DELIVERY);
        assertThat(response.getRoutePoints().getRoutePoints()).anyMatch(rp -> rp.getType() == RoutePointType.ORDER_RETURN);
    }

    private void enableTransferAct() {
        Long courierId = AutoTestContextHolder.getContext().getUserId();
        courierPropertiesIds = lmsTplApiClient.createPropertiesForCourier(courierId,
                Map.of(
                        NEW_PICKUP_FLOW_ENABLED, "true",
                        TRANSFER_ACT_FOR_ORDER_PICKUP_ENABLED, "true"
                ));
        List<LmsSortingCenterPropertyDetailDto> properties =
                lmsScApiClient.getPropertiesForSortingCenter(DEFAULT_SC_ID);

        if (properties.stream().anyMatch(property -> property.getKey().equals(ONLINE_TRANSFER_ACT))) {
            LmsSortingCenterPropertyDetailDto transferActProperty =
                    properties.stream().filter(property -> property.getKey().equals(ONLINE_TRANSFER_ACT))
                            .findFirst().get();
            if (transferActProperty.getValue().equals("false")) {
                lmsScApiClient.deletePropertiesForSortingCenter(List.of(transferActProperty.getScPropertyId()));
                createTransferActPropertyForSc();
            }
        } else {
            createTransferActPropertyForSc();
        }

    }

    private void createTransferActPropertyForSc() {
        sortingCenterPropertiesIds = lmsScApiClient.createPropertiesForSortingCenter(DEFAULT_SC_ID,
                Map.of(ONLINE_TRANSFER_ACT, "true"));
    }

    private void deleteFlagTransferAct() {
        lmsTplApiClient.deletePropertiesForCourier(courierPropertiesIds);
        if (sortingCenterPropertiesIds != null) {
            lmsScApiClient.deletePropertiesForSortingCenter(sortingCenterPropertiesIds);
        }
    }
}
