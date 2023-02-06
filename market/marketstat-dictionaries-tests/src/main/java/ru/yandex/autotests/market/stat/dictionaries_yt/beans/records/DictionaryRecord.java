package ru.yandex.autotests.market.stat.dictionaries_yt.beans.records;

import com.google.common.base.Preconditions;
import ru.yandex.autotests.market.common.differ.WithId;
import ru.yandex.autotests.market.stat.dictionaries_yt.beans.DictionaryIdField;
import ru.yandex.autotests.market.stat.mappers.RowMapperUtils;
import ru.yandex.autotests.market.stat.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by kateleb on 06.06.17.
 */
public interface DictionaryRecord extends WithId {

    default Map<String, String> getQueryCondition() {
        Map<String, String> result = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            DictionaryIdField dfAnnotation = field.getAnnotation(DictionaryIdField.class);
            if (dfAnnotation != null) {
                if (dfAnnotation.isForQuery()) {
                    Optional<Object> o = Optional.ofNullable(ReflectionUtils.getField(field, this));
                    result.put(RowMapperUtils.getFieldName(field), o.map(Object::toString).orElse(null));
                }
            }
        }
        Preconditions.checkState(!result.isEmpty(), "Query condition is empty");
        return result;
    }

    default String toCondition() {
        return getQueryCondition().entrySet().stream()
            .filter(e -> e.getValue() != null)
            .map(e -> e.getKey() + " = '" + e.getValue() + "'")
            .collect(Collectors.joining(" AND ", "(", ")"));
    }

    default String id() {
        Map<String, String> result = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            DictionaryIdField dfAnnotation = field.getAnnotation(DictionaryIdField.class);
            if (dfAnnotation != null) {
                if (dfAnnotation.isIdPart()) {
                    Optional<Object> o = Optional.ofNullable(ReflectionUtils.getField(field, this));
                    result.put(field.getName(), o.map(Object::toString).orElse(""));
                }
            }
        }
        if (result.isEmpty()) {
            throw new RuntimeException("Can't calculate id");
        }
        return result.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
            .map(Map.Entry::getValue).collect(Collectors.joining("|"));
    }
}
