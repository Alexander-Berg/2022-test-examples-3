package ru.yandex.crypta.graph2.matching.human.workflow.component.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.matching.human.workflow.component.ops.gen.RandomComponentGenerator;
import ru.yandex.crypta.graph2.model.matching.component.score.HumanMultiHistogramScoringStrategy;
import ru.yandex.crypta.graph2.model.matching.graph.cryptaid.CryptaIdDispenserByNeighboursWeight;
import ru.yandex.crypta.graph2.model.matching.merge.algo.split.SplitByMinEdgeAlgorithm;
import ru.yandex.crypta.graph2.model.soup.edge.weight.DefaultEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeInfoProvider;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.misc.time.TimeUtils;

public class CalculateComponentInfoReducerPerfTest {

    @Test
    public void test() {

        int numberOfEdges = 30;
        int iters = 100;

        EdgeInfoProvider edgeInfoProvider = new DefaultEdgeInfoProvider();
        HumanMultiHistogramScoringStrategy componentScoringStrategy =
                new HumanMultiHistogramScoringStrategy();
        CalculateComponentInfoReducer reducer = new CalculateComponentInfoReducer(
                edgeInfoProvider,
                componentScoringStrategy,
                new CryptaIdDispenserByNeighboursWeight(edgeInfoProvider),
                new SplitByMinEdgeAlgorithm(componentScoringStrategy, edgeInfoProvider));

        ListF<YTreeMapNode> testRecs = new RandomComponentGenerator(numberOfEdges).generateComponent().toRecs();
        System.out.println("Test recs generated");

        long start = System.currentTimeMillis();
        for (int iter : Cf.range(0, iters)) {

            System.out.println(String.format("Iter %d: %s",
                    iter,
                    TimeUtils.millisecondsToSecondsStringToNow(start)
            ));

            YtTestHelper.testYsonMultiEntityReducer(reducer, testRecs);
        }

        long end = System.currentTimeMillis();

        System.out.println("TOTAL: " + TimeUtils.millisecondsToSecondsString(end - start));
        System.out.println("AVG: " + TimeUtils.millisecondsToSecondsString((end - start) / iters));


    }


}
