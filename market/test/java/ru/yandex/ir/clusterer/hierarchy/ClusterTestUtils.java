package ru.yandex.ir.clusterer.hierarchy;

import org.junit.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author mkrasnoperov
 */
public class ClusterTestUtils {
    public static void assertSameClusterSet(List<int[]> expectedClusters, List<int[]> clusters) {
        Assert.assertEquals(clustersToSets(expectedClusters), clustersToSets(clusters));
    }

    private static Set<Set<Integer>> clustersToSets(List<int[]> clusters) {
        return clusters.stream().map(
            x -> Arrays.stream(x).mapToObj(y -> y).collect(Collectors.toSet())
        ).collect(Collectors.toSet());
    }
}
