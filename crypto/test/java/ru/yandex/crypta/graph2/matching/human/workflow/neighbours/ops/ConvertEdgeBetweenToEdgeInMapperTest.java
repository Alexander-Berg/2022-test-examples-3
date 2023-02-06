package ru.yandex.crypta.graph2.matching.human.workflow.neighbours.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.proto.EdgeInComponent;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;

import static org.junit.Assert.assertEquals;

public class ConvertEdgeBetweenToEdgeInMapperTest {
    @Test
    public void mapperTest() throws Exception {

        String cc1 = "cc1";
        String cc2 = "cc2";

        String mk = MergeKey.betweenComponents(cc1, cc2).getMergeKey();
        EdgeBetweenComponents edge = EdgeBetweenComponents.newBuilder()
                .setLeftCryptaId(cc1)
                .setRightCryptaId(cc2)
                .setMergeKey(mk)
                .build();
        ListF<EdgeBetweenComponents> input = Cf.list(edge);
        ConvertEdgeBetweenToEdgeInMapper mapper = new ConvertEdgeBetweenToEdgeInMapper();

        LocalYield<EdgeInComponent> result = YtTestHelper.testMapper(mapper, input);
        ListF<EdgeInComponent> outEdges = result.getRecsByIndex(0).sortedBy(EdgeInComponent::getCryptaId);

        assertEquals(2, outEdges.size());
        assertEquals(cc1, outEdges.get(0).getCryptaId());
        assertEquals(cc2, outEdges.get(1).getCryptaId());


        assertEquals(mk, outEdges.get(0).getMergeKey());
        assertEquals(mk, outEdges.get(1).getMergeKey());
    }
}
