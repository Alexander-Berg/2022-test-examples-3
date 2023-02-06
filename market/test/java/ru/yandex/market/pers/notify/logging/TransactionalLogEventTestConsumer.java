package ru.yandex.market.pers.notify.logging;

import java.util.ArrayList;
import java.util.List;

public class TransactionalLogEventTestConsumer<T> implements TransactionalLogEventConsumer<T> {

    private final Class<T> entityClass;

    private final List<TransactionalLogEvent<T>> events = new ArrayList<>();

    public TransactionalLogEventTestConsumer(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    @Override
    public Class<T> getEntityClass() {
        return this.entityClass;
    }

    @Override
    public void accept(TransactionalLogEvent<T> event) {
        events.add(event);
    }

    public List<TransactionalLogEvent<T>> getEvents() {
        return events;
    }

    public void clear() {
        this.events.clear();
    }
}
