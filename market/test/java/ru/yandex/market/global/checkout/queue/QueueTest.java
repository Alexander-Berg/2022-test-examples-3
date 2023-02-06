package ru.yandex.market.global.checkout.queue;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nonnull;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;
import ru.yoomoney.tech.dbqueue.api.EnqueueResult;
import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer;
import ru.yoomoney.tech.dbqueue.config.DatabaseAccessLayer;
import ru.yoomoney.tech.dbqueue.config.QueueService;
import ru.yoomoney.tech.dbqueue.config.QueueShard;
import ru.yoomoney.tech.dbqueue.settings.ExtSettings;
import ru.yoomoney.tech.dbqueue.settings.FailRetryType;
import ru.yoomoney.tech.dbqueue.settings.FailureSettings;
import ru.yoomoney.tech.dbqueue.settings.PollSettings;
import ru.yoomoney.tech.dbqueue.settings.ProcessingMode;
import ru.yoomoney.tech.dbqueue.settings.ProcessingSettings;
import ru.yoomoney.tech.dbqueue.settings.QueueConfig;
import ru.yoomoney.tech.dbqueue.settings.QueueId;
import ru.yoomoney.tech.dbqueue.settings.QueueLocation;
import ru.yoomoney.tech.dbqueue.settings.QueueSettings;
import ru.yoomoney.tech.dbqueue.settings.ReenqueueRetryType;
import ru.yoomoney.tech.dbqueue.settings.ReenqueueSettings;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.queue.base.AbstractOrderIdConsumer;
import ru.yandex.market.global.checkout.domain.queue.base.AbstractOrderIdProducer;
import ru.yandex.market.global.checkout.domain.queue.base.LongPayloadTransformer;
import ru.yandex.market.global.checkout.domain.queue.base.QueueDefinition;

import static ru.yandex.market.global.checkout.domain.queue.base.QueueTaskUtil.QUEUE_TABLE_NAME;

@Slf4j
public class QueueTest extends BaseFunctionalTest {

    private static final QueueId TEST_QUEUE_ID = new QueueId("test");
    private static final QueueConfig TEST_QUEUE_CONFIG = new QueueConfig(
            QueueLocation.builder()
                    .withTableName(QUEUE_TABLE_NAME)
                    .withQueueId(TEST_QUEUE_ID)
                    .build(),
            QueueSettings.builder()
                    .withPollSettings(PollSettings.builder()
                            .withBetweenTaskTimeout(Duration.ofMillis(1))
                            .withNoTaskTimeout(Duration.ofMillis(1))
                            .withFatalCrashTimeout(Duration.ofMillis(1))
                            .build()
                    )
                    .withFailureSettings(FailureSettings.builder()
                            .withRetryType(FailRetryType.GEOMETRIC_BACKOFF)
                            .withRetryInterval(Duration.ofMinutes(1)).build()
                    )
                    .withReenqueueSettings(ReenqueueSettings.builder()
                            .withRetryType(ReenqueueRetryType.MANUAL)
                            .build()
                    )
                    .withExtSettings(ExtSettings.builder()
                            .withSettings(Map.of())
                            .build()
                    )
                    .withProcessingSettings(ProcessingSettings.builder()
                            .withProcessingMode(ProcessingMode.SEPARATE_TRANSACTIONS)
                            .withThreadCount(1)
                            .build()
                    )
                    .build()
    );

    private static final QueueDefinition TEST_QUEUE_DEFINITION = new QueueDefinition() {
        @Override
        public QueueId getQueueId() {
            return TEST_QUEUE_ID;
        }

        @Override
        public QueueConfig getQueueConfig() {
            return TEST_QUEUE_CONFIG;
        }

        @Override
        public TaskPayloadTransformer<?> getTransformer() {
            return LongPayloadTransformer.getInstance();
        }
    };

    @Autowired
    public QueueShard<DatabaseAccessLayer> shard;

    @Autowired
    public QueueService queueService;

    @SneakyThrows
    @Test
    public void testQueueProcessTask() {
        AtomicLong counter = new AtomicLong(0);

        QueueProducer<Long> producer = new AbstractOrderIdProducer(TEST_QUEUE_DEFINITION, shard) {
            @Override
            public EnqueueResult enqueue(@Nonnull EnqueueParams<Long> enqueueParams) {
                return super.enqueue(enqueueParams);
            }
        };

        QueueConsumer<Long> consumer = new AbstractOrderIdConsumer(TEST_QUEUE_DEFINITION) {
            @Nonnull
            @Override
            public TaskExecutionResult execute(@Nonnull Task<Long> task) {
                counter.addAndGet(task.getPayloadOrThrow());
                return TaskExecutionResult.finish();
            }
        };

        queueService.registerQueue(consumer);
        queueService.start(TEST_QUEUE_ID);

        producer.enqueue(EnqueueParams.create(10L));
        Thread.sleep(1000);

        Assertions.assertThat(counter.get()).isEqualTo(10L);
    }
}
