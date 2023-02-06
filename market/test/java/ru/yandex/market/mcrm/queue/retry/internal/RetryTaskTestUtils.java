package ru.yandex.market.mcrm.queue.retry.internal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.google.common.collect.Sets;
import org.junit.Assert;
import org.springframework.stereotype.Service;

import ru.yandex.market.crm.util.CrmStrings;
import ru.yandex.market.crm.util.HostNames;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.mcrm.queue.retry.RetryTaskPriority;

@Service
public class RetryTaskTestUtils {

    public static final String SERVICE_KEY = "default";

    @Inject
    RetryTaskDao dao;

    public static byte[] randomBytea() {
        return CrmStrings.getBytes(UUID.randomUUID().toString());
    }


    public RetryTask createTask(int seconds) {
        return createTask(seconds, RetryTaskPriority.NORMAL);
    }

    public RetryTask createTask(int seconds, @Nonnull RetryTaskPriority priority) {
        return createTask(seconds, priority, null);
    }

    public RetryTask createTask(int seconds, @Nonnull RetryTaskPriority priority, Integer group) {
        RetryTask task = new RetryTask();
        task.setCode(Randoms.string());
        task.setAttemptNumber(ThreadLocalRandom.current().nextInt());
        task.setNextAttemptTime(OffsetDateTime.now().plusSeconds(seconds));
        task.setConfiguration(randomBytea());
        task.setContext(randomBytea());
        task.setServiceKey(SERVICE_KEY);
        task.setPriority(priority);
        task.setGroupId(group);

        dao.add(task);
        return task;
    }

    public RetryTask getSingleTask() {
        List<RetryTask> tasks = dao.getTasks();

        Assert.assertNotNull(tasks);
        Assert.assertEquals(1, tasks.size());

        return tasks.get(0);
    }

    public long getSingleTaskId() {
        return getSingleTask().getId();
    }

    public void assertTasksIsEmpty() {
        List<RetryTask> tasks = dao.getTasks();

        Assert.assertNotNull(tasks);
        Assert.assertEquals(0, tasks.size());
    }

    public RetryTask getSingleTaskForProcess() {
        RetryTask task = dao.getTaskForProcess(HostNames.instanceKey(), Sets.newHashSet(0), SERVICE_KEY);

        Assert.assertNotNull(task);

        return task;
    }
}
