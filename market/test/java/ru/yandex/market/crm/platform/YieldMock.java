package ru.yandex.market.crm.platform;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.protobuf.Message;

import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.reducers.Yield;

/**
 * Простая реализация {@link Yield} для использования в тестах
 */
public class YieldMock implements Yield {

    private Multimap<String, Message> added = Multimaps.newMultimap(new HashMap<>(), LinkedHashSet::new);
    private Multimap<String, Message> removed = Multimaps.newMultimap(new HashMap<>(), LinkedHashSet::new);

    public <T extends Message> Collection<T> getAdded(String config) {
        return (Collection<T>) added.get(config);
    }

    public <T extends Message> Collection<T> getRemoved(String factId) {
        return (Collection<T>) removed.get(factId);
    }

    public boolean isRemoved(String config, Message fact) {
        return removed.containsEntry(config, fact);
    }

    @Override
    public void add(String config, Message fact) {
        added.remove(config, fact);
        added.put(config, fact);

        removed.remove(config, fact);
    }

    @Override
    public void add(FactConfig config, Message fact) {
        add(config.getId(), fact);
    }

    @Override
    public void remove(FactConfig config, Message fact) {
        remove(config.getId(), fact);
    }

    @Override
    public void remove(String config, Message fact) {
        removed.put(config, fact);
        added.remove(config, fact);
    }
}
