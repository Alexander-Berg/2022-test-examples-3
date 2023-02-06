package ru.yandex.crypta.graph2.model.matching.component.score;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.crypta.graph.engine.proto.TStats;
import ru.yandex.crypta.graph.engine.proto.TStatsOptions;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;

import static org.junit.Assert.assertEquals;

public class NativeHumanScoringStrategyTest {

    @Test
    public void checkNativeCall() {
        String cryptaId = "123";

        TStatsOptions options = TStatsOptions.newBuilder().build();
        NativeHumanScoringStrategy scoringStrategy = new NativeHumanScoringStrategy(options);
        TStats stats = scoringStrategy.collectStats(new Component(cryptaId), new GraphInfo());
        assertEquals(cryptaId, String.valueOf(stats.getId()));
    }

    @Test
    public void compareHumanStrategiesTest() {
        Tuple2<Component, GraphInfo> graph = HumanMultiHistogramScoringStrategyTest.generateGraph();
        Component component = graph.get1();
        GraphInfo graphInfo = graph.get2();

        double crossDeviceWeight = 7.5;
        MetricsTree metricsTree = new HumanMultiHistogramScoringStrategy(false, Option.of(crossDeviceWeight))
                .scoreTree(component, graphInfo);


        TStatsOptions options = TStatsOptions.newBuilder().setCrossDeviceWeight(7.5).build();
        MetricsTree nativeMetricsTree = new NativeHumanScoringStrategy(options).scoreTree(component, graphInfo);

        var nativeScores = nativeMetricsTree.getChildren();

        for (var score : metricsTree.getChildren().entrySet()) {
            double scoreValue = score.getValue();
            double nativeScoreValue = nativeScores.getOrElse(score.getKey(), 0.);

            assertEquals(scoreValue, nativeScoreValue, 1e-7);
        }
        assertEquals(metricsTree.getScore(), nativeMetricsTree.getScore(), 1e-7);
    }
}
