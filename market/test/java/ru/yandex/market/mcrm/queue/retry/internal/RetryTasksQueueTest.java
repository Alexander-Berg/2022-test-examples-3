package ru.yandex.market.mcrm.queue.retry.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Collections2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.mcrm.handshake.AvailabilityConfiguration;
import ru.yandex.market.mcrm.handshake.HandshakeDao;
import ru.yandex.market.mcrm.handshake.HandshakeServiceImpl;
import ru.yandex.market.mcrm.queue.retry.RetryServiceTestConfiguration;
import ru.yandex.market.mcrm.queue.retry.RetryTaskPriority;

@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RetryServiceTestConfiguration.class)
@TestPropertySource("classpath:/ru/yandex/market/mcrm/queue/retry/test.properties")
public class RetryTasksQueueTest {

    @Inject
    RetryTaskDao dao;
    @Inject
    RetryTaskTestUtils utils;
    @Inject
    FastRetryTasksQueue queue;
    @Inject
    HandshakeDao handshakeDao;
    @Inject
    HandshakeServiceImpl handshakeService;
    @Inject
    AvailabilityConfiguration availabilityConfiguration;

    @BeforeEach
    public void setUp() {
        dao.deleteTasks();
        queue.reset();
    }

    @Test
    public void lockedGroup() {
        int count = queue.lockedGroupsCount();
        Assertions.assertEquals(257, count, "Должно быть заблокировано 256 (0-255) групп и 1 для null");
    }

    @Test
    public void readByNextAttempt() {
        RetryTask t_1_0 = utils.createTask(-1, RetryTaskPriority.HIGH, 1);
        RetryTask t_1_1 = utils.createTask(2, RetryTaskPriority.HIGH, 1);
        utils.createTask(100, RetryTaskPriority.HIGH, 1);

        AbstractRetryTasksQueue.RetryTaskGroup group = queue.getGroup(1);
        AbstractRetryTasksQueue.RetryTasksGroupQueue groupQueue = group.getQueue(RetryTaskPriority.HIGH);
        queue.ensure(RetryTaskPriority.HIGH, AbstractRetryTasksQueue.RetryTaskGroup::getHigh);
        Deque<RetryTaskInfo> tasks = groupQueue.getTasks();

        Assertions.assertEquals(2, tasks.size());
        Collection<Long> ids = Collections2.transform(tasks, RetryTaskInfo::getId);
        Assertions.assertTrue(ids.contains(t_1_0.getId()), "Должны загрузить уже просроченные таски");
        Assertions.assertTrue(
                ids.contains(t_1_1.getId()),
                "Должны загрузить таски которые подлежат выполнению в ближайшие prefetchInterval секунд");

        List<RetryTask> processed = new ArrayList<>();
        queue.next(t -> processed.add(t));
        queue.next(t -> processed.add(t));

        Assertions.assertEquals(1, processed.size(),
                "Должны получить просроченные таски и не должны еще непросроченные");
        Assertions.assertTrue(processed.contains(t_1_0));
    }


    @Test
    public void taskOrder() throws Exception {
        RetryTask t_0 = utils.createTask(-100, RetryTaskPriority.LOW, 1);
        RetryTask t_1 = utils.createTask(-50, RetryTaskPriority.LOW, null);
        RetryTask t_2 = utils.createTask(-75, RetryTaskPriority.NORMAL, null);
        RetryTask t_3 = utils.createTask(-25, RetryTaskPriority.NORMAL, 2);
        RetryTask t_4 = utils.createTask(-45, RetryTaskPriority.HIGH, null);
        RetryTask t_5 = utils.createTask(-10, RetryTaskPriority.HIGH, 3);
        RetryTask t_6 = utils.createTask(1, RetryTaskPriority.HIGH, 4);
        RetryTask t_7 = utils.createTask(1, RetryTaskPriority.NORMAL, 4);
        RetryTask t_8 = utils.createTask(1, RetryTaskPriority.LOW, 4);

        List<RetryTask> processed = getAllRetryTasks();

        // Получили только задачи, время выполнения которых уже наступило
        // Последовательность получения задач: сортированы по приоритету и времени выполнения
        Assertions.assertEquals(6, processed.size());
        Assertions.assertEquals(t_4, processed.get(0));
        Assertions.assertEquals(t_5, processed.get(1));
        Assertions.assertEquals(t_2, processed.get(2));
        Assertions.assertEquals(t_3, processed.get(3));
        Assertions.assertEquals(t_0, processed.get(4));
        Assertions.assertEquals(t_1, processed.get(5));

        Thread.sleep(1_000);

        // Получили только новые просроченные задачи
        // Последовательность получения задач: сортированы по приоритету и времени выполнения
        processed = getAllRetryTasks();
        Assertions.assertEquals(3, processed.size());
        Assertions.assertEquals(t_6, processed.get(0));
        Assertions.assertEquals(t_7, processed.get(1));
        Assertions.assertEquals(t_8, processed.get(2));
    }

    @Test
    public void reshard() throws Exception {
        int count = queue.lockedGroupsCount();
        Assertions.assertEquals(257, count, "Должно быть заблокировано 256 (0-255) групп и 1 для null");

        String otherHost = Randoms.string();
        handshakeDao.addHandshake(otherHost, "dc1", availabilityConfiguration.getServiceKey());

        handshakeService.update();
        queue.updateGroups();
        Thread.sleep(100);

        int countAfterAddHost = queue.lockedGroupsCount();
        Assertions.assertEquals(129, countAfterAddHost, "Должно быть заблокировано 128 групп и 1 для null (128 групп " +
                "должны освободиться т.к. " +
                "появился новый хост)");

        handshakeDao.deleteHandshake(otherHost, availabilityConfiguration.getServiceKey());

        handshakeService.update();
        queue.updateGroups();
        Thread.sleep(100);

        int countAfterRelease = queue.lockedGroupsCount();
        Assertions.assertEquals(257, countAfterRelease, "Должно быть заблокировано 256 групп и 1 для null (128 групп " +
                "должны взять т.к. " +
                "исчез один хост)");

    }

    /**
     * Если задача была удалена после кеширования, то она не должна выполняться.
     * <p>
     * https://st.yandex-team.ru/OCRM-7400
     */
    @Test
    public void deleteTask() throws Exception {
        RetryTask t_0 = utils.createTask(-100, RetryTaskPriority.LOW, 1);

        // принудительно загружаем задачи в кеш, что бы в кеше была задача. которую удалим из бд ниже
        queue.ensure(RetryTaskPriority.LOW, AbstractRetryTasksQueue.RetryTaskGroup::getLow);
        // удаляем из базы. но в кеше задача должна остаться
        dao.delete(t_0);

        List<RetryTask> processed = getAllRetryTasks();

        Assertions.assertEquals(
                0, processed.size(), "Не должны получить задачи т.к. должны инвалидировать каждую задачу перед ее выполнением");
    }

    private List<RetryTask> getAllRetryTasks() {
        List<RetryTask> processed = new ArrayList<>();
        for (int i = 0; i < 10; ++i) {
            queue.next(t -> {
                processed.add(t);
                dao.delete(t);
            });
        }
        return processed;
    }
}
