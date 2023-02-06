package ru.yandex.market.abo.core.rating.operational;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.abo.api.entity.rating.operational.RatingMetric;
import ru.yandex.market.abo.api.entity.rating.operational.RatingRange;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author artemmz
 * @date 03/09/19.
 */
public class RatingMetricRangesMatrixTest {
    private static final Map<RatingMetric, List<RatingRange>> EXPECTED_RANGE_MAP = List.of(
            new RatingRange(RatingMetric.LATE_SHIP_RATE, 0.0, 10.0, 5),
            new RatingRange(RatingMetric.LATE_SHIP_RATE, 10.0, 15.0, 4),
            new RatingRange(RatingMetric.LATE_SHIP_RATE, 15.0, 20.0, 3),
            new RatingRange(RatingMetric.LATE_SHIP_RATE, 20.0, 25.0, 2),
            new RatingRange(RatingMetric.LATE_SHIP_RATE, 25.0, 100.0, 1),

            new RatingRange(RatingMetric.CANCELLATION_RATE, 0.0, 5.0, 5),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 5.0, 7.0, 4),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 7.0, 10.0, 3),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 10.0, 15.0, 2),
            new RatingRange(RatingMetric.CANCELLATION_RATE, 15.0, 100.0, 1),

            new RatingRange(RatingMetric.RETURN_RATE, 0.0, 1.0, 5),
            new RatingRange(RatingMetric.RETURN_RATE, 1.0, 3.0, 4),
            new RatingRange(RatingMetric.RETURN_RATE, 3.0, 5.0, 3),
            new RatingRange(RatingMetric.RETURN_RATE, 5.0, 10.0, 2),
            new RatingRange(RatingMetric.RETURN_RATE, 10.0, 100.0, 1),

            new RatingRange(RatingMetric.DSBS_LATE_DELIVERY_RATE, 0.0, 10.0, 5),
            new RatingRange(RatingMetric.DSBS_LATE_DELIVERY_RATE, 10.0, 15.0, 4),
            new RatingRange(RatingMetric.DSBS_LATE_DELIVERY_RATE, 15.0, 20.0, 3),
            new RatingRange(RatingMetric.DSBS_LATE_DELIVERY_RATE, 20.0, 25.0, 2),
            new RatingRange(RatingMetric.DSBS_LATE_DELIVERY_RATE, 25.0, 100.0, 1),

            new RatingRange(RatingMetric.DSBS_CANCELLATION_RATE, 0.0, 5.0, 5),
            new RatingRange(RatingMetric.DSBS_CANCELLATION_RATE, 5.0, 7.0, 4),
            new RatingRange(RatingMetric.DSBS_CANCELLATION_RATE, 7.0, 10.0, 3),
            new RatingRange(RatingMetric.DSBS_CANCELLATION_RATE, 10.0, 15.0, 2),
            new RatingRange(RatingMetric.DSBS_CANCELLATION_RATE, 15.0, 100.0, 1),

            new RatingRange(RatingMetric.DSBS_RETURN_RATE, 0.0, 1.0, 5),
            new RatingRange(RatingMetric.DSBS_RETURN_RATE, 1.0, 3.0, 4),
            new RatingRange(RatingMetric.DSBS_RETURN_RATE, 3.0, 5.0, 3),
            new RatingRange(RatingMetric.DSBS_RETURN_RATE, 5.0, 10.0, 2),
            new RatingRange(RatingMetric.DSBS_RETURN_RATE, 10.0, 100.0, 1),

            new RatingRange(RatingMetric.CROSSDOCK_LATE_SHIP_RATE, 0.0, 10.0, 5),
            new RatingRange(RatingMetric.CROSSDOCK_LATE_SHIP_RATE, 10.0, 15.0, 4),
            new RatingRange(RatingMetric.CROSSDOCK_LATE_SHIP_RATE, 15.0, 20.0, 3),
            new RatingRange(RatingMetric.CROSSDOCK_LATE_SHIP_RATE, 20.0, 25.0, 2),
            new RatingRange(RatingMetric.CROSSDOCK_LATE_SHIP_RATE, 25.0, 100.0, 1),

            new RatingRange(RatingMetric.CROSSDOCK_PLAN_FACT_RATE, 0.0, 5.0, 5),
            new RatingRange(RatingMetric.CROSSDOCK_PLAN_FACT_RATE, 5.0, 7.0, 4),
            new RatingRange(RatingMetric.CROSSDOCK_PLAN_FACT_RATE, 7.0, 10.0, 3),
            new RatingRange(RatingMetric.CROSSDOCK_PLAN_FACT_RATE, 10.0, 15.0, 2),
            new RatingRange(RatingMetric.CROSSDOCK_PLAN_FACT_RATE, 15.0, 100.0, 1),

            new RatingRange(RatingMetric.CROSSDOCK_RETURN_RATE, 0.0, 1.0, 5),
            new RatingRange(RatingMetric.CROSSDOCK_RETURN_RATE, 1.0, 3.0, 4),
            new RatingRange(RatingMetric.CROSSDOCK_RETURN_RATE, 3.0, 5.0, 3),
            new RatingRange(RatingMetric.CROSSDOCK_RETURN_RATE, 5.0, 10.0, 2),
            new RatingRange(RatingMetric.CROSSDOCK_RETURN_RATE, 10.0, 100.0, 1),

            new RatingRange(RatingMetric.FF_LATE_SHIP_RATE, 0.0, 10.0, 5),
            new RatingRange(RatingMetric.FF_LATE_SHIP_RATE, 10.0, 15.0, 4),
            new RatingRange(RatingMetric.FF_LATE_SHIP_RATE, 15.0, 20.0, 3),
            new RatingRange(RatingMetric.FF_LATE_SHIP_RATE, 20.0, 25.0, 2),
            new RatingRange(RatingMetric.FF_LATE_SHIP_RATE, 25.0, 100.0, 1),

            new RatingRange(RatingMetric.FF_PLANFACT_RATE, 0.0, 5.0, 5),
            new RatingRange(RatingMetric.FF_PLANFACT_RATE, 5.0, 7.0, 4),
            new RatingRange(RatingMetric.FF_PLANFACT_RATE, 7.0, 10.0, 3),
            new RatingRange(RatingMetric.FF_PLANFACT_RATE, 10.0, 15.0, 2),
            new RatingRange(RatingMetric.FF_PLANFACT_RATE, 15.0, 100.0, 1),

            new RatingRange(RatingMetric.FF_RETURN_RATE, 0.0, 20.0, 5),
            new RatingRange(RatingMetric.FF_RETURN_RATE, 20.0, 30.0, 4),
            new RatingRange(RatingMetric.FF_RETURN_RATE, 30.0, 40.0, 3),
            new RatingRange(RatingMetric.FF_RETURN_RATE, 40.0, 60.0, 2),
            new RatingRange(RatingMetric.FF_RETURN_RATE, 60.0, 100.0, 1),

            new RatingRange(RatingMetric.TOTAL, 95.0, 100.0, 5),
            new RatingRange(RatingMetric.TOTAL, 80.0, 95.0, 4),
            new RatingRange(RatingMetric.TOTAL, 60.0, 80.0, 3),
            new RatingRange(RatingMetric.TOTAL, 40.0, 60.0, 2),
            new RatingRange(RatingMetric.TOTAL, 0.0, 40.0, 1)
    ).stream()
            .sorted(Comparator.comparing(RatingRange::getScore))
            .collect(Collectors.groupingBy(RatingRange::getMetric));

    @Test
    void asMap() {
        assertEquals(EXPECTED_RANGE_MAP, rangesMatrix().asMap());
    }

    public static RatingMetricRangesMatrix rangesMatrix() {
        return new RatingMetricRangesMatrix(List.of(
                initRanges(RatingMetric.CANCELLATION_RATE, 15, 10, 7, 5, 0, true),
                initRanges(RatingMetric.LATE_SHIP_RATE, 25, 20, 15, 10, 0, true),
                initRanges(RatingMetric.RETURN_RATE, 10, 5, 3, 1, 0, true),
                initRanges(RatingMetric.DSBS_CANCELLATION_RATE, 15, 10, 7, 5, 0, true),
                initRanges(RatingMetric.DSBS_LATE_DELIVERY_RATE, 25, 20, 15, 10, 0, true),
                initRanges(RatingMetric.DSBS_RETURN_RATE, 10, 5, 3, 1, 0, true),
                initRanges(RatingMetric.CROSSDOCK_PLAN_FACT_RATE, 15, 10, 7, 5, 0, true),
                initRanges(RatingMetric.CROSSDOCK_LATE_SHIP_RATE, 25, 20, 15, 10, 0, true),
                initRanges(RatingMetric.CROSSDOCK_RETURN_RATE, 10, 5, 3, 1, 0, true),
                initRanges(RatingMetric.FF_PLANFACT_RATE, 15, 10, 7, 5, 0, true),
                initRanges(RatingMetric.FF_LATE_SHIP_RATE, 25, 20, 15, 10, 0, true),
                initRanges(RatingMetric.FF_RETURN_RATE, 60, 40, 30, 20, 0, true),
                initRanges(RatingMetric.TOTAL, 40, 60, 80, 95, 100, false)
        ));
    }

    private static RatingMetricRanges initRanges(RatingMetric metric,
                                                 double star1bound,
                                                 double star2bound,
                                                 double star3bound,
                                                 double star4bound,
                                                 double star5bound,
                                                 boolean inverted) {
        RatingMetricRanges ranges = new RatingMetricRanges();
        ranges.setMetric(metric);
        ranges.setStar1bound(star1bound);
        ranges.setStar2bound(star2bound);
        ranges.setStar3bound(star3bound);
        ranges.setStar4bound(star4bound);
        ranges.setStar5bound(star5bound);
        ranges.setInverted(inverted);
        return ranges;
    }
}
