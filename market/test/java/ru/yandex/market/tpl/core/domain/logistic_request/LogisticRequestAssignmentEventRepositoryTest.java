package ru.yandex.market.tpl.core.domain.logistic_request;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestCommand;
import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestAssignmentEventType.ASSIGNED;
import static ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequestAssignmentEventType.UNASSIGNED;

/**
 * @author sekulebyakin
 */
@RequiredArgsConstructor
public class LogisticRequestAssignmentEventRepositoryTest extends TplAbstractTest {

    private final LogisticRequestCommandService logisticRequestCommandService;
    private final LogisticRequestAssignmentEventRepository repository;
    private final TestUserHelper testUserHelper;
    private final Clock clock;

    @Test
    void findAssignedLogisticRequestsTest() {

        var user1 = testUserHelper.findOrCreateUser(10L);
        var user2 = testUserHelper.findOrCreateUser(20L);

        // создаем для каждого юзера по 5 логистических заявок
        var lrs = IntStream.range(0, 10)
                .mapToObj(i -> generateLR(String.valueOf(i)))
                .collect(Collectors.toList());
        for (int i = 0; i < 5; i++) {
            saveEvent(i, user1.getId(), lrs.get(i).getId(), ASSIGNED);
        }
        for (int i = 5; i < 10; i++) {
            saveEvent(i, user2.getId(), lrs.get(i).getId(), ASSIGNED);
        }

        // с user1 снимаем 2 заявки, с user2 одну
        saveEvent(10, user1.getId(), lrs.get(1).getId(), UNASSIGNED);
        saveEvent(11, user1.getId(), lrs.get(3).getId(), UNASSIGNED);
        saveEvent(12, user2.getId(), lrs.get(7).getId(), UNASSIGNED);

        // Для user 1 должны получить заявки с индексами 0, 2 и 4
        var assignedLogisticRequestIds = repository.findAssignedLogisticRequests(user1.getId());
        assertThat(assignedLogisticRequestIds).hasSize(3);
        assertThat(assignedLogisticRequestIds).containsAll(List.of(
                lrs.get(0).getId(), lrs.get(2).getId(), lrs.get(4).getId()
        ));

        // Для user2 должны получить заявки с индексами 5, 6, 8 и 9
        assignedLogisticRequestIds = repository.findAssignedLogisticRequests(user2.getId());
        assertThat(assignedLogisticRequestIds).hasSize(4);
        assertThat(assignedLogisticRequestIds).containsAll(List.of(
                lrs.get(5).getId(), lrs.get(6).getId(), lrs.get(8).getId(), lrs.get(9).getId()
        ));
    }

    private void saveEvent(int gap, long userId, long lrId, LogisticRequestAssignmentEventType type) {
        repository.save(LogisticRequestAssignmentEventLogEntry.builder()
                .logisticRequestId(lrId)
                .userId(userId)
                .eventDate(Instant.now(clock).plus(gap, ChronoUnit.MINUTES))
                .eventType(type)
                .build());
    }

    private LogisticRequest generateLR(String extId) {
        return logisticRequestCommandService.createLogisticRequest(
                SpecialRequestCommand.CreateSpecialRequest.builder()
                        .externalId(extId)
                        .deliveryServiceId(5L)
                        .intervalFrom(LocalDateTime.of(2022, 2, 20, 10, 0))
                        .intervalTo(LocalDateTime.of(2022, 2, 20, 15, 0))
                        .specialRequestType(SpecialRequestType.LOCKER_INVENTORY)
                        .build()
        );
    }

}
