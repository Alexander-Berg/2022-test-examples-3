package ru.yandex.market.ff.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DispenserTest {

    private static final Map<Long, List<String>> TEST_DATA
            = Map.of(
            1L, List.of("test"),
            2L, List.of("test2")
    );

    @Test
    void toMapWhenOkTest() {
        Dispenser dispenser = new Dispenser();
        Map<Long, List<String>> map = dispenser.toMap(this::getBatch, List.of(1L, 2L));
        assertFalse(map.isEmpty());
        assertEquals("test", map.get(1L).get(0));
        assertEquals("test2", map.get(2L).get(0));
    }

    @Test
    void toMapWhenFewBatchTest() {
        Dispenser dispenser = new Dispenser(1);
        Map<Long, List<String>> map = dispenser.toMap(this::getBatch, List.of(1L, 2L));
        assertFalse(map.isEmpty());
        assertEquals("test", map.get(1L).get(0));
        assertEquals("test2", map.get(2L).get(0));
    }

    @Test
    void toMapWhenIdsEmptyTest() {
        Dispenser dispenser = new Dispenser();
        Map<Long, List<String>> map = dispenser.toMap(this::getBatch, new ArrayList<>());
        assertTrue(map.isEmpty());
    }

    @Test
    void toMapWhenFunctionReturnsEmptyTest() {
        Dispenser dispenser = new Dispenser();
        Map<Long, List<String>> map = dispenser.toMap(this::getEmptyBatch, List.of(1L));
        assertTrue(map.isEmpty());
    }

    @Test
    void toMapWhenFunctionReturnsNullTest() {
        Dispenser dispenser = new Dispenser();
        Map<Long, List<String>> map = dispenser.toMap(this::getNullBatch, List.of(1L));
        assertTrue(map.isEmpty());
    }

    private Map<Long, List<String>> getBatch(Collection<Long> ids) {
        Map<Long, List<String>> result = new HashMap<>();
        ids.forEach(x -> result.put(x, TEST_DATA.get(x)));
        return result;
    }

    private Map<Long, List<String>> getEmptyBatch(Collection<Long> ids) {
        return new HashMap<>();
    }

    private Map<Long, List<String>> getNullBatch(Collection<Long> ids) {
        return null;
    }

}
