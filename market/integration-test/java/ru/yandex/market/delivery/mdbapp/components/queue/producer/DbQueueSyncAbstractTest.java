package ru.yandex.market.delivery.mdbapp.components.queue.producer;

import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.delivery.mdbapp.AllMockContextualTest;
import ru.yandex.market.delivery.mdbapp.components.queue.GenericJsonTransformer;
import ru.yandex.market.delivery.mdbapp.components.queue.TaskActorTransformer;
import ru.yandex.market.delivery.mdbapp.configuration.queue.AbstractQueueDefinition;
import ru.yandex.market.delivery.mdbapp.configuration.queue.QueueInitializationSynchronizer;
import ru.yandex.market.delivery.mdbapp.configuration.queue.SpringSingleShardRouter;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;
import ru.yandex.money.common.dbqueue.api.QueueConsumer;
import ru.yandex.money.common.dbqueue.api.QueueProducer;
import ru.yandex.money.common.dbqueue.api.QueueShardRouter;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;
import ru.yandex.money.common.dbqueue.api.TaskPayloadTransformer;
import ru.yandex.money.common.dbqueue.settings.QueueId;
import ru.yandex.money.common.dbqueue.spring.SpringQueueConsumer;
import ru.yandex.money.common.dbqueue.spring.SpringQueueInitializer;

/**
 * Подробности тут https://st.yandex-team.ru/DELIVERY-23045
 */
@ContextConfiguration(classes = DbQueueSyncAbstractTest.Config.class)
public abstract class DbQueueSyncAbstractTest extends AllMockContextualTest {

    private static final QueueId QUEUE_ID = new QueueId("test.sample.queue.id");

    static boolean earlyEnqueueHappened = false;
    static Throwable thrownByEarlyEnqueue = null;

    @SpyBean
    protected QueueProducer<Object> testSampleProducer;
    @SpyBean
    protected QueueInitializationSynchronizer synchronizer;
    @SpyBean
    protected SpringQueueInitializer springQueueInitializer;

    @AfterEach
    public void tearDown() {
        earlyEnqueueHappened = false;
        thrownByEarlyEnqueue = null;
    }

    @Nullable
    static Throwable getThrowable(Callable<?> callable) {
        try {
            callable.call();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    // Проверка того, что это именно то исключение, которое нашли в логах: https://paste.yandex-team.ru/3515265
    void checkException(Throwable throwable) {
        softly.assertThat(throwable).isNotNull();
        softly.assertThat(throwable.getClass()).isEqualTo(NullPointerException.class);
        StackTraceElement topStackTraceElement = throwable.getStackTrace()[0];
        softly.assertThat(topStackTraceElement.getClassName())
            .isEqualTo("ru.yandex.money.common.dbqueue.api.impl.TransactionalProducer");
        softly.assertThat(topStackTraceElement.getLineNumber())
            .isEqualTo(44);
    }

    /**
     * Конфигурируем специальный продюсер, который будет генерировать задачу сразу как только он создается.
     * Ожидаем, что это произойдет еще до инициализации dq-queue.
     * Исключение, если оно выбрасывается, записываем в поле тестового класса для последующей проверки.
     */
    public static class ProducerWrapper extends ActorEnrichingQueueProducerDecorator<Object> {

        ProducerWrapper(
            TaskActorTransformer taskActorTransformer,
            QueueInitializationSynchronizer queueInitializationSynchronizer
        ) {
            super(
                new MeasuredQueueProducer<>(QUEUE_ID, Object.class, queueInitializationSynchronizer),
                taskActorTransformer,
                queueInitializationSynchronizer
            );
        }

        @PostConstruct
        public void init() {
            try {
                earlyEnqueueHappened = true;
                this.enqueue(enqueueParams());
            } catch (Throwable e) {
                thrownByEarlyEnqueue = e;
            }
        }
    }

    @Nonnull
    static EnqueueParams<Object> enqueueParams() {
        return EnqueueParams.create(new Object());
    }

    // Далее - просто конфигурация еще одного типа db-queue задачи без особой логики

    @TestConfiguration
    static class Config extends AbstractQueueDefinition<Object> {

        private final JdbcTemplate jdbcTemplate;
        private final TransactionTemplate transactionTemplate;
        private final ObjectMapper mapper;
        private final TaskActorTransformer taskActorTransformer;
        private final QueueInitializationSynchronizer queueInitializationSynchronizer;

        Config(
            JdbcTemplate jdbcTemplate,
            TransactionTemplate transactionTemplate,
            @Qualifier("commonJsonMapper") ObjectMapper mapper,
            TaskActorTransformer taskActorTransformer,
            QueueInitializationSynchronizer queueInitializationSynchronizer
        ) {
            this.jdbcTemplate = jdbcTemplate;
            this.transactionTemplate = transactionTemplate;
            this.mapper = mapper;
            this.taskActorTransformer = taskActorTransformer;
            this.queueInitializationSynchronizer = queueInitializationSynchronizer;
            setMaxAttempts(1L);
            setMaxFailedTasks(1L);
        }

        @Override
        public QueueId getQueueId() {
            return QUEUE_ID;
        }

        @Override
        public QueueConsumer<Object> queueConsumerBean() {
            return null;
        }

        @Bean("testSampleProducer")
        @Override
        public QueueProducer<Object> queueProducerBean() {
            return new ProducerWrapper(taskActorTransformer, queueInitializationSynchronizer);
        }

        @Bean(value = "testSampleShardrouter")
        @Override
        public QueueShardRouter<Object> queueShardRouterBean() {
            return new SpringSingleShardRouter<>(QUEUE_ID, Object.class, jdbcTemplate, transactionTemplate);
        }

        @Override
        public TaskPayloadTransformer<Object> taskPayloadTransformerBean() {
            return null;
        }

        @Bean("testSampleConsumer")
        public SpringQueueConsumer<Object> testSampleConsumer() {
            return new TestSampleConsumer();
        }

        @Bean("testSamplePayloadTransformer")
        public GenericJsonTransformer testSamplePayloadTransformer() {
            return new TestSampleTransformer(mapper);
        }
    }

    public static class TestSampleConsumer extends SpringQueueConsumer<Object> {

        TestSampleConsumer() {
            super(QUEUE_ID, Object.class);
        }

        @Nonnull
        @Override
        public TaskExecutionResult execute(@Nonnull Task<Object> task) {
            return TaskExecutionResult.finish();
        }
    }

    public static class TestSampleTransformer extends GenericJsonTransformer<Object> {

        TestSampleTransformer(ObjectMapper mapper) {
            super(QUEUE_ID, Object.class, mapper);
        }
    }
}
