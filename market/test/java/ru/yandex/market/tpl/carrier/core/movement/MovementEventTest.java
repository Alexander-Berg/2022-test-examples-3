package ru.yandex.market.tpl.carrier.core.movement;

import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementHistoryEventType;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunHelper;
import ru.yandex.market.tpl.carrier.core.domain.run.RunManager;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;
import ru.yandex.market.tpl.carrier.core.domain.user.transport.Transport;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.carrier.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.carrier.core.domain.usershift.commands.UserShiftCommand;

@RequiredArgsConstructor(onConstructor_ = @Autowired)

@CoreTestV2
public class MovementEventTest {

    private final RunGenerator runGenerator;
    private final RunHelper runHelper;
    private final RunManager runManager;
    private final TestUserHelper testUserHelper;

    private final UserShiftCommandService userShiftCommandService;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final Clock clock;

    private Run run;
    private Movement movement;
    private UserShift userShift;

    private User user;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(1L);
        var transport = testUserHelper.findOrCreateTransport();

        run = runGenerator.generate();
        movement = run.streamMovements().findFirst().orElseThrow();

        userShift = runHelper.assignUserAndTransport(run, user, transport);
    }

    @Test
    void shouldGenerateCargoReceivedEvent() {
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishCollectDropships(userShift.streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());

        List<MovementHistoryEvent> content = movementHistoryEventRepository.findByMovementId(movement.getId(),
                Pageable.unpaged()).getContent();
        Assertions.assertThat(content).anySatisfy(e ->
                Assertions.assertThat(e.getType()).isEqualTo(MovementHistoryEventType.CARGO_RECEIVED)
        );
    }

    @Test
    void shouldGenerateMovementEventOnOutboundAreaReached() {
        testUserHelper.openShift(user, userShift.getId());
        userShiftCommandService.arriveAtRoutePointArea(new UserShiftCommand.ArriveAtRoutePointArea(
                userShift.getId(),
                userShift.streamCollectDropshipTasks().findFirst().orElseThrow().getId(),
                clock.instant()
        ));

        List<MovementHistoryEvent> content = movementHistoryEventRepository.findByMovementId(movement.getId(),
                Pageable.unpaged()).getContent();
        Assertions.assertThat(content).anySatisfy(e ->
                Assertions.assertThat(e.getType()).isEqualTo(MovementHistoryEventType.OUTBOUND_WAREHOUSE_REACHED)
        );
    }

    @Test
    void shouldGenerateMovementEventOnInboundAreaReached() {
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishCollectDropships(userShift.streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());
        userShiftCommandService.arriveAtRoutePointArea(new UserShiftCommand.ArriveAtRoutePointArea(
                userShift.getId(),
                userShift.streamReturnRoutePoints().findFirst().orElseThrow().getId(),
                clock.instant()
        ));

        List<MovementHistoryEvent> content = movementHistoryEventRepository.findByMovementId(movement.getId(),
                Pageable.unpaged()).getContent();
        Assertions.assertThat(content).anySatisfy(e ->
                Assertions.assertThat(e.getType()).isEqualTo(MovementHistoryEventType.INBOUND_WAREHOUSE_REACHED)
        );
    }

    @Test
    void shouldGenerateCargoDeliveredEvent() {
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishCollectDropships(userShift.streamCollectDropshipTasks().findFirst().orElseThrow().getRoutePoint());
        testUserHelper.finishFullReturnAtEnd(userShift);

        List<MovementHistoryEvent> content = movementHistoryEventRepository.findByMovementId(movement.getId(),
                Pageable.unpaged()).getContent();
        Assertions.assertThat(content)
                .anyMatch(e -> e.getType() == MovementHistoryEventType.CARGO_DELIVERED);
    }

    @Test
    void shouldGenerateAssignedEventOnUserReassigned() {
        movementHistoryEventRepository.deleteAll();

        User other = testUserHelper.findOrCreateUser(6789, Company.DEFAULT_COMPANY_NAME, "+79295585060");
        runManager.assignUser(run.getId(), other.getId());

        var events = run.streamMovements()
                .map(Movement::getId)
                .map(id -> movementHistoryEventRepository.findByMovementId(id, Pageable.unpaged()))
                .map(Page::getContent)
                .flatMap(List::stream).collect(Collectors.toList());
        Assertions.assertThat(events)
                .allMatch(e -> MovementHistoryEventType.DROPSHIP_TASK_CREATED.equals(e.getType()));
        Assertions.assertThat(events.size()).isEqualTo(run.streamMovements().count());
    }

    @Test
    void shouldGenerateAssignedEventOnTransportReassigned() {
        movementHistoryEventRepository.deleteAll();

        Transport other = testUserHelper.findOrCreateTransport("Бэтмобиль", Company.DEFAULT_COMPANY_NAME);
        runManager.assignTransport(run.getId(), other.getId());

        var events = run.streamMovements()
                .map(Movement::getId)
                .map(id -> movementHistoryEventRepository.findByMovementId(id, Pageable.unpaged()))
                .map(Page::getContent)
                .flatMap(List::stream).collect(Collectors.toList());
        Assertions.assertThat(events)
                .allMatch(e -> MovementHistoryEventType.DROPSHIP_TASK_CREATED.equals(e.getType()));
        Assertions.assertThat(events.size()).isEqualTo(run.streamMovements().count());
    }
}
