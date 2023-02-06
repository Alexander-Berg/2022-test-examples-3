package ru.yandex.crypta.graph2.matching.human.workflow.merge_apply.ops;

import java.util.HashMap;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.matching.merge.MergeNeighbour;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferStatus;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeScore;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;


public class AddChangedCryptaIdReducerTest {
    @Test
    public void reduceTest() {

        String cryptaidNew = "new";
        String cryptaidOld = "old";

        String cryptaidNeighbourLeft = "left"; // mergeKey with old : left_old
        String cryptaidNeighbourRight = "right"; // mergeKey with old : old_right

        MergeKey mkOldLeft = MergeKey.betweenComponents(cryptaidOld, cryptaidNeighbourLeft);
        MergeKey mkOldRight = MergeKey.betweenComponents(cryptaidOld, cryptaidNeighbourRight);

        MergeOffer mergeOffer1 = new MergeOffer(
                MergeKey.EMPTY,
                cryptaidOld,
                cryptaidNew,
                Option.of(new MetricsTree(1.0)),
                new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, 0)
        ).withStatus(MergeOfferStatus.INIT);
        MergeNeighbour mergeNeighbourLeft = new MergeNeighbour(cryptaidOld, mkOldLeft);
        MergeNeighbour mergeNeighbourLeftRevert = new MergeNeighbour(cryptaidNeighbourLeft, mkOldLeft);
        MergeNeighbour mergeNeighbourRight = new MergeNeighbour(cryptaidOld, mkOldRight);
        MergeNeighbour mergeNeighbourRightRevert = new MergeNeighbour(cryptaidNeighbourRight, mkOldRight);

        AddChangedCryptaIdReducer reducer = new AddChangedCryptaIdReducer();


        ListF<MergeOffer> mergeOffers = Cf.list(mergeOffer1);
        ListF<MergeNeighbour> mergeNeighbours = Cf.list(
                mergeNeighbourLeft, mergeNeighbourLeftRevert, mergeNeighbourRight, mergeNeighbourRightRevert
        );

        ListF<YTreeMapNode> inRecs = Cf.list(
                YtTestHelper.toYsonRecs(reducer, mergeOffers, 0),
                YtTestHelper.toYsonRecs(reducer, mergeNeighbours, 1)
        ).flatten();

        LocalYield<YTreeMapNode> result = YtTestHelper.testReducer(reducer, inRecs);

        ListF<MergeKeyWithNewCryptaIds> mergeKeyWithNewCryptaIds = YtTestHelper.fromYsonRecs(reducer,
                MergeKeyWithNewCryptaIds.class, result, 0);

        HashMap<MergeKey, ListF<MergeKey>> mergeKeys = new HashMap<>();
        for (MergeKeyWithNewCryptaIds mk : mergeKeyWithNewCryptaIds) {
            MergeKey mergeKey = mk.getMergeKey();
            MergeKey newMergeKey = MergeKey.betweenComponents(mk.getNewLeftCryptaId(), mk.getNewRightCryptaId());

            if (mergeKeys.containsKey(mergeKey)) {
                mergeKeys.put(mergeKey, mergeKeys.get(mergeKey).plus(newMergeKey));
            } else {
                mergeKeys.put(mergeKey, Cf.list(newMergeKey));
            }
        }

        assertEquals(2, mergeKeys.size());

        assertEquals(
                Cf.list(
                        MergeKey.betweenComponents(cryptaidNeighbourLeft, cryptaidNew),
                        mkOldLeft
                ),
                mergeKeys.get(mkOldLeft).sorted()
        );

        assertEquals(
                Cf.list(
                        MergeKey.betweenComponents(cryptaidNeighbourRight, cryptaidNew),
                        mkOldRight
                ),
                mergeKeys.get(mkOldRight).sorted()
        );
    }
}
