package ru.yandex.market.checkout.checkouter.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.tasks.Partition;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PartitionUtilsTest {

    @Test
    public void test() {
        List<Partition> result = PartitionUtils.split(15, 1);
        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getLeftBound());
        assertEquals(15, result.get(0).getRightBound());


        result = PartitionUtils.split(15, 2);
        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getLeftBound());
        assertEquals(8, result.get(0).getRightBound());
        assertEquals(8, result.get(1).getLeftBound());
        assertEquals(15, result.get(1).getRightBound());

        result = PartitionUtils.split(2, 4);
        assertEquals(4, result.size());
        assertEquals(0, result.get(0).getLeftBound());
        assertEquals(1, result.get(0).getRightBound());
        assertEquals(1, result.get(1).getLeftBound());
        assertEquals(2, result.get(1).getRightBound());
        assertEquals(0, result.get(2).getLeftBound());
        assertEquals(1, result.get(2).getRightBound());
        assertEquals(1, result.get(3).getLeftBound());
        assertEquals(2, result.get(3).getRightBound());
    }
}
