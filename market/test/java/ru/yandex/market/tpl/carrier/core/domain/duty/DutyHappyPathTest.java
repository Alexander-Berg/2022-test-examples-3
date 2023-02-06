package ru.yandex.market.tpl.carrier.core.domain.duty;

import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.duty.commands.DutyCommand;
import ru.yandex.market.tpl.carrier.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunDuty;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunStatus;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDoc;
import ru.yandex.market.tpl.carrier.core.domain.run.car_request_docs.CarRequestDocRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DutyTask;
import ru.yandex.market.tpl.carrier.core.domain.usershift.DutyTaskStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.RoutePointStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftStatus;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTestV2
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class DutyHappyPathTest {
    private final DutyGenerator dutyGenerator;
    private final UserShiftCommandService userShiftCommandService;
    private final RunHelper runHelper;
    private final TestUserHelper testUserHelper;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final DutyCommandService dutyCommandService;
    private final CarRequestDocRepository carRequestDocRepository;

    Duty duty;
    RunDuty runDuty;
    Run dutyRun;

    Transport transport;

    User user;

    DeliveryService deliveryService;

    Company company;

    OrderWarehouse warehouse1;
    OrderWarehouse warehouse2;

    @BeforeEach
    void setup() {
        company = testUserHelper.findOrCreateCompany("new_company");
        deliveryService = testUserHelper.deliveryService(10001L, Set.of(company));
        duty = dutyGenerator.generate(db -> db.deliveryServiceId(deliveryService.getId()).name("test_duty"));
        runDuty = duty.getRunDuty().get(0);
        dutyRun = runDuty.getRun();
        transport = testUserHelper.findOrCreateTransport();
        user = testUserHelper.findOrCreateUser(1234L);

        warehouse1 = orderWarehouseGenerator.generateWarehouse();
        warehouse2 = orderWarehouseGenerator.generateWarehouse();

    }

    @Test
    @Transactional
    void happyPathWithoutDutyRun() {
        List<CarRequestDoc> docs = carRequestDocRepository.findCarRequestDocByRun(dutyRun);
        assertThat(docs).hasSize(1);

        runHelper.assignUserAndTransport(dutyRun, user, transport);

        UserShift shift = dutyRun.getFirstAssignedShift();
        testUserHelper.openShift(user, shift.getId());

        assertThat(shift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(shift.getFirstRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_TRANSIT);
        assertThat(shift.getFirstRoutePoint().getDutyTask().getStatus()).isEqualTo(DutyTaskStatus.NOT_STARTED);

        testUserHelper.arriveAtRoutePoint(shift.getFirstRoutePoint());

        assertThat(shift.getFirstRoutePoint().getStatus()).isEqualTo(RoutePointStatus.IN_PROGRESS);
        assertThat(shift.getFirstRoutePoint().getDutyTask().getStatus()).isEqualTo(DutyTaskStatus.DUTY_STARTED);
        assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_STARTED);

        userShiftCommandService.finishDutyTask(user, new UserShiftCommand.FinishDutyTask(shift.getId(),
                shift.getCurrentRoutePoint().getId(), shift.getCurrentRoutePoint().getDutyTask().getId()));

        assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_FINISHED);
        assertThat(shift.getFirstRoutePoint().getDutyTask().getStatus()).isEqualTo(DutyTaskStatus.FINISHED);
        assertThat(shift.getFirstRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(shift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
        assertThat(dutyRun.getStatus()).isEqualTo(RunStatus.COMPLETED);
    }

    @Test
    @Transactional
    void happyPathWithoutDutyStartingDuty() {
        List<CarRequestDoc> docs = carRequestDocRepository.findCarRequestDocByRun(dutyRun);
        assertThat(docs).hasSize(1);
        runHelper.assignUserAndTransport(dutyRun, user, transport);

        UserShift shift = dutyRun.getFirstAssignedShift();

        assertThat(shift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_CREATED);
        assertThat(shift.getFirstRoutePoint().getStatus()).isEqualTo(RoutePointStatus.NOT_STARTED);
        assertThat(shift.getFirstRoutePoint().getDutyTask().getStatus()).isEqualTo(DutyTaskStatus.NOT_STARTED);

        dutyCommandService.dutyFinish(new DutyCommand.DutyFinish(duty.getId()));

        assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_FINISHED);
        assertThat(shift.getFirstRoutePoint().getDutyTask().getStatus()).isEqualTo(DutyTaskStatus.CANCELLED);
        assertThat(shift.getFirstRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(shift.getStatus()).isEqualTo(UserShiftStatus.SHIFT_FINISHED);
        assertThat(dutyRun.getStatus()).isEqualTo(RunStatus.COMPLETED);
    }

    @Test
    @Transactional
    void happyPathWithDutyRun() {
        runHelper.assignUserAndTransport(dutyRun, user, transport);

        UserShift shift = dutyRun.getFirstAssignedShift();
        testUserHelper.openShift(user, shift.getId());
        testUserHelper.arriveAtRoutePoint(shift.getFirstRoutePoint());

        assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_STARTED);
        assertThat(dutyRun.getName()).isEqualTo("test_duty");
        assertThat(dutyRun.getPriceCents()).isEqualTo(600_000L);

        runHelper.createDutyRun(warehouse1, warehouse2, duty, dutyRun);

        List<CarRequestDoc> docs = carRequestDocRepository.findCarRequestDocByRun(dutyRun);
        assertThat(docs).hasSize(2);

        DutyTask dutyTask = duty.getRun().getFirstAssignedShift().getCurrentRoutePoint().getDutyTask();
        assertThat(duty.getStatus()).isEqualTo(DutyStatus.TRIP_CREATED);
        assertThat(dutyTask.getStatus()).isEqualTo(DutyTaskStatus.TRIP_CREATED);
        assertThat(dutyRun.getName()).isEqualTo("test_route");
        assertThat(dutyRun.getPriceCents()).isEqualTo(10_000_000L);

        userShiftCommandService.finishDutyTask(user, new UserShiftCommand.FinishDutyTask(shift.getId(),
                shift.getCurrentRoutePoint().getId(), shift.getCurrentRoutePoint().getDutyTask().getId()));

        assertThat(duty.getStatus()).isEqualTo(DutyStatus.DUTY_FINISHED);
        assertThat(shift.getFirstRoutePoint().getDutyTask().getStatus()).isEqualTo(DutyTaskStatus.FINISHED);
        assertThat(shift.getFirstRoutePoint().getStatus()).isEqualTo(RoutePointStatus.FINISHED);
        assertThat(shift.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
        assertThat(dutyTask.getStatus()).isEqualTo(DutyTaskStatus.FINISHED);
        assertThat(dutyRun.getStatus()).isEqualTo(RunStatus.STARTED);
    }
}
