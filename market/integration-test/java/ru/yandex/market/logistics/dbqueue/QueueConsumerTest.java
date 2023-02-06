package ru.yandex.market.logistics.dbqueue;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;
import ru.yoomoney.tech.dbqueue.config.QueueShardId;

public class QueueConsumerTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Обработка события после лимита")
    void consumeAfterLimit() {
        QueueTypeConsumer queueTypeConsumer = consumers.stream()
            .filter(c -> c.getQueueConfig().getLocation().getQueueId().asString().equals("TEST_QUEUE_LIMIT"))
            .findFirst()
            .orElseThrow();

        softly.assertThat(queueTypeConsumer.execute(task(3)).getActionType())
            .isEqualTo(TaskExecutionResult.Type.FINISH);
    }

    @Test
    @DisplayName("Обработка события до лимита")
    void consumeBeforeLimit() {
        QueueTypeConsumer queueTypeConsumer = consumers.stream()
            .filter(c -> c.getQueueConfig().getLocation().getQueueId().asString().equals("TEST_QUEUE_LIMIT"))
            .findFirst()
            .orElseThrow();

        softly.assertThatCode(() -> queueTypeConsumer.execute(task(1)))
            .hasMessage("payload is absent").isInstanceOf(IllegalArgumentException.class);
    }

    @Nonnull
    private Task<?> task(int attempt) {
        return Task.builder(new QueueShardId("id"))
            .withAttemptsCount(attempt)
            .build();
    }
}
