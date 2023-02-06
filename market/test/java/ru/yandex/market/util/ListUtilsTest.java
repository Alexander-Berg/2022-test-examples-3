package ru.yandex.market.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author artemmz
 * created on 15.03.17.
 */
public class ListUtilsTest {

    @Test
    public void cast() throws Exception {
        List strings = Arrays.asList("why", "so", "serious", null, "?");
        List<String> castList = ListUtils.cast(strings, String.class);
        assertEquals(strings, castList);
    }

    @Test(expected = ClassCastException.class)
    public void castExc() throws Exception {
        List longs = new ArrayList();
        longs.add(1L);
        longs.add(1L);
        longs.add(1);
        longs.add(null);
        ListUtils.cast(longs, Long.class);
    }

    @Test
    public void testListMapping() {
        List<Integer> source = Arrays.asList(1, 2, null, 5);
        List<String> expected = Arrays.asList("1", "2", "null", "5");

        final List<String> result = ListUtils.toList(source, String::valueOf);
        Assert.assertEquals(expected.size(), result.size());
        for (int i = 0;
             i < result.size();
             i++) {
            assertEquals(expected.get(i), result.get(i));
        }
    }

    @Test
    public void testListToSetMapping() {
        List<Integer> source = Arrays.asList(1, 2, null, 5);
        Set<String> expected = new HashSet<>(Arrays.asList("1", "2", "null", "5"));

        final Set<String> result = ListUtils.toSet(source, String::valueOf);
        assertEquals(expected.size(), result.size());
        assertEquals(expected.size(), result.stream().filter(expected::contains).count());
        assertEquals(expected.size(), expected.stream().filter(result::contains).count());
    }

    @Test
    public void testListToMapMapping() {
        List<String> source = Arrays.asList("1", "2", "5");
        Map<Integer, String> expected = new HashMap<>();
        expected.put(1, "1");
        expected.put(2, "2");
        expected.put(5, "5");

        final Map<Integer, String> result = ListUtils.toMap(source, Integer::parseInt);
        assertEquals(expected.size(), result.size());
        assertEquals(expected.size(), result.keySet().stream().filter(expected::containsKey).count());
        assertEquals(expected.size(), expected.keySet().stream().filter(result::containsKey).count());
    }
}
