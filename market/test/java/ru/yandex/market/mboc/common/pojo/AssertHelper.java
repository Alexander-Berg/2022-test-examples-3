package ru.yandex.market.mboc.common.pojo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;

/**
 * @author masterj
 */
class AssertHelper<T> {
    private final List<String> ignoredForEqualsAndHashCodeFields;
    private final Class<T> clazz;

    AssertHelper(Class<T> clazz, String... ignoredForEqualsAndHashCodeFields) {
        this.clazz = clazz;
        this.ignoredForEqualsAndHashCodeFields = Arrays.asList(ignoredForEqualsAndHashCodeFields);

        Assertions.assertThat(getPojoFields())
            .containsAll(this.ignoredForEqualsAndHashCodeFields);
    }

    boolean areSameByField(T instance0, T instance1, String fieldToCompareBy) {
        try {
            Field field = clazz.getDeclaredField(fieldToCompareBy);
            field.setAccessible(true);

            Object value0 = field.get(instance0);
            Object value1 = field.get(instance1);
            return Objects.equals(value0, value1);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    List<FieldExclusion> excludeFieldsFromPojoByOne() {
        List<String> fields = getNotIgnoredPojoFields();
        return fields.stream()
            .map(includedField -> {
                HashSet<String> allFieldsButIncludedAndIgnored = new HashSet<>(fields);
                allFieldsButIncludedAndIgnored.remove(includedField);
                allFieldsButIncludedAndIgnored.addAll(ignoredForEqualsAndHashCodeFields);
                String[] excludedFields = allFieldsButIncludedAndIgnored.toArray(new String[0]);
                return new FieldExclusion(includedField, excludedFields);
            })
            .collect(Collectors.toList());
    }

    private List<String> getNotIgnoredPojoFields() {
        List<String> result = getPojoFields();
        result.removeAll(ignoredForEqualsAndHashCodeFields);
        return result;
    }

    private List<String> getPojoFields() {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(x -> !Modifier.isStatic(x.getModifiers()))
            .filter(x -> !Modifier.isFinal(x.getModifiers()))
            .map(Field::getName)
            .collect(Collectors.toList());
    }

    static class FieldExclusion {
        private final String includedField;
        private final String[] excludedFields;

        private FieldExclusion(String includedField, String[] excludedFields) {
            this.includedField = includedField;
            this.excludedFields = excludedFields;
        }

        String getIncludedField() {
            return includedField;
        }

        String[] getExcludedFields() {
            return excludedFields;
        }
    }
}
