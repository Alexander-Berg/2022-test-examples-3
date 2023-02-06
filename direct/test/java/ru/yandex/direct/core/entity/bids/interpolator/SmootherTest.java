package ru.yandex.direct.core.entity.bids.interpolator;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.bids.container.interpolator.Cap;
import ru.yandex.direct.core.entity.bids.container.interpolator.CapKey;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.utils.math.Point;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
public class SmootherTest {
    @Test
    public void round() {
        Double round = InterpolatorUtils.round(12.2356, 2);
        assertThat(round).isEqualTo(12.24);
    }

    @Test
    public void diffAndSmooth() {
        List<Point> pointList = asList(Point.fromDoubles(1, 2), Point.fromDoubles(1, 2));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.USD, 10);
        List<Point> smoothedPoints = smoother.diffAndSmooth(pointList);

        assertThat(smoothedPoints).hasSize(4);
        assertThat(smoothedPoints.get(0)).isEqualByComparingTo(Point.fromDoubles(1.5, 0));
    }

    @Test
    public void integrate() {
        List<Point> pointList = asList(Point.fromDoubles(1, 2), Point.fromDoubles(1, 2));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.USD, 10);
        List<Point> smoothedPoints = smoother.integrate(pointList);
        assertThat(smoothedPoints).hasSize(3);
        assertThat(smoothedPoints.get(0)).isEqualByComparingTo(Point.fromDoubles(0, 0));
        assertThat(smoothedPoints.get(2)).isEqualByComparingTo(Point.fromDoubles(1, 4));
    }

    @Test
    public void shift() {
        List<Point> pointList = asList(Point.fromDoubles(0, 0), Point.fromDoubles(1, 1),
                Point.fromDoubles(1, 2));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.USD, 10);
        List<Point> smoothedPoints = smoother.shift(pointList);
        assertThat(smoothedPoints).hasSize(3);
        assertThat(smoothedPoints.get(0)).isEqualByComparingTo(Point.fromDoubles(0, 1));
        assertThat(smoothedPoints.get(1)).isEqualByComparingTo(Point.fromDoubles(1, 2));
        assertThat(smoothedPoints.get(2)).isEqualByComparingTo(Point.fromDoubles(1, 2));
    }

    @Test
    public void removeInsignificantPoints() {
        List<Point> pointList = asList(Point.fromDoubles(0, 0), Point.fromDoubles(1, 0),
                Point.fromDoubles(2, 0));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.USD, 10);
        List<Point> withoutInsignificantPoints = smoother.removeNotBordersPointsOfConstantFunctionIntervals(pointList);
        assertThat(withoutInsignificantPoints).hasSize(2);
        assertThat(withoutInsignificantPoints.get(0)).isEqualByComparingTo(Point.fromDoubles(0, 0));
        assertThat(withoutInsignificantPoints.get(1)).isEqualByComparingTo(Point.fromDoubles(2, 0));
    }

    @Test
    public void execute() {
        List<Point> pointList = asList(Point.fromDoubles(1, 2), Point.fromDoubles(1, 2));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.YND_FIXED, 10);
        List<Point> smoothedPoints = smoother.execute(pointList);
        assertThat(smoothedPoints).hasSize(4);
    }

    @Test
    public void executeNoInterpolationCap() {
        List<Point> pointList = Collections.singletonList(Point.fromDoubles(2, 2));

        CapFactory capFactory = new CapFactory();
        Cap cap = capFactory.getCap(new CapKey(false, 1, "noInterpolation"));

        Smoother smoother = new Smoother(cap, CurrencyCode.YND_FIXED, 0);
        List<Point> smoothedPoints = smoother.execute(pointList);

        assertThat(smoothedPoints).hasSize(2);
        assertThat(smoothedPoints.get(0)).isEqualByComparingTo(Point.fromDoubles(0, 2));
        assertThat(smoothedPoints.get(1)).isEqualByComparingTo(Point.fromDoubles(2.01, 2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_NotSortedPoints() {
        List<Point> pointList = asList(Point.fromDoubles(2, 3), Point.fromDoubles(1, 2));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.YND_FIXED, 10);
        smoother.execute(pointList);
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_DecreasingPoints() {
        List<Point> pointList = asList(Point.fromDoubles(1, 2), Point.fromDoubles(2, 1));
        Cap cap = Cap.fromPoints(
                asList(Point.fromDoubles(0.5, 0.5), Point.fromDoubles(0.5, 0.5)));
        Smoother smoother = new Smoother(cap, CurrencyCode.YND_FIXED, 10);
        smoother.execute(pointList);
    }
}
