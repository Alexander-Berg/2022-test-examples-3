package ru.yandex.crypta.graph2.matching.human.workflow.neighbours.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.component.ComponentNeighbours;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferStatus;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeScore;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;

import static org.junit.Assert.assertEquals;

public class CalculateComponentNeighboursTest {
    @Test
    public void testCalculateNeighbours() throws Exception {

        String cc1 = "cc1";
        String cc2 = "cc2";
        String cc3 = "cc3";

        EdgeScore fakeScore = new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, 0);

        MergeOffer mergeOffer1 =
                new MergeOffer(MergeKey.EMPTY, cc1, cc2, Option.empty(), fakeScore).withStatus(MergeOfferStatus.INIT);
        MergeOffer mergeOffer2 =
                new MergeOffer(MergeKey.EMPTY, cc1, cc3, Option.empty(), fakeScore).withStatus(MergeOfferStatus.INIT);

        CalculateComponentNeighbours reducer = new CalculateComponentNeighbours();

        ListF<MergeOffer> recs = Cf.list(mergeOffer1, mergeOffer2);
        LocalYield<ComponentNeighbours> result = YtTestHelper.testReducer(reducer, recs);

        ListF<ComponentNeighbours> componentNeighbours = result.getAllRecs();
        assertEquals(1, componentNeighbours.size());
        ComponentNeighbours componentNeighbours1 = componentNeighbours.first();

        assertEquals(cc1, componentNeighbours1.getCryptaId());
        assertEquals(Cf.list(cc2, cc3).sorted(),
                componentNeighbours1.getNeighbours().sorted());

    }

}
