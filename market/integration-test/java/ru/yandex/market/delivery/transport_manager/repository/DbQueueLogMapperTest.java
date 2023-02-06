package ru.yandex.market.delivery.transport_manager.repository;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueEntity;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueEntityIdType;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueTaskLog;
import ru.yandex.market.delivery.transport_manager.domain.entity.dbqueue.DbQueueTaskState;
import ru.yandex.market.delivery.transport_manager.domain.filter.DbQueueLogFilter;
import ru.yandex.market.delivery.transport_manager.queue.task.TaskType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.DbQueueLogMapper;

@DatabaseSetup("/repository/health/dbqueue/empty.xml")
@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnectionDbQueue"})
public class DbQueueLogMapperTest extends AbstractContextualTest {

    @Autowired
    private DbQueueLogMapper mapper;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2021-09-02T10:39:00.00Z"), ZoneOffset.UTC);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testInsertWithRelations() {
        updateSequence("dbqueue.task_log", 1);
        DbQueueTaskLog log = new DbQueueTaskLog()
            .setQueueName(TaskType.CANCEL_MOVEMENT)
            .setLastStart(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T10:20:00.00Z"), ZoneOffset.UTC))
            .setLastEnd(ZonedDateTime.now(clock))
            .setState(DbQueueTaskState.OK)
            .setPayload("payload")
            .setTaskId(1L);

        Set<DbQueueEntity> entities = Set.of(
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID),
            new DbQueueEntity().setId(2L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID)
        );

        mapper.persistWithRelations(log, entities);
    }

    @Test
    void testInsertWithEmptyRelationsNoExcept() {
        DbQueueTaskLog log = new DbQueueTaskLog()
            .setQueueName(TaskType.CANCEL_MOVEMENT)
            .setState(DbQueueTaskState.OK)
            .setTaskId(1L);

        mapper.persistWithRelations(log, null);
        mapper.persistWithRelations(log, Set.of());
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        DbQueueTaskLog log = new DbQueueTaskLog()
            .setQueueName(TaskType.PUT_INBOUND)
            .setLastStart(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T10:20:00.00Z"), ZoneOffset.UTC))
            .setState(DbQueueTaskState.EXECUTING)
            .setTaskId(2L);
        mapper.persistLog(log);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    void testGetByTask() {
        DbQueueTaskLog log = mapper.getLogByTaskId(14L);
        DbQueueTaskLog expected = new DbQueueTaskLog()
            .setId(4L)
            .setQueueName(TaskType.GET_MOVEMENT)
            .setState(DbQueueTaskState.FAILED)
            .setTaskId(14L)
            .setPayload("payload")
            .setLastStart(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T08:20:00.00Z"), ZoneId.systemDefault()))
            .setLastEnd(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T08:39:00.00Z"), ZoneId.systemDefault()));

        assertThatModelEquals(expected, log, "created", "updated");
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    void findExpired() {
        clock.setFixed(Instant.parse("2021-09-15T04:01:00.00Z"), ZoneOffset.UTC);
        Set<Long> expired = mapper.findExpired(
            Instant.now(clock),
            14,
            Set.of(DbQueueTaskState.FINALLY_FAILED, DbQueueTaskState.OK)
        );

        assertContainsExactlyInAnyOrder(new ArrayList<>(expired), 1L, 5L);
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/logs.xml")
    @ExpectedDatabase(
        value = "/repository/dbqueue/after/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteByIds() {
        mapper.deleteByIds(Set.of(1L, 4L, 5L));
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/search_logs.xml")
    void testGetLogsByEntities() {

        List<DbQueueEntity> entities = List.of(
            new DbQueueEntity().setId(10L).setIdType(DbQueueEntityIdType.MOVEMENT_ID),
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID),
            new DbQueueEntity().setId(100L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_REQUEST_ID)
        );

        List<DbQueueTaskLog> logs = mapper.getLogsByRelatedEntityData(
            entities,
            new DbQueueLogFilter().setTaskType(TaskType.PUT_INBOUND),
            Pageable.unpaged()
        );

        logs.forEach(log -> log.setUpdated(null));

        softly.assertThat(logs).containsExactly(
            new DbQueueTaskLog()
                .setId(1L)
                .setState(DbQueueTaskState.OK)
                .setQueueName(TaskType.PUT_INBOUND)
                .setTaskId(1L)
                .setRequestId("request")
                .setLastStart(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T10:20:00.00Z"), ZoneId.systemDefault()))
                .setLastEnd(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T10:39:00.00Z"), ZoneId.systemDefault()))
                .setCreated(ZonedDateTime.ofInstant(Instant.parse("2021-09-01T03:00:00.00Z"), ZoneId.systemDefault())),

            new DbQueueTaskLog()
                .setId(3L)
                .setState(DbQueueTaskState.EXECUTING)
                .setQueueName(TaskType.PUT_INBOUND)
                .setTaskId(3L)
                .setFailureMessage("error")
                .setLastStart(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T07:20:00.00Z"), ZoneId.systemDefault()))
                .setLastEnd(ZonedDateTime.ofInstant(Instant.parse("2021-09-02T07:39:00.00Z"), ZoneId.systemDefault()))
                .setCreated(ZonedDateTime.ofInstant(Instant.parse("2021-08-31T03:00:00.00Z"), ZoneId.systemDefault()))
        );
    }

    @Test
    @DatabaseSetup("/repository/dbqueue/search_logs.xml")
    void testCountByEntities() {
        List<DbQueueEntity> entities = List.of(
            new DbQueueEntity().setId(10L).setIdType(DbQueueEntityIdType.MOVEMENT_ID),
            new DbQueueEntity().setId(1L).setIdType(DbQueueEntityIdType.TRANSPORTATION_ID),
            new DbQueueEntity().setId(100L).setIdType(DbQueueEntityIdType.TRANSPORTATION_UNIT_REQUEST_ID)
        );

        softly.assertThat(mapper.countFilteredByEntities(
            entities,
            new DbQueueLogFilter().setTaskType(TaskType.PUT_INBOUND))
        ).isEqualTo(2);
    }
}
