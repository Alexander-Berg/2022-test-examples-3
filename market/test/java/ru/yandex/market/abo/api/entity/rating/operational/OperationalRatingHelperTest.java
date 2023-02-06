package ru.yandex.market.abo.api.entity.rating.operational;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.abo.api.entity.rating.operational.OperationalRatingHelper.getRange;

/**
 * @author artemmz
 * @date 01/10/2019.
 */
class OperationalRatingHelperTest {
    private static final Map<RatingMetric, List<RatingRange>> RANGE_MAP = Stream.of(
            new RatingRange(RatingMetric.TOTAL, 95.0, 100.0, 5),
            new RatingRange(RatingMetric.TOTAL, 80.0, 95.0, 4),
            new RatingRange(RatingMetric.TOTAL, 60.0, 80.0, 3),
            new RatingRange(RatingMetric.TOTAL, 40.0, 60.0, 2),
            new RatingRange(RatingMetric.TOTAL, 0.0, 40.0, 1),

            new RatingRange(RatingMetric.CANCELLATION_RATE, 0.0, 5.0, 5),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 5.0, 7.0, 4),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 7.0, 10.0, 3),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 10.0, 15.0, 2),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 15.0, 100.0, 1)
    ).collect(Collectors.groupingBy(RatingRange::getMetric));

    @Test
    void testGetRange() {
        assertEquals(new RatingRange(RatingMetric.TOTAL, 95, 100, 5), getRange(RANGE_MAP, RatingMetric.TOTAL, 100));
        assertEquals(new RatingRange(RatingMetric.TOTAL, 80, 95, 4), getRange(RANGE_MAP, RatingMetric.TOTAL, 80));

        assertEquals(new RatingRange(RatingMetric.CANCELLATION_RATE, 7.0, 10.0, 3),
                getRange(RANGE_MAP, RatingMetric.CANCELLATION_RATE, 7));
        assertEquals(new RatingRange(RatingMetric.CANCELLATION_RATE, 5.0, 7.0, 4),
                getRange(RANGE_MAP, RatingMetric.CANCELLATION_RATE, 6));
    }
}