package ru.yandex.market.logistic.api.utils;

import com.fasterxml.jackson.databind.JavaType;


public abstract class ParsingXmlWrapperTest<T, R> extends ParsingXmlTest<T> {
    private final Class<R> innerType;

    public ParsingXmlWrapperTest(Class<T> type, Class<R> innerType, String fileName) {
        super(type, fileName);
        this.innerType = innerType;
    }

    @Override
    protected JavaType getType() {
        return mapper.getTypeFactory().constructParametricType(type, innerType);
    }
}
