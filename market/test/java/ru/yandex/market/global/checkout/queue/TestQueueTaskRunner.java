package ru.yandex.market.global.checkout.queue;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;
import ru.yoomoney.tech.dbqueue.config.QueueShardId;

@UtilityClass
public class TestQueueTaskRunner {

    @SneakyThrows
    public <PayloadT> void runTaskThrowOnFail(QueueConsumer<PayloadT> consumer, PayloadT payload) {
        TaskExecutionResult result = consumer.execute(Task.<PayloadT>builder(new QueueShardId(""))
                .withPayload(payload)
                .build());

        switch (result.getActionType()) {
            case REENQUEUE:
                Thread.sleep(result.getExecutionDelayOrThrow().toMillis());
                runTaskThrowOnFail(consumer, payload);
                break;

            case FAIL:
                throw new RuntimeException("Task execution failed!");

            case FINISH:
                break;
        }
    }

    @SneakyThrows
    public <PayloadT> void runTaskOnceThrowOnFail(QueueConsumer<PayloadT> consumer, PayloadT payload) {
        TaskExecutionResult result = consumer.execute(Task.<PayloadT>builder(new QueueShardId(""))
                .withPayload(payload)
                .build());

        switch (result.getActionType()) {
            case FAIL:
                throw new RuntimeException("Task execution failed!");

            case REENQUEUE:
            case FINISH:
                break;
        }
    }

    @SneakyThrows
    public <PayloadT> TaskExecutionResult runTaskAndReturnResult(QueueConsumer<PayloadT> consumer, PayloadT payload) {
        return consumer.execute(Task.<PayloadT>builder(new QueueShardId(""))
                .withPayload(payload)
                .build());
    }

}
