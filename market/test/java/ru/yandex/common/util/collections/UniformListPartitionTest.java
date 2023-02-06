package ru.yandex.common.util.collections;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class UniformListPartitionTest {

    @Test
    public void empty() {
        List<Integer> list = Collections.emptyList();
        List<List<Integer>> partitioned = new UniformListPartition<>(list, 10);
        Assert.assertTrue(partitioned.isEmpty());
    }

}
