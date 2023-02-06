package ru.yandex.crypta.graph2.matching.human.workflow.neighbours.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.component.ComponentNeighbours;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeInComponent;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;

public class CalculateComponentNeighboursReducerTest {
    @Test
    public void testCalculateNeighbours() throws Exception {

        String cc1 = "cc1";
        String cc2 = "cc2";
        String cc3 = "cc3";

        String mk31 = MergeKey.betweenComponents(cc3, cc1).getMergeKey();
        String mk32 = MergeKey.betweenComponents(cc3, cc2).getMergeKey();

        var builder3 = EdgeInComponent.newBuilder().setCryptaId(cc3);
        ListF<EdgeInComponent> input = Cf.list(
                builder3.setMergeKey(mk31).build(),
                builder3.setMergeKey(mk32).build()
        );

        CalculateComponentNeighboursReducer reducer = new CalculateComponentNeighboursReducer();

        LocalYield<YTreeMapNode> result = YtTestHelper.testOneOfProtoReducerWithYsonOutput(reducer, input, null);

        ListF<ComponentNeighbours> records = result.getRecsByIndex(0).map(r -> reducer.parse(r, ComponentNeighbours.class));

        assertEquals(1, records.size());

        ComponentNeighbours record0 = records.get(0);
        assertEquals(cc3, record0.getCryptaId());
        assertEquals(Cf.list(cc1, cc2), record0.getNeighbours().toList().sorted());

    }
}
