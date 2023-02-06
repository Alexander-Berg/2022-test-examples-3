package ru.yandex.market.crm.util;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RangesTest {

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"[2..3]", Range.closed(2, 3)},
                {"[2..3)", Range.closedOpen(2, 3)},
                {"(2..3]", Range.openClosed(2, 3)},
                {"(2..3)", Range.open(2, 3)},
                {"5", Range.closed(5, 5)},
                {"(-∞..+∞)", Range.all()},
                {"(..)", Range.all()},
                {"(2..)", Range.greaterThan(2)},
                {"(..3)", Range.lessThan(3)},
                {"[2..)", Range.atLeast(2)},
                {"(..3]", Range.atMost(3)},
                {"(2..+∞)", Range.greaterThan(2)},
                {"(-∞..3)", Range.lessThan(3)},
                {"[2..+∞)", Range.atLeast(2)},
                {"(-∞..3]", Range.atMost(3)},
        });
    }

    @ParameterizedTest(name = "{index}: valueOf(\"{0}\") = {1}")
    @MethodSource("data")
    public void valueOf(String value, Range<Integer> range) {
        Range<Integer> actual = Ranges.valueOf(value, Integer::valueOf);
        Assertions.assertEquals(range, actual);
    }
}
