package ru.yandex.market.mbo.lightmapper.test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author prediger
 */
public class LongGenericMapperRepositoryMock<T> extends GenericMapperRepositoryMock<T, Long> {
    private final AtomicLong atomicLong = new AtomicLong();

    public LongGenericMapperRepositoryMock(BiConsumer<T, Long> idSetter, Function<T, Long> idGetter) {
        super(idSetter, idGetter);
    }

    @Override
    protected Long nextId() {
        return atomicLong.incrementAndGet();
    }
}
