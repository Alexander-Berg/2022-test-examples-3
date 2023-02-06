package ru.yandex.crypta.graph2.model.matching.merge.algo.split;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.CollectionF;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeRecord;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.component.score.ComponentScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.component.score.SimpleStupidScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.helper.DatesCountEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferStatus;
import ru.yandex.crypta.graph2.model.matching.merge.algo.split.inner.ComponentConnectivityInspector;
import ru.yandex.crypta.graph2.model.matching.merge.algo.split.inner.ComponentSplitDendrogram;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeInfoProvider;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;


public class SplitAlgorithmTest {

    private static final TEdgeRecord ARTIFICIAL_EDGE_TYPE = TEdgeRecord.newBuilder()
            .setProps(TEdgeProps.newBuilder()
                    .setActivityType(TEdgeProps.EActivityType.NONE)
                    .setEdgeStrength(TEdgeProps.EEdgeStrength.ARTIFICIAL)
            ).build();

    private EdgeInfoProvider edgeInfoProvider = new DatesCountEdgeInfoProvider();
    private EdgeInfoProvider trustedEdgeInfoProvider = new EdgeInfoProvider() {
        @Override
        public double getEdgeWeight(Edge edge) {
            return edgeInfoProvider.getEdgeWeight(edge);
        }

        @Override
        public double getEdgeWeight(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
            return edgeInfoProvider.getEdgeWeight(edge);
        }

        @Override
        public TEdgeRecord getEdgeTypeConfig(Edge edge) {
            return ARTIFICIAL_EDGE_TYPE;
        }

        @Override
        public TEdgeRecord getEdgeTypeConfig(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
            return ARTIFICIAL_EDGE_TYPE;
        }
    };

    private ComponentScoringStrategy componentMax1 = new SimpleStupidScoringStrategy(1);
    private ComponentScoringStrategy componentMax3 = new SimpleStupidScoringStrategy(3);

    private Edge edge(String id1, EIdType id1Type, String id2, EIdType id2Type, int datesCount) {
        return edge(id1, id1Type, id2, id2Type, ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG, datesCount);
    }

    private Edge edge(String id1, EIdType id1Type, String id2, EIdType id2Type,
                      ESourceType sourceType, ELogSourceType logSource,
                      int datesCount) {
        ListF<String> dates = Cf.range(0, datesCount).map(Object::toString);
        return new Edge(id1, id1Type, id2, id2Type, sourceType, logSource, dates);
    }

    private Component component(CollectionF<Edge> edges) {
        Component component = new Component();
        component.setInnerEdges(edges.unique());
        component.setVertices(edges.flatMap(Edge::getVertices).unique());
        return component;
    }

    @Test
    public void trySplitComponent() throws Exception {

        SetF<Edge> cluster1Edges = Cf.set(
                edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, 2),
                edge("e2", EIdType.EMAIL, "p1", EIdType.PHONE, 3),
                edge("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, 2)
        );

        SetF<Edge> cluster2Edges = Cf.set(
                edge("y2", EIdType.YANDEXUID, "p2", EIdType.PHONE, 2),
                edge("p2", EIdType.PHONE, "d1", EIdType.IDFA, 2)
        );

        SetF<Edge> betweenEdge = Cf.set(
                edge("y1", EIdType.YANDEXUID, "p2", EIdType.PHONE, 1)
        );

        Component component = component(
                cluster1Edges.plus(cluster2Edges.plus(betweenEdge))
        );

        assertTrue(ComponentConnectivityInspector.isConnected(component));


        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax3);

        GraphInfo graphInfo = mock(GraphInfo.class);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfo);

        assertTrue(split.isPresent());

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();
        assertEquals(2, clusteredComponents.size());

        ListF<SetF<Edge>> clusteredEdges = clusteredComponents.map(Component::getInnerEdges);

        Assert.assertContains(clusteredEdges, cluster1Edges);
        Assert.assertContains(clusteredEdges, cluster2Edges);


    }

    @Test
    public void splitRecursively() throws Exception {

        SetF<Edge> cluster1Edges = Cf.set(
                edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, 4),
                edge("e2", EIdType.EMAIL, "p1", EIdType.PHONE, 3),
                edge("p1", EIdType.PHONE, "y1", EIdType.YANDEXUID, 4)
        );

        SetF<Edge> cluster2Edges = Cf.set(
                edge("y2", EIdType.YANDEXUID, "p2", EIdType.PHONE, 3),
                edge("p2", EIdType.PHONE, "d1", EIdType.IDFA, 3)
        );

        SetF<Edge> group1ConnectingEdge = Cf.set(
                edge("y1", EIdType.YANDEXUID, "p2", EIdType.PHONE, 2)
        );

        SetF<Edge> group1Edges = cluster1Edges.plus(cluster2Edges).plus(group1ConnectingEdge);

        SetF<Edge> cluster3Edges = Cf.set(
                edge("y3", EIdType.YANDEXUID, "p3", EIdType.PHONE, 2),
                edge("p3", EIdType.PHONE, "d2", EIdType.IDFA, 2)
        );

        Edge edgeBetweenGroups = edge("d1", EIdType.IDFA, "d2", EIdType.IDFA, 1);

        Component component = component(
                group1Edges.plus(cluster3Edges).plus(edgeBetweenGroups)
        );

        assertTrue(ComponentConnectivityInspector.isConnected(component));

        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax3);

        GraphInfo graphInfo = mock(GraphInfo.class);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfo);

        assertTrue(split.isPresent());

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();
        assertEquals(3, clusteredComponents.size());

        ListF<SetF<Edge>> clusteredEdges = clusteredComponents.map(Component::getInnerEdges);

        Assert.assertContains(clusteredEdges, cluster1Edges);
        Assert.assertContains(clusteredEdges, cluster2Edges);
        Assert.assertContains(clusteredEdges, cluster3Edges);


    }

    @Test
    public void splitSingleEdge() throws Exception {

        Edge edge = edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, 2);

        Component component = component(Cf.list(edge));

        assertTrue(ComponentConnectivityInspector.isConnected(component));

        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax1);

        GraphInfo graphInfo = mock(GraphInfo.class);

        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfo);

        assertTrue(split.isPresent());

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();

        assertEquals(2, clusteredComponents.size());
        Assert.forAll(clusteredComponents, cp -> cp.getVertices().size() == 1);

        ListF<String> sorted = clusteredComponents.map(cp -> cp.getVertices().single().getId()).sorted();
        assertEquals(Cf.list("e2", "y1"), sorted);


    }

    @Test
    public void splitTripleEdge() throws Exception {

        Edge edge1 = edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG, 1);
        Edge edge2 = edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, ESourceType.ACCOUNT_MANAGER,
                ELogSourceType.METRIKA_MOBILE_LOG, 2);
        Edge edge3 = edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, ESourceType.APP_URL_REDIR,
                ELogSourceType.METRIKA_MOBILE_LOG, 2);

        Component component = component(Cf.list(edge1, edge2, edge3));

        assertTrue(ComponentConnectivityInspector.isConnected(component));

        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax1);

        GraphInfo graphInfo = mock(GraphInfo.class);

        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfo);

        assertTrue(split.isPresent());

        CollectionF<Component> clusteredComponents = split.get().allSplitComponents();

        assertEquals(2, clusteredComponents.size());
        Assert.forAll(clusteredComponents, cp -> cp.getVertices().size() == 1);

        ListF<String> sorted = clusteredComponents.map(cp -> cp.getVertices().single().getId()).sorted();
        assertEquals(Cf.list("e2", "y1"), sorted);

        assertEquals(3, split.get().getEdgesBetween().size());


    }

    @Test
    public void dontSplitSingleEdgeByScore() throws Exception {

        Edge edge = edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, 2);

        Component component = component(Cf.list(edge));

        assertTrue(ComponentConnectivityInspector.isConnected(component));

        SplitAlgorithm splitAlgorithm = getSplitAlgorithm(componentMax3);

        GraphInfo graphInfo = mock(GraphInfo.class);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfo);

        assertTrue(split.isPresent());

        ListF<ComponentSplitDendrogram> splitResult = split.get().flattenTree();
        assertTrue(splitResult.filter(s -> s.getStatus().equals(MergeOfferStatus.SPLIT)).isEmpty());
        assertTrue(splitResult.filter(s -> s.getStatus().equals(MergeOfferStatus.FAILED_BY_SCORE)).isNotEmpty());

    }

    @Test
    public void dontEventTrySplitTrustedEdge() throws Exception {

        Edge edge = edge("y1", EIdType.YANDEXUID, "e2", EIdType.EMAIL, 2);

        Component component = component(Cf.list(edge));

        assertTrue(ComponentConnectivityInspector.isConnected(component));

        SplitAlgorithm splitAlgorithm = new SplitByMinEdgeAlgorithm(
                componentMax1,
                trustedEdgeInfoProvider
        );

        GraphInfo graphInfo = mock(GraphInfo.class);
        Option<ComponentSplitDendrogram> split = splitAlgorithm.split(component, graphInfo);

        assertFalse(split.isPresent());
    }

    private SplitAlgorithm getSplitAlgorithm(ComponentScoringStrategy scoreStrategy) {
        return new SplitByMinEdgeAlgorithm(
                scoreStrategy,
                edgeInfoProvider
        );
    }

}
