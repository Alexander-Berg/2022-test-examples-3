package ru.yandex.money.common.dbqueue.spring;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;

import ru.yandex.money.common.dbqueue.init.QueueExecutionPool;
import ru.yandex.money.common.dbqueue.init.QueueRegistry;
import ru.yandex.money.common.dbqueue.settings.QueueConfig;

import static java.util.Objects.requireNonNull;

public class SpringQueueInitializerWithoutConsumers extends SpringQueueInitializer {

    private static final Logger log = LoggerFactory.getLogger(SpringQueueInitializerWithoutConsumers.class);

    @Nonnull
    private final QueueRegistry queueRegistry;
    @Nonnull
    private final QueueExecutionPool executionPool;
    @Nonnull
    private final SpringQueueConfigContainer configContainer;
    @Nonnull
    private final SpringQueueCollector queueCollector;

    /**
     * Конструктор
     *
     * @param configContainer настройки очередей
     * @param queueCollector  поставщик бинов, связанных с очередями
     * @param executionPool   менеджер запуска очередей
     */
    public SpringQueueInitializerWithoutConsumers(@Nonnull SpringQueueConfigContainer configContainer,
                                                  @Nonnull SpringQueueCollector queueCollector,
                                                  @Nonnull QueueExecutionPool executionPool) {
        super(configContainer, queueCollector, executionPool);
        this.configContainer = requireNonNull(configContainer);
        this.queueCollector = requireNonNull(queueCollector);
        this.executionPool = requireNonNull(executionPool);
        this.queueRegistry = requireNonNull(executionPool.getQueueRegistry());
    }

    private void init() {
        wireQueueConfig();

        queueCollector.getTaskListeners().forEach(queueRegistry::registerTaskLifecycleListener);
        queueCollector.getThreadListeners().forEach(queueRegistry::registerThreadLifecycleListener);
        queueCollector.getExecutors().forEach(queueRegistry::registerExternalExecutor);
        queueRegistry.finishRegistration();
        queueCollector.getConsumers().values().forEach(SpringQueueConsumer::onInitialized);
        queueCollector.getProducers().values().forEach(SpringQueueProducer::onInitialized);
    }

    private void wireQueueConfig() {
        queueCollector.getProducers().forEach((queueId, producer) -> {
            QueueConfig queueConfig = requireNonNull(configContainer.getQueueConfig(queueId).orElse(null));
            SpringTaskPayloadTransformer payloadTransformer = requireNonNull(
                    queueCollector.getTransformers().get(queueId));
            SpringQueueShardRouter shardRouter = requireNonNull(queueCollector.getShardRouters().get(queueId));

            producer.setPayloadTransformer(payloadTransformer);
            producer.setProducerShardRouter(shardRouter);
            producer.setQueueConfig(queueConfig);
        });
    }

    @Override
    public void destroy() {
        executionPool.shutdown();
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (executionPool.isInitialized()) {
            log.info("Queue initialization skipped");
            return;
        }
        init();
        executionPool.init();
        executionPool.start();
    }
}
