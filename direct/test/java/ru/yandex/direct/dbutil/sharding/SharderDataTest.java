package ru.yandex.direct.dbutil.sharding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.InOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class SharderDataTest {

    private static final List<String> TEST_OBJECTS = Arrays.asList("Object1", "Object2", "Object3");

    @Test
    public void canObtainAllElementsFromShardedData() {
        Map<Integer, List<String>> expectedData = new HashMap<>();
        expectedData.put(1, TEST_OBJECTS.subList(0, 1));
        expectedData.put(2, TEST_OBJECTS.subList(1, 2));
        expectedData.put(3, TEST_OBJECTS.subList(2, 3));
        ShardedData<String> shardedData = new ShardedData<>(expectedData);

        List<String> actualList = shardedData.getData()
                .stream()
                .sorted()
                .collect(Collectors.toList());

        assertThat("ShardedData#getData возвращает все элементы", actualList, beanDiffer(TEST_OBJECTS));
    }

    @Test
    public void byDefaultConsumerExecutionIsNotSplitted() {
        TestConsumer consumer = mock(TestConsumer.class);

        Map<Integer, List<String>> dataMap = new HashMap<>();
        dataMap.put(1, TEST_OBJECTS);
        ShardedData<String> shardedData = new ShardedData<>(dataMap);

        shardedData.forEach(consumer);

        verify(consumer).accept(1, TEST_OBJECTS);
    }

    @Test
    public void canSplitConsumerExecutionByChunks() {
        TestConsumer consumer = mock(TestConsumer.class);

        Map<Integer, List<String>> dataMap = new HashMap<>();
        dataMap.put(1, TEST_OBJECTS);
        ShardedData<String> shardedData = new ShardedData<>(dataMap);

        shardedData
                .chunkedBy(1)
                .forEach(consumer);

        verify(consumer).accept(1, TEST_OBJECTS.subList(0, 1));
        verify(consumer).accept(1, TEST_OBJECTS.subList(1, 2));
        verify(consumer).accept(1, TEST_OBJECTS.subList(2, 3));
    }

    @Test
    public void consumerCalledForEveryShard() {
        TestConsumer consumer = mock(TestConsumer.class);

        Map<Integer, List<String>> dataMap = new HashMap<>();
        dataMap.put(1, TEST_OBJECTS.subList(0, 1));
        dataMap.put(2, TEST_OBJECTS.subList(1, 2));
        dataMap.put(3, TEST_OBJECTS.subList(2, 3));
        ShardedData<String> shardedData = new ShardedData<>(dataMap);

        shardedData.forEach(consumer);

        verify(consumer).accept(1, TEST_OBJECTS.subList(0, 1));
        verify(consumer).accept(2, TEST_OBJECTS.subList(1, 2));
        verify(consumer).accept(3, TEST_OBJECTS.subList(2, 3));
    }

    @Test
    public void forEachIteratesShardsInAscOrder() {
        TestConsumer consumer = mock(TestConsumer.class);

        Map<Integer, List<String>> dataMap = new LinkedHashMap<>();
        dataMap.put(3, TEST_OBJECTS.subList(2, 3));
        dataMap.put(1, TEST_OBJECTS.subList(0, 1));
        dataMap.put(2, TEST_OBJECTS.subList(1, 2));
        ShardedData<String> shardedData = new ShardedData<>(dataMap);

        shardedData.forEach(consumer);

        InOrder inOrder = inOrder(consumer);

        inOrder.verify(consumer).accept(1, TEST_OBJECTS.subList(0, 1));
        inOrder.verify(consumer).accept(2, TEST_OBJECTS.subList(1, 2));
        inOrder.verify(consumer).accept(3, TEST_OBJECTS.subList(2, 3));
    }

    @Test
    public void canGetChunkedStream() {
        TestConsumer consumer = mock(TestConsumer.class);

        Map<Integer, List<String>> dataMap = new HashMap<>();
        dataMap.put(1, TEST_OBJECTS);
        ShardedData<String> shardedData = new ShardedData<>(dataMap);

        shardedData
                .chunkedBy(2)
                .stream()
                .forEach(e -> consumer.accept(e.getKey(), e.getValue()));

        InOrder inOrder = inOrder(consumer);

        inOrder.verify(consumer).accept(1, TEST_OBJECTS.subList(0, 2));
        inOrder.verify(consumer).accept(1, TEST_OBJECTS.subList(2, 3));
    }


    private interface TestConsumer extends BiConsumer<Integer, List<String>> {
    }
}
