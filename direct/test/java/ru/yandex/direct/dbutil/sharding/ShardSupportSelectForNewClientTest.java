package ru.yandex.direct.dbutil.sharding;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.dbutil.exception.AliveShardNotFoundException;
import ru.yandex.direct.dbutil.exception.NoAvailableShardsException;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;
import ru.yandex.direct.dbutil.wrapper.ShardedDb;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ShardSupportSelectForNewClientTest {
    private static final int SHARD1 = 1;
    private static final int SHARD2 = 2;
    private static final int SHARD3 = 3;
    private static final int SHARD4 = 4;

    private static final int NUM_OF_PPC_SHARDS = 4;

    private static final List<ShardWeight> SHARD_WEIGHTS = Arrays.asList(
            new ShardWeight(SHARD1, 3),
            new ShardWeight(SHARD2, 2),
            new ShardWeight(SHARD3, 1),
            new ShardWeight(SHARD4, 0));

    private DatabaseWrapperProvider databaseWrapperProvider;
    private ShardSupport shardSupport;

    @Before
    public void setUp() {
        databaseWrapperProvider = mock(DatabaseWrapperProvider.class);
        when(databaseWrapperProvider.isAlive(any(ShardedDb.class), anyInt()))
                .thenReturn(true);
        when(databaseWrapperProvider.getShardWeight(any(ShardedDb.class), anyInt()))
                .thenAnswer(a -> SHARD_WEIGHTS.get((int) a.getArguments()[1] - 1));
        shardSupport = new ShardSupport(databaseWrapperProvider,
                mock(ShardedValuesGenerator.class), NUM_OF_PPC_SHARDS);
    }

    @Test(expected = NoAvailableShardsException.class)
    public void testNoAvailableShards() {
        shardSupport = new ShardSupport(databaseWrapperProvider,
                mock(ShardedValuesGenerator.class), 0);

        shardSupport.selectShardForNewClient();
    }

    @Test(expected = AliveShardNotFoundException.class)
    public void testAliveShardNotFoundException() {
        when(databaseWrapperProvider.isAlive(any(ShardedDb.class), anyInt()))
                .thenReturn(false);

        shardSupport.selectShardForNewClient();
    }

    @Test
    public void testSelectSuccess() {
        when(databaseWrapperProvider.isAlive(any(ShardedDb.class), anyInt()))
                .thenAnswer(a -> (int) a.getArguments()[1] == SHARD1);

        int selectedShard = shardSupport.selectShardForNewClient();

        assertThat(selectedShard, equalTo(SHARD1));
    }
}
