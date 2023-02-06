package ru.yandex.crypta.graph2.model.matching.component.score;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;

public class MetricsTreeTest {

    @Test
    public void minus() {
        MetricsTree metricsTree1 = new MetricsTree(1231, Cf.map(
                "sub1", 0.0,
                "sub2", -1.0,
                "sub3", 89.9)
        );
        MetricsTree metricsTree2 = new MetricsTree(1231, Cf.map(
                "sub2", -1.0,
                "sub3", 99.9,
                "sub4", 1.0)
        );

        MetricsTree diff = metricsTree2.minus(metricsTree1);

        assertEquals(metricsTree1.getScore(), metricsTree2.getScore(), Double.MIN_NORMAL);

        Assert.assertListsEqual(
                Cf.list("sub1", "sub2", "sub3", "sub4"),
                diff.getChildren().keySet().sorted()
        );

        assertEquals(0.0, diff.getChildren().getTs("sub1"), Double.MIN_NORMAL);
        assertEquals(0.0, diff.getChildren().getTs("sub2"), Double.MIN_NORMAL);
        assertEquals(10.0, diff.getChildren().getTs("sub3"), Double.MIN_NORMAL);
        assertEquals(1.0, diff.getChildren().getTs("sub4"), Double.MIN_NORMAL);


    }
}
