package ru.yandex.market.tsup.repository.mappers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.dbqueue.base.QueueType;
import ru.yandex.market.tsup.domain.entity.dbqueue.TaskLog;
import ru.yandex.market.tsup.domain.entity.dbqueue.TaskState;

@DatabaseSetup("/repository/dbqueue/empty.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
public class DbQueueLogMapperTest extends AbstractContextualTest {

    @Autowired
    private DbQueueLogMapper mapper;

    private static final TaskLog TASK_LOG = new TaskLog()
        .setFailureMessage("error")
        .setState(TaskState.FINALLY_FAILED)
        .setPayload("{\"id\":2}")
        .setTaskId(2L)
        .setId(2L)
        .setCreated(Instant.parse("2021-09-01T07:00:00Z").atZone(ZoneId.systemDefault()))
        .setLastStart(Instant.parse("2021-09-01T15:00:00Z").atZone(ZoneId.systemDefault()))
        .setLastEnd(Instant.parse("2021-09-01T15:00:01Z").atZone(ZoneId.systemDefault()))
        .setUpdated(Instant.parse("2021-09-01T15:00:01Z").atZone(ZoneId.systemDefault()))
        .setRequestId("12")
        .setQueueName(QueueType.PIPELINE_CUBE_RUNNER);

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-09-16T17:00:00Z"), ZoneOffset.UTC);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_create.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void create() {
        TaskLog toCreate = new TaskLog()
            .setPayload("{\"id\":1}")
            .setQueueName(QueueType.PIPELINE_CUBE_RUNNER)
            .setState(TaskState.NEW)
            .setRequestId("12345")
            .setTaskId(5L);
        mapper.persistLog(toCreate);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_delete.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void delete() {
        mapper.deleteByIds(List.of(2L));
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    void get() {
        TaskLog log = mapper.getLogByTaskId(2L);

        softly.assertThat(log).isEqualTo(TASK_LOG);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    void findOutdated() {
        List<Long> expired = mapper.findExpired(Instant.now(clock), 14, TaskState.FINAL_STATUSES);
        softly.assertThat(expired).containsExactly(2L);
    }
}
