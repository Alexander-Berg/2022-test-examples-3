package ru.yandex.market.crm.util;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CrmCollectionsTest {

    @Test
    public void distinctFlat() {
        List<Collection<String>> values = Arrays.asList(
                Arrays.asList("a"),
                Arrays.asList("b", "c"),
                Arrays.asList("a")
        );

        Collection<String> result = CrmCollections.distinctFlat(values);

        Assertions.assertEquals(3,
                result.size(), "Должны получить три элемента т.к. в исходной коллекции два раза повторяется \"a\"");
        Assertions.assertTrue(result.contains("a"));
        Assertions.assertTrue(result.contains("b"));
        Assertions.assertTrue(result.contains("c"));
    }

    @Test
    public void flat() {
        List<Collection<String>> values = Arrays.asList(
                Arrays.asList("a"),
                Arrays.asList("b", "c"),
                Arrays.asList("a")
        );

        List<String> result = CrmCollections.flat(values);

        Assertions.assertEquals(4, result.size());
        Assertions.assertEquals("a", result.get(0));
        Assertions.assertEquals("b", result.get(1));
        Assertions.assertEquals("c", result.get(2));
        Assertions.assertEquals("a", result.get(3));
    }

    @Test
    public void isEmpty_empty() {
        boolean result = CrmCollections.isEmpty(new ArrayList<>());

        Assertions.assertTrue(result);
    }

    @Test
    public void isEmpty_notEmpty() {
        boolean result = CrmCollections.isEmpty(Arrays.asList("a"));

        Assertions.assertFalse(result);
    }

    @Test
    public void isEmpty_null() {
        boolean result = CrmCollections.isEmpty((Collection<?>) null);

        Assertions.assertTrue(result);
    }

    @Test
    public void indexWithResolveConflicts() {
        int key = 0;
        List<Integer> values = Arrays.asList(7, 3, 11, 5);

        Map<Integer, Integer> index = CrmCollections.indexer(values, (v) -> key)
                .resolver(Comparator.comparing(Integer::intValue))
                .index();

        Integer actual = index.get(key);
        Assertions.assertEquals(
                Integer.valueOf(11), actual,
                "должны получить максимальное число т.к. решаем конфлик при помощи простого сравнения");
    }

    @Test
    public void indexWithoutResolveConflicts() {
        int key = 0;
        List<Integer> values = Arrays.asList(7, 3, 11, 5);

        Map<Integer, Integer> index = CrmCollections.index(values, (v) -> key);

        Integer actual = index.get(key);
        Assertions.assertEquals(
                Integer.valueOf(7), actual, "должны получить первое число т.к. не решаем конфликт");
    }

    @Test
    public void indexWithResolveConflictsForDates() {
        int key = 0;
        List<OffsetDateTime> values = Arrays.asList(OffsetDateTime.MIN, OffsetDateTime.now(), OffsetDateTime.MAX);

        Map<Integer, OffsetDateTime> index = CrmCollections.indexer(values, (v) -> key)
                .resolver(Comparator.comparing(Function.identity()))
                .index();

        OffsetDateTime actual = index.get(key);
        Assertions.assertEquals(
                OffsetDateTime.MAX, actual, "должны получить максимальное число т.к. решаем конфлик при помощи " +
                        "простого сравнения");
    }

    @Test
    public void asSetAcceptDuplicateElements() {
        Assertions.assertEquals(1, CrmCollections.asSet("A", "A").size());
    }
}
