package ru.yandex.market.jmf.trigger.test;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityAdapterService;
import ru.yandex.market.jmf.entity.EntityInstanceStrategy;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.metaclass.Metaclass;

public class TransientEntityInstanceStrategy implements EntityInstanceStrategy {
    private final EntityAdapterService entityAdapterService;

    public TransientEntityInstanceStrategy(EntityAdapterService entityAdapterService) {
        this.entityAdapterService = entityAdapterService;
    }

    @Override
    public boolean isPossible(Metaclass metaclass) {
        return metaclass.getFqn().equals(Fqn.of("transientEntity"));
    }

    @Override
    public Entity newInstance(Fqn fqn) {
        return entityAdapterService.wrap(new TransientEntityImpl());
    }
}
