package ru.yandex.direct.dbutil.sharding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;

public class GroupByShardTest {

    private static final List<String> TEST_OBJECTS = Arrays.asList("Object1", "Object2", "Object3");

    @Test
    public void allObjectInOneShard() {
        Map<Integer, List<String>> expectedData = new HashMap<>();
        expectedData.put(1, TEST_OBJECTS);
        ShardHelper shardHelper = helperWithMockedShards(1, 1, 1);

        ShardedData<String> shardedData = shardHelper
                .groupByShard(TEST_OBJECTS, ShardKey.ORDER_ID, t -> t);

        assertThat("все объекты в 1м шарде", shardedData.getShardedDataMap(), beanDiffer(expectedData));
    }

    @Test
    public void objectInDifferentShards() {
        Map<Integer, List<String>> expectedData = new HashMap<>();
        expectedData.put(1, TEST_OBJECTS.subList(0, 1));
        expectedData.put(2, TEST_OBJECTS.subList(1, 2));
        expectedData.put(3, TEST_OBJECTS.subList(2, 3));
        ShardHelper shardHelper = helperWithMockedShards(1, 2, 3);

        ShardedData<String> shardedData = shardHelper
                .groupByShard(TEST_OBJECTS, ShardKey.ORDER_ID, t -> t);

        assertThat("все объекты в разных шардах", shardedData.getShardedDataMap(), beanDiffer(expectedData));
    }

    @Test
    public void notFoundShardForObject() {
        Map<Integer, List<String>> expectedData = new HashMap<>();
        expectedData.put(1, TEST_OBJECTS.subList(0, 1));
        expectedData.put(3, TEST_OBJECTS.subList(2, 3));

        ShardHelper shardHelper = helperWithMockedShards(1, null, 3);

        ShardedData<String> shardedData = shardHelper
                .groupByShard(TEST_OBJECTS, ShardKey.ORDER_ID, t -> t);

        assertThat("для 2го элемента не найден шард", shardedData.getShardedDataMap(), beanDiffer(expectedData));
    }

    private ShardHelper helperWithMockedShards(Integer... values) {
        ShardSupport shardSupport = mock(ShardSupport.class);
        when(shardSupport.getShards(any(), any()))
                .thenReturn(Arrays.asList(values));
        return new ShardHelper(shardSupport);
    }
}
