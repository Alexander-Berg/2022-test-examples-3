package ru.yandex.market.markup2.core.stubs.persisters;

import ru.yandex.market.markup2.core.MockMarkupTestBase;
import ru.yandex.market.markup2.dao.DefaultPersister;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author york
 * @since 24.05.2018
 */
public class DefaultPersisterStub<KEY, VALUE> extends DefaultPersister<KEY, VALUE> {
    private Integer sequence = 1;

    protected Map<KEY, VALUE> storage = new HashMap<>();

    public DefaultPersisterStub() {
        super(null, null, null);
    }

    @Override
    protected VALUE getValue(KEY key) {
        return storage.get(key);
    }

    @Override
    protected List<VALUE> getValues(KEY[] keys) {
        return getValues(Arrays.asList(keys));
    }

    @Override
    protected List<VALUE> getValues(Collection<KEY> keys) {
        return keys.stream().map(k -> storage.get(k))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    protected List<VALUE> getAllValues() {
        return storage.values().stream().collect(Collectors.toList());
    }

    @Override
    protected List<VALUE> getAllValues(int limit) {
        return storage.values().stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    protected int remove(Collection<KEY> keys) {
        return keys.stream().map(k -> storage.remove(k)).mapToInt(v -> v != null ? 1 : 0).sum();
    }

    protected Collection<KEY> upsertAll(Collection<VALUE> values, Function<VALUE, KEY> keyExtractor) {
        return values.stream()
            .map(v -> upsert(v, keyExtractor))
            .collect(Collectors.toList());
    }

    protected KEY upsert(VALUE value, Function<VALUE, KEY> keyExtractor) {
        KEY key = keyExtractor.apply(value);
        persist(key, value);
        return key;
    }

    protected void persist(KEY key, VALUE value) {
        storage.put(key, value);
    }

    protected <T> List<VALUE> getByValue(T value, Function<VALUE, T> extractor) {
        return storage.values().stream()
            .filter(v -> Objects.equals(value, extractor.apply(v)))
            .collect(Collectors.toList());
    }

    protected <T> List<VALUE> getByValues(Collection<T> values, Function<VALUE, T> extractor) {
        return storage.values().stream()
            .filter(v -> values.contains(extractor.apply(v)))
            .collect(Collectors.toList());
    }


    protected List<VALUE> getByCriteria(Predicate<VALUE> predicate) {
        return storage.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    protected void updateFields(KEY key, VALUE value, Object... fieldNames) {
        VALUE curValue = getValue(key);
        for (int i = 0; i < fieldNames.length; i++) {
            String field = (String) fieldNames[i];
            Object fieldValue = MockMarkupTestBase.getField(value, field);
            MockMarkupTestBase.setField(curValue, field, fieldValue);
        }
    }

    @Override
    public long generateNextLong() {
        return sequence++;
    }

    @Override
    public int generateNextInt() {
        return sequence++;
    }

    @Override
    public List<Long> generateNextLongs(int count) {
        sequence += count;
        return IntStream.range(sequence - count, sequence).mapToLong(i -> i).boxed().collect(Collectors.toList());
    }

    @Override
    public List<Integer> generateNextInts(int count) {
        sequence += count;
        return IntStream.range(sequence - count, sequence).boxed().collect(Collectors.toList());
    }

}
