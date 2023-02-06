package ru.yandex.travel.hotels.common.token;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class OccupancyTests {
    @Test
    public void testSingle() {
        assertEquals(Occupancy.fromString("1").getAdults(), 1);
        assertEquals(Occupancy.fromString("3").getAdults(), 3);
    }

    @Test
    public void testWithChildren() {
        Occupancy occ = Occupancy.fromString("1-2,3,4");
        assertEquals(occ.getAdults(), 1);
        assertEquals(occ.getChildren().size(), 3);
        assertEquals(occ.getChildren().get(0).intValue(), 2);
        assertEquals(occ.getChildren().get(1).intValue(), 3);
        assertEquals(occ.getChildren().get(2).intValue(), 4);
    }

    @Test(expected = RuntimeException.class)
    public void testEmpty() {
        Occupancy.fromString("");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidAdult() {
        Occupancy.fromString("zd1");
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidChild() {
        Occupancy.fromString("1-3,4-1");
    }

    @Test
    public void testToExpediaString() {
        assertEquals(Occupancy.fromString("1").toExpediaString(), "1");
        assertEquals(Occupancy.fromString("1-3").toExpediaString(), "1-3");
        assertEquals(Occupancy.fromString("2-4,6").toExpediaString(), "2-4,6");
    }

    @Test
    public void testToBookingString() {
        assertEquals(Occupancy.fromString("1").toBookingString(), "A");
        assertEquals(Occupancy.fromString("1-3").toBookingString(), "A,3");
        assertEquals(Occupancy.fromString("2-4,6").toBookingString(), "A,A,4,6");
    }

    @Test
    public void testToHotelsCombinedString() {
        assertEquals(Occupancy.fromString("1").toHotelsCombinedString(), "1");
        assertEquals(Occupancy.fromString("1-3").toHotelsCombinedString(), "1:3");
        assertEquals(Occupancy.fromString("2-4,6").toHotelsCombinedString(), "2:4,6");
    }
}
