package ru.yandex.market.logistics.utilizer.util.matcher;

import org.skyscreamer.jsonassert.ValueMatcher;

public class IgnoreValueMatcher implements ValueMatcher<Object> {
    @Override
    public boolean equal(Object o, Object t1) {
        return true;
    }
}
