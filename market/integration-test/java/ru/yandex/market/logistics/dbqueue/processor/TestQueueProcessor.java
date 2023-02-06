package ru.yandex.market.logistics.dbqueue.processor;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.logistics.dbqueue.DbQueueProcessor;
import ru.yandex.market.logistics.dbqueue.QueueProcessor;

@Service
@DbQueueProcessor("TEST_QUEUE")
public class TestQueueProcessor implements QueueProcessor<TestQueuePayload> {

    @Nonnull
    @Override
    public TaskExecutionResult execute(TestQueuePayload payload) {
        if (StringUtils.isBlank(payload.getData())) {
            throw new IllegalStateException("payload data is blank");
        }

        return payload.getExpectedResult();
    }
}
