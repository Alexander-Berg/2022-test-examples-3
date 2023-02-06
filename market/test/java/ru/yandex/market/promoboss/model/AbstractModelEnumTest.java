package ru.yandex.market.promoboss.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractModelEnumTest<API, ENUM extends ModelEnum<?, API>> {
    private final Class<ENUM> enumClass;
    private final Class<API> apiClass;

    protected AbstractModelEnumTest(Class<API> apiClass, Class<ENUM> enumClass) {
        this.apiClass = apiClass;
        this.enumClass = enumClass;
    }

    private Collection<ENUM> getEnumValuesCollection() {
        return Arrays.stream(enumClass.getEnumConstants()).toList();
    }

    private void uniqueMappedCollection(Function<ENUM, ?> mapper) {
        var list = getEnumValuesCollection().stream().map(mapper).toList();
        assertEquals(list.size(), list.stream().distinct().toList().size());
    }

    @Test
    void uniqueApiValues() {
        uniqueMappedCollection(ModelEnum::getApiValue);
    }

    @Test
    void uniqueDatabaseValues() {
        uniqueMappedCollection(ModelEnum::getDatabaseValue);
    }

    @Test
    void usedAllApiValues() {
        assertTrue(
                getEnumValuesCollection()
                        .stream()
                        .map(ModelEnum::getApiValue)
                        .collect(Collectors.toSet())
                        .containsAll(Arrays.stream(apiClass.getEnumConstants()).toList())
        );
    }
}
