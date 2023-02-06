package ru.yandex.market.logistics.calendaring.config

import org.apache.commons.lang.BooleanUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.market.logistics.calendaring.config.dbqueue.DbqueueProperties
import ru.yandex.market.logistics.calendaring.config.dbqueue.shard.CustomQueueShard
import ru.yandex.market.logistics.calendaring.dbqueue.DbqueueTaskType
import ru.yandex.market.logistics.calendaring.dbqueue.base.BaseDbqueueConsumer
import ru.yandex.market.logistics.calendaring.dbqueue.listners.LoggingTaskListener
import ru.yandex.market.logistics.calendaring.dbqueue.registry.DbqueueConfigRegistry
import ru.yandex.money.common.dbqueue.config.*
import ru.yandex.money.common.dbqueue.config.impl.NoopThreadLifecycleListener
import ru.yandex.money.common.dbqueue.settings.QueueConfig
import ru.yandex.money.common.dbqueue.settings.QueueId
import ru.yandex.money.common.dbqueue.settings.QueueLocation
import ru.yandex.money.common.dbqueue.settings.QueueSettings
import java.time.Duration
import java.util.*
import javax.sql.DataSource


@Configuration
open class DbqueueNotActiveConfig(private val dbqueueProperties: DbqueueProperties) {

    val log: Logger = LoggerFactory.getLogger(LoggingTaskListener::class.java)

    @Bean
    open fun queueService(
        queueShard: QueueShard,
        allConsumers: List<BaseDbqueueConsumer<*>>
    ): QueueService {
        return QueueService(
            listOf(queueShard),
            NoopThreadLifecycleListener.getInstance(),
            loggingTaskListener()
        )
    }

    @Bean
    open fun queueShard(dataSource: DataSource, transactionOperations: TransactionOperations): QueueShard {
        return CustomQueueShard(
            DatabaseDialect.POSTGRESQL,
            QueueTableSchema.builder().build(),
            QueueShardId(dbqueueProperties.queueShardId),
            JdbcTemplate(dataSource),
            transactionOperations
        )
    }

    @Bean
    open fun queueRegister(): DbqueueConfigRegistry {
        val map: MutableMap<DbqueueTaskType, QueueConfig> = EnumMap(DbqueueTaskType::class.java)
        for (taskType in DbqueueTaskType.values()) {
            map[taskType] = queueConfig(taskType)
        }
        return DbqueueConfigRegistry(map)
    }

    @Bean
    open fun loggingTaskListener(): TaskLifecycleListener {
        return LoggingTaskListener()
    }

    private fun queueConfig(taskType: DbqueueTaskType): QueueConfig {
        log.info("Building config for queue {}", taskType)
        return QueueConfig(
            QueueLocation.builder()
                .withTableName(dbqueueProperties.tableName)
                .withQueueId(QueueId(taskType.name))
                .build(),
            QueueSettings.builder()
                .withBetweenTaskTimeout(
                    Duration.ofMillis(
                        (taskType.delayBetweenTasks ?: dbqueueProperties.betweenTaskTimeout).toLong()
                    )
                )
                .withNoTaskTimeout(
                    Duration.ofMillis(dbqueueProperties.noTaskTimeout.toLong())
                )
                .withThreadCount(getThreadCount(taskType))
                .withProcessingMode(taskType.processingMode)
                .withRetryInterval(
                    Optional.ofNullable(taskType.retryInterval.toLong())
                        .map { millis: Long -> Duration.ofMillis(millis) }
                        .orElse(null)
                )
                .withRetryType(Optional.ofNullable(taskType.taskRetryType).orElse(null))
                .build()
        )
    }

    private fun getThreadCount(taskType: DbqueueTaskType): Int {
        if (BooleanUtils.isFalse(dbqueueProperties.processingEnabled)) {
            log.info("Queue processing is disabled")
            return 0
        }
        return Optional.ofNullable(taskType.threadCount)
            .orElse(dbqueueProperties.threadCountPerQueue)
    }
}
