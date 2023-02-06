package ru.yandex.crypta.graph2.matching.human.workflow.neighbours.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeBetweenComponents;
import ru.yandex.crypta.graph2.model.matching.edge.EdgeInComponent;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferStatus;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.MultiEdgeKey;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeScore;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;

import static org.junit.Assert.assertEquals;

public class ProcessMergeOffersTest {

    @Test
    public void reduce() {
        ProcessMergeOffers reducer = new ProcessMergeOffers();

        String c1 = "c1";
        String c2 = "c2";
        String changedCryptaId = "c3";

        Edge e1 = new Edge("x1", EIdType.YANDEXUID, "x2", EIdType.IDFA, ESourceType.APP_METRICA_SDK,
                ELogSourceType.ACCESS_LOG, Cf.list());
        Edge e2 = new Edge("x1", EIdType.YANDEXUID, "x2", EIdType.IDFA, ESourceType.APP_URL_REDIR,
                ELogSourceType.ACCESS_LOG, Cf.list());

        MultiEdgeKey multiEdgeKey = e1.getMultiEdgeKey();

        MergeKey mergeKey = MergeKey.fromEdge(multiEdgeKey);
        MergeOffer mergeOffer = new MergeOffer(
                mergeKey,
                c1, c2,
                Option.empty(), new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, 0)
        ).withStatus(MergeOfferStatus.CONFIRMED);
        MergeOffer oppositeOffer = mergeOffer.opposite().withStatus(MergeOfferStatus.NOT_CONFIRMED);
        oppositeOffer.setFromCryptaId(changedCryptaId);

        ListF<YTreeMapNode> mergeOffersRecs = YtTestHelper.toYsonRecs(reducer, Cf.list(
                mergeOffer,
                oppositeOffer
        ), 0);

        ListF<YTreeMapNode> edgeRecs = YtTestHelper.toYsonRecs(reducer, Cf.list(
                EdgeBetweenComponents.fromEdge(e1, c1, c2).withMergeKey(mergeKey),
                EdgeBetweenComponents.fromEdge(e2, c1, c2).withMergeKey(mergeKey)
        ), 1);

        LocalYield<YTreeMapNode> result = YtTestHelper.testReducer(reducer, mergeOffersRecs.plus(edgeRecs));

        // check merge offers
        ListF<MergeOffer> outMergeOffers = YtTestHelper.fromYsonRecs(reducer, MergeOffer.class, result, 0);

        ListF<MergeOffer> outOffer1 = outMergeOffers.filter(mo ->
                mo.getFromCryptaId().equals(c1) && mo.getToCryptaId().equals(changedCryptaId)
        );
        assertEquals(1, outOffer1.size());

        ListF<MergeOffer> outOffer2 = outMergeOffers.filter(mo ->
                mo.getFromCryptaId().equals(changedCryptaId) && mo.getToCryptaId().equals(c1)
        );
        assertEquals(1, outOffer2.size());

        // check edges between
        ListF<EdgeInComponent> outEdges = YtTestHelper.fromYsonRecs(reducer, EdgeInComponent.class, result, 1);
        assertEquals(4, outEdges.size());

        assertEquals(2, outEdges.filter(e ->
                e.getCryptaId().equals(c1)
        ).size());

        assertEquals(2, outEdges.filter(e ->
                e.getCryptaId().equals(changedCryptaId)
        ).size());
    }
}
