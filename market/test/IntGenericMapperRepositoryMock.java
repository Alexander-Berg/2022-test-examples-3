package ru.yandex.market.mbo.lightmapper.test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author s-ermakov
 */
public class IntGenericMapperRepositoryMock<T> extends GenericMapperRepositoryMock<T, Integer> {
    private final AtomicInteger atomicInteger = new AtomicInteger();

    public IntGenericMapperRepositoryMock(BiConsumer<T, Integer> idSetter, Function<T, Integer> idGetter) {
        super(idSetter, idGetter);
    }

    @Override
    protected Integer nextId() {
        return atomicInteger.incrementAndGet();
    }
}
