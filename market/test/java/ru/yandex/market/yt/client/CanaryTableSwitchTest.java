package ru.yandex.market.yt.client;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class CanaryTableSwitchTest {
    static final String REPLICA_ASYNC = "replica-async";
    static final String REPLICA_SYNC = "replica-sync";
    static final String TABLE = "table";
    final YtClientProxy mainCluster = makeClient("markov");
    final YtClientProxy replicaAsync = makeClient(REPLICA_ASYNC);
    final YtClientProxy replicaSync = makeClient(REPLICA_SYNC);

    static YtClientProxy makeClient(String clusterName) {
        var mock = Mockito.mock(YtClientProxy.class);
        when(mock.unsafe()).thenReturn(mock);
        when(mock.getClusterName()).thenReturn(clusterName);
        return mock;
    }

    @Test
    void lifecycleWithAsyncReplica() {
        // given
        try (var proxySource = makeProxySource(true)) {
            assertThat(proxySource.getCurrentClient())
                    .as("initially (before start/update) main cluster is selected")
                    .isSameAs(mainCluster);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_SYNC, REPLICA_ASYNC)
                    .as("sync replica is always preferred")
                    .isSameAs(replicaSync);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_ASYNC)
                    .as("but if enabled, async replica can be selected too")
                    .isSameAs(replicaAsync);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_SYNC, REPLICA_ASYNC)
                    .as("sync replica is always preferred")
                    .isSameAs(replicaSync);

            setAliveReplicasAndAssertCurrentClient(proxySource)
                    .as("keep using last used client until somebody is alive")
                    .isSameAs(replicaSync);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_ASYNC)
                    .as("but if enabled, async replica can be selected too")
                    .isSameAs(replicaAsync);
        }
    }

    @Test
    void lifecycleWithoutAsyncReplica() {
        // given
        try (var proxySource = makeProxySource(false)) {
            assertThat(proxySource.getCurrentClient())
                    .as("initially (before start/update) main cluster is selected")
                    .isSameAs(mainCluster);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_SYNC, REPLICA_ASYNC)
                    .as("sync replica is always preferred")
                    .isSameAs(replicaSync);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_ASYNC)
                    .as("async replicas are not used if not desired, keep using last used client")
                    .isSameAs(replicaSync);

            setAliveReplicasAndAssertCurrentClient(proxySource, REPLICA_SYNC, REPLICA_ASYNC)
                    .as("sync replica is always preferred")
                    .isSameAs(replicaSync);

            setAliveReplicasAndAssertCurrentClient(proxySource)
                    .as("keep using last used client until somebody is alive")
                    .isSameAs(replicaSync);
        }
    }

    private ObjectAssert<YtClientProxy> setAliveReplicasAndAssertCurrentClient(
            CanaryTableSwitch proxySource,
            String... aliveReplicas
    ) {
        setAliveReplicas(aliveReplicas);
        proxySource.updateActiveCluster();
        return assertThat(proxySource.getCurrentClient());
    }

    private CanaryTableSwitch makeProxySource(boolean canUseAsyncReplicas) {
        return new CanaryTableSwitch(
                TABLE,
                mainCluster,
                new YtClientReplicas(List.of(
                        replicaSync,
                        replicaAsync
                )),
                1,
                false,
                canUseAsyncReplicas
        );
    }

    private void setAliveReplicas(String... aliveReplicas) {
        when(mainCluster.get(TABLE, "replicas"))
                .thenReturn(Stream.of(aliveReplicas)
                        .reduce(
                                YTree.mapBuilder(),
                                (map, r) -> map.key(r).value(makeReplicaNode(r, r.endsWith("-sync"))),
                                (map1, map2) -> map1
                        ).buildMap());
    }

    private static YTreeNode makeReplicaNode(String clusterName, boolean sync) {
        return YTree.mapBuilder()
                .key("cluster_name").value(clusterName)
                .key("mode").value(sync
                        ? "sync"
                        : "async")
                .buildMap();
    }
}
