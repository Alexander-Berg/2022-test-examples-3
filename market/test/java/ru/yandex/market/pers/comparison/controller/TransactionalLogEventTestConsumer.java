package ru.yandex.market.pers.comparison.controller;

import java.util.ArrayList;
import java.util.List;

import ru.yandex.market.pers.comparison.logging.TransactionalLogEvent;
import ru.yandex.market.pers.comparison.logging.TransactionalLogEventConsumer;

public class TransactionalLogEventTestConsumer<T> implements TransactionalLogEventConsumer<T> {

    private final Class<T> entityClass;

    private final List<TransactionalLogEvent<T>> events = new ArrayList<>();

    public TransactionalLogEventTestConsumer(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    public Class<T> getEntityClass() {
        return this.entityClass;
    }

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
