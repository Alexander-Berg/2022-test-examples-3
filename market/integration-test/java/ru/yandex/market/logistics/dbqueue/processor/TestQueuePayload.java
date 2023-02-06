package ru.yandex.market.logistics.dbqueue.processor;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import ru.yandex.market.logistics.dbqueue.payload.QueuePayload;

@SuperBuilder
@Jacksonized
@Getter
public class TestQueuePayload extends QueuePayload {

    private final String data;

    private final TaskExecutionResult expectedResult;
}
