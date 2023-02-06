package ru.yandex.market.sdk.userinfo.matcher.dsl;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import org.hamcrest.Matcher;

import ru.yandex.market.sdk.userinfo.matcher.CommonMatcher;

/**
 * @authror dimkarp93
 */
public class MatcherDsl<T> {
    private final Class<T> clazz;

    Set<FieldDsl<? super T, ?>> fields = new HashSet<>();

    public MatcherDsl(Class<T> clazz) {
        this.clazz = clazz;
    }

    public <V> void add(FieldDsl<T, V> matcher) {
        fields.add(matcher);
    }

    public <V> void add(String name, Matcher<V> matcher, Function<T, V> extractor) {
        add(new FieldDsl<>(name, matcher, extractor));
    }

    public void addAll(MatcherDsl<? super T> dsl) {
        fields.addAll(dsl.fields);
    }

    public CommonMatcher<T> toMatcher() {
        return new CommonMatcher<>(clazz, fields);
    }
}
