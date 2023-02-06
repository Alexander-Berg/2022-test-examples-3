package ru.yandex.crypta.graph2.matching.human.workflow.merge.ops;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.model.matching.component.ComponentCenter;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferPriority;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeScore;
import ru.yandex.crypta.graph2.testlib.YtTestHelper;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MergeConsensusTest {

    private MergeOfferPriority mergeOfferPriority = new MergeOfferPriority(true);

    @Test
    public void testSingleEdgeConsensus() {

        // equal components
        testSingleEdgeConsensus(3, 0, 3, 0);

        // left is bigger
        testSingleEdgeConsensus(3, 10, 3, 0);

        // right is bigger
        testSingleEdgeConsensus(3, 100, 3, 200);

        // left is single
        testSingleEdgeConsensus(1, 1, 2, 2);

        // right is single
        testSingleEdgeConsensus(1, 1, 2, 2);

        // both are single
        testSingleEdgeConsensus(1, 1, 1, 1);
    }

    private void testSingleEdgeConsensus(int fromComponentSize, int fromNeighboursCount,
                                         int toComponentSize, int toNeighboursCount) {
        MergeOffer offer1 = weightedMergeOffer(
                "c1", fromComponentSize, fromNeighboursCount,
                "c2", toComponentSize, toNeighboursCount,
                1.0
        );

        ListF<MergeOffer> offers = Cf.list(
                offer1,
                offer1.opposite()
        );

        LocalYield<MergeOffer> yield2 = applyConsensus(offers);

        ListF<MergeOffer> mergeDecisions = yield2.getRecsByIndex(0);
        ListF<MergeOffer> okStatuses = yield2.getRecsByIndex(1);
        ListF<MergeOffer> notOkStatuses = yield2.getRecsByIndex(2);

        System.out.println(mergeDecisions);
        System.out.println(okStatuses);
        System.out.println(notOkStatuses);


        assertEquals(1, mergeDecisions.size());
        assertEquals(2, okStatuses.size());
        assertEquals(0, notOkStatuses.size());
    }

    @Test
    public void testIncreasingLineOffersConsensus() {

        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 10);
        ComponentCenter cc2 = new ComponentCenter("cc2", EIdType.UUID, 20);
        ComponentCenter cc3 = new ComponentCenter("cc3", EIdType.UUID, 30);
        ComponentCenter cc4 = new ComponentCenter("cc4", EIdType.UUID, 20);


        MergeOffer offer1 = standardOffer(cc1, cc2);
        MergeOffer offer2 = standardOffer(cc2, cc3);
        MergeOffer offer3 = standardOffer(cc3, cc4);
        ListF<MergeOffer> lineTopology = Cf.list(
                offer1,
                offer1.opposite(),
                offer2,
                offer2.opposite(),
                offer3,
                offer3.opposite()
        );

        LocalYield<MergeOffer> yield2 = applyConsensus(lineTopology);

        ListF<MergeOffer> mergeDecisions = yield2.getRecsByIndex(0);
        ListF<MergeOffer> okStatuses = yield2.getRecsByIndex(1);
        ListF<MergeOffer> notOkStatuses = yield2.getRecsByIndex(2);

        System.out.println(mergeDecisions);
        System.out.println(okStatuses);
        System.out.println(notOkStatuses);

        // only decisions to the most connected (cc3) should be approved
        SetF<String> toCryptaId = mergeDecisions.map(MergeOffer::getToCryptaId).unique();
        assertEquals(1, toCryptaId.size());
        assertEquals(cc3.getCcId(), toCryptaId.single());

        SetF<String> mergedCryptaIds = okStatuses.flatMap(
                ok -> Cf.list(ok.getFromCryptaId(), ok.getToCryptaId())
        ).unique();

        assertEquals(Cf.list(cc2.getCcId(), cc3.getCcId(), cc4.getCcId()), mergedCryptaIds.sorted());

        SetF<String> notMergedCryptaIds = notOkStatuses.flatMap(
                ok -> Cf.list(ok.getFromCryptaId(), ok.getToCryptaId())
        ).unique();

        assertEquals(Cf.list(cc1.getCcId(), cc2.getCcId()), notMergedCryptaIds.sorted());

        assertEquals(2, mergeDecisions.size());
        assertEquals(4, okStatuses.size());  // direct an opposite decisions
        assertEquals(2, notOkStatuses.size());  // direct an opposite decisions

    }

    @Test
    public void fullStarMerge() {

        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 3);
        ComponentCenter cc2 = new ComponentCenter("cc2", EIdType.UUID, 1);
        ComponentCenter cc3 = new ComponentCenter("cc3", EIdType.UUID, 1);
        ComponentCenter cc4 = new ComponentCenter("cc4", EIdType.UUID, 1);


        MergeOffer offer1 = standardOffer(cc1, cc2);
        MergeOffer offer2 = standardOffer(cc1, cc3);
        MergeOffer offer3 = standardOffer(cc1, cc4);
        ListF<MergeOffer> starTopology = Cf.list(
                offer1,
                offer1.opposite(),
                offer2,
                offer2.opposite(),
                offer3,
                offer3.opposite()
        );

        LocalYield<MergeOffer> yield2 = applyConsensus(starTopology);

        ListF<MergeOffer> mergeDecisions = yield2.getRecsByIndex(0);
        ListF<MergeOffer> okStatuses = yield2.getRecsByIndex(1);
        ListF<MergeOffer> notOkStatuses = yield2.getRecsByIndex(2);

        System.out.println(mergeDecisions);
        System.out.println(okStatuses);
        System.out.println(notOkStatuses);

        // everybody is connected to the center of star
        SetF<String> toCryptaId = mergeDecisions.map(MergeOffer::getToCryptaId).unique();
        assertEquals(1, toCryptaId.size());
        assertEquals(cc1.getCcId(), toCryptaId.single());

        SetF<String> mergedCryptaIds = okStatuses.flatMap(
                ok -> Cf.list(ok.getFromCryptaId(), ok.getToCryptaId())
        ).unique();

        assertEquals(Cf.list(cc1.getCcId(), cc2.getCcId(), cc3.getCcId(), cc4.getCcId()), mergedCryptaIds.sorted());

        assertEquals(3, mergeDecisions.size());
        assertEquals(6, okStatuses.size());  // direct an opposite decisions
        assertEquals(0, notOkStatuses.size());  // direct an opposite decisions

    }

    @Test
    public void partialStarMerge() {
        // there is a conflicting offer from single-vertex neighbour component
        // this offer should be accepted because of link weight, and star must be merged only partially
        partialStarMerge(2, 1); // less than decider
        partialStarMerge(2, 2); // equals to decider
        partialStarMerge(2, 3); // more than decider
    }

    private void partialStarMerge(int deciderNeighboursCount, int competitorNeighboursCount) {

        ComponentCenter starCenter = new ComponentCenter("cc1", EIdType.UUID, 3);
        ComponentCenter cc2 = new ComponentCenter("cc2", EIdType.UUID, 1);
        ComponentCenter cc4 = new ComponentCenter("cc4", EIdType.UUID, 1);
        ComponentCenter decider = new ComponentCenter("decider", EIdType.UUID, deciderNeighboursCount);
        ComponentCenter competitor = new ComponentCenter("competitor", EIdType.UUID, competitorNeighboursCount);

        MergeOffer starOffer1 = weightedMergeOffer(starCenter, cc2, 0.1);
        MergeOffer starOffer2 = weightedMergeOffer(starCenter, decider, 0.2);
        MergeOffer starOffer3 = weightedMergeOffer(starCenter, cc4, 0.3);

        MergeOffer conflictOffer = weightedMergeOffer(decider, competitor, 0.5);
        ListF<MergeOffer> starWithConflictTopology = Cf.list(
                starOffer1,
                starOffer1.opposite(),
                starOffer2,
                starOffer2.opposite(),
                starOffer3,
                starOffer3.opposite(),
                conflictOffer,
                conflictOffer.opposite()
        );

        LocalYield<MergeOffer> yield2 = applyConsensus(starWithConflictTopology);

        ListF<MergeOffer> mergeDecisions = yield2.getRecsByIndex(0);
        ListF<MergeOffer> okStatuses = yield2.getRecsByIndex(1);
        ListF<MergeOffer> notOkStatuses = yield2.getRecsByIndex(2);

        System.out.println(mergeDecisions);
        System.out.println(okStatuses);
        System.out.println(notOkStatuses);

        MergeOffer conflictDecisionDirection = (deciderNeighboursCount >= competitorNeighboursCount)
                ? conflictOffer.opposite()
                : conflictOffer;

        // only first star offer and out-of-star offer are confirmed
        ListF<MergeOffer> confirmed = Cf.list(starOffer3.opposite(), conflictDecisionDirection).sorted();
        Assert.assertListsEqual(confirmed, mergeDecisions.sorted());

        // two other star offers are not confirmed
        assertTrue(notOkStatuses.containsAllTs(Cf.list(starOffer1, starOffer2)));

        assertEquals(2, mergeDecisions.size());
        assertEquals(4, okStatuses.size());  // direct an opposite decisions
        assertEquals(4, notOkStatuses.size());  // direct an opposite decisions

    }

    @Test
    public void partialStarMergeWithForceSingleComponentsMerge() {
        MergeOffer starOffer1 = weightedMergeOffer(
                "center", 1, 4,
                "cc1", 1, 1, 2
        );
        MergeOffer starOffer2 = weightedMergeOffer(
                "center", 1, 4,
                "cc2", 1, 1, 2
        );
        MergeOffer starOffer3 = weightedMergeOffer(
                "center", 1, 4,
                "cc3", 1, 1, 2
        );
        MergeOffer conflictOffer = weightedMergeOffer(
                "center", 1, 4,
                "large_component", 7, 5, 4
        );

        ListF<MergeOffer> topology = Cf.list(
                starOffer1,
                starOffer1.opposite(),
                starOffer2,
                starOffer2.opposite(),
                starOffer3,
                starOffer3.opposite(),
                conflictOffer,
                conflictOffer.opposite()
        );

        // single vertices aren't merged, because large component is better
        LocalYield<MergeOffer> yield = applyConsensus(topology, new MergeOfferPriority());
        ListF<MergeOffer> mergeDecisions = yield.getRecsByIndex(0);

        assertEquals(1, mergeDecisions.size());
        assertEquals("center", mergeDecisions.get(0).getFromCryptaId());
        assertEquals("large_component", mergeDecisions.get(0).getToCryptaId());

        // merge all single vertices even in case of conflict
        LocalYield<MergeOffer> yield2 = applyConsensus(topology, new MergeOfferPriority(true));
        ListF<MergeOffer> mergeDecisions2 = yield2.getRecsByIndex(0);

        // all three single components are merged
        assertEquals(3, mergeDecisions2.size());
        String singleLeader = mergeDecisions2
                .stream()
                .map(MergeOffer::getToCryptaId)
                .collect(toSet())
                .iterator().next();
        assertEquals("center", singleLeader);

    }

    @Test
    public void twoStarsMerge() {

        ListF<Double> star1Weights = Cf.list(1.0, 2.0, 3.0);
        ListF<Double> star2Weights = Cf.list(1.0, 2.0, 3.0);
        ListF<Double> linkWeights = Cf.list(1.0, 2.0, 3.0);
        ListF<Boolean> intermediate = Cf.list(true, false);

        int itersSum = 0;
        int testsCount = 0;
        for (Double star1Weight : star1Weights) {
            for (Double star2Weight : star2Weights) {
                for (Double linkWeight : linkWeights) {
                    for (Boolean i : intermediate) {
                        itersSum += twoStarsMerge(star1Weight, star2Weight, linkWeight, i);
                        testsCount++;
                    }
                }
            }
        }

        double averageConvergence = (double) itersSum / testsCount;
        System.out.println("Average convergence: " + averageConvergence);
        Assert.equals(2.648, averageConvergence, 0.01);
    }

    private int twoStarsMerge(double star1Weights, double star2Weights, double linkWeight,
                              boolean linkWithIntermediateNode) {
        ComponentCenter star1Center = new ComponentCenter("star1", EIdType.UUID, 3);
        ComponentCenter star1c1 = new ComponentCenter("c11", EIdType.UUID, 2);
        ComponentCenter star1c2 = new ComponentCenter("c12", EIdType.UUID, 1);
        ComponentCenter star1c3 = new ComponentCenter("c13", EIdType.UUID, 1);
        ListF<MergeOffer> star1Offers = Cf.list(
                weightedMergeOffer(star1Center, star1c1, star1Weights),
                weightedMergeOffer(star1Center, star1c1, star1Weights).opposite(),
                weightedMergeOffer(star1Center, star1c2, star1Weights),
                weightedMergeOffer(star1Center, star1c2, star1Weights).opposite(),
                weightedMergeOffer(star1Center, star1c3, star1Weights),
                weightedMergeOffer(star1Center, star1c3, star1Weights).opposite()
        );

        ComponentCenter star2Center = new ComponentCenter("star2", EIdType.UUID, 3);
        ComponentCenter star2c1 = new ComponentCenter("c21", EIdType.UUID, 2);
        ComponentCenter star2c2 = new ComponentCenter("c22", EIdType.UUID, 1);
        ComponentCenter star2c3 = new ComponentCenter("c23", EIdType.UUID, 1);
        ListF<MergeOffer> star2Offers = Cf.list(
                weightedMergeOffer(star2Center, star2c1, star2Weights),
                weightedMergeOffer(star2Center, star2c1, star2Weights).opposite(),
                weightedMergeOffer(star2Center, star2c2, star2Weights),
                weightedMergeOffer(star2Center, star2c2, star2Weights).opposite(),
                weightedMergeOffer(star2Center, star2c3, star2Weights),
                weightedMergeOffer(star2Center, star2c3, star2Weights).opposite()
        );

        ListF<MergeOffer> linkBetweenStars;
        if (linkWithIntermediateNode) {
            ComponentCenter intermediateNode = new ComponentCenter("inter", EIdType.UUID, 2);
            linkBetweenStars = Cf.list(
                    weightedMergeOffer(star1c1, intermediateNode, linkWeight),
                    weightedMergeOffer(star1c1, intermediateNode, linkWeight).opposite(),
                    weightedMergeOffer(intermediateNode, star2c1, linkWeight),
                    weightedMergeOffer(intermediateNode, star2c1, linkWeight).opposite()
            );
        } else {
            linkBetweenStars = Cf.list(
                    weightedMergeOffer(star1c1, star2c1, linkWeight),
                    weightedMergeOffer(star1c1, star2c1, linkWeight).opposite()
            );
        }

        ListF<MergeOffer> offersLeft = star1Offers.plus(star2Offers.plus(linkBetweenStars));

        int iter = 0;
        // all cases converge within 3 consensus iterations
        while (offersLeft.isNotEmpty() && iter < 4) {

            iter++;

            LocalYield<MergeOffer> yield = applyConsensus(offersLeft);

            ListF<MergeOffer> decisions = yield.getRecsByIndex(0);
            Assert.isTrue(decisions.size() > 0);

            ListF<MergeOffer> confirmedOffers = yield.getRecsByIndex(1);
            Assert.isTrue(confirmedOffers.size() > 0);
            Assert.isTrue(confirmedOffers.size() % 2 == 0);  // for both direct and opposite

            ListF<MergeOffer> unconfirmedOffers = yield.getRecsByIndex(2);
            Assert.isTrue(unconfirmedOffers.size() % 2 == 0);  // for both direct and opposite

            offersLeft = unconfirmedOffers; // keep all unconfirmed for next iter
        }

        Assert.equals(0, offersLeft.size(), offersLeft.mkString("\n"));

        return iter;

    }

    @Test
    public void testEqualTriangleConsensus() {

        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 3);
        ComponentCenter cc2 = new ComponentCenter("cc2", EIdType.UUID, 3);
        ComponentCenter cc3 = new ComponentCenter("cc3", EIdType.UUID, 3);

        // both cc2 and cc3 think they are leaders in compare to cc1 just because they are alphabetically better
        MergeOffer offer1 = weightedMergeOffer(cc1, cc2, 3.0);
        MergeOffer offer2 = weightedMergeOffer(cc2, cc3, 0.0);
        MergeOffer offer3 = weightedMergeOffer(cc3, cc1, 3.0);
        ListF<MergeOffer> triangleTopology = Cf.list(
                offer1,
                offer1.opposite(),
                offer2,
                offer2.opposite(),
                offer3,
                offer3.opposite()
        );

        LocalYield<MergeOffer> yield2 = applyConsensus(triangleTopology);

        ListF<MergeOffer> mergeDecisions = yield2.getRecsByIndex(0);
        ListF<MergeOffer> okStatuses = yield2.getRecsByIndex(1);
        ListF<MergeOffer> notOkStatuses = yield2.getRecsByIndex(2);

        System.out.println(mergeDecisions);
        System.out.println(okStatuses);
        System.out.println(notOkStatuses);

        SetF<String> mergedCryptaIds = mergeDecisions.flatMap(
                ok -> Cf.list(ok.getFromCryptaId(), ok.getToCryptaId())
        ).unique();

        assertEquals(Cf.list(cc1.getCcId(), cc3.getCcId()), mergedCryptaIds.sorted());

        assertEquals(1, mergeDecisions.size());
        assertEquals(2, okStatuses.size());  // direct an opposite decisions
        assertEquals(4, notOkStatuses.size());  // direct an opposite decisions

    }

    private LocalYield<MergeOffer> applyConsensus(ListF<MergeOffer> offers) {
        return applyConsensus(offers, mergeOfferPriority);
    }

    private LocalYield<MergeOffer> applyConsensus(ListF<MergeOffer> offers, MergeOfferPriority mergeOfferPriority) {
        ChooseMergeOrderReducer step1 = new ChooseMergeOrderReducer(mergeOfferPriority);
        ConfirmMergeOrderReducer step2 = new ConfirmMergeOrderReducer(mergeOfferPriority);
//        ConfirmMergeDecisionReducer step2 = new ConfirmMergeDecisionReducer();

        LocalYield<MergeOffer> yield1 = YtTestHelper.testReducer(step1, offers);
        return YtTestHelper.testReducer(step2, yield1.getAllRecs());
    }

    private MergeOffer standardOffer(ComponentCenter cc1, ComponentCenter cc2) {
        return weightedMergeOffer(cc1, cc2, 1.0);
    }

    private MergeOffer weightedMergeOffer(ComponentCenter cc1, ComponentCenter cc2, double weight) {
        // here we use ccId instead of cryptaId to compare human-readable strings instead of numbers
        MergeOffer mergeOffer = new MergeOffer(
                MergeKey.betweenComponents(cc1.getCcId(), cc2.getCcId()),
                cc1.getCcId(),
                cc2.getCcId(),
                Option.of(new MetricsTree(1.0)),
                new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, weight)
        );
        mergeOffer.setFromCryptaIdComponentWeight(cc1.getCcNeighboursCount().get());
        mergeOffer.setToCryptaIdComponentWeight(cc2.getCcNeighboursCount().get());
        return mergeOffer;
    }

    private MergeOffer weightedMergeOffer(String cryptaId1, int cryptaId1Size, int cryptaId1Neighbours,
                                          String cryptaId2, int cryptaId2Size, int cryptaId2Neighbours,
                                          double weight) {
        MergeOffer mergeOffer = new MergeOffer(
                MergeKey.betweenComponents(cryptaId1, cryptaId2),
                cryptaId1,
                cryptaId2,
                Option.of(new MetricsTree(1.0)),
                new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, weight)
        );
        mergeOffer.setFromCryptaIdComponentSize(cryptaId1Size);
        mergeOffer.setFromCryptaIdNeighboursCount(cryptaId1Neighbours);
        mergeOffer.setFromCryptaIdComponentWeight(cryptaId1Neighbours);

        mergeOffer.setToCryptaIdComponentSize(cryptaId2Size);
        mergeOffer.setToCryptaIdNeighboursCount(cryptaId2Neighbours);
        mergeOffer.setToCryptaIdComponentWeight(cryptaId2Neighbours);

        return mergeOffer;
    }
}
