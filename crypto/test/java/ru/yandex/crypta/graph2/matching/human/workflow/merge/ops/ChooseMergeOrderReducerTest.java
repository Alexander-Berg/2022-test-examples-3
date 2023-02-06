package ru.yandex.crypta.graph2.matching.human.workflow.merge.ops;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.crypta.graph.soup.config.proto.TEdgeProps;
import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.matching.component.ComponentCenter;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferPriority;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferStatus;
import ru.yandex.crypta.graph2.model.matching.score.MetricsTree;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeScore;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;

public class ChooseMergeOrderReducerTest {
    @Test
    public void everybodyIsConnectingToHugeCryptaId() {
        ChooseMergeOrderReducer reducer = new ChooseMergeOrderReducer(new MergeOfferPriority());

        ComponentCenter hugeStar = new ComponentCenter("huge_star", EIdType.IDFA, 100);
        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 10);
        ComponentCenter cc2 = new ComponentCenter("cc2", EIdType.UUID, 20);

        List<MergeOffer> offers = Arrays.asList(
                standardOffer(hugeStar, cc1),
                standardOffer(hugeStar, cc2),
                standardOffer(cc1, hugeStar),
                standardOffer(cc2, hugeStar)
        );

        LocalYield<MergeOffer> yield = new LocalYield<>();
        reducer.reduce(offers.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);

        ListF<MergeOffer> result = yield.getAllRecs();
        assertEquals(4, result.size());

        ListF<MergeOffer> hugeStarOffers =
                result.filter(mo -> mo.opposite().getFromCryptaId().equals(hugeStar.getCryptaId()));
        Assert.forAll(hugeStarOffers, mo -> mo.getStatus().equals(MergeOfferStatus.DECISION_LEADER));

        ListF<MergeOffer> otherOffers =
                result.filter(mo -> !mo.opposite().getFromCryptaId().equals(hugeStar.getCryptaId()));
        Assert.forAll(otherOffers, mo -> mo.getStatus().equals(MergeOfferStatus.DECISION_FOLLOWER));

    }

    @Test
    public void connectingToTheMostConnectedBiggerCryptaId() {
        ChooseMergeOrderReducer reducer = new ChooseMergeOrderReducer(new MergeOfferPriority());

        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 10);
        ComponentCenter hugeStar1 = new ComponentCenter("hugeStar1", EIdType.IDFA, 100);
        ComponentCenter hugeStar2 = new ComponentCenter("hugeStar2", EIdType.UUID, 200);

        List<MergeOffer> offers = Arrays.asList(
                weightedMergeOffer(cc1, hugeStar1, 20),
                weightedMergeOffer(cc1, hugeStar2, 10)
        );

        LocalYield<MergeOffer> yield = new LocalYield<>();
        reducer.reduce(offers.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);

        ListF<MergeOffer> result = yield.getAllRecs();
        assertEquals(2, result.size());

        ListF<MergeOffer> confirmed = result.filter(o -> o.getStatus().equals(MergeOfferStatus.DECISION_FOLLOWER));
        assertEquals(1, confirmed.size());

        ListF<MergeOffer> rejected = result.filter(o -> o.getStatus().equals(MergeOfferStatus.DECISION_REJECTED));
        assertEquals(1, rejected.size());

        assertEquals(hugeStar1.getCryptaId(), confirmed.first().opposite().getToCryptaId());

    }

    @Test
    public void connectingComponentsOfTheSameSizeDifferentLinkStrength() {
        ChooseMergeOrderReducer reducer = new ChooseMergeOrderReducer(new MergeOfferPriority());

        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 100);
        ComponentCenter hugeStar1 = new ComponentCenter("hugeStar1", EIdType.IDFA, 100);
        ComponentCenter hugeStar2 = new ComponentCenter("hugeStar2", EIdType.YANDEXUID, 100);

        List<MergeOffer> offers = Arrays.asList(
                weightedMergeOffer(cc1, hugeStar1, 20),
                weightedMergeOffer(cc1, hugeStar2, 10)
        );

        LocalYield<MergeOffer> yield = new LocalYield<>();
        reducer.reduce(offers.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);

        ListF<MergeOffer> result = yield.getAllRecs();
        assertEquals(2, result.size());

        ListF<MergeOffer> confirmed = result.filter(o -> o.getStatus().equals(MergeOfferStatus.DECISION_FOLLOWER));
        assertEquals(1, confirmed.size());

        ListF<MergeOffer> rejected = result.filter(o -> o.getStatus().equals(MergeOfferStatus.DECISION_REJECTED));
        assertEquals(1, rejected.size());

        assertEquals(hugeStar1.getCryptaId(), confirmed.first().opposite().getToCryptaId());

    }

    @Test
    public void connectingComponentsOfTheSameSize() {
        ChooseMergeOrderReducer reducer = new ChooseMergeOrderReducer(new MergeOfferPriority());

        ComponentCenter cc1 = new ComponentCenter("cc1", EIdType.UUID, 100);
        ComponentCenter cc2 = new ComponentCenter("hugeStar1", EIdType.IDFA, 100);

        MergeOffer mergeOffer = weightedMergeOffer(cc1, cc2, 20);

        List<MergeOffer> offers = Arrays.asList(
                mergeOffer,
                mergeOffer.opposite()
        );

        LocalYield<MergeOffer> yield = new LocalYield<>();
        reducer.reduce(offers.iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);

        ListF<MergeOffer> result = yield.getAllRecs();
        assertEquals(2, result.size());

        Assert.forAll(result, o -> o.getStatus().equals(MergeOfferStatus.DECISION_FOLLOWER));

    }

    private MergeOffer standardOffer(ComponentCenter cc1, ComponentCenter cc2) {
        return weightedMergeOffer(cc1, cc2, 1.0);
    }

    private MergeOffer weightedMergeOffer(ComponentCenter cc1, ComponentCenter cc2, double weight) {
        MergeOffer mergeOffer = new MergeOffer(
                MergeKey.betweenComponents(cc1.getCryptaId(), cc2.getCryptaId()),
                cc1.getCryptaId(),
                cc2.getCryptaId(),
                Option.of(new MetricsTree(1.0)),
                new EdgeScore(TEdgeProps.EEdgeStrength.USUAL, weight)
        );
        mergeOffer.setFromCryptaIdComponentWeight(cc1.getCcNeighboursCount().get());
        mergeOffer.setToCryptaIdComponentWeight(cc2.getCcNeighboursCount().get());
        return mergeOffer;
    }


}
