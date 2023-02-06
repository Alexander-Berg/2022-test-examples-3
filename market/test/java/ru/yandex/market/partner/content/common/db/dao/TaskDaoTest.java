package ru.yandex.market.partner.content.common.db.dao;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.BaseDbCommonTest;
import ru.yandex.market.partner.content.common.db.jooq.enums.LockStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.daos.LockInfoDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.LockInfo;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Task;
import ru.yandex.market.partner.content.common.engine.parameter.EmptyData;
import ru.yandex.market.partner.content.common.engine.parameter.Param;
import ru.yandex.market.partner.content.common.entity.PriorityIdentityWrapper;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskDaoTest extends BaseDbCommonTest {

    @Autowired
    private TaskDao taskDao;

    @Autowired
    private PipelineDao pipelineDao;

    private LockInfoDao lockInfoDao;

    @Before
    public void init() {
        lockInfoDao = new LockInfoDao(configuration);
    }

    @Test
    public void getAvailableForProcessTaskIds_returnsJustCreatedTask() {
        Pipeline pipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        pipelineDao.insert(pipeline);
        pipeline = pipelineDao.fetchByLockId(pipeline.getLockId()).get(0);
        Task task = taskDao.createTask(pipeline.getId(), new EmptyData(), "testTaskAction");

        List<PriorityIdentityWrapper> taskIds = taskDao.getAvailableForProcessTaskIds();
        assertThat(taskIds).hasSize(1);
        assertThat(taskIds.get(0).getId()).isEqualTo(task.getId());
    }

    @Test
    public void getAvgStartCount() {
        Pipeline pipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        pipelineDao.insert(pipeline);
        for (int i = 0; i < 10; i++) {
            Task task = taskDao.createTask(pipeline.getId(), new EmptyData(), "testTaskAction"+i%2);
            task.setStartCnt(2);
            taskDao.update(task);
        }
        Map<String,Double> taskStats = taskDao.getAvgTaskStartByTaskAction();
        assertThat(taskStats).hasSize(2);
        assertThat(taskStats.values()).allMatch(v->v==2.0);
    }

    @Test
    public void getAvgStartCountWithNullAvg() {
        Pipeline pipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        pipelineDao.insert(pipeline);
        for (int i = 0; i < 10; i++) {
            Task task = taskDao.createTask(pipeline.getId(), new EmptyData(), "testTaskAction"+i%2);
            taskDao.update(task);
        }
        Map<String,Double> taskStats = taskDao.getAvgTaskStartByTaskAction();
        assertThat(taskStats).hasSize(2);
        assertThat(taskStats.values()).allMatch(v->v==0.0);
    }

    @Test
    public void getAvailableForProcessTaskIds_returnsTaskWithNextScheduleDateInPast() {
        Pipeline pipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        pipelineDao.insert(pipeline);
        pipeline = pipelineDao.fetchByLockId(pipeline.getLockId()).get(0);
        Task task = taskDao.createTask(pipeline.getId(), new EmptyData(), "testTaskAction");
        task.setNextScheduleTs(Timestamp.from(Instant.now().minus(Duration.ofMinutes(10))));
        taskDao.update(task);

        List<PriorityIdentityWrapper> taskIds = taskDao.getAvailableForProcessTaskIds();
        assertThat(taskIds).hasSize(1);
        assertThat(taskIds.get(0).getId()).isEqualTo(task.getId());
    }

    @Test
    public void getAvailableForProcessTaskIds_doesNotReturnTaskWithNextScheduleDateInFuture() {
        Pipeline pipeline = createPipeline(Instant.now().minus(Duration.ofDays(5)));
        pipelineDao.insert(pipeline);
        pipeline = pipelineDao.fetchByLockId(pipeline.getLockId()).get(0);
        Task task = taskDao.createTask(pipeline.getId(), new EmptyData(), "testTaskAction");
        task.setNextScheduleTs(Timestamp.from(Instant.now().plus(Duration.ofMinutes(10))));
        taskDao.update(task);

        List<PriorityIdentityWrapper> taskIds = taskDao.getAvailableForProcessTaskIds();
        assertThat(taskIds).hasSize(0);
    }


    private Pipeline createPipeline(Instant instant) {
        final Timestamp timestamp = Timestamp.from(instant);
        final LockInfo lockInfo = new LockInfo();
        lockInfo.setStatus(LockStatus.FREE);
        lockInfo.setCreateTime(timestamp);
        lockInfoDao.insert(lockInfo);
        Param emptyData = new EmptyData();
        final Pipeline pipeline = new Pipeline();
        pipeline.setInputData(emptyData);
        pipeline.setType(PipelineType.GOOD_CONTENT_SINGLE_XLS);
        pipeline.setStartDate(timestamp);
        pipeline.setUpdateDate(timestamp);
        pipeline.setLockId(lockInfo.getId());
        return pipeline;
    }
}
