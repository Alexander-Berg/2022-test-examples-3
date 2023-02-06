package ru.yandex.market.deepmind.common.mocks;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import ru.yandex.market.mbo.storage.StorageKeyValueService;

@SuppressWarnings("unchecked")
public class StorageKeyValueServiceMock extends StorageKeyValueService {

    private final Map<String, Object> keyValueMap = Collections.synchronizedMap(new HashMap<>());

    @Override
    public <T> void putValue(@Nonnull String key, T value) {
        checkKey(key);
        keyValueMap.put(key, value);
    }

    @Override
    public void putOffsetDateTime(String key, @Nullable OffsetDateTime value) {
        putValue(key, value);
    }

    @Override
    public OffsetDateTime getOffsetDateTime(String key, @Nullable OffsetDateTime defaultValue) {
        return getValue(key, defaultValue, OffsetDateTime.class);
    }

    @Override
    public <T> T getValue(String key, Class<T> clazz) {
        checkKey(key);
        return (T) keyValueMap.get(key);
    }

    @Nonnull
    @Override
    public <T> List<T> getList(String key, Class<T> elementClass) {
        checkKey(key);
        if (!keyValueMap.containsKey(key)) {
            return Collections.emptyList();
        } else {
            return (List<T>) keyValueMap.get(key);
        }
    }

    @Nonnull
    @Override
    public <T> List<T> getCachedList(String key, Class<T> elementClass) {
        return getList(key, elementClass);
    }

    @Override
    public <T> T getCachedValue(String key, @Nullable T defaultValue, Class<T> clazz) {
        return getValue(key, defaultValue, clazz);
    }

    @Override
    public void invalidateCache() {
        keyValueMap.clear();
    }
}
