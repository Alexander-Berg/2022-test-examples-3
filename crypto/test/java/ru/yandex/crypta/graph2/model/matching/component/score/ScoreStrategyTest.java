package ru.yandex.crypta.graph2.model.matching.component.score;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.component.score.extractors.EmailsCountExtractor;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.props.Outlier;
import ru.yandex.crypta.graph2.model.soup.props.VertexPropertiesCollector;
import ru.yandex.crypta.graph2.model.soup.vertex.Vertex;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ScoreStrategyTest {

    private Component componentOfSize(int size) {
        Component cp = new Component();
        for (Integer vIdx : Cf.range(0, size)) {
            cp.addVertex(new Vertex(String.valueOf(vIdx), EIdType.YANDEXUID));
        }
        return cp;
    }

    private ComponentScoringStrategy constantScore(String name, double score) {
        return new ComponentScoringStrategy() {
            @Override
            public MetricsTree scoreTree(Component component, GraphInfo graphInfo) {
                return new MetricsTree(score);
            }

            @Override
            public String getName() {
                return name;
            }
        };
    }

    @Test
    public void testHistogramScore() throws Exception {
        HistogramCountsScoringStrategy hist = HistogramCountsScoringStrategy.metric("name", "desc")
                .scoringCount((component, graphInfo) -> component.getVertices().size())
                .lessOrEqualAs(10, 1.0)
                .andTheRestAs(0.0)
                .andPenalizeEmpty()
                .build();

        GraphInfo graphInfo = mock(GraphInfo.class);

        assertEquals(1.0, hist.scoreTree(componentOfSize(5), graphInfo).getScore(), 0.01);
        assertEquals(1.0, hist.scoreTree(componentOfSize(10), graphInfo).getScore(), 0.01);
        assertEquals(0.0, hist.scoreTree(componentOfSize(11), graphInfo).getScore(), 0.01);
        assertEquals(-1.0, hist.scoreTree(componentOfSize(0), graphInfo).getScore(), 0.01);

    }

    @Test
    public void testMultiScore() throws Exception {

        WeightedMultiScoringStrategy multiScore = new WeightedMultiScoringStrategy()
                .weighting(constantScore("a", 10), 2)
                .weighting(constantScore("b", 3), 3);

        GraphInfo graphInfo = mock(GraphInfo.class);

        assertEquals(29.0, multiScore.scoreTree(componentOfSize(5), graphInfo).getScore(), 0.01);

    }

    @Test
    public void testWeightedHistStrategy() throws Exception {
        HistogramCountsScoringStrategy maxAt2LoginsHist = HistogramCountsScoringStrategy
                .metric(
                        "logins_count", "Count of logins"
                ).scoringCount(
                        (cp, graphInfo) -> cp.getVertices().filter(v -> v.getIdType().equals(EIdType.LOGIN)).unique().size()
                ).lessOrEqualAs(1, 0.5).lessOrEqualAs(4, 0.3).andTheRestAs(0.2)
                .build();

        HistogramCountsScoringStrategy min1maxAt6VerticesHist = HistogramCountsScoringStrategy
                .metric(
                        "vertices_count", "Count of vertices"
                ).scoringCount(
                        (cp, graphInfo) -> cp.getVertices().unique().size()
                ).lessOrEqualAs(0, 0).lessOrEqualAs(3, 0.2).lessOrEqualAs(6, 0.6).andTheRestAs(0.2)
                .build();

        WeightedMultiScoringStrategy scorer = new WeightedMultiScoringStrategy()
                .weighting(maxAt2LoginsHist, 1)
                .weighting(min1maxAt6VerticesHist, 2);

        GraphInfo graphInfo = mock(GraphInfo.class);

        // incrementally add vertices
        Component cp = new Component();

        // yuids as 0, but 0 logins is still ok
        assertEquals(0.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y1", EIdType.YANDEXUID));
        assertEquals(0.9, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l1", EIdType.LOGIN));
        assertEquals(0.9, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l2", EIdType.LOGIN));
        // two logins are worse than one
        assertEquals(0.7, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l3", EIdType.LOGIN));
        // but 4 vertices are much better than three
        assertEquals(1.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("e3", EIdType.EMAIL));
        assertEquals(1.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y2", EIdType.YANDEXUID));
        assertEquals(1.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("d1", EIdType.IDFA));
        // too many vertices
        assertEquals(0.7, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y3", EIdType.YANDEXUID));
        assertEquals(0.7, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

    }

    @Test
    public void testTrueHistogramCountsScoringStrategy() throws Exception {
        TrueHistogramCountsScoringStrategy maxAt2LoginsHist = TrueHistogramCountsScoringStrategy
                .metric(
                        "logins_count", "Count of logins"
                ).scoringCount(
                        (cp, graphInfo) -> cp.getVertices().filter(v -> v.getIdType().equals(EIdType.LOGIN)).unique().size()
                ).lessOrEqualAs(1, 0.5)
                .lessOrEqualAs(4, 0.3)
                .andTheRest()
                .build();

        TrueHistogramCountsScoringStrategy min1maxAt6VerticesHist = TrueHistogramCountsScoringStrategy
                .metric(
                        "vertices_count", "Count of vertices"
                ).scoringCount(
                        (cp, graphInfo) -> cp.getVertices().unique().size()
                ).lessOrEqualAs(0, 0)
                .lessOrEqualAs(3, 0.2)
                .lessOrEqualAs(6, 0.6)
                .andTheRest()
                .build();

        WeightedMultiScoringStrategy scorer = new WeightedMultiScoringStrategy()
                .weighting(maxAt2LoginsHist, 1)
                .weighting(min1maxAt6VerticesHist, 2);

        GraphInfo graphInfo = mock(GraphInfo.class);

        // incrementally add vertices
        Component cp = new Component();

        // yuids as 0, but 0 logins is still ok
        assertEquals(0.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y1", EIdType.YANDEXUID));
        assertEquals(0.63, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l1", EIdType.LOGIN));
        assertEquals(0.63, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l2", EIdType.LOGIN));
        // two logins are worse than one
        assertEquals(0.23, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l3", EIdType.LOGIN));
        // but 4 vertices are much better than three
        assertEquals(0.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("e3", EIdType.EMAIL));
        assertEquals(0.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y2", EIdType.YANDEXUID));
        assertEquals(0.5, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("d1", EIdType.IDFA));
        // too many vertices
        assertEquals(0.1, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y3", EIdType.YANDEXUID));
        assertEquals(0.1, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);
    }

    @Test
    public void countScoringStrategyTest() throws Exception {
        HistogramCountsScoringStrategy strategy = HistogramCountsScoringStrategy
                .metric(
                        "emails_count", "Count of emails"
                ).scoringCount(
                        new EmailsCountExtractor(false)
                ).lessOrEqualAs(0, 0)
                .lessOrEqualAs(3, 0.6)
                .lessOrEqualAs(5, 0.3)
                .uniformDecreasingRange(6, 20, 0.1)
                .andTheRestAs(0)
                .build();
        for (var item : strategy.getCountsProbs().entrySet()) {
            System.out.println(item.getKey() + " " + item.getValue());
        }
        assertEquals(0.3, strategy.getCountsProbs().ceilingEntry(4).getValue(), 0.01);
    }

    @Test
    public void uniformIncreasingAndDecreasing() throws Exception {
        HistogramCountsScoringStrategy scorer = HistogramCountsScoringStrategy
                .metric(
                        "vertices_count", "Count of vertices"
                ).scoringCount(
                        (cp, graphInfo) -> cp.getVertices().size()
                ).uniformIncreasingRange(0, 5, 0.7)
                .uniformDecreasingRange(5, 8, 0.3).andTheRestAs(0)
                .build();

        GraphInfo graphInfo = mock(GraphInfo.class);

        Component cp = new Component();
        cp.addVertex(new Vertex("y1", EIdType.YANDEXUID));
        assertEquals(0.0466, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("l1", EIdType.LOGIN));
        assertEquals(0.0933, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y2", EIdType.YANDEXUID));
        assertEquals(0.1399, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y3", EIdType.YANDEXUID));
        assertEquals(0.1866, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y4", EIdType.YANDEXUID));
        assertEquals(0.2333, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        // decreases here!
        cp.addVertex(new Vertex("y5", EIdType.YANDEXUID));
        assertEquals(0.15, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y6", EIdType.YANDEXUID));
        assertEquals(0.0999, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        cp.addVertex(new Vertex("y7", EIdType.YANDEXUID));
        assertEquals(0.0499, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);

        // 0 starting from here!
        cp.addVertex(new Vertex("y8", EIdType.YANDEXUID));
        assertEquals(0, scorer.scoreTree(cp, graphInfo).getScore(), 0.01);
    }

    @Test
    public void AnomalyScoringStrategy() throws Exception {
        Vertex outlierVertex1 = new Vertex("1@yandex.ru", EIdType.EMAIL);
        Vertex outlierVertex2 = new Vertex("2@yandex.ru", EIdType.EMAIL);
        Vertex normVertex = new Vertex("3423423423", EIdType.YANDEXUID);

        SetF<Vertex> componentsVertices = Cf.set(outlierVertex1, outlierVertex2, normVertex);

        Component mergedComponent = new Component(componentsVertices);
        MapF<String, Component> components = Cf.map("component", mergedComponent);
        MapF<Vertex, Component> vertexToComponents = Cf.hashMap();
        VertexPropertiesCollector verticesProperties = new VertexPropertiesCollector();
        ListF<Edge> edgesBetweenComponents = Cf.arrayList();

        Outlier outlier1 = new Outlier(
                outlierVertex1,
                "1",
                "source",
                "1@yandex.ru",
                "email",
                -0.2);

        Outlier outlier2 = new Outlier(
                outlierVertex2,
                "2",
                "source",
                "2@yandex.ru",
                "email",
                -0.6);

        MapF<Vertex, Outlier> outliers = Cf.hashMap();
        outliers.put(new Vertex("1@yandex.ru", EIdType.EMAIL), outlier1);
        outliers.put(new Vertex("2@yandex.ru", EIdType.EMAIL), outlier2);

        verticesProperties.setOutliers(outliers);

        GraphInfo graphInfo = new GraphInfo(components, vertexToComponents, verticesProperties, edgesBetweenComponents);

        MetricsTree anomalySS = new AnomalyScoringStrategy("anomaly_score", "Anomaly score")
                .scoreTree(mergedComponent, graphInfo);

        assertEquals(anomalySS.getScore(), 1.0, 0.0001);
    }
}
