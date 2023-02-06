package ru.yandex.market.checkout.checkouter.tasks.eventinspector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author sergeykoles
 * Created on: 27.04.18
 */
public class IdRangesTest {

    private static final List<Long> MAIN_SET = Collections.unmodifiableList(
            Stream.of(1, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 17)
                    .map(i -> (long) i)
                    .collect(Collectors.toList())
    );
    private static final String MAIN_STRING = "1\n" +
            "3-10\n" +
            "12-15\n" +
            "17";

    @Test
    public void testRangeParsing() {
        IdRanges idRanges = IdRanges.parse(MAIN_STRING);

        List<Long> actual = idRanges.longStream()
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        assertThat(actual, Matchers.equalTo(MAIN_SET));
    }

    @Test
    public void testRangeRebuild() {
        IdRanges ranges = new IdRanges();
        ArrayList<Long> src = new ArrayList<>(MAIN_SET);
        src.add(6L);
        Collections.shuffle(src);
        ranges.addAll(src);

        assertThat(ranges.toString(), Matchers.equalTo(MAIN_STRING));
    }

    @Test
    public void testSize() {
        IdRanges idRanges = IdRanges.parse(MAIN_STRING);
        assertThat(idRanges.longSize(), Matchers.equalTo((long) MAIN_SET.size()));
        assertEquals(0L, new IdRanges().longSize());
    }
}
