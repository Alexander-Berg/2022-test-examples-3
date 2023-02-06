package ru.yandex.market.mboc.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.github.benas.randombeans.api.EnhancedRandom;

public class ObjectsEqualByFieldsUtil<T> {
    private final T cleanObject;
    private final Function<T, T> copySupplier;
    private final BiFunction<T, T, Boolean> equalityFunction;
    private final Set<String> ignoredFields;
    private final Map<String, Supplier<?>> specialCases;


    private Set<String> equalFields = Collections.emptySet();
    private Set<String> nonEqualFields = Collections.emptySet();

    public ObjectsEqualByFieldsUtil(
            T cleanObject,
            Function<T, T> copySupplier,
            BiFunction<T, T, Boolean> equalityFunction,
            Set<String> ignoredFields,
            Map<String, Supplier<?>> specialCases
    ) {
        this.cleanObject = cleanObject;
        this.copySupplier = copySupplier;
        this.equalityFunction = equalityFunction;
        this.ignoredFields = ignoredFields;
        this.specialCases = specialCases;
    }

    public void findEquality(EnhancedRandom random) {
        var allFieldsExceptIgnored = Arrays.stream(cleanObject.getClass().getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .map(Field::getName)
                .filter(f -> !ignoredFields.contains(f))
                .collect(Collectors.toSet());

        equalFields = new HashSet<>();
        nonEqualFields = new HashSet<>();
        for (String field : allFieldsExceptIgnored) {
            try {
                checkField(random, field);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkField(EnhancedRandom random, String field) throws NoSuchFieldException, IllegalAccessException {
        for (int attempt = 0; attempt < 5; attempt++) {
            // In case of bad random
            var fieldObj = cleanObject.getClass().getDeclaredField(field);
            fieldObj.setAccessible(true);
            var newValue = specialCases.containsKey(field)
                    ? specialCases.get(field).get()
                    : random.nextObject(fieldObj.getType());
            var objectWithDifferentField = copySupplier.apply(cleanObject);
            fieldObj.set(objectWithDifferentField, newValue);

            if (!equalityFunction.apply(cleanObject, objectWithDifferentField)) {
                nonEqualFields.add(field);
                return;
            }
        }
        equalFields.add(field);
    }

    public Set<String> getEqualFields() {
        return equalFields;
    }

    public Set<String> getNonEqualFields() {
        return nonEqualFields;
    }
}
