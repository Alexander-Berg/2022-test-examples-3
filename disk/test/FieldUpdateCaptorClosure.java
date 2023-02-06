package ru.yandex.chemodan.app.lentaloader.test;

import ru.yandex.bolts.collection.Option;

/**
 * @author dbrylev
 */
public class FieldUpdateCaptorClosure<P, V> {
    private final FieldUpdateCaptor<P, V> captor;
    private final Option<V> previous;

    public FieldUpdateCaptorClosure(FieldUpdateCaptor<P, V> captor, Option<V> previous) {
        this.captor = captor;
        this.previous = previous;
    }

    public FieldUpdate<V> with(P parameter) {
        return captor.update(previous, parameter);
    }

    public FieldUpdate<V> with(Option<P> parameter) {
        return captor.update(previous, parameter);
    }
}
