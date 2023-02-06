package ru.yandex.travel.hotels.searcher;

import org.junit.Test;

import ru.yandex.travel.hotels.common.token.Occupancy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CapacityTests {
    @Test
    public void testAdultsByRule() {
        assertEquals(Capacity.fromRule(5, 0, 0).toString(), "<=5");
    }

    @Test
    public void testAdultsByRuleZeroNumberNonzeroAge() {
        assertEquals(Capacity.fromRule(5, 0, 2).toString(), "<=5");
    }

    @Test
    public void testAdultsWithChildrenByRule() {
        assertEquals(Capacity.fromRule(2, 3, 5).toString(), "<=2-5,5,5");
    }

    @Test
    public void testAdultsFromOccupancy() {
        assertEquals(Capacity.fromOccupancy(Occupancy.fromString("2")).toString(), "==2");
    }

    @Test
    public void testAdultsWithChildreFromOccupancy() {
        assertEquals(Capacity.fromOccupancy(Occupancy.fromString("2-1,2")).toString(), "==2-1,2");
    }

    @Test
    public void testMatchExact() {
        Occupancy occupancy = Occupancy.fromString("2-1,2");
        Capacity capacity = Capacity.fromOccupancy(occupancy);
        assertTrue(capacity.matches(occupancy));
    }

    @Test
    public void testMatchExactChildReorder() {
        Occupancy occupancy1 = Occupancy.fromString("2-1,2");
        Occupancy occupancy2 = Occupancy.fromString("2-2,1");
        Capacity capacity = Capacity.fromOccupancy(occupancy1);
        assertTrue(capacity.matches(occupancy2));
    }

    @Test
    public void testDoesNotMatchExactGreater() {
        Occupancy occupancy1 = Occupancy.fromString("2");
        Occupancy occupancy2 = Occupancy.fromString("1");
        Capacity capacity = Capacity.fromOccupancy(occupancy1);
        assertFalse(capacity.matches(occupancy2));
    }

    @Test
    public void testDoesNotMatchExactLower() {
        Occupancy occupancy1 = Occupancy.fromString("2");
        Occupancy occupancy2 = Occupancy.fromString("4");
        Capacity capacity = Capacity.fromOccupancy(occupancy1);
        assertFalse(capacity.matches(occupancy2));
    }

    @Test
    public void testDoesNotMatchExactChildren() {
        Occupancy occupancy1 = Occupancy.fromString("2");
        Occupancy occupancy2 = Occupancy.fromString("2-10");
        Capacity capacity = Capacity.fromOccupancy(occupancy1);
        assertFalse(capacity.matches(occupancy2));
    }

    @Test
    public void testDoesNotMatchExactChildrenNoChildren() {
        Occupancy occupancy1 = Occupancy.fromString("2-10");
        Occupancy occupancy2 = Occupancy.fromString("2");
        Capacity capacity = Capacity.fromOccupancy(occupancy1);
        assertFalse(capacity.matches(occupancy2));
    }

    @Test
    public void testMatchRuleEquals() {
        Occupancy occupancy = Occupancy.fromString("2");
        Capacity capacity = Capacity.fromRule(2, 0, 0);
        assertTrue(capacity.matches(occupancy));
    }

    @Test
    public void testMatchRuleLess() {
        Occupancy occupancy = Occupancy.fromString("1");
        Capacity capacity = Capacity.fromRule(2, 0, 0);
        assertTrue(capacity.matches(occupancy));
    }

    @Test
    public void testMatchChildAsAdult() {
        Occupancy occupancy = Occupancy.fromString("1-2");
        Capacity capacity = Capacity.fromRule(2, 0, 0);
        // Номер двухместный, не разрешает дополнительных детей, но один взрослый с ребенком в нем ок (ребенок
        // считается за взрослого)
        assertTrue(capacity.matches(occupancy));
    }

    @Test
    public void testDoesNotMatchTooManyChildren() {
        Occupancy occupancy = Occupancy.fromString("1-2,4");
        Capacity capacity = Capacity.fromRule(2, 0, 0);
        assertFalse(capacity.matches(occupancy));
    }

    @Test
    public void testMatchOneChildAsChildOneChildAsAdult() {
        Occupancy occupancy = Occupancy.fromString("1-2,4");
        Capacity capacity = Capacity.fromRule(2, 1, 2);
        assertTrue(capacity.matches(occupancy));
    }

    @Test
    public void testDoesNotMatchOneChildTooOldOneChildAsAdult() {
        Occupancy occupancy = Occupancy.fromString("1-3,4");
        Capacity capacity = Capacity.fromRule(2, 1, 2);
        assertFalse(capacity.matches(occupancy));
    }

    @Test
    public void testCalcRoomCount1() {
        Occupancy occupancy = Occupancy.fromString("2");
        Capacity capacity = Capacity.fromRule(2, 0, 0);
        assertEquals(capacity.calculateRoomCount(occupancy), 1);
    }

    @Test
    public void testCalcRoomCount2() {
        Occupancy occupancy = Occupancy.fromString("2");
        Capacity capacity = Capacity.fromRule(1, 0, 0);
        assertEquals(capacity.calculateRoomCount(occupancy), 2);
    }

    @Test
    public void testCalcRoomCount2AndAHalf() {
        Occupancy occupancy = Occupancy.fromString("3");
        Capacity capacity = Capacity.fromRule(2, 0, 0);
        assertEquals(capacity.calculateRoomCount(occupancy), 2);
    }

    @Test
    public void testCalcRoomCount3WithChildren() {
        Occupancy occupancy = Occupancy.fromString("3-3,3,3");
        Capacity capacity = Capacity.fromRule(1, 1, 3);
        assertEquals(capacity.calculateRoomCount(occupancy), 3);
    }

    @Test
    public void testCalcRoomCount3WithChildrenNotMatch() {
        Occupancy occupancy = Occupancy.fromString("3-3,3,3,3");
        Capacity capacity = Capacity.fromRule(1, 1, 3);
        assertEquals(capacity.calculateRoomCount(occupancy), 0);
    }

    @Test
    public void testMultiply2() {
        Capacity capacity = Capacity.fromRule(1, 1, 3);
        assertEquals(capacity.multiply(2).toString(), "<=2-3,3");
    }

    @Test
    public void testMultiply3() {
        Capacity capacity = Capacity.fromRule(1, 1, 3);
        assertEquals(capacity.multiply(3).toString(), "<=3-3,3,3");
    }
}
