package ru.yandex.market.tpl.carrier.core.domain.run;

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.tpl.carrier.core.domain.duty.Duty;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouse;

@RequiredArgsConstructor

@Component
public class RunHelper {

    private final RunRepository runRepository;
    private final RunManager runManager;
    private final RunCommandService runCommandService;

    private final MovementGenerator movementGenerator;

    public Run confirm(Run run) {
        runCommandService.confirm(new RunCommand.Confirm(run.getId()));
        return runRepository.findByIdOrThrow(run.getId());
    }

    @Transactional
    public UserShift assignUserAndTransport(Run run, User user, Transport transport) {
        runManager.assignUser(run.getId(), user.getId());
        runManager.assignTransport(run.getId(), transport.getId());

        return runRepository.findByIdOrThrow(run.getId()).getFirstAssignedShift();
    }

    @Transactional
    public Set<UserShift> assignUserShifts(Long runId, List<AssignUserShift> userShifts) {
        return assignUserShifts(runId, userShifts, true);
    }

    @Transactional
    public Set<UserShift> assignUserShifts(Long runId, List<AssignUserShift> userShifts, boolean isMultiShifted) {
        userShifts.forEach(us -> {
            Run run = runRepository.findByIdOrThrow(runId);
            runCommandService.setMultiShiftedProperty(runId, isMultiShifted);
            runManager.assignUserAndTransportMultiShift(run, null, us.getUserId(),
                    us.getTransportId(), us.getPassingPoint() + 1, us.isLast());
        });
        return runRepository.findByIdOrThrow(runId).getUserShifts();
    }

    @Transactional
    public Run createDutyRun(OrderWarehouse warehouse1, OrderWarehouse warehouse2, Duty duty, Run dutyRun) {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder()
                .orderWarehouse(warehouse1)
                .orderWarehouseTo(warehouse2)
                .build());

        runCommandService.startDutyRun(RunCommand.StartDutyRun.builder()
                .price(10_000_000L)
                .totalCount(2)
                .routeName("test_route")
                .externalId("TMM123")
                .runId(dutyRun.getId())
                .build());
        runCommandService.addOrUpdateItem(new RunCommand.CreateOrUpdateItem(dutyRun.getId(), RunItemData.builder()
                .movement(movement)
                .orderNumber(0)
                .fromIndex(0)
                .toIndex(1)
                .build()));

        return dutyRun;
    }

    @Value
    public static class AssignUserShift {
        @Nullable
        Long userShiftId;
        long userId;
        long transportId;
        int passingPoint;
        boolean isLast;
    }
}
