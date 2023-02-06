package ru.yandex.direct.utils.math;

import java.util.List;

import org.assertj.core.data.Offset;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.direct.utils.math.MathUtils.interpolateLinear;

public class MathUtilsInterpolateLinearMultiplePointsTest {
    private static final Offset<Double> DOUBLE_PRECISION_OFFSET = Offset.offset(0.000001);

    private static final Point POINT_1 = Point.fromDoubles(10, 5);
    private static final Point POINT_2 = Point.fromDoubles(16, 8);
    private static final Point POINT_3 = Point.fromDoubles(20, 12);
    private static final Point POINT_4 = Point.fromDoubles(23, 18);

    private static final List<Point> TWO_POINTS = asList(POINT_1, POINT_2);
    private static final List<Point> THREE_POINTS = asList(POINT_1, POINT_2, POINT_3);
    private static final List<Point> FOUR_POINTS = asList(POINT_1, POINT_2, POINT_3, POINT_4);

    @Test
    public void OnePoint_TargetPointIsInTheMiddle() {
        Point result = interpolateLinear(18, singletonList(POINT_1));
        assertTrue(result.getY() == POINT_1.getY());
    }

    @Test
    public void TwoPoints_TargetPointIsToTheLeft() {
        Point result = interpolateLinear(8, TWO_POINTS);
        assertThat(result.getY()).isCloseTo(5, DOUBLE_PRECISION_OFFSET);
    }

    @Test
    public void TwoPoints_TargetPointIsBetweenPoints() {
        Point result = interpolateLinear(12, TWO_POINTS);
        assertTrue(result.getY() == 6);
    }

    @Test
    public void TwoPoints_TargetPointIsToTheRight() {
        Point result = interpolateLinear(18, TWO_POINTS);
        assertThat(result.getY()).isCloseTo(8, DOUBLE_PRECISION_OFFSET);
    }

    @Test
    public void ThreePoints_TargetPointIsToTheLeft() {
        Point result = interpolateLinear(8, THREE_POINTS);
        assertThat(result.getY()).isCloseTo(5.0, DOUBLE_PRECISION_OFFSET);
    }

    @Test
    public void ThreePoints_TargetPointIsBetween1And2() {
        Point result = interpolateLinear(12, THREE_POINTS);
        assertTrue(result.getY() == 6);
    }

    @Test
    public void ThreePoints_TargetPointIsBetween2And3() {
        Point result = interpolateLinear(17, THREE_POINTS);
        assertTrue(result.getY() == 9);
    }

    @Test
    public void ThreePoints_TargetPointIsToTheRight() {
        Point result = interpolateLinear(21, THREE_POINTS);
        assertThat(result.getY()).isCloseTo(12, DOUBLE_PRECISION_OFFSET);
    }

    @Test
    public void FourPoints_TargetPointIsInTheMiddle() {
        Point result = interpolateLinear(18, FOUR_POINTS);
        assertTrue(result.getY() == 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ZeroPoints_TargetPointIsInTheMiddle() {
        Point result = interpolateLinear(18, emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void null_TargetPointIsInTheMiddle() {
        Point result = interpolateLinear(18, null);
    }
}
