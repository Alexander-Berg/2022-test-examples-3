package ru.yandex.direct.utils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SetsDiffTest {
    @Parameterized.Parameter
    public Set<Long> leftElements;

    @Parameterized.Parameter(1)
    public Set<Long> rightElements;

    @Parameterized.Parameter(2)
    public String diff;

    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                { null, null, "All 0 elements in sets are the same" },
                { null, Set.of(234L, 123L, 657L),
                        "left missed elements not found, right missed elements: [123, 234, 657], " +
                                "equals elements count: 0"},
                { Set.of(234L, 123L, 657L), null,
                        "left missed elements: [123, 234, 657], right missed elements not found," +
                                " equals elements count: 0"},
                { Set.of(), Set.of(234L, 123L, 657L),
                        "left missed elements not found, right missed elements: [123, 234, 657], " +
                                "equals elements count: 0"},
                { Set.of(234L, 123L, 657L), Set.of(),
                        "left missed elements: [123, 234, 657], right missed elements not found, " +
                                "equals elements count: 0"},
                { Set.of(234L, 123L, 657L), Set.of(123L, 234L, 657L),
                        "All 3 elements in sets are the same"},
                { Set.of(234L, 123L, 657L), Set.of(123L, 657L, 432L),
                        "left missed elements: [234], right missed elements: [432], equals elements count: 2"},
                { Set.of(1000L, 234L, 123L, 657L), Set.of(123L, 657L, 432L, 300L),
                        "left missed elements: [234, 1000], right missed elements: [300, 432], " +
                                "equals elements count: 2"},
                { Set.of(1000L, 2000L, 500L, 250L, 125L), Set.of(333L, 666L, 222L, 9999L, 7777L),
                        "left missed elements: [125, 250, 500, 1000, 2000], " +
                                "right missed elements: [222, 333, 666, 7777, 9999], equals elements count: 0"},
        });
    }

    @Test
    public void test() {
        SetsDiff<Long> setsDiff =
                SetsDiff.createDiff(leftElements, rightElements, Comparator.naturalOrder());
        assertEquals(diff, setsDiff.toString());
    }
}
