package ru.yandex.market.logistic.api.utils;

import com.fasterxml.jackson.databind.JavaType;

public abstract class ParsingWrapperTest<T, R> extends ParsingTest<T> {
    private final Class<R> innerType;

    public ParsingWrapperTest(Class<T> type, Class<R> innerType, String fileName) {
        super(type, fileName);
        this.innerType = innerType;
    }

    @Override
    protected JavaType getType() {
        return mapper.getTypeFactory().constructParametricType(type, innerType);
    }
}
