package ru.yandex.market.wms.common.spring.matchers;

import org.skyscreamer.jsonassert.ValueMatcher;

public class IgnoreValueMatcher implements ValueMatcher<Object> {
    @Override
    public boolean equal(Object o, Object t1) {
        return true;
    }
}
