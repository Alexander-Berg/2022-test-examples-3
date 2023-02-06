package ru.yandex.market.partner.content.common.db.dao;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.ScheduledTaskStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.ScheduledTaskType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.ScheduledTask;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SCHEDULED_TASK;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;

public class ScheduledTaskServiceTest extends BaseDbCommonTest {

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Test
    public void shouldSaveNewTask() {
        dsl().insertInto(SERVICE_INSTANCE)
                .values(0L, "host", 8080, true, Timestamp.valueOf(LocalDateTime.now())).execute();

        scheduledTaskService.saveNewTask(ScheduledTaskType.CLEAN_ERROR_PROTO_MESSAGE, 0L);

        List<ScheduledTask> dataCleanerTasks = dsl().selectFrom(SCHEDULED_TASK)
                .fetchInto(ScheduledTask.class);

        assertEquals(1, dataCleanerTasks.size());

        ScheduledTask fromDb = dataCleanerTasks.get(0);
        assertEquals(ScheduledTaskType.CLEAN_ERROR_PROTO_MESSAGE, fromDb.getType());
        assertEquals(ScheduledTaskStatus.IN_PROGRESS, fromDb.getStatus());
    }

    @Test
    @SuppressWarnings("checkstyle:magicnumber")
    public void shouldGetLastTask() {
        Timestamp lastTimestamp = Timestamp.valueOf(LocalDateTime.now().minusHours(3));
        dsl().insertInto(SCHEDULED_TASK, SCHEDULED_TASK.TYPE, SCHEDULED_TASK.STATUS, SCHEDULED_TASK.START_DATE)
            .values(ScheduledTaskType.CLEAN_ERROR_PROTO_MESSAGE, ScheduledTaskStatus.FINISHED, lastTimestamp)
            .values(ScheduledTaskType.CLEAN_ERROR_PROTO_MESSAGE, ScheduledTaskStatus.FINISHED,
                    Timestamp.valueOf(LocalDateTime.now().minusMonths(2)))
            .values(ScheduledTaskType.CLEAN_ERROR_PROTO_MESSAGE, ScheduledTaskStatus.FINISHED,
                    Timestamp.valueOf(LocalDateTime.now().minusMonths(7)))
            .execute();

        Optional<ScheduledTask> lastTask =
            scheduledTaskService.getLastTask(ScheduledTaskType.CLEAN_ERROR_PROTO_MESSAGE);

        assertTrue(lastTask.isPresent());
        assertEquals(lastTimestamp, lastTask.get().getStartDate());
    }

}
