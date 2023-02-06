package ru.yandex.market.tpl.carrier.core.domain.run;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.Movement;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementHistoryEventType;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEvent;
import ru.yandex.market.tpl.carrier.core.domain.movement.event.history.MovementHistoryEventRepository;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.user.User;

@RequiredArgsConstructor(onConstructor_=@Autowired)

@CoreTestV2
class RunManagerTest {

    private final RunManager runManager;
    private final RunHelper runHelper;
    private final RunGenerator runGenerator;
    private final TestUserHelper testUserHelper;
    private final MovementHistoryEventRepository movementHistoryEventRepository;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;

    private Run run;
    private Movement movement;
    private User user;
    private User user2;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(1L);
        user2 = testUserHelper.findOrCreateUser(2L, Company.DEFAULT_COMPANY_NAME, "+792722234563");

        run = runGenerator.generate();
        movement = run.streamMovements().findAny().orElseThrow();
    }

    @Test
    void shouldNotGenerateEventIfOnlyUserAssigned() {
        runManager.assignUser(run.getId(), user.getId());

        List<MovementHistoryEvent> events = movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged()).getContent();

        List<MovementHistoryEventType> eventTypes = events.stream()
                .map(MovementHistoryEvent::getType)
                .collect(Collectors.toList());

        Assertions.assertThat(eventTypes).doesNotContain(MovementHistoryEventType.DROPSHIP_TASK_CREATED);
    }

    @Test
    void shouldGenerateEventIfUserAndTransportAreAssigned() {
        runManager.assignUser(run.getId(), user.getId());
        runManager.assignTransport(run.getId(), testUserHelper.findOrCreateTransport().getId());

        List<MovementHistoryEvent> events = movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged()).getContent();

        List<MovementHistoryEventType> eventTypes = events.stream()
                .map(MovementHistoryEvent::getType)
                .collect(Collectors.toList());

        Assertions.assertThat(eventTypes).contains(MovementHistoryEventType.DROPSHIP_TASK_CREATED);
    }

    @Test
    void shouldGenerate2EventIfUserAndTransportAreAssigned() {
        runManager.assignUser(run.getId(), user.getId());
        runManager.assignTransport(run.getId(), testUserHelper.findOrCreateTransport().getId());

        runManager.assignUser(run.getId(), user2.getId());

        List<MovementHistoryEvent> events = movementHistoryEventRepository.findByMovementId(movement.getId(), Pageable.unpaged()).getContent();

        List<MovementHistoryEventType> eventTypes = events.stream()
                .map(MovementHistoryEvent::getType)
                .collect(Collectors.toList());

        Assertions.assertThat(eventTypes).filteredOn(Predicate.isEqual(MovementHistoryEventType.DROPSHIP_TASK_CREATED)).hasSize(2);
    }

    @Test
    void shouldSaveCollectTaskAndReturnId() {
        runManager.assignUser(run.getId(), user.getId());
        runManager.assignTransport(run.getId(), testUserHelper.findOrCreateTransport().getId());

        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());

            List<RunItem> runItems = run.streamRunItems().toList();
            Assertions.assertThat(runItems)
                    .allSatisfy(ri -> {
                        Assertions.assertThat(ri.getCollectTaskId()).isNotNull();
                        Assertions.assertThat(ri.getReturnTaskId()).isNotNull();
                    });
            return null;
        });
    }
}
