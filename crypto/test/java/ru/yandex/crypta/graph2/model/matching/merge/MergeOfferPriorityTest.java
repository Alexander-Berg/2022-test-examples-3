package ru.yandex.crypta.graph2.model.matching.merge;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.model.matching.helper.DatesActivityEdgeInfoProvider;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeScore;
import ru.yandex.crypta.graph2.testlib.Permutations;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static ru.yandex.crypta.graph.soup.config.proto.ELogSourceType.SOUP_PREPROCESSING;
import static ru.yandex.crypta.graph.soup.config.proto.ELogSourceType.WEBVISOR_LOG;
import static ru.yandex.crypta.graph.soup.config.proto.ESourceType.MD5_HASH;
import static ru.yandex.crypta.graph.soup.config.proto.ESourceType.WEBVISOR;


public class MergeOfferPriorityTest {


    @Test
    public void sortMergeOfferByActivityAsc() throws Exception {

        DatesActivityEdgeInfoProvider edgeWeightProvider = new DatesActivityEdgeInfoProvider();
        MergeOfferPriority mergeOfferPriority = new MergeOfferPriority();

        Edge usualEdge3 = new Edge("v1", EIdType.YANDEXUID, "v2", EIdType.EMAIL,
                WEBVISOR, WEBVISOR_LOG,
                Cf.list("d1", "d2", "d3"));

        Edge usualEdge4 = new Edge("v3", EIdType.YANDEXUID, "v4", EIdType.EMAIL,
                WEBVISOR, WEBVISOR_LOG,
                Cf.list("d1", "d2", "d3", "d4"));

        Edge usualEdge0 = new Edge("v3", EIdType.YANDEXUID, "v5", EIdType.EMAIL,
                WEBVISOR, WEBVISOR_LOG,
                Cf.list());

        Edge artificialEdge0 = new Edge("v1", EIdType.PHONE, "v2", EIdType.PHONE_MD5,
                MD5_HASH, SOUP_PREPROCESSING,
                Cf.list("d1"));

        String cc1 = "cc1";
        String cc2 = "cc2";
        String cc3 = "cc3";


        EdgeScore edgeScore1 = edgeWeightProvider.getMultiEdgeScore(Cf.list(usualEdge0, usualEdge3));
        MergeOffer mergeOfferByUsualEdges = new MergeOffer(MergeKey.EMPTY, cc1, cc2, Option.empty(), edgeScore1);

        EdgeScore edgeScore2 = edgeWeightProvider.getMultiEdgeScore(Cf.list(usualEdge3, usualEdge4));
        MergeOffer mergeOfferByStrongerUsualEdges =
                new MergeOffer(MergeKey.EMPTY, cc1, cc3, Option.empty(), edgeScore2);

        EdgeScore edgeScore3 = edgeWeightProvider.getMultiEdgeScore(Cf.list(usualEdge0, artificialEdge0));
        MergeOffer mergeOfferWithArtificialEdge = new MergeOffer(MergeKey.EMPTY, cc1, cc3, Option.empty(), edgeScore3);

        MergeOffer mergeOfferByVertex = new MergeOffer(MergeKey.EMPTY, cc1, cc3, Option.empty(),
                new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, 100));

        ListF<MergeOffer> expected = Cf.list(
                mergeOfferByUsualEdges,
                mergeOfferByStrongerUsualEdges,
                mergeOfferByVertex,
                mergeOfferWithArtificialEdge
        );

        for (ListF<MergeOffer> offersSorting : Permutations.allPermutations(
                mergeOfferByVertex, mergeOfferByUsualEdges, mergeOfferByStrongerUsualEdges,
                mergeOfferWithArtificialEdge)) {
            ListF<MergeOffer> actual = mergeOfferPriority.sortMergeOfferByActivityAsc(offersSorting);
            Assert.assertListsEqual(expected, actual);
        }

    }

    @Test
    public void testSuccessStatusGoesFirst() {

        MergeOffer o1 = standardOffer("starCenter", "cc1");
        MergeOffer o2 = standardOffer("starCenter", "cc2");
        MergeOffer o3 = standardOffer("starCenter", "cc3");
        MergeOffer o4 = standardOffer("starCenter", "cc4");
        MergeOffer o5 = weightedMergeOffer("starCenter", "cc5", 2.0);
        ListF<MergeOffer> offers = Cf.list(
                o1,
                o2.withStatus(MergeOfferStatus.DECISION_REJECTED),
                o3.withStatus(MergeOfferStatus.DECISION_FOLLOWER),
                o4,
                o5.withStatus(MergeOfferStatus.DECISION_REJECTED)
        );

        MergeOfferPriority mergeOfferPriority = new MergeOfferPriority();
        ListF<MergeOffer> result = mergeOfferPriority.sortMergeOfferByActivityDesc(offers);

        Assert.assertListsEqual(Cf.list(
                o5, // weighted goes first
                o3, // in case of equals weights, ok goes next
                o2, // in case of equals weights, failed goes after of
                o4, // null safety and deterministic order
                o1
        ), result);
    }

    @Test
    public void testSingleVertexForceMerge() {

        MergeOffer o1 = standardOffer("starCenter", "cc1");
        MergeOffer o2 = standardOffer("starCenter", "cc2");
        MergeOffer o3 = standardOffer("starCenter", "cc3");
        MergeOffer o4 = standardOffer("starCenter", "cc4");
        MergeOffer o5 = weightedMergeOffer("starCenter", "cc5", 2.0);

        ListF<MergeOffer> offers = Cf.list(
                o1,
                o2.withStatus(MergeOfferStatus.DECISION_REJECTED),
                o3.withStatus(MergeOfferStatus.DECISION_FOLLOWER),
                o4,
                o5.withStatus(MergeOfferStatus.DECISION_REJECTED)
        );

        o1.setFromCryptaIdNeighboursCount(1);
        o1.setFromCryptaIdComponentSize(1);

        MergeOfferPriority mergeOfferPriority = new MergeOfferPriority(true);
        ListF<MergeOffer> result = mergeOfferPriority.sortMergeOfferByActivityDesc(offers);

        Assert.assertListsEqual(Cf.list(
                o1, // single vertex force merge
                o5, // weighted goes first9
                o3, // in case of equals weights, ok goes next
                o2, // in case of equals weights, failed goes after of
                o4  // null safety and deterministic order
        ), result);
    }

    private MergeOffer standardOffer(String cc1, String cc2) {
        return weightedMergeOffer(cc1, cc2, 1.0);
    }

    private MergeOffer weightedMergeOffer(String cc1, String cc2, double weight) {
        return new MergeOffer(MergeKey.betweenComponents(cc1, cc2),
                cc1, cc2,
                Option.of(new MetricsTree(1.0)),
                new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, weight)
        );
    }
}
