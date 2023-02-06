package ru.yandex.market.ff.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.ff.config.JpaConfig;
import ru.yandex.market.ff.config.ServiceConfiguration;
import ru.yandex.market.ff.config.dbqueue.CustomQueueShard;
import ru.yandex.market.ff.config.dbqueue.EmptyThreadLifecycleListener;
import ru.yandex.market.ff.config.dbqueue.GeneralShardRouter;
import ru.yandex.market.ff.config.dbqueue.JsonSerializablePayloadTransformer;
import ru.yandex.market.ff.config.dbqueue.LoggingTaskListener;
import ru.yandex.market.ff.model.dbqueue.ExecutionQueueItemPayload;
import ru.yandex.market.ff.model.enums.DbQueueType;
import ru.yandex.market.ff.service.DbQueueLogService;
import ru.yandex.money.common.dbqueue.api.QueueShard;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.TaskLifecycleListener;
import ru.yandex.money.common.dbqueue.init.QueueExecutionPool;
import ru.yandex.money.common.dbqueue.init.QueueRegistry;
import ru.yandex.money.common.dbqueue.settings.QueueConfig;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;
import ru.yandex.money.common.dbqueue.settings.QueueSettings;
import ru.yandex.money.common.dbqueue.spring.SpringQueueCollector;
import ru.yandex.money.common.dbqueue.spring.SpringQueueConfigContainer;
import ru.yandex.money.common.dbqueue.spring.SpringQueueInitializer;
import ru.yandex.money.common.dbqueue.spring.SpringQueueInitializerWithoutConsumers;
import ru.yandex.money.common.dbqueue.spring.SpringTaskPayloadTransformer;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT;

/**
 * Конфигурация для db-queue.
 */
@Configuration
@Import({
        JpaConfig.class,
        DbQueueWorkersConfigWithoutConsumers.class,
        ServiceConfiguration.class,
})
public class DbQueueConfigWithoutConsumers {

    @Bean
    public SpringQueueConfigContainer springQueueConfigContainer() {
        return new SpringQueueConfigContainer(getConfigs());
    }

    @Bean(name = "internalObjectMapper")
    public ObjectMapper internalObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(NON_ABSENT);

        return objectMapper;
    }

    @Bean
    public SpringQueueCollector springQueueCollector(JdbcTemplate jdbcTemplate,
                                                     TransactionTemplate transactionTemplate,
                                                     @Qualifier("internalObjectMapper") ObjectMapper objectMapper) {
        SpringQueueCollector collector = new SpringQueueCollector();
        createTransformers(objectMapper).forEach(tr ->
                collector.postProcessBeforeInitialization(tr, tr.getQueueId() + "Transformer"));
        createShards(jdbcTemplate, transactionTemplate).forEach((type, shard) ->
                collector.postProcessBeforeInitialization(
                        GeneralShardRouter.of(type, shard), type.name() + "ShardRouter"));
        return collector;
    }

    @Bean
    public SpringQueueInitializer springQueueInitializer(SpringQueueConfigContainer springQueueConfigContainer,
                                                         SpringQueueCollector springQueueCollector,
                                                         DbQueueLogService queueLogService,
                                                         @Qualifier("internalObjectMapper") ObjectMapper objectMapper) {
        TaskLifecycleListener lifecycleListener = new LoggingTaskListener(queueLogService, objectMapper);

        return new SpringQueueInitializerWithoutConsumers(springQueueConfigContainer, springQueueCollector,
                new QueueExecutionPool(new QueueRegistry(), lifecycleListener, new EmptyThreadLifecycleListener()));
    }

    private List<QueueConfig> getConfigs() {
        return DbQueueType.streamValues().map(val -> new QueueConfig(
                QueueLocation.builder().withTableName("queue_tasks").withQueueId(new QueueId(val.name())).build(),
                QueueSettings.builder()
                        .withBetweenTaskTimeout(Duration.ofSeconds(val.getBetweenTasksTimeout()))
                        .withNoTaskTimeout(Duration.ofSeconds(val.getNoTaskTimeout()))
                        .withRetryType(val.getTaskRetryType())
                        .withRetryInterval(Duration.ofSeconds(val.getRetryInterval()))
                        .withProcessingMode(val.getProcessingMode())
                        .build()
        )).collect(Collectors.toList());
    }

    private Stream<SpringTaskPayloadTransformer<? extends ExecutionQueueItemPayload>> createTransformers(
            ObjectMapper objectMapper) {
        return DbQueueType.streamValues().map(x -> JsonSerializablePayloadTransformer.of(x, objectMapper));
    }

    private Map<DbQueueType, QueueShard> createShards(JdbcTemplate jdbcTemplate,
                                                      TransactionTemplate transactionTemplate) {
        return DbQueueType.streamValues()
                .collect(
                        Collectors.toMap(
                                Function.identity(),
                                val -> new CustomQueueShard(new QueueShardId(val.name()), jdbcTemplate,
                                        transactionTemplate)
                        )
                );
    }
}
