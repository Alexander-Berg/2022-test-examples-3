package ru.yandex.crypta.graph2.matching.human.workflow.merge_apply.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKeyWithNewCryptaIds;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class ApplyMergeDecisionToEdgesBetweenReducerTest {
    @Test
    public void reduceTest() {

        ApplyMergeDecisionToEdgesBetweenReducer reducer = new ApplyMergeDecisionToEdgesBetweenReducer();

        String cryptaIdOld1 = "old1";
        String cryptaIdOld2 = "old2";

        String cryptaIdNew1 = "new1";
        String cryptaIdNew2 = "new2";

        String cryptaIdOldMerged1 = "oldmerged1";
        String cryptaIdOldMerged2 = "oldmerged2";

        String cryptaIdNewMerged = "newmerge";

        MergeKey mk = MergeKey.betweenComponents(cryptaIdOld1, cryptaIdOld2);
        MergeKeyWithNewCryptaIds mkWithNewLeftCryptaId =
                new MergeKeyWithNewCryptaIds(mk, cryptaIdOld1)
                        .updateFromTo(cryptaIdOld1, cryptaIdNew1);
        MergeKeyWithNewCryptaIds mkWithOldCryptaId =
                new MergeKeyWithNewCryptaIds(mk, cryptaIdOld2);
        MergeKeyWithNewCryptaIds mkWithNewRightCryptaId =
                new MergeKeyWithNewCryptaIds(mk, cryptaIdOld1)
                        .updateFromTo(cryptaIdOld2, cryptaIdNew2);

        MergeKey mkMerged = MergeKey.betweenComponents(cryptaIdOldMerged1, cryptaIdOldMerged2);
        MergeKeyWithNewCryptaIds mkMergedWithNewCryptaId =
                new MergeKeyWithNewCryptaIds(mkMerged, cryptaIdNewMerged)
                        .updateFromTo(cryptaIdOldMerged1, cryptaIdNewMerged);
        MergeKeyWithNewCryptaIds mkMergedWithNewCryptaIdRight =
                new MergeKeyWithNewCryptaIds(mkMerged, cryptaIdNewMerged)
                        .updateFromTo(cryptaIdOldMerged2, cryptaIdNewMerged);

        Edge edge1 = new Edge(
                "111", EIdType.YANDEXUID,
                "d1", EIdType.IDFA,
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list()
        );
        Edge edge2 = new Edge(
                "123", EIdType.YANDEXUID,
                "234", EIdType.IDFA,
                ESourceType.APP_METRICA,
                ELogSourceType.METRIKA_MOBILE_LOG,
                Cf.list()
        );
        EdgeBetweenComponents edgeBetween = new EdgeBetweenComponents(edge1, mk, cryptaIdOld1, cryptaIdOld2);
        EdgeBetweenComponents edgeMerged = new EdgeBetweenComponents(edge2, mkMerged, cryptaIdOldMerged2,
                cryptaIdOldMerged1);

        ListF<MergeKeyWithNewCryptaIds> mergeKeys = Cf.list(
                mkWithNewLeftCryptaId, mkWithNewRightCryptaId, mkWithOldCryptaId,
                mkMergedWithNewCryptaId, mkMergedWithNewCryptaIdRight
        );
        ListF<EdgeBetweenComponents> edges = Cf.list(edgeBetween, edgeMerged);

        ListF<YTreeMapNode> inRecs = Cf.list(
                YtTestHelper.toYsonRecs(reducer, mergeKeys, 0),
                YtTestHelper.toYsonRecs(reducer, edges, 1)
        ).flatten();

        LocalYield<YTreeMapNode> result = YtTestHelper.testReducer(reducer, inRecs);

        ListF<EdgeInComponent> edgesIn =
                YtTestHelper.fromYsonRecs(reducer, EdgeInComponent.class, result, 0);
        ListF<EdgeBetweenWithNewCryptaIds> edgesBetween =
                YtTestHelper.fromYsonRecs(reducer, EdgeBetweenWithNewCryptaIds.class, result, 1);

        assertEquals(1, edgesIn.size());
        assertEquals(2, edgesBetween.size());

        assertEquals(cryptaIdNewMerged, edgesIn.getO(0).get().getCryptaId());

        assertEquals(cryptaIdNew1, edgesBetween.get(0).getMergeKey().getNewLeftCryptaId());
        assertEquals(cryptaIdNew2, edgesBetween.get(0).getMergeKey().getNewRightCryptaId());
        assertEquals(mk, edgesBetween.get(0).getMergeKey().getMergeKey());
        assertEquals(mk, edgesBetween.get(1).getMergeKey().getMergeKey());
    }

    @Test
    public void testOppositeEdgeBetween() {
        // leftCryptaId > rightCryptaId in edge
        // Opposite because for merge_key lefCryptaId <= rightCryptaId.

        ApplyMergeDecisionToEdgesBetweenReducer reducer = new ApplyMergeDecisionToEdgesBetweenReducer();

        String cryptaIdLeft = "c2";
        String cryptaIdRight = "c1";

        String cryptaIdRightNew = "c3";

        MergeKey mk = MergeKey.betweenComponents(cryptaIdLeft, cryptaIdRight);
        MergeKeyWithNewCryptaIds mkWithNewCryptaId =
                new MergeKeyWithNewCryptaIds(mk, cryptaIdLeft)
                        .updateFromTo(cryptaIdRight, cryptaIdRightNew);

        assertEquals(cryptaIdLeft, mkWithNewCryptaId.getRightCryptaId());  // because c1 < c2

        Edge edge = new Edge(
                "111", EIdType.YANDEXUID,
                "d1", EIdType.IDFA,
                ESourceType.APP_METRICA, ELogSourceType.METRIKA_MOBILE_LOG, Cf.list()
        );
        EdgeBetweenComponents edgeBetween = new EdgeBetweenComponents(edge, mk, cryptaIdLeft, cryptaIdRight); //
        // opposite
        ListF<MergeKeyWithNewCryptaIds> mergeKeys = Cf.list(mkWithNewCryptaId);
        ListF<EdgeBetweenComponents> edges = Cf.list(edgeBetween);

        ListF<YTreeMapNode> inRecs = Cf.list(
                YtTestHelper.toYsonRecs(reducer, mergeKeys, 0),
                YtTestHelper.toYsonRecs(reducer, edges, 1)
        ).flatten();

        LocalYield<YTreeMapNode> result = YtTestHelper.testReducer(reducer, inRecs);
        ListF<EdgeBetweenWithNewCryptaIds> edgesBetween =
                YtTestHelper.fromYsonRecs(reducer, EdgeBetweenWithNewCryptaIds.class, result, 1);

        assertEquals(2, edgesBetween.size());

        EdgeBetweenWithNewCryptaIds edgeOut = edgesBetween.get(0);
        assertEquals(mk, edgeOut.getMergeKey().getMergeKey());
        assertEquals(cryptaIdRightNew, edgeOut.getMergeKey().getNewLeftCryptaId());
        assertEquals(cryptaIdLeft, edgeOut.getMergeKey().getNewRightCryptaId());

        assertTrue(edgeOut.isOpposite());
    }
}
