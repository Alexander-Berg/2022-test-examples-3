package ru.yandex.market.logistics.dbqueue.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.logistics.dbqueue.BaseTest;
import ru.yandex.market.logistics.dbqueue.TestQueueMetaProvider;
import ru.yandex.market.logistics.dbqueue.controller.ResourceNotFoundException;
import ru.yandex.market.logistics.dbqueue.dao.DbQueueTaskDao;
import ru.yandex.market.logistics.dbqueue.domain.DbQueueTask;
import ru.yandex.money.common.dbqueue.dao.QueueDao;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;

class DbQueueTaskServiceTest extends BaseTest {

    private final DbQueueTaskDao dbQueueTaskDao = Mockito.mock(DbQueueTaskDao.class);
    private final QueueDao queueDao = Mockito.mock(QueueDao.class);
    private final String tableName = "table";

    private final DbQueueTaskService service =
        new DbQueueTaskServiceImpl(dbQueueTaskDao, new TestQueueMetaProvider(), queueDao, tableName);

    @Test
    public void testReEnqueueMissing() {
        Long id = 10L;
        Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> service.reenqueue(id),
            "DBQueue task with id " + id + " not found"
        );
    }

    @Test
    public void testReEnqueue() {
        final long taskId = 10L;
        final String queueName = "test.queue.10";

        Mockito.when(dbQueueTaskDao.getById(taskId)).thenReturn(Optional.of(
            DbQueueTask.builder()
                .id(taskId)
                .queueName(queueName)
                .payload("{}")
                .createTime(Instant.MAX)
                .processTime(Instant.MAX)
                .attempt(1)
                .reenqueueAttempt(0)
                .totalAttempt(1)
                .build()
        ));

        service.reenqueue(taskId);

        Mockito.verify(queueDao).reenqueue(
            Mockito.eq(
                QueueLocation.builder()
                    .withQueueId(new QueueId(queueName))
                    .withTableName(tableName)
                    .build()
            ),
            Mockito.eq(taskId),
            Mockito.eq(Duration.ZERO)
        );
    }

    @Test
    public void testRemoveMissing() {
        long id = 10L;
        Assertions.assertThrows(
            ResourceNotFoundException.class,
            () -> service.remove(id),
            "DBQueue task with id " + id + " not found"
        );
    }

    @Test
    public void testRemove() {
        final long taskId = 10L;
        final String queueName = "test.queue.10";

        Mockito.when(dbQueueTaskDao.getById(taskId)).thenReturn(Optional.of(
            DbQueueTask.builder()
                .id(taskId)
                .queueName(queueName)
                .payload("{}")
                .createTime(Instant.MAX)
                .processTime(Instant.MAX)
                .attempt(1)
                .reenqueueAttempt(0)
                .totalAttempt(1)
                .build()
        ));

        service.remove(taskId);

        Mockito.verify(queueDao).deleteTask(
            Mockito.eq(
                QueueLocation.builder()
                    .withQueueId(new QueueId(queueName))
                    .withTableName(tableName)
                    .build()
            ),
            Mockito.eq(taskId)
        );
    }
}
