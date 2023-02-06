package ru.yandex.market.tpl.common.db.test;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.db.queue.base.BaseQueueConsumer;
import ru.yandex.market.tpl.common.db.queue.base.LoggingTaskListener;
import ru.yandex.market.tpl.common.db.queue.log.QueueLog;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogRepository;
import ru.yandex.market.tpl.common.db.queue.model.DbQueue;
import ru.yandex.money.common.dbqueue.api.QueueConsumer;
import ru.yandex.money.common.dbqueue.api.QueueShard;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.internal.QueueProcessingStatus;
import ru.yandex.money.common.dbqueue.internal.runner.BaseQueueRunner;
import ru.yandex.money.common.dbqueue.internal.runner.QueueRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author valter
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DbQueueTestUtil {

    private final QueueLogRepository queueLogRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    private final LoggingTaskListener loggingTaskListener;
    private final List<? extends BaseQueueConsumer<?>> queueConsumers;

    public void assertQueueHasSingleEvent(DbQueue queueType, String eventId) {
        List<String> logs = getQueue(queueType);
        assertThat(logs.size()).isEqualTo(1);
        assertThat(logs.get(0)).isEqualTo(eventId);
    }

    public void assertQueueHasSize(DbQueue queueType, int count) {
        List<String> logs = getQueue(queueType);
        assertThat(logs.size()).isEqualTo(count);
    }

    public void assertTasksHasSize(DbQueue queueType, int count) {
        List<Task> tasks = getTasks(queueType);
        assertThat(tasks).hasSize(count);
    }

    public boolean isEmpty(DbQueue queueType) {
        return jdbcTemplate.queryForList(
                "select '1' from queue_task where queue_name = :queue_name",
                Map.of("queue_name", queueType.name()),
                String.class
        ).isEmpty();
    }

    public List<String> getQueue(DbQueue queueType) {
        return queueLogRepository.findAll().stream()
                .filter(ql -> Objects.equals(ql.getQueueName(), queueType.name()))
                .map(QueueLog::getEntityId)
                .collect(Collectors.toList());
    }

    public void clear(DbQueue queueType) {
        queueLogRepository.findAll().stream()
                .filter(ql -> Objects.equals(ql.getQueueName(), queueType.name()))
                .forEach(queueLogRepository::delete);
        jdbcTemplate.update(
                "delete from queue_task where queue_name = :queueName",
                Map.of("queueName", queueType.name())
        );
    }

    public List<Long> getTasksIds(DbQueue queueType) {
        return queueLogRepository.findAll().stream()
                .filter(ql -> Objects.equals(ql.getQueueName(), queueType.name()))
                .map(QueueLog::getTaskId)
                .collect(Collectors.toList());
    }

    public List<Task> getTasks(DbQueue queueType) {
        return jdbcTemplate.query(
                "SELECT * FROM queue_task qt WHERE qt.queue_name = :queueName",
                Map.of("queueName", queueType.name()),
                (rs, rowNum) -> new Task(
                        new QueueShardId("fake-shard-id"),
                        rs.getString("task"),
                        rs.getLong("attempt"),
                        ZonedDateTime.now(),
                        null,
                        rs.getString("actor")
                )
        );
    }


    public void executeSingleQueueItem(DbQueue queueType) {
        QueueConsumer<?> consumer = queueConsumer(queueType);
        QueueRunner queueRunner = queueRunner(queueType, consumer);
        assertThat(queueRunner.runQueue(consumer)).isEqualTo(QueueProcessingStatus.PROCESSED);
    }

    public void executeAllQueueItems(DbQueue queueType) {
        QueueConsumer<?> consumer = queueConsumer(queueType);
        QueueRunner queueRunner = queueRunner(queueType, consumer);
        //noinspection StatementWithEmptyBody
        while (queueRunner.runQueue(consumer) != QueueProcessingStatus.SKIPPED) {
            // do nothing
        }
    }

    private QueueConsumer<?> queueConsumer(DbQueue queueType) {
        return queueConsumers.stream()
                .filter(c -> c.getQueueId().asString().equals(queueType.name()))
                .findAny()
                .orElseThrow();
    }

    private QueueRunner queueRunner(DbQueue queueType, QueueConsumer<?> consumer) {
        return BaseQueueRunner.Factory.createQueueRunner(
                consumer,
                new QueueShard(new QueueShardId(queueType.name()), jdbcTemplate.getJdbcTemplate(), transactionTemplate),
                loggingTaskListener, null
        );
    }

}
