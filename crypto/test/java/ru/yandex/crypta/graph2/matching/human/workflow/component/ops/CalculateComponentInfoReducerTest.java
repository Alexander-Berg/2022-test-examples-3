package ru.yandex.crypta.graph2.matching.human.workflow.component.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen.SingleLineComponentGenerator;
import ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen.SingleVertexComponentGenerator;
import ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen.TestComponent;
import ru.yandex.crypta.graph2.matching.human.workflow.component.ops.indevice.TestIndeviceEdgeTypeConfigProvider;
import ru.yandex.crypta.graph2.model.matching.component.ComponentCenter;
import ru.yandex.crypta.graph2.model.matching.component.score.SimpleStupidScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.graph.cryptaid.CryptaIdDispenserByNeighboursWeight;
import ru.yandex.crypta.graph2.model.matching.merge.algo.split.SplitByMinEdgeAlgorithm;
import ru.yandex.crypta.graph2.model.matching.vertex.VertexInComponent;
import ru.yandex.crypta.graph2.model.soup.edge.weight.DefaultEdgeInfoProvider;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;

public class CalculateComponentInfoReducerTest {
    @Test
    public void singleVertexComponent() {

        CalculateComponentInfoReducer reducer = getReducer();

        TestComponent singleVertexComponent = new SingleVertexComponentGenerator().generateComponent();
        ListF<YTreeMapNode> inVerticesRecs = singleVertexComponent.toRecs();

        LocalYield<YTreeMapNode> outRecs = YtTestHelper.testReducer(reducer, inVerticesRecs);

        ListF<VertexInComponent> outVertices = YtTestHelper.fromYsonRecs(reducer, VertexInComponent.class, outRecs, 0);
        ListF<EdgeInComponent> outEdges = YtTestHelper.fromYsonRecs(reducer, EdgeInComponent.class, outRecs, 2);

        assertEquals(1, outVertices.size());
        assertEquals(0, outEdges.size());

        VertexInComponent y1Out = outVertices.first();

        // vertex become cryptaId for itself
        assertEquals(ComponentCenter.fromVertex(y1Out.getVertex()).getCryptaId(), y1Out.getCryptaId());

    }

    private CalculateComponentInfoReducer getReducer() {
        DefaultEdgeInfoProvider edgeWeightProvider = new DefaultEdgeInfoProvider(
                new TestIndeviceEdgeTypeConfigProvider()
        );
        SimpleStupidScoringStrategy componentScoringStrategy = new SimpleStupidScoringStrategy(20);
        return new CalculateComponentInfoReducer(
                edgeWeightProvider,
                componentScoringStrategy,
                new CryptaIdDispenserByNeighboursWeight(edgeWeightProvider),
                new SplitByMinEdgeAlgorithm(componentScoringStrategy, edgeWeightProvider));
    }

    @Test
    public void testLineSplit() {
        CalculateComponentInfoReducer reducer = getReducer();

        TestComponent component = new SingleLineComponentGenerator(100).generateComponent();
        ListF<YTreeMapNode> inVerticesRecs = component.toRecs();

        LocalYield<YTreeMapNode> outRecs = YtTestHelper.testReducer(reducer, inVerticesRecs);

        ListF<VertexInComponent> outVertices = YtTestHelper.fromYsonRecs(reducer, VertexInComponent.class, outRecs, 0);
        ListF<EdgeInComponent> outEdges = YtTestHelper.fromYsonRecs(reducer, EdgeInComponent.class, outRecs, 2);
        ListF<EdgeBetweenComponents> betweenEdges = YtTestHelper.fromYsonRecs(reducer, EdgeBetweenComponents.class,
                outRecs, 3);

        assertEquals(component.getVertices().size(), outVertices.size());
        assertEquals(component.getEdges().size(), outEdges.size() + betweenEdges.size());

        assertEquals(betweenEdges.size() + 1, outVertices.map(VertexInComponent::getCryptaId).unique().size());

    }
}
