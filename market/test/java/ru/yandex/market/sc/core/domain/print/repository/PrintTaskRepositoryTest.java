package ru.yandex.market.sc.core.domain.print.repository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.sc.core.domain.print.model.DestinationType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PrintTaskRepositoryTest {

    private final JdbcTemplate jdbcTemplate;
    private final TestFactory testFactory;
    private final PrintTaskRepository printTaskRepository;
    private final Clock clock;

    private User user;

    @BeforeEach
    void init() {
        SortingCenter sortingCenter = testFactory.storedSortingCenter();
        user = testFactory.storedUser(sortingCenter, 1);
    }

    @Test
    public void findLastDayTasksByUserSortingCenter() {
        addPrintTaskToDB(1L, user.getId());
        addPrintTaskToDB(2L, user.getId());
        var user2 = testFactory.storedUser(testFactory.storedSortingCenter(2), 2);
        addPrintTaskToDB(3L, user2.getId());
        addPrintTaskToDB(4L, user2.getId());
        var user3 = testFactory.storedUser(testFactory.storedSortingCenter(2), 3);
        addPrintTaskToDB(5L, user3.getId());
        addPrintTaskToDB(6L, user3.getId());

        List<PrintTask> printTasksForUserSC = printTaskRepository.findLastDayTasksByUserSortingCenter(user2.getId());
        assertThat(printTasksForUserSC).hasSize(4);
        printTasksForUserSC.forEach(printTask -> assertThat(printTask.getUserId()).isIn(user2.getId(), user3.getId()));
    }

    @Test
    public void findLastDayTasksByUserId() {
        addPrintTaskToDB(1L, user.getId());
        addPrintTaskToDB(2L, user.getId());
        var user2 = testFactory.storedUser(testFactory.storedSortingCenter(2), 2);
        addPrintTaskToDB(3L, user2.getId());
        addPrintTaskToDB(4L, user2.getId());
        var user3 = testFactory.storedUser(testFactory.storedSortingCenter(2), 3);
        addPrintTaskToDB(5L, user3.getId());
        addPrintTaskToDB(6L, user3.getId());

        List<PrintTask> printTasksForUser = printTaskRepository.findLastDayTasksByUserId(user2.getId());
        assertThat(printTasksForUser).hasSize(2);
        printTasksForUser.forEach(printTask -> assertThat(printTask.getUserId()).isEqualTo(user2.getId()));
    }

    @Test
    public void findByNonTerminalStatuses() {
        addPrintTaskToDB(1L, user.getId(), PrintTaskStatus.CREATED, LocalDateTime.now(clock));
        addPrintTaskToDB(2L, user.getId(), PrintTaskStatus.PROCESSING, LocalDateTime.now(clock));
        addPrintTaskToDB(3L, user.getId(), PrintTaskStatus.COMPLETED, LocalDateTime.now(clock));
        addPrintTaskToDB(4L, user.getId(), PrintTaskStatus.FAILED, LocalDateTime.now(clock));
        addPrintTaskToDB(5L, user.getId(), PrintTaskStatus.PROCESSING, LocalDateTime.now(clock).minusHours(25));
        addPrintTaskToDB(6L, user.getId(), PrintTaskStatus.CREATED, LocalDateTime.now(clock).minusDays(2));
        addPrintTaskToDB(7L, user.getId(), PrintTaskStatus.PROCESSING, LocalDateTime.now(clock).minusHours(24));
        var printTaskIds = printTaskRepository
                .findByNonTerminalStatusesSince(LocalDateTime.now(clock).minusHours(24)).stream()
                .map(PrintTask::getId)
                .toList();
        assertThat(printTaskIds).containsExactlyInAnyOrder(1L, 2L, 7L);
        printTaskIds = printTaskRepository
                .findByNonTerminalStatusesBefore(LocalDateTime.now(clock).minusHours(24)).stream()
                .map(PrintTask::getId)
                .toList();
        assertThat(printTaskIds).containsExactlyInAnyOrder(5L, 6L);
    }

    private void addPrintTaskToDB(Long id, Long userId) {
        addPrintTaskToDB(id, userId, PrintTaskStatus.CREATED, LocalDateTime.now(clock));
    }

    private void addPrintTaskToDB(Long id, Long userId, PrintTaskStatus status, LocalDateTime updateTime) {
        jdbcTemplate.update("""
                        INSERT INTO print_task (id,created_at,updated_at,job_id,destination,destination_type,status,
                        status_updated_at,result_description,template_type,copies,user_id)
                        VALUES (?,now(),now(),?,'printer0',?,?,?,?,?,1,?)
                        """,
                id, id, DestinationType.PLAIN.name(), status.name(), updateTime, "(successful-ok)",
                PrintTemplateType.LOT.name(), userId
        );
    }
}
