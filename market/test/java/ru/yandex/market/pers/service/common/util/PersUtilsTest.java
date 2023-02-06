package ru.yandex.market.pers.service.common.util;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author grigor-vlad
 * 07.04.2022
 */
public class PersUtilsTest {

    @Test
    public void simpleTestListBatchUpdate() {
        List<Integer> numbers = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        int batchSize = 2;
        List<Integer> newNumbers = new ArrayList<>();

        PersUtils.listBatchUpdate(numbers, batchSize,
            newNumbers::addAll
        );
        //check that lists are identical
        assertEquals(numbers, newNumbers);
    }
}
