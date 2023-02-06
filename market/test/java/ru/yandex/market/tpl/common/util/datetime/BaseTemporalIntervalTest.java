package ru.yandex.market.tpl.common.util.datetime;

import java.time.Instant;
import java.util.Optional;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class BaseTemporalIntervalTest {

    @Test
    void intersect() {
        Interval interval1 = Interval.valueOf("2011-12-03T10:15:30Z/2011-12-03T13:15:30Z");
        Interval interval2 = Interval.valueOf("2011-12-03T12:15:30Z/2011-12-03T17:15:30Z");
        Interval interval3 = Interval.valueOf("2011-12-03T14:15:30Z/2011-12-03T17:15:30Z");
        Optional<Range<Instant>> range1O = interval1.intersect(interval2);
        Optional<Range<Instant>> range2O = interval1.intersect(interval3);
        assertThat(range1O).isNotEmpty();
        assertThat(range1O).contains(Range.closedOpen(
                Instant.parse("2011-12-03T12:15:30Z"),
                Instant.parse("2011-12-03T13:15:30Z")
        ));
        assertThat(range2O).isEmpty();
    }
}
