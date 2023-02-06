package ru.yandex.market.logistics.iris.configuration;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionOperations;

import ru.yandex.market.logistics.iris.configuration.queue.listeners.EmptyListener;
import ru.yandex.market.logistics.iris.configuration.queue.listeners.EmptyTaskListener;
import ru.yandex.market.logistics.iris.configuration.queue.support.GeneralShardRouter;
import ru.yandex.market.logistics.iris.configuration.queue.support.JsonSerializablePayloadTransformer;
import ru.yandex.market.logistics.iris.jobs.model.ExecutionQueueItemPayload;
import ru.yandex.market.logistics.iris.jobs.model.QueueType;
import ru.yandex.market.logistics.iris.repository.CustomQueueShard;
import ru.yandex.money.common.dbqueue.api.QueueShard;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.init.QueueExecutionPool;
import ru.yandex.money.common.dbqueue.init.QueueRegistry;
import ru.yandex.money.common.dbqueue.settings.QueueConfig;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.settings.QueueLocation;
import ru.yandex.money.common.dbqueue.settings.QueueSettings;
import ru.yandex.money.common.dbqueue.spring.SpringQueueCollector;
import ru.yandex.money.common.dbqueue.spring.SpringQueueConfigContainer;
import ru.yandex.money.common.dbqueue.spring.SpringQueueInitializer;
import ru.yandex.money.common.dbqueue.spring.SpringQueueProducer;
import ru.yandex.money.common.dbqueue.spring.SpringTaskPayloadTransformer;

import static ru.yandex.market.logistics.iris.configuration.queue.DbQueueConfiguration.DB_QUEUE_OBJECT_MAPPER;

@Configuration
public class DbQueueTestConfiguration {

    private final ObjectMapper dbQueueObjectMapper;

    @Autowired
    public DbQueueTestConfiguration(@Qualifier(DB_QUEUE_OBJECT_MAPPER) ObjectMapper dbQueueObjectMapper) {
        this.dbQueueObjectMapper = dbQueueObjectMapper;
    }

    @Bean
    @Primary
    SpringQueueConfigContainer springQueueConfigContainer() {
        return new SpringQueueConfigContainer(getConfigs());
    }

    @Bean
    @Primary
    SpringQueueInitializer springQueueInitializer(SpringQueueConfigContainer springQueueConfigContainer,
                                                  SpringQueueCollector springQueueCollector) {
        return new SpringQueueInitializer(springQueueConfigContainer, springQueueCollector,
            new QueueExecutionPool(new QueueRegistry(), new EmptyTaskListener(), new EmptyListener()));
    }

    @Bean
    @Primary
    SpringQueueCollector springQueueCollector(JdbcTemplate jdbcTemplate, TransactionOperations operations) {
        SpringQueueCollector collector = new TestSpringQueueCollector();

        createTransformers().forEach(tr ->
            collector.postProcessBeforeInitialization(tr, tr.getQueueId() + "Transformer"));

        createShards(jdbcTemplate, operations).forEach((type, shard) ->
            collector.postProcessBeforeInitialization(
                GeneralShardRouter.of(type, shard), type.name() + "ShardRouter"));

        return collector;
    }

    private List<QueueConfig> getConfigs() {
        return QueueType.streamValues().map(val -> new QueueConfig(
            QueueLocation.builder().withTableName("queue_tasks").withQueueId(new QueueId(val.name())).build(),
            QueueSettings.builder()
                .withBetweenTaskTimeout(Duration.ofMillis(200L))
                .withNoTaskTimeout(Duration.ofMillis(200L))
                .withRetryInterval(Duration.ofMillis(200L))
                .build()
        )).collect(Collectors.toList());
    }

    private Stream<SpringTaskPayloadTransformer<? extends ExecutionQueueItemPayload>> createTransformers() {
        return QueueType.streamValues()
            .map(queueType -> JsonSerializablePayloadTransformer.of(queueType, dbQueueObjectMapper));
    }

    private Map<QueueType, QueueShard> createShards(JdbcTemplate jdbcTemplate,
                                                    TransactionOperations operations) {
        return QueueType.streamValues()
            .collect(Collectors.toMap(Function.identity(),
                val -> new CustomQueueShard(new QueueShardId(val.name()), jdbcTemplate, operations)));
    }

    private static class TestSpringQueueCollector extends SpringQueueCollector {

        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (SpringQueueProducer.class.isAssignableFrom(bean.getClass())) {
                bean = Mockito.spy(bean);
            }
            return super.postProcessBeforeInitialization(bean, beanName);
        }
    }
}
