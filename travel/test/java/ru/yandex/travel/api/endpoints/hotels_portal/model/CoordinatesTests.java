package ru.yandex.travel.api.endpoints.hotels_portal.model;

import org.junit.Test;

import ru.yandex.travel.api.models.hotels.Coordinates;

import static org.junit.Assert.assertEquals;

public class CoordinatesTests {
    private static final double eps = 1e-10;

    @Test
    public void testFromString() {
        Coordinates c = Coordinates.of("37.622504,55.753215");
        assertEquals(37.622504, c.getLon(), eps);
        assertEquals(55.753215, c.getLat(), eps);
    }

    @Test
    public void testToString() {
        Coordinates coordinates = new Coordinates();
        coordinates.setLon(37.622504);
        coordinates.setLat(55.753215);
        assertEquals("37.622504,55.753215", coordinates.toString());
    }
}
