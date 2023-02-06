package ru.yandex.market.tpl.core.domain.usershift;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.task.CollectDropshipTaskStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.movement.Movement;
import ru.yandex.market.tpl.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewCollectDropshipRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class CollectDropshipTaskTest {

    private final MovementGenerator movementGenerator;
    private final UserShiftCommandService userShiftCommandService;
    private final TestUserHelper testUserHelper;
    private final Clock clock;

    @Test
    void testCreatedDropshipTask() {
        Movement movement = movementGenerator.generate(MovementCommand.Create.builder().build());

        User user = testUserHelper.findOrCreateUser(1L);
        UserShift userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
        CollectDropshipTask collectDropshipTask = userShiftCommandService.addCollectDropshipTask(user,
                new UserShiftCommand.AddCollectDropshipTask(
                userShift.getId(),
                NewCollectDropshipRoutePointData.builder()
                        .movement(movement)
                        .expectedArrivalTime(clock.instant())
                        .name("test")
                        .address(new RoutePointAddress(
                                movement.getWarehouse().getAddress().getAddress(),
                                movement.getWarehouse().getAddress().getGeoPoint()))
                        .build()
        ));

        assertThat(collectDropshipTask.getStatus()).isEqualTo(CollectDropshipTaskStatus.NOT_STARTED);
        assertThat(collectDropshipTask.getRoutePoint().getRoutePointAddress()).isEqualTo(new RoutePointAddress(
                        movement.getWarehouse().getAddress().getAddress(),
                        movement.getWarehouse().getAddress().getGeoPoint()
                )
        );
        assertThat(collectDropshipTask.getMovementId()).isEqualTo(movement.getId());
    }

}
