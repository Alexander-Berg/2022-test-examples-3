package ru.yandex.travel.api.endpoints.hotels_portal.model;

import org.junit.Test;

import ru.yandex.travel.api.models.hotels.BoundingBox;
import ru.yandex.travel.api.models.hotels.Coordinates;

import static org.junit.Assert.assertEquals;

public class BoundingBoxTests {
    private static final double eps = 1e-10;

    @Test
    public void testFromString() {
        BoundingBox bb = BoundingBox.of("37.622504,55.753215~39.622504,59.753215");
        assertEquals(37.622504, bb.getLeftDown().getLon(), eps);
        assertEquals(55.753215, bb.getLeftDown().getLat(), eps);
        assertEquals(39.622504, bb.getUpRight().getLon(), eps);
        assertEquals(59.753215, bb.getUpRight().getLat(), eps);
    }

    @Test
    public void testToString() {
        Coordinates leftDown = new Coordinates();
        leftDown.setLon(37.622504);
        leftDown.setLat(55.753215);
        Coordinates upRight = new Coordinates();
        upRight.setLon(39.622504);
        upRight.setLat(59.753215);
        BoundingBox boundingBox = BoundingBox.of(leftDown, upRight);
        assertEquals("37.622504,55.753215~39.622504,59.753215", boundingBox.toString());
    }
}
