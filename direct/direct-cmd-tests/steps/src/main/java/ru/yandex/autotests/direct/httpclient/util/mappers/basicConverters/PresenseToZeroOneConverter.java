package ru.yandex.autotests.direct.httpclient.util.mappers.basicConverters;

import org.dozer.DozerConverter;

/**
 * Created by shmykov on 14.04.15.
 */
public class PresenseToZeroOneConverter extends DozerConverter<Object, Integer> {

    public PresenseToZeroOneConverter() {
        super(Object.class, Integer.class);
    }

    @Override
    public Integer convertTo(Object source, Integer destination) {
        return (source == null || source.equals("")) ? 0 : 1;
    }

    @Override
    public Object convertFrom(Integer source, Object destination) {
        return source == 0 ? null : new Object();
    }
}