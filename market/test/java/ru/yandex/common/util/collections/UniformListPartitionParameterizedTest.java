package ru.yandex.common.util.collections;

import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.common.util.math.MathUtils;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@RunWith(Parameterized.class)
public class UniformListPartitionParameterizedTest {

    @Parameterized.Parameter(0)
    public int listSize;

    @Parameterized.Parameter(1)
    public int partitionSize;

    @Test
    public void test() {
        List<Integer> list = generate(listSize);
        List<List<Integer>> partitioned = new UniformListPartition<>(list, partitionSize);

        SortedSet<Integer> sizes = new TreeSet<>();
        for (List<Integer> integers : partitioned) {
            sizes.add(integers.size());
        }

        //для пустого списка
        if (listSize <= partitionSize) {
            Assert.assertEquals(1, sizes.size());
        } else {
            Assert.assertTrue(sizes.size() <= 2);
            Assert.assertTrue(sizes.last() - sizes.first() <= 1);
        }

        //Number of partitions should correspond requested parameters
        Assert.assertEquals(MathUtils.divideUp(list.size(), partitionSize), partitioned.size());

        //Total partitions' size should be equal to original list size
        Assert.assertEquals(listSize, partitioned.stream().map(List::size).reduce(Integer::sum).orElse(0).intValue());

    }

    private List<Integer> generate(int count) {
        return Stream.iterate(1, i -> i + 1).limit(count).collect(Collectors.toList());
    }

    @Parameterized.Parameters
    public static Iterable<Object[]> args() {
        return Arrays.asList(
                new Integer[]{5, 10},
                new Integer[]{10, 10},
                new Integer[]{10, 7},
                new Integer[]{10, 9},
                new Integer[]{6, 7},
                new Integer[]{9, 8},
                new Integer[]{25, 13},
                new Integer[]{100, 10},
                new Integer[]{100, 7}
        );
    }
}
