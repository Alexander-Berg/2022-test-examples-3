package ru.yandex.market.robot.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author inenakhov
 */
public class BatchingUtilsTest {
    /**
     * Случай когда число элементов в массиве совпадает с количеством батчей на которое нужно разбить.
     * @throws Exception
     */
    @Test
    public void splitBorderCase1() throws Exception {
        ArrayList<Integer> input = Lists.newArrayList(0, 1, 2);
        int batchCount = 3;

        List<List<Integer>> splits = BatchingUtils.split(input, batchCount);
        for (int i = 0; i < 3; i++) {
            List<Integer> split = splits.get(i);
            assertEquals(1, split.size());
            assertTrue(i == split.get(0));
        }
    }

    /**
     * Случай когда число элементов в массиве меньше количества батчей на которое нужно разбить.
     * @throws Exception
     */
    @Test
    public void splitBorderCase2() throws Exception {
        ArrayList<Integer> input = Lists.newArrayList(0, 1, 2);
        int batchCount = 5;

        List<List<Integer>> splits = BatchingUtils.split(input, batchCount);
        assertEquals(1, splits.size());
        assertEquals(3, splits.get(0).size());
        assertTrue(splits.get(0).containsAll(input));
    }

    @Test
    public void splitIllegalArguments() throws Exception {
        try {
            BatchingUtils.split(Lists.newArrayList(1, 2, 3), 0);
            assertFalse(true);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }

        try {
            BatchingUtils.split(Lists.newArrayList(), 5);
            assertFalse(true);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    @Test
    public void splitPartsSize() throws Exception {
        int batchCount = 9;
        int inputSize = 170;

        ArrayList<Integer> input = new ArrayList<>();
        for (int i = 0; i < inputSize; i++) {
            input.add(i);
        }

        List<List<Integer>> splits = BatchingUtils.split(input, batchCount);

        for (int i = 0; i < splits.size(); i++) {
            for (int j = i; j < splits.size(); j++) {
                assertTrue(Math.abs(splits.get(i).size() - splits.get(j).size()) < 2);
            }
        }

        assertEquals(batchCount, splits.size());
    }
}