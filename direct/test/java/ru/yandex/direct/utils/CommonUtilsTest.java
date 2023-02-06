package ru.yandex.direct.utils;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

public class CommonUtilsTest {

    @Test
    public void testIfNotNull() {
        assertThat("null->null", CommonUtils.ifNotNull((List) null, List::size), nullValue());
        assertThat("not null->function result", CommonUtils.ifNotNull(emptyList(), List::size), equalTo(0));
    }

    @Test
    public void listToMapWorksCorrect() {
        Set<Integer> set = new HashSet<>(asList(1, 2, 3));
        Map<Integer, Integer> actual = listToMap(set, x -> x + 1);
        assertThat(actual, allOf(
                hasEntry(2, 1),
                hasEntry(3, 2),
                hasEntry(4, 3)
        ));
    }

    @Test
    public void testFormatApproxDuration() {
        Assert.assertEquals("1:05:01s", CommonUtils.formatApproxDuration(Duration.ofSeconds(3901)));
        Assert.assertEquals("5:01s", CommonUtils.formatApproxDuration(Duration.ofSeconds(301)));
        Assert.assertEquals("1s", CommonUtils.formatApproxDuration(Duration.ofSeconds(1)));
    }

    @Test
    public void testIsValidId() {
        assertTrue("Возвращаем истину на корректном положительном значении", CommonUtils.isValidId(1L));
        assertTrue("Возвращаем истину на корректном положительном значении", CommonUtils.isValidId(Long.MAX_VALUE));
    }

    @Test
    public void testIsValidIdFalseZero() {
        assertTrue("Возвращаем ложь на ноль", !CommonUtils.isValidId(0L));
    }

    @Test
    public void testIsValidIdFalseNull() {
        assertTrue("Возвращаем ложь на null", !CommonUtils.isValidId(null));
    }

    @Test
    public void testIsValidIdFalseNegative() {
        assertTrue("Возвращаем ложь на отрицательном значении", !CommonUtils.isValidId(-1L));
        assertTrue("Возвращаем ложь на отрицательном значении", !CommonUtils.isValidId(Long.MIN_VALUE));
    }

    @Test
    public void numbersCompressed() {
        Map<Integer, Long> numbers = ImmutableMap.<Integer, Long>builder()
                .put(2, 5L)
                .put(3, 4L)
                .put(6, 5L)
                .build();
        Map<Integer, Integer> compressedMap = CommonUtils.compressNumbers(numbers);
        assertThat(compressedMap.keySet(), hasSize(3));
        assertThat(compressedMap, allOf(hasEntry(2, 1), hasEntry(3, 2), hasEntry(6, 1)));
    }
}
