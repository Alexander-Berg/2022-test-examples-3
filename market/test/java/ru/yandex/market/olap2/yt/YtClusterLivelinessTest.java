package ru.yandex.market.olap2.yt;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.market.olap2.leader.LeaderElector;
import ru.yandex.market.olap2.leader.Shutdowner;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.inside.yt.kosher.operations.Operation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

public class YtClusterLivelinessTest {
    private final LeaderElector le = Mockito.mock(LeaderElector.class);

    {
        Mockito.doAnswer(invocation -> {
            Shutdowner.ThrowingRunnable r = invocation.getArgument(1, Shutdowner.ThrowingRunnable.class);
            r.run();
            return null;
        }).when(le).doOnLeader(Mockito.any(), Mockito.any());
    }

    private final Operation liveOperation = Mockito.mock(Operation.class);

    {
        doNothing().when(liveOperation).awaitAndThrowIfNotSuccess();
    }

    private final Yt liveYt = Mockito.mock(Yt.class);
    private final YtOperations liveOperations = Mockito.mock(YtOperations.class);

    {
        Mockito.when(liveYt.operations()).thenReturn(liveOperations);
        Mockito.when(liveOperations.mapReduceAndGetOp(any())).thenReturn(liveOperation);
    }

    private final Operation deadOperation = Mockito.mock(Operation.class);

    {
        doThrow(new RuntimeException("Yt Cluster is dead")).when(deadOperation).awaitAndThrowIfNotSuccess();
    }

    private final Yt deadYt = Mockito.mock(Yt.class);
    private final YtOperations deadOperations = Mockito.mock(YtOperations.class);


    {
        Mockito.when(deadYt.operations()).thenReturn(deadOperations);
        Mockito.when(deadOperations.mapReduceAndGetOp(any())).thenReturn(deadOperation);
    }

    private final YtWrapper ytWrapper = Mockito.mock(YtWrapper.class);

    {
        Mockito.when(ytWrapper.yt(Mockito.eq(new YtCluster("dead_yt_cluster")))).thenReturn(deadYt);
        Mockito.when(ytWrapper.yt(Mockito.eq(new YtCluster("live_yt_cluster_1")))).thenReturn(liveYt);
        Mockito.when(ytWrapper.yt(Mockito.eq(new YtCluster("live_yt_cluster_2")))).thenReturn(liveYt);
    }


    @Test
    public void mustReturnLiveClusters() {
        YtClusterLiveliness liveliness = new YtClusterLiveliness(
                Arrays.asList("live_yt_cluster_1", "live_yt_cluster_2", "dead_yt_cluster"),
                le,
                ytWrapper,
                null
        );

        liveliness.checkYtClusterLiveliness();

        assertEquals(ImmutableSet.of(new YtCluster("live_yt_cluster_1"), new YtCluster("live_yt_cluster_2")),
                liveliness.liveYtClusters());
    }

    @Test
    public void noClustersAlive() {
        YtClusterLiveliness liveliness = new YtClusterLiveliness(
                Arrays.asList("dead_yt_cluster"),
                le,
                ytWrapper,
                null
        );

        liveliness.checkYtClusterLiveliness();

        assertEquals(Collections.emptySet(), liveliness.liveYtClusters());
    }

}
