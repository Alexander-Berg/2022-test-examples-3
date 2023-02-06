package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCommonRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.task.persistence.FlowTaskEntity;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkRepository;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.projection.TaskStatus;
import ru.yandex.market.tpl.core.task.service.TaskRepository;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class AddRoutePointForFlowTaskTest extends TplAbstractTest {

    private final UserShiftCommandService commandService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final LogisticRequestLinkRepository logisticRequestLinkRepository;
    private final TaskRepository taskRepository;

    private PickupPoint pickupPoint;
    private UserShift userShift;
    private User user;

    @BeforeEach
    void setupShift() {
        user = testUserHelper.findOrCreateUser(12345L);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.of(2000, 1, 1));
        pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER,
                333L, DeliveryService.DEFAULT_DS_ID);
    }

    @Test
    void addLockerFlowAfterAnotherTaskTest() {

        addLockerTask();
        addFlowTask();

        transactionTemplate.execute(status -> {
            var flowTask = checkTwoTasksOnOneLockerRoutePoint(userShift.getId());
            checkFlowTaskPersistence(flowTask);
            return null;
        });
    }

    @Test
    void addLockerFlowBeforeAnotherTaskTest() {

        addFlowTask();
        addLockerTask();

        transactionTemplate.execute(status -> {
            var flowTask = checkTwoTasksOnOneLockerRoutePoint(userShift.getId());
            checkFlowTaskPersistence(flowTask);
            return null;
        });
    }

    private void addFlowTask() {
        var specialRequest1 = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .type(SpecialRequestType.LOCKER_INVENTORY)
                        .pickupPointId(pickupPoint.getId())
                        .build()
        );
        var specialRequest2 = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .type(SpecialRequestType.LOCKER_INVENTORY)
                        .pickupPointId(pickupPoint.getId())
                        .build()
        );

        commandService.addFlowTask(user, new UserShiftCommand.AddFlowTask(
                userShift.getId(),
                TaskFlowType.LOCKER_INVENTORY,
                NewCommonRoutePointData.builder()
                        .type(RoutePointType.LOCKER_DELIVERY)
                        .address(new RoutePointAddress("my_address", GeoPointGenerator.generateLonLat()))
                        .expectedArrivalTime(Instant.now())
                        .name("my_name")
                        .pickupPointId(pickupPoint.getId())
                        .withLogisticRequests(List.of(specialRequest1, specialRequest2))
                        .build()
        ));
    }

    private void addLockerTask() {
        var lockerOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build()
        );
        testUserHelper.addLockerDeliveryTaskToShift(user, userShift, lockerOrder);
    }

    private void checkFlowTaskPersistence(FlowTaskEntity flowTask) {
        assertThat(flowTask.getFlowType()).isEqualTo(TaskFlowType.LOCKER_INVENTORY);
        assertThat(flowTask.getStatus()).isEqualTo(TaskStatus.NOT_STARTED);
        assertThat(flowTask.getName()).isEqualTo(TaskFlowType.LOCKER_INVENTORY.getFlowName());
        assertThat(flowTask.getPickupPointId()).isEqualTo(pickupPoint.getId());

        var links = logisticRequestLinkRepository.findLinksForTask(flowTask.getId());
        assertThat(links).hasSize(2);

        var actions = taskRepository.getTaskActions(flowTask.getId());
        assertThat(actions).isNotEmpty();
    }

    private FlowTaskEntity checkTwoTasksOnOneLockerRoutePoint(long userShiftId) {
        var userShift = userShiftRepository.findByIdOrThrow(userShiftId);
        var routePoints = userShift.getRoutePoints().stream()
                .filter(rp -> Objects.equals(rp.getType(), RoutePointType.LOCKER_DELIVERY))
                .collect(Collectors.toList());
        assertThat(routePoints).hasSize(1);
        var routePoint = routePoints.get(0);

        var tasks = routePoint.getTasks();
        assertThat(tasks).hasSize(2);

        if (tasks.get(0) instanceof FlowTaskEntity) {
            return (FlowTaskEntity) tasks.get(0);
        } else {
            return (FlowTaskEntity) tasks.get(1);
        }
    }

}
