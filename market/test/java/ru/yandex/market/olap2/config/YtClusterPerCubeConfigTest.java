package ru.yandex.market.olap2.config;


import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import ru.yandex.market.olap2.model.YtCluster;
import ru.yandex.market.olap2.model.YtClustersPerCubeHolder;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;


@Slf4j
public class YtClusterPerCubeConfigTest {
    @Test
    public void mustParseCubetoYtClusterConfig() throws IOException {
        Map<String, Set<YtCluster>> result = YtClusterPerCubeConfig.getCubesToClusters(
                YtClusterPerCubeConfig.CUBE_TO_CLUSTERS_CONFIG_FILE);
        assertThat(result.size(), is(2));
        assertThat(result.get("test_cube_with_one_cluster").size(), is(1));
        assertThat(result.get("test_cube_with_one_cluster"), equalTo(ImmutableSet.of(new YtCluster("yt_cluster_1"))));
        assertThat(result.get("test_cube_with_two_clusters").size(), is(2));
        assertThat(result.get("test_cube_with_two_clusters"), equalTo(ImmutableSet.of(
                new YtCluster("yt_cluster_1"), new YtCluster("yt_cluster_2"))));
    }

    @Test
    public void mustGenerateBean() throws IOException {
        YtClustersPerCubeHolder holder = new YtClusterPerCubeConfig().ytClustersPerCubeHolder();
        assertThat(holder.getCubesToClusters().size(), is(2));
        assertTrue(holder.loadFromCluster("test_cube_with_one_cluster", new YtCluster("yt_cluster_1")));
        assertFalse(holder.loadFromCluster("test_cube_with_one_cluster", new YtCluster("yt_cluster_2")));
        assertTrue(holder.loadFromCluster("test_cube_with_two_clusters", new YtCluster("yt_cluster_1")));
        assertTrue(holder.loadFromCluster("non_present_in_config", new YtCluster("any_cluster")));
    }
}
