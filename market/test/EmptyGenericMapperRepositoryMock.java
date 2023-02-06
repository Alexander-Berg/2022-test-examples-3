package ru.yandex.market.mbo.lightmapper.test;

import java.util.function.Function;

/**
 * @author s-ermakov
 */
public class EmptyGenericMapperRepositoryMock<Item, ItemKey> extends GenericMapperRepositoryMock<Item, ItemKey> {

    public EmptyGenericMapperRepositoryMock(Function<Item, ItemKey> idGetter) {
        super(null, idGetter);
    }

    @Override
    protected ItemKey nextId() {
        throw new IllegalStateException("Don't expected nextId() to be called");
    }
}
