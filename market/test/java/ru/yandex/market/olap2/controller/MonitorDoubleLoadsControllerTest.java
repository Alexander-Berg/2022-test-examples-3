package ru.yandex.market.olap2.controller;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.olap2.dao.MetadataDao;
import ru.yandex.market.olap2.model.TableAndPartition;
import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.model.YtClustersPerCubeHolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MonitorDoubleLoadsControllerTest {

    private final YtClustersPerCubeHolder ytClustersPerCube = new YtClustersPerCubeHolder(ImmutableMap.of(
            "cubes_clickhouse__restricted_to_cluster_1", ImmutableSet.of(new YtCluster("cluster_1"))
    ));

    @Test
    public void mustHaveNoDoubleLoads() {
        MetadataDao dao = Mockito.mock(MetadataDao.class);
        Mockito.when(dao.getTodaysEventsByTablePartitionByCluster()).thenReturn(
                ImmutableMap.of(
                        new TableAndPartition("cubes_clickhouse__path_all_loads_equal", null), ImmutableMap.of(
                                new YtCluster("cluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L)),
                                new YtCluster("cluster_2"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L))
                        ),
                        new TableAndPartition("cubes_clickhouse__path_waiting_for_cluster_2", null), ImmutableMap.of(
                                new YtCluster("cluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L)),
                                new YtCluster("cluster_2"), Arrays.asList(Pair.of(10L, 100L))
                        ),
                        new TableAndPartition("cubes_clickhouse__path_loads_from_cluster_1_only", null), ImmutableMap.of(
                                new YtCluster("cluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L))
                        ),
                        new TableAndPartition("cubes_clickhouse__restricted_to_cluster_1", null), ImmutableMap.of(
                                new YtCluster("cluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L)),
                                new YtCluster("cluster_2"), Arrays.asList(Pair.of(12L, 102L), Pair.of(13L, 103L))
                        )
                )
        );
        MonitorDoubleLoadsController c = new MonitorDoubleLoadsController(dao, ytClustersPerCube);
        assertThat(c.noDoubleLoads().getBody(), Matchers.is(JugglerConstants.OK));
    }

    @Test
    public void mustHaveDoubleLoads() {
        MetadataDao dao = Mockito.mock(MetadataDao.class);
        Mockito.when(dao.getTodaysEventsByTablePartitionByCluster()).thenReturn(
                ImmutableMap.of(
                        new TableAndPartition("cubes_clickhouse__path_last_load_differs", null), ImmutableMap.of(
                                new YtCluster("cluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L)),
                                new YtCluster("cluster_2"), Arrays.asList(Pair.of(10L, 100L), Pair.of(12L, 101L))
                        ),
                        new TableAndPartition("cubes_clickhouse__path_first_load_differs", null), ImmutableMap.of(
                                new YtCluster("cluster_1"), Arrays.asList(Pair.of(10L, 100L), Pair.of(11L, 101L)),
                                new YtCluster("cluster_2"), Arrays.asList(Pair.of(11L, 100L), Pair.of(11L, 101L))
                        )
                )
        );
        MonitorDoubleLoadsController c = new MonitorDoubleLoadsController(dao, ytClustersPerCube);
        assertTrue(c.noDoubleLoads().getBody(), c.noDoubleLoads().getBody().startsWith(JugglerConstants.CRIT));
    }

    @Test
    public void parallelListsMustBeEqualByShortestList() {
        List<List<Integer>> lists = new ArrayList<>();
        lists.add(Arrays.asList(1, 2, 3));
        lists.add(Arrays.asList(1, 2));
        lists.add(Arrays.asList(1, 2, 4));
        assertTrue(lists.toString(), MonitorDoubleLoadsController.checkAllEqual(lists));
    }

    @Test
    public void parallelListsMustBeEqualByEmptyList() {
        // это не корректное поведение для трёх кластеров, но у нас больше двух не предвидится
        List<List<Integer>> lists = new ArrayList<>();
        lists.add(Arrays.asList(1, 2));
        lists.add(Collections.emptyList());
        lists.add(Arrays.asList(3, 4));
        assertTrue(lists.toString(), MonitorDoubleLoadsController.checkAllEqual(lists));
    }

    @Test
    public void parallelListsMustBeEqual() {
        List<List<Integer>> lists = new ArrayList<>();
        lists.add(Arrays.asList(1, 2, 3));
        lists.add(Arrays.asList(1, 2, 3));
        lists.add(Arrays.asList(1, 2, 3, 4));
        assertTrue(lists.toString(), MonitorDoubleLoadsController.checkAllEqual(lists));
    }

    @Test
    public void parallelListsMustNotBeEqual() {
        List<List<Integer>> lists = new ArrayList<>();
        lists.add(Arrays.asList(1, 2, 3));
        lists.add(Arrays.asList(1, 2, 4));
        lists.add(Arrays.asList(1, 2, 3));
        assertFalse(lists.toString(), MonitorDoubleLoadsController.checkAllEqual(lists));
    }

}
