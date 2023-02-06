package ru.yandex.crypta.graph2.model.matching.component.score;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.Outlier;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class AnomalyScoringStrategyTest {

    @Test
    public void scoreTree() {
        Vertex outlier = new Vertex("outlier", EIdType.LOGIN);
        Vertex v1 = new Vertex("12938923840192831", EIdType.YANDEXUID);
        Vertex v2 = new Vertex("394857398475938458", EIdType.YANDEXUID);

        SetF<Vertex> graphVertex = Cf.set(v1, v2, outlier);
        SetF<Vertex> leftComponentVertex = Cf.set(v1);
        SetF<Vertex> rightComponentVertex = Cf.set(v2, outlier);

        Component mergedComponent = new Component(graphVertex);
        Component leftComponent = new Component(leftComponentVertex);
        Component rightComponent = new Component(rightComponentVertex);

        MapF<String, Component> components = Cf.map("left", leftComponent, "right", rightComponent);
        MapF<Vertex, Component> vertexToComponents = Cf.map();
        VertexPropertiesCollector vp = new VertexPropertiesCollector();
        vp.setOutliers(Cf.map(outlier, new Outlier(outlier, "1", "source", "outlier", "login", -0.75)));
        ListF<Edge> edgedBtwComponents = Cf.list();

        GraphInfo graphInfo = new GraphInfo(components, vertexToComponents, vp, edgedBtwComponents);

        AnomalyScoringStrategy strategy = new AnomalyScoringStrategy("anoamly", "anomaly");
        MetricsTree mergedMT = strategy.scoreTree(mergedComponent, graphInfo);
        MetricsTree leftMT = strategy.scoreTree(leftComponent, graphInfo);
        MetricsTree rightMT = strategy.scoreTree(rightComponent, graphInfo);

        assertEquals(0.5, mergedMT.getScore(), 0.001);
        assertEquals(0.5, rightMT.getScore(), 0.001);
        assertEquals(1.0, leftMT.getScore(), 0.001);
    }
}
