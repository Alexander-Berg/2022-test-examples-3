package ru.yandex.market.sdk.userinfo.matcher.dsl;

import java.util.Objects;
import java.util.function.Function;

import org.hamcrest.Matcher;

/**
 * @authror dimkarp93
 */
public class FieldDsl<T, V> {
    private final String name;
    private final Matcher<V> matcher;
    private final Function<T, V> extractor;

    public FieldDsl(String name, Matcher<V> matcher, Function<T, V> extractor) {
        this.name = name;
        this.matcher = matcher;
        this.extractor = extractor;
    }

    public String getName() {
        return name;
    }

    public Matcher<V> getMatcher() {
        return matcher;
    }

    public Function<T, V> getExtractor() {
        return extractor;
    }

    public V extract(T item) {
        return extractor.apply(item);
    }

    public boolean matches(T item) {
        return matcher.matches(extract(item));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldDsl<?, ?> that = (FieldDsl<?, ?>) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
