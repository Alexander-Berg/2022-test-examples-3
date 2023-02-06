package ru.yandex.market.dsm.dbqueue;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.config.DatabaseDialect;
import ru.yoomoney.tech.dbqueue.config.QueueShard;
import ru.yoomoney.tech.dbqueue.config.QueueShardId;
import ru.yoomoney.tech.dbqueue.config.QueueTableSchema;
import ru.yoomoney.tech.dbqueue.config.impl.NoopTaskLifecycleListener;
import ru.yoomoney.tech.dbqueue.internal.processing.QueueProcessingStatus;
import ru.yoomoney.tech.dbqueue.internal.runner.BaseQueueRunner;
import ru.yoomoney.tech.dbqueue.internal.runner.QueueRunner;
import ru.yoomoney.tech.dbqueue.spring.dao.SpringDatabaseAccessLayer;

import ru.yandex.market.dsm.external.dbqueue.base.BaseQueueConsumer;
import ru.yandex.market.dsm.external.dbqueue.log.BaseQueueLogService;
import ru.yandex.market.dsm.external.dbqueue.log.domain.QueueLog;
import ru.yandex.market.dsm.external.dbqueue.log.domain.QueueLogRepository;
import ru.yandex.market.dsm.external.dbqueue.model.DbQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.dsm.dbqueue.DsmDbQueueConstants.SINGLE_SHARD_ID;

/**
 * @author valter
 */
@Service
public class DbQueueTestUtil {

    private final QueueLogRepository queueLogRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final List<BaseQueueConsumer<?>> queueConsumers;


    @Autowired
    public DbQueueTestUtil(QueueLogRepository queueLogRepository, NamedParameterJdbcTemplate jdbcTemplate,
                           TransactionTemplate transactionTemplate, BaseQueueLogService queueLogService,
                           List<BaseQueueConsumer<?>> queueConsumers) {
        this.queueLogRepository = queueLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
        this.queueConsumers = queueConsumers;
    }

    public void assertQueueHasSingleEvent(DbQueue queueType, String eventId) {
        List<String> logs = getQueue(queueType);
        assertThat(logs.size()).isEqualTo(1);
        assertThat(logs.get(0)).isEqualTo(eventId);
    }

    public void assertQueueLogHasSize(DbQueue queueType, int count) {
        List<String> logs = getQueue(queueType);
        assertThat(logs.size()).isEqualTo(count);
    }

    public void assertQueueHasSize(DbQueue queueType, int count) {
        var tasks = getTasks(queueType);
        assertThat(tasks.size()).isEqualTo(count);
    }

    public boolean isEmpty(DbQueue queueType) {
        return jdbcTemplate.queryForList(
                "select '1' from queue_tasks where queue_name = :queue_name",
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
                "DELETE FROM queue_tasks qt WHERE qt.queue_name = :queueName ",
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
                "SELECT * FROM queue_tasks qt WHERE qt.queue_name = :queueName",
                Map.of("queueName", queueType.name()),
                (rs, rowNum) -> Task.builder(new QueueShardId("fake-shard-id"))
                        .withPayload(rs.getString("payload"))
                        .withAttemptsCount(rs.getLong("attempt"))
                        .withCreatedAt(ZonedDateTime.now())
                        .build()
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
        return BaseQueueRunner.Factory.create(
                consumer,
                getQueueShard(),
                new NoopTaskLifecycleListener()
        );
    }

    private QueueShard<?> getQueueShard() {
        var databaseAccessLayer = new SpringDatabaseAccessLayer(
                DatabaseDialect.POSTGRESQL, QueueTableSchema.builder().build(),
                jdbcTemplate.getJdbcTemplate(),
                transactionTemplate);
        return new QueueShard<>(new QueueShardId(SINGLE_SHARD_ID),
                databaseAccessLayer);
    }

}
