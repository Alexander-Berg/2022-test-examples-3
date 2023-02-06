package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacleaner;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.dao.ScheduledTaskService;
import ru.yandex.market.partner.content.common.db.jooq.enums.ScheduledTaskStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.ScheduledTaskType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.ScheduledTask;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SCHEDULED_TASK;
import static ru.yandex.market.partner.content.common.db.jooq.Tables.SERVICE_INSTANCE;

public class ScheduledTaskActionTest extends BaseDbCommonTest {

    @Autowired
    private ScheduledTaskService scheduledTaskService;

    @Before
    public void setUp() {
        dsl().insertInto(SERVICE_INSTANCE)
                .values(0L, "host", 8080, true, Timestamp.valueOf(LocalDateTime.now())).execute();
    }

    @Test
    public void testScheduledTaskAction() throws InterruptedException, ExecutionException {

        dsl().selectFrom(SERVICE_INSTANCE).fetch();
        // run task and check status after completion
        ScheduledTaskAction taskAction = getScheduledTask();
        taskAction.apply(new EmptyData());
        ScheduledTask lastTask = scheduledTaskService.getLastTask(taskAction.getType()).get();
        assertThat(lastTask.getStatus()).isEqualTo(ScheduledTaskStatus.FINISHED);

        // mock long task
        dsl().insertInto(SCHEDULED_TASK, SCHEDULED_TASK.TYPE, SCHEDULED_TASK.STATUS,
                SCHEDULED_TASK.START_DATE, SCHEDULED_TASK.SERVICE_INSTANCE_ID)
                .values(ScheduledTaskType.CLEAN_OLD_DATA, ScheduledTaskStatus.IN_PROGRESS,
                        Timestamp.valueOf(LocalDateTime.now()), 0L)
                .execute();
        Long longTaskId = scheduledTaskService.getLastTask(taskAction.getType()).get().getId();

        // try run task while the other running
        taskAction.apply(new EmptyData());
        lastTask = scheduledTaskService.getLastTask(taskAction.getType()).get();
        assertThat(lastTask.getId()).isEqualTo(longTaskId);
        assertThat(lastTask.getStatus()).isEqualTo(ScheduledTaskStatus.IN_PROGRESS);

        // long task is finally failed
        dsl().update(SCHEDULED_TASK).set(SCHEDULED_TASK.STATUS, ScheduledTaskStatus.FAILED)
                .where(SCHEDULED_TASK.ID.eq(longTaskId))
                .execute();

        // rerun task after fail
        taskAction.apply(new EmptyData());
        lastTask = scheduledTaskService.getLastTask(taskAction.getType()).get();
        assertThat(lastTask.getId()).isNotEqualTo(longTaskId);
        assertThat(lastTask.getStatus()).isEqualTo(ScheduledTaskStatus.FINISHED);
    }

    private ScheduledTaskAction getScheduledTask() {
        return new ScheduledTaskAction(1, true, scheduledTaskService, 0) {
            @Override
            protected ScheduledTaskType getType() {
                return ScheduledTaskType.CLEAN_OLD_DATA;
            }

            @Override
            protected void runAction() {

            }
        };
    }
}
