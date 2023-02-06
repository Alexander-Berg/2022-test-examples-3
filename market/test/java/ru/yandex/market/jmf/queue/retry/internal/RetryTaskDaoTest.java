package ru.yandex.market.jmf.queue.retry.internal;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.HostNames;
import ru.yandex.market.jmf.queue.retry.RetryServiceTestConfiguration;
import ru.yandex.market.jmf.queue.retry.RetryTaskPriority;
import ru.yandex.market.jmf.tx.TxService;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RetryServiceTestConfiguration.class)
@TestPropertySource("classpath:/ru/yandex/market/jmf/queue/retry/test.properties")
public class RetryTaskDaoTest {

    @Inject
    RetryTaskDao dao;
    @Inject
    RetryTaskTestUtils utils;
    @Inject
    TxService txService;

    @Test
    public void checkAttemptNumber() {
        RetryTask task = utils.createTask(-1000);
        RetryTask fromDb = utils.getSingleTaskForProcess();

        Assertions.assertEquals(task.getAttemptNumber(), fromDb.getAttemptNumber());
    }

    @Test
    public void checkConfiguration() {
        RetryTask task = utils.createTask(-1000);
        RetryTask fromDb = utils.getSingleTaskForProcess();

        Assertions.assertArrayEquals(task.getConfiguration(), fromDb.getConfiguration());
    }

    @Test
    public void checkContext() {
        RetryTask task = utils.createTask(-1000);
        RetryTask fromDb = utils.getSingleTaskForProcess();

        Assertions.assertArrayEquals(task.getContext(), fromDb.getContext());
    }

    @Test
    public void checkNextAttemptTime() {
        RetryTask task = utils.createTask(-1000);
        RetryTask fromDb = utils.getSingleTaskForProcess();

        Assertions.assertEquals(task.getNextAttemptTime().toInstant(), fromDb.getNextAttemptTime().toInstant());
    }

    @Test
    public void checkTaskInFuture() {
        // настройка системы
        utils.createTask(1000);

        // вызов системы
        RetryTask task = dao.getTaskForProcess(HostNames.instanceKey(),
                Sets.newHashSet(0),
                RetryTaskTestUtils.SERVICE_KEY);

        Assertions.assertNull(task, "Должны получить пусту коллекцию т.к. создана задача, время выполнения которой " +
                "еще не " +
                "наступило");
    }

    @Test
    public void getTask_empty() {
        RetryTask task = dao.getTaskForProcess(HostNames.instanceKey(),
                Sets.newHashSet(0),
                RetryTaskTestUtils.SERVICE_KEY);

        Assertions.assertNull(task, "Должны получать пустой список, а не null");
    }

    @Test
    public void free() {
        // настройка системы
        RetryTask task = utils.createTask(1000);

        // вызов системы
        dao.freeLocks(task.getId(), HostNames.instanceKey());

        // проверяем отсутствие исключенимя
    }

    @Test
    public void freeOrhaned() {
        // настройка системы
        utils.createTask(1000);

        // вызов системы
        dao.deleteOrphaned(10);

        // проверяем отсутствие исключенимя
    }

    @Test
    public void correctServiceKey() {
        utils.createTask(-1000);

        RetryTask task = dao.getTaskForProcess(HostNames.instanceKey(),
                Sets.newHashSet(0),
                RetryTaskTestUtils.SERVICE_KEY);

        Assertions.assertNotNull(task, "Должны получить задачу сервиса");
    }

    @Test
    public void getFirstQueuedTaskTime() {
        Instant time = dao.getFirstQueuedTaskTime(RetryTaskPriority.HIGH);
        Assertions.assertNull(time, "В базе нет ни одной таски с заданным приоритетом");

        utils.createTask(0, RetryTaskPriority.HIGH);
        Instant taskTime = dao.getFirstQueuedTaskTime(RetryTaskPriority.HIGH);
        Assertions.assertNotNull(taskTime);
    }

    @Test
    public void getTaskForProcess_priority() {
        RetryTask hTask = utils.createTask(-10, RetryTaskPriority.HIGH);
        RetryTask nTask = utils.createTask(-100, RetryTaskPriority.NORMAL);
        RetryTask lTask = utils.createTask(-1000, RetryTaskPriority.LOW);

        utils.createTask(100, RetryTaskPriority.HIGH);
        utils.createTask(100, RetryTaskPriority.NORMAL);
        utils.createTask(100, RetryTaskPriority.LOW);

        RetryTask task = dao.getTaskForProcess(HostNames.instanceKey(),
                Sets.newHashSet(0),
                RetryTaskTestUtils.SERVICE_KEY);
        Assertions.assertEquals(
                hTask.getCode(), task.getCode(), "Должны получить таску с наивысшим приоритетом несмотря на то, что " +
                        "она не самая " +
                        "просроченная");

        dao.delete(hTask);

        task = dao.getTaskForProcess(HostNames.instanceKey(),
                Sets.newHashSet(0),
                RetryTaskTestUtils.SERVICE_KEY);
        Assertions.assertEquals(
                nTask.getCode(), task.getCode(), "Должны получить таску со средним приоритетом несмотря на то, что " +
                        "она не самая " +
                        "просроченная");

        dao.delete(nTask);

        task = dao.getTaskForProcess(HostNames.instanceKey(),
                Sets.newHashSet(0),
                RetryTaskTestUtils.SERVICE_KEY);
        Assertions.assertEquals(
                lTask.getCode(), task.getCode(), "Должны получить таску со наименьшим приоритетом");
    }

    @Test
    public void selectWrongServiceKey() {
        utils.createTask(-1000);

        RetryTask task = dao.getTaskForProcess(HostNames.instanceKey(), Collections.emptyList(), "wrong_key");

        // задача другого сервиса не возвращается
        Assertions.assertNull(task, "Должны получать только задачи нужного сервиса");
    }

    @Test
    public void getTasksForProcess_groupNotNull() {
        RetryTask t1 = utils.createTask(-1, RetryTaskPriority.NORMAL, 1);
        RetryTask t2 = utils.createTask(1, RetryTaskPriority.NORMAL, 1);
        RetryTask t3 = utils.createTask(100, RetryTaskPriority.NORMAL, 1);

        List<RetryTaskDao.FetchRequest> requests = List.of(new RetryTaskDao.FetchRequest(1,
                RetryTaskPriority.NORMAL, Duration.ofSeconds(10), 100));
        List<RetryTaskInfo> tasks = dao.getTasksForProcess(requests, RetryTaskTestUtils.SERVICE_KEY);

        Assertions.assertEquals(2, tasks.size());
        List<Long> ids = Lists.transform(tasks, RetryTaskInfo::getId);
        Assertions.assertTrue(ids.contains(t1.getId()));
        Assertions.assertTrue(ids.contains(t2.getId()));
    }

    @Test
    public void getTasksForProcess_groupIsNull() {
        RetryTask t1 = utils.createTask(-1, RetryTaskPriority.NORMAL, null);
        RetryTask t2 = utils.createTask(1, RetryTaskPriority.NORMAL, null);
        RetryTask t3 = utils.createTask(100, RetryTaskPriority.NORMAL, null);

        List<RetryTaskDao.FetchRequest> requests = List.of(new RetryTaskDao.FetchRequest(null,
                RetryTaskPriority.NORMAL, Duration.ofSeconds(10), 100));
        List<RetryTaskInfo> tasks = dao.getTasksForProcess(requests, RetryTaskTestUtils.SERVICE_KEY);

        Assertions.assertEquals(2, tasks.size());
        List<Long> ids = Lists.transform(tasks, RetryTaskInfo::getId);
        Assertions.assertTrue(ids.contains(t1.getId()));
        Assertions.assertTrue(ids.contains(t2.getId()));
    }

    @BeforeEach
    public void setUp() {
        txService.runInNewTx(() -> dao.deleteTasks());
    }
}
