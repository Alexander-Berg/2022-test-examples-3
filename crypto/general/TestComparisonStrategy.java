package ru.yandex.crypta.graph.engine.exp.stats.ops;

import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.engine.proto.TStatsOptions;
import ru.yandex.crypta.graph2.model.matching.component.Component;
import ru.yandex.crypta.graph2.model.matching.component.GraphInfo;
import ru.yandex.crypta.graph2.model.matching.component.score.ComponentScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.component.score.HumanMultiHistogramScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.component.score.NativeHumanScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;


public class TestComparisonStrategy implements ComponentScoringStrategy {

    private final ComponentScoringStrategy firstStrategy;
    private final ComponentScoringStrategy secondStrategy;
    private static final double EPS = 1e-7;

    public TestComparisonStrategy(TStatsOptions options) {
        firstStrategy = new HumanMultiHistogramScoringStrategy(false, Option.of(options.getCrossDeviceWeight()));
        secondStrategy = new NativeHumanScoringStrategy(options);
    }

    @Override
    public MetricsTree scoreTree(Component component, GraphInfo graphInfo) {
        MetricsTree firstTree = firstStrategy.scoreTree(component, graphInfo);
        MetricsTree secondTree = secondStrategy.scoreTree(component, graphInfo);

        checkEquality(component.getCryptaId(), firstTree, secondTree);

        return firstTree;
    }

    private void checkEquality(String cryptaId, MetricsTree firstTree, MetricsTree secondTree) {
        for (var item : firstTree.getChildren().entrySet()) {
            double firstValue = item.getValue();
            double secondValue = secondTree.getChildren().getOrElse(item.getKey(), 0.);
            if (Math.abs(firstValue - secondValue) > EPS) {
                throw new UnsupportedOperationException("Component " + cryptaId + " score " + item.getKey() + " " + firstValue + " != " + secondValue);
            }
        }
        if (Math.abs(firstTree.getScore() - secondTree.getScore()) > EPS || firstTree.getChildren().size() != secondTree.getChildren().size()) {
            throw new UnsupportedOperationException("Component " + cryptaId + " score " + firstTree.getScore() + " != " + secondTree.getScore());
        }
    }

    @Override
    public String getName() {
        return "comparison: " + firstStrategy.getName() + " " + secondStrategy.getName();
    }
}
