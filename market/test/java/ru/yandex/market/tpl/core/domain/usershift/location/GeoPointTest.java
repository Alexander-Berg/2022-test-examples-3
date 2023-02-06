package ru.yandex.market.tpl.core.domain.usershift.location;

import java.math.BigDecimal;

import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeoPointTest {

    @Test
    void distance() {
        GeoPoint p1 = GeoPoint.ofLatLon(new BigDecimal(55.735634), new BigDecimal(37.593043));
        GeoPoint p2 = GeoPoint.ofLatLon(new BigDecimal(55.736693), new BigDecimal(37.589529));
        double distance = p1.distanceInMeters(p2);
        assertThat(distance).isCloseTo(249, Percentage.withPercentage(5));
    }

    @Test
    void invalidValues() {
        assertThrows(IllegalArgumentException.class, () -> GeoPoint.ofLatLon(0, -181));
        assertThrows(IllegalArgumentException.class, () -> GeoPoint.ofLatLon(0, 181));
        assertThrows(IllegalArgumentException.class, () -> GeoPoint.ofLatLon(-91, 0));
        assertThrows(IllegalArgumentException.class, () -> GeoPoint.ofLatLon(91, 0));
    }

    @Test
    void distanceTo() {
        assertEquals(GeoPoint.EARTH_RADIUS * Math.PI * 1000,
                GeoPoint.ofLatLon(0., 0.).distanceInMeters(GeoPoint.ofLatLon(0., 180.)));
        assertEquals(GeoPoint.EARTH_RADIUS * Math.PI * 1000,
                GeoPoint.ofLatLon(-90., 0.).distanceInMeters(GeoPoint.ofLatLon(90, 0.)));
        assertTrue(GeoPoint.ofLatLon(0., -180.).distanceInMeters(GeoPoint.ofLatLon(0., 180.)) < 0.00001);
    }

}
