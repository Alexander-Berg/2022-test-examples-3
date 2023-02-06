package ru.yandex.common.services.maps.router;

import org.hamcrest.number.IsCloseTo;
import org.junit.Assert;

import ru.yandex.common.services.maps.router.model.RouterAreaData;
import ru.yandex.common.services.maps.router.model.RouterGpsCoordinate;
import ru.yandex.common.services.maps.router.model.RouterMetaData;
import ru.yandex.common.services.maps.router.model.RouterSegmentData;

import javax.annotation.ParametersAreNonnullByDefault;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * Утилитный класс для тестов, связанных с Яндекс Маршрутизатором.
 *
 * @author Vladislav Bauer
 */
@ParametersAreNonnullByDefault
final class YandexMapsRouterTestUtils {

    private static final double GPS_CALCULATION_ERROR = 50.0;


    private YandexMapsRouterTestUtils() {
        throw new UnsupportedOperationException();
    }


    static void checkSegments(final List<RouterSegmentData> segments) {
        segments.forEach(segment -> checkCoordinates(segment.getMetaData(), segment.getPath()));
    }

    static void checkCoordinates(final RouterMetaData metaData, final List<RouterGpsCoordinate> coordinates) {
        assertThat(metaData, notNullValue());
        assertThat(coordinates, not(empty()));

        final double expectedDistance = metaData.getLength();
        final double actualDistance = RouterGpsCoordinate.distance(coordinates);
        Assert.assertThat(actualDistance, IsCloseTo.closeTo(expectedDistance, GPS_CALCULATION_ERROR));

        final RouterAreaData boundedBy = metaData.getBoundedBy();
        coordinates.forEach(coordinate -> {
            checkCoordinatePart(coordinate.getLatitude());
            checkCoordinatePart(coordinate.getLongitude());

            assertThat(boundedBy.isInside(coordinate), equalTo(true));
            assertThat(boundedBy.isOutside(coordinate), equalTo(false));
        });
    }

    private static void checkCoordinatePart(final BigDecimal value) {
        assertThat(value.compareTo(BigDecimal.ZERO), equalTo(1));
    }

}
