package ru.yandex.chemodan.app.lentaloader.test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.misc.test.Assert;

/**
 * @author dbrylev
 */
public abstract class FieldUpdate<T> {

    public static <T> FieldUpdate<T> changedTo(Option<T> value) {
        return new Changed<>(value);
    }

    public static <T> FieldUpdate<T> throttled(Option<T> value) {
        return new Throttled<>(value);
    }

    public static <T> FieldUpdate<T> ignored() {
        return new Ignored<>();
    }

    public void assertChangedTo(T value) {
        assertChangedTo(Option.of(value));
    }

    public void assertChangedToNone() {
        assertChangedTo(Option.empty());
    }

    public void assertChangedTo(Option<T> value) {
        Assert.equals(Changed.class, getClass());
        Assert.equals(value, ((Changed) this).value);
    }

    public void assertThrottledTo(T value) {
        assertThrottledTo(Option.of(value));
    }

    public void assertThrottledToNone() {
        assertThrottledTo(Option.empty());
    }

    public void assertThrottledTo(Option<T> value) {
        Assert.equals(Throttled.class, getClass());
        Assert.equals(value, ((Throttled) this).value);
    }

    public void assertIgnored() {
        Assert.equals(Ignored.class, getClass());
    }

    public static class Throttled<T> extends FieldUpdate<T> {
        public final Option<T> value;

        public Throttled(Option<T> value) {
            this.value = value;
        }
    }

    public static class Ignored<T> extends FieldUpdate<T> {
    }

    public static class Changed<T> extends FieldUpdate<T> {
        public final Option<T> value;

        public Changed(Option<T> value) {
            this.value = value;
        }
    }
}
