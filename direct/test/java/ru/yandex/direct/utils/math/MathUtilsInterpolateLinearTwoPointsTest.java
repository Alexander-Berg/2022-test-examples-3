package ru.yandex.direct.utils.math;

import org.assertj.core.data.Offset;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.utils.math.MathUtils.interpolateLinear;

public class MathUtilsInterpolateLinearTwoPointsTest {
    private static final Offset<Double> DOUBLE_PRECISION_OFFSET = Offset.offset(0.000001);

    private static final Point POINT_LEFT = Point.fromDoubles(10, 5);
    private static final Point POINT_RIGHT = Point.fromDoubles(16, 8);

    @Test
    public void interpolateLinearTwoPoints_PointInside() {
        Point result = interpolateLinear(12, POINT_LEFT, POINT_RIGHT);
        assertEquals(6, result.getY(), 0.0);
    }

    @Test
    public void interpolateLinearTwoPoints_PointIsOffToTheLeft() {
        Point result = interpolateLinear(8, POINT_LEFT, POINT_RIGHT);
        assertThat(result.getY()).isCloseTo(5.0, DOUBLE_PRECISION_OFFSET);
    }

    @Test
    public void interpolateLinearTwoPoints_PointIsOffToTheRight() {
        Point result = interpolateLinear(18, POINT_LEFT, POINT_RIGHT);
        assertThat(result.getY()).isCloseTo(8.0, DOUBLE_PRECISION_OFFSET);
    }

    @Test
    public void interpolateLinearTwoPoints_PointIsOnTheLeftBorder() {
        Point result = interpolateLinear(POINT_LEFT.getX(), POINT_LEFT, POINT_RIGHT);
        assertEquals(result.getY(), POINT_LEFT.getY(), 0.0);
    }

    @Test
    public void interpolateLinearTwoPoints_PointIsOnTheRightBorder() {
        Point result = interpolateLinear(POINT_RIGHT.getX(), POINT_LEFT, POINT_RIGHT);
        assertEquals(result.getY(), POINT_RIGHT.getY(), 0.0);
    }
}
