package ru.yandex.market.tpl.core.domain.usershift;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.exception.TplDeliveryTaskFindException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DropoffCargoAcceptCommandHandlerUnitTest {

    public static final long EXISTED_MOVEMENT_ID = 1L;
    public static final long ACCEPTED_CARGO_ID = 2L;
    public static final long NOT_EXISTED_MOVEMENT = 2L;

    @Test
    void handle_ExistedMovement() {
        //given
        UserShift userShift = TestUserShiftFactory.buildWithDropOffTask(EXISTED_MOVEMENT_ID);

        userShift.streamLockerDeliveryTasks()
                .forEach(task -> task.setOrdinalNumber(123));

        var handler = DropoffCargoAcceptCommandHandler.builder()
                .command(UserShiftCommand.AcceptDropOffCargosToDeliver.of(
                        userShift.getId(),
                        Map.of(EXISTED_MOVEMENT_ID, List.of(ACCEPTED_CARGO_ID))
                ))
                .build();

        //when
        handler.handle(userShift);

        //then
        List<LockerSubtask> dropOffReturnsSubTasks = userShift
                .streamLockerDeliveryTasks()
                .flatMap(LockerDeliveryTask::streamDropOffReturnSubtasks)
                .collect(Collectors.toList());

        assertThat(dropOffReturnsSubTasks).hasSize(1);
        LockerSubtaskDropOff lockerSubtaskDropOffReturn =
                dropOffReturnsSubTasks.iterator().next().getLockerSubtaskDropOff();

        assertNotNull(lockerSubtaskDropOffReturn);
        assertEquals(EXISTED_MOVEMENT_ID, lockerSubtaskDropOffReturn.getMovementId());
        assertEquals(ACCEPTED_CARGO_ID, lockerSubtaskDropOffReturn.getDropoffCargoId());
    }

    @Test
    void handle_SkippedMovement() {
        //given
        UserShift userShift = TestUserShiftFactory.buildWithDropOffTask(EXISTED_MOVEMENT_ID);

        var handler = DropoffCargoAcceptCommandHandler.builder()
                .command(UserShiftCommand.AcceptDropOffCargosToDeliver.of(
                        userShift.getId(),
                        Map.of(NOT_EXISTED_MOVEMENT, List.of(ACCEPTED_CARGO_ID))
                ))
                .build();

        //when
        assertThrows(
                TplDeliveryTaskFindException.class,
                () -> handler.handle(userShift));
    }



}
