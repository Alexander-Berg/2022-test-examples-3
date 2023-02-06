package ru.yandex.market.queuedcalls.model;

import javax.annotation.Nonnull;

import ru.yandex.market.queuedcalls.QueuedCallObjectType;
import ru.yandex.market.queuedcalls.QueuedCallType;

import static ru.yandex.market.queuedcalls.model.TestQCObjectType.OTHER_TYPE;
import static ru.yandex.market.queuedcalls.model.TestQCObjectType.SOME_TYPE;

public enum TestQCType implements QueuedCallType {
    FIRST(SOME_TYPE), SECOND(OTHER_TYPE), THIRD(SOME_TYPE);

    private final QueuedCallObjectType objectType;

    TestQCType(QueuedCallObjectType objectType) {
        this.objectType = objectType;
    }

    @Nonnull
    @Override
    public QueuedCallObjectType getObjectIdType() {
        return objectType;
    }

    @Override
    public int getId() {
        return ordinal();
    }
}
