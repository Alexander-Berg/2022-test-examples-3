package ru.yandex.crypta.graph2.model.matching.merge.algo.split;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.component.score.ComponentScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.component.score.SimpleStupidScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.merge.algo.score.WeightedLinkScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.merge.algo.split.inner.ComponentConnectivityInspector;
import ru.yandex.crypta.graph2.model.matching.merge.algo.split.inner.ComponentSplitDendrogram;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.edge.weight.SurvivalEdgeInfoProvider;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static ru.yandex.crypta.graph2.model.matching.merge.algo.score.WeightedLinkScoringStrategy.SPLIT_SCORE_FOR_FORCE_SPLITTING;

public class MaxMinSplitAlgorithmTest {
    private static final double STRONG_SURVIVAL_WEIGHT = 0.6;
    private EdgeInfoProvider edgeInfoProvider = new SurvivalEdgeInfoProvider();
    private ComponentScoringStrategy componentMax5 = new SimpleStupidScoringStrategy(5);
    private ComponentScoringStrategy componentMax7 = new SimpleStupidScoringStrategy(7);
    private GraphInfo graphInfoMock = mock(GraphInfo.class);

    @Test
    public void splitByMaximumComponentSize() {
        // prepare component from two linked clusters
        SetF<Edge> cluster1Edges = Cf.set(
                edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL,
                        STRONG_SURVIVAL_WEIGHT),
                edge("e2", EIdType.EMAIL, "p1", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> cluster2Edges = Cf.set(
                edge("y2", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p2", EIdType.PHONE, "d1", EIdType.IDFA, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> betweenEdge = Cf.set(
                edge("y1", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT)
        );

        Component component = component(
                cluster1Edges.plus(cluster2Edges.plus(betweenEdge))
        );

        // do split
        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax5, 5, 10);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfoMock);

        // check results
        assertTrue(split.isPresent());
        assertTrue(split.get().getStatus().isSuccess());
        assertTrue(split.get().getScoreGain().getChildren().containsKeyTs("split"));
        // not by link score
        assertTrue(split.get().getScoreGain().getChildren().getTs("split") < Double.MIN_VALUE);

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();
        assertEquals(2, clusteredComponents.size());

        ListF<SetF<Edge>> clusteredEdges = clusteredComponents.map(Component::getInnerEdges);

        Assert.assertContains(clusteredEdges, cluster1Edges);
        Assert.assertContains(clusteredEdges, cluster2Edges);

        // don't do split
        Component sameComponent = component(
                cluster1Edges.plus(cluster2Edges.plus(betweenEdge))
        );

        SplitAlgorithm splitAlgorithmWithLargerGraphSize = getSplitAlgorithm(componentMax7, 10, 10);
        Option<ComponentSplitDendrogram> noSplit = splitAlgorithmWithLargerGraphSize.split(sameComponent,
                graphInfoMock);

        // check results
        assertTrue(noSplit.isPresent());
        assertFalse(noSplit.get().getStatus().isSuccess());
    }


    @Test
    public void splitComponentByLargeComponentInLinkScore() {
        // prepare component from two linked clusters
        SetF<Edge> cluster1Edges = Cf.set(
                edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, STRONG_SURVIVAL_WEIGHT),
                edge("e2", EIdType.EMAIL, "p1", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> cluster2Edges = Cf.set(
                edge("y2", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p2", EIdType.PHONE, "d1", EIdType.IDFA, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> betweenEdge = Cf.set(
                edge("y1", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT)
        );

        Component component = component(
                cluster1Edges.plus(cluster2Edges.plus(betweenEdge))
        );

        // do split
        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax7, 3, 0);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfoMock);

        // check results
        assertTrue(split.isPresent());
        assertTrue(split.get().getStatus().isSuccess());
        assertTrue(split.get().getScoreGain().getChildren().containsKeyTs("split"));
        // by link score
        assertEquals(-SPLIT_SCORE_FOR_FORCE_SPLITTING, split.get().getScoreGain().getChildren().getTs("split"), 0.01);

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();
        assertEquals(2, clusteredComponents.size());

        ListF<SetF<Edge>> clusteredEdges = clusteredComponents.map(Component::getInnerEdges);

        Assert.assertContains(clusteredEdges, cluster1Edges);
        Assert.assertContains(clusteredEdges, cluster2Edges);
    }

    @Test
    public void splitComponentByZeroLink() {
        // prepare component from two linked clusters
        SetF<Edge> cluster1Edges = Cf.set(
                edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, STRONG_SURVIVAL_WEIGHT),
                edge("e2", EIdType.EMAIL, "p1", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> cluster2Edges = Cf.set(
                edge("y2", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p2", EIdType.PHONE, "d1", EIdType.IDFA, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> betweenEdge = Cf.set(
                edge("y1", EIdType.YANDEXUID, "p2", EIdType.PHONE, 0.1)
        );

        Component component = component(
                cluster1Edges.plus(cluster2Edges.plus(betweenEdge))
        );

        // do split
        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax7, 10, 0);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfoMock);

        // check results
        assertTrue(split.isPresent());
        assertTrue(split.get().getStatus().isSuccess());
        assertTrue(split.get().getScoreGain().getChildren().containsKeyTs("split"));
        // by link score
        assertEquals(-0.9, split.get().getScoreGain().getChildren().getTs("split"), 0.01);

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();
        assertEquals(2, clusteredComponents.size());

        ListF<SetF<Edge>> clusteredEdges = clusteredComponents.map(Component::getInnerEdges);

        Assert.assertContains(clusteredEdges, cluster1Edges);
        Assert.assertContains(clusteredEdges, cluster2Edges);
    }


    @Test
    public void splitComponentRecursively() {

        // prepare hierarchical graph
        SetF<Edge> cluster1Edges = Cf.set(
                edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, STRONG_SURVIVAL_WEIGHT),
                edge("e2", EIdType.EMAIL, "p1", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> cluster2Edges = Cf.set(
                edge("y3", EIdType.YANDEXUID, "p3", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p3", EIdType.PHONE, "d2", EIdType.IDFA, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> cluster3Edges = Cf.set(
                edge("y2", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT),
                edge("p2", EIdType.PHONE, "d1", EIdType.IDFA, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> group1ConnectingEdge = Cf.set(
                edge("y1", EIdType.YANDEXUID, "p2", EIdType.PHONE, STRONG_SURVIVAL_WEIGHT)
        );

        SetF<Edge> group1Edges = cluster1Edges.plus(cluster2Edges).plus(group1ConnectingEdge);

        Edge edgeBetweenGroups = edge("d1", EIdType.IDFA, "d2", EIdType.IDFA, 1);

        Component component = component(
                group1Edges.plus(cluster3Edges).plus(edgeBetweenGroups)
        );

        // do split
        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax5, 3, 0);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfoMock);

        // check results
        assertTrue(split.isPresent());
        assertTrue(split.get().getStatus().isSuccess());

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();
        assertEquals(3, clusteredComponents.size());

        ListF<SetF<Edge>> clusteredEdges = clusteredComponents.map(Component::getInnerEdges);

        Assert.assertContains(clusteredEdges, cluster1Edges);
        Assert.assertContains(clusteredEdges, cluster2Edges);
        Assert.assertContains(clusteredEdges, cluster3Edges);
    }

    private Edge edge(String id1, EIdType id1Type, String id2, EIdType id2Type, double survivalWeight) {
        return edge(
                id1, id1Type, id2, id2Type,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG,
                survivalWeight
        );
    }

    private Edge edge(String id1, EIdType id1Type, String id2, EIdType id2Type,
                      ESourceType sourceType, ELogSourceType logSource,
                      double survivalWeight) {
        return new Edge(
                id1, id1Type, id2, id2Type,
                sourceType, logSource,
                Option.empty(), Option.of(0.0), Option.of(survivalWeight)
        );
    }

    private Component component(CollectionF<Edge> edges) {
        Component component = new Component();
        component.setInnerEdges(edges.unique());
        component.setVertices(edges.flatMap(Edge::getVertices).unique());

        assertTrue(ComponentConnectivityInspector.isConnected(component));

        return component;
    }

    private SplitAlgorithm getSplitAlgorithm(ComponentScoringStrategy scoreStrategy, int maxSize,
                                             int minSizeToTrySplit) {
        return new MaxMinSplitAlgorithm(
                scoreStrategy,
                edgeInfoProvider,
                new WeightedLinkScoringStrategy(
                        edgeInfoProvider,
                        maxSize,
                        maxSize,
                        minSizeToTrySplit,
                        0
                )
        );
    }

}
