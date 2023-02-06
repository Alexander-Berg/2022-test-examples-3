package ru.yandex.market.jmf.configuration.api.test.impl;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;

import ru.yandex.market.jmf.configuration.api.ConfigurationService;
import ru.yandex.market.jmf.configuration.api.Property;

@Service
public class InMemoryConfigurationService implements ConfigurationService {
    private final Map<String, Object> values = new ConcurrentHashMap<>();

    @Override
    public <T> T getValue(@Nonnull String name) {
        return (T) values.get(name);
    }

    @Override
    public <T> T getCached(@Nonnull String name, Duration lifetime) {
        return getValue(name);
    }

    @Override
    public <T> T getValue(@Nonnull Property<T> property) {
        return getValue(property.key());
    }

    @Override
    public <T> T getValue(@Nonnull Property<T> property, T defaultValue) {
        return Optional.ofNullable(getValue(property)).orElse(defaultValue);
    }

    @Override
    public void setValue(@Nonnull String name, Object value) {
        if (value == null) {
            return;
        }
        values.put(name, value);
    }

    @Override
    public void setValues(@Nonnull Map<String, Object> values) {
        values.putAll(Maps.filterValues(values, Objects::nonNull));
    }
}
