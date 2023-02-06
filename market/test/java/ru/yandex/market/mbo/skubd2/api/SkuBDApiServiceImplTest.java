package ru.yandex.market.mbo.skubd2.api;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class SkuBDApiServiceImplTest {

    @Test
    public void split() {
        testSplit(199, 199);
        testSplit(200, 200);

        testSplit(1000, 1000);
        testSplit(1100, 1100);
    }

    private static void testSplit(int numElements, int expectedBatchCount) {
        List<List<Integer>> batch = SkuBDApiServiceImpl.split(
                IntStream.range(0, numElements).boxed().collect(Collectors.toList())
        );
        Assert.assertEquals(expectedBatchCount, batch.size());
    }
}
