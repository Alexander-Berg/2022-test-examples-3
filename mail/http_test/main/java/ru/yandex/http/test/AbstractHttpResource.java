package ru.yandex.http.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class AbstractHttpResource implements HttpResource {
    private final Queue<Throwable> exceptions = new ConcurrentLinkedQueue<>();

    @Override
    public List<Throwable> exceptions() {
        return new ArrayList<>(exceptions);
    }

    @Override
    public void exception(final Throwable t) {
        exceptions.add(t);
    }
}

