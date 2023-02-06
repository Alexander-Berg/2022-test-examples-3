package ru.yandex.market.ff.repository.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class SelectWithInStatementQueryExecutorTest {

    @Test
    public void testCorrectSplit() {
        List<Long> list = createList(1, 2101);
        List<List<Long>> splits = new ArrayList<>();
        SelectWithInStatementQueryExecutor.executeWithBatches(batch -> {
            List<Long> listBatch = new ArrayList<>(batch);
            splits.add(listBatch);
            return listBatch;
        }, list);
        assertEquals(3, splits.size());
        List<Long> firstListExpected = createList(1, 1000);
        List<Long> secondListExpected = createList(1001, 1000);
        List<Long> thirdListExpected = Stream
            .concat(createList(2001, 101).stream(), Collections.nCopies(149, (Long) null).stream())
            .collect(Collectors.toList());
        assertEquals(firstListExpected, splits.get(0));
        assertEquals(secondListExpected, splits.get(1));
        assertEquals(thirdListExpected, splits.get(2));
    }

    private List<Long> createList(long firstElement, int size) {
        return Stream.iterate(firstElement, i -> i <= firstElement + size - 1, i -> i + 1).collect(Collectors.toList());
    }
}
