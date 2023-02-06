package ru.yandex.market.delivery.transport_manager.util;

import lombok.experimental.UtilityClass;

import ru.yandex.market.delivery.transport_manager.queue.base.QueueTaskDto;
import ru.yandex.market.delivery.transport_manager.queue.base.TaskExecutionResult;
import ru.yandex.market.delivery.transport_manager.queue.base.consumer.BaseQueueConsumer;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.config.QueueShardId;

import static org.assertj.core.api.Assertions.assertThat;

@UtilityClass
@SuppressWarnings("HideUtilityClassConstructor")
public class DbQueueUtils {

    // ya.make fails if static is not explicitly specified. I don't know why
    public static <T> Task<T> createTask(T payload) {
        return Task.<T>builder(new QueueShardId("1"))
            .withPayload(payload)
            .build();
    }

    public <T extends QueueTaskDto> void assertExecutedSuccessfully(
        BaseQueueConsumer<T> consumer,
        T payload
    ) {
        assertThat(
            consumer.executeTask(createTask(payload))
        ).isEqualTo(
            TaskExecutionResult.finish()
        );
    }

    public <T extends QueueTaskDto> void assertExecutedWithFailure(
        BaseQueueConsumer<T> consumer,
        T payload
    ) {
        try {
            assertThat(
                consumer.executeTask(createTask(payload)).getType()
            ).isEqualTo(
                TaskExecutionResult.Type.FAIL
            );
        } catch (Throwable ex) {
            // Exception is OK
        }
    }
}
