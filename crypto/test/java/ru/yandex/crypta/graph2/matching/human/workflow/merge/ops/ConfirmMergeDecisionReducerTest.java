package ru.yandex.crypta.graph2.matching.human.workflow.merge.ops;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import ru.yandex.crypta.graph2.dao.yt.local.LocalYield;
import ru.yandex.crypta.graph2.dao.yt.local.StatisticsSlf4jLoggingImpl;
import ru.yandex.crypta.graph2.model.matching.merge.MergeKey;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOffer;
import ru.yandex.crypta.graph2.model.matching.merge.MergeOfferStatus;
import ru.yandex.misc.test.Assert;

import static org.junit.Assert.assertEquals;

public class ConfirmMergeDecisionReducerTest {

    @Test
    public void key() throws Exception {

        ConfirmMergeDecisionReducer reducer = new ConfirmMergeDecisionReducer();

        String cc1 = "cc1";
        String cc2 = "cc2";

        MergeOffer leader = new MergeOffer(MergeKey.EMPTY, cc2, cc1).withStatus(MergeOfferStatus.DECISION_LEADER);
        MergeOffer follower = new MergeOffer(MergeKey.EMPTY, cc1, cc2).withStatus(MergeOfferStatus.DECISION_FOLLOWER);

        LocalYield<MergeOffer> yield = new LocalYield<>();
        reducer.reduce(Arrays.asList(follower, leader).iterator(), yield, new StatisticsSlf4jLoggingImpl(), null);
        List<MergeOffer> decisions = yield.getRecsByIndex(0);

        // single decision
        assertEquals(1, decisions.size());

        MergeOffer singleDecision = decisions.get(0);
        assertEquals(MergeOfferStatus.CONFIRMED, singleDecision.getStatus());
        // decision is where leader says to join
        assertEquals(leader.getToCryptaId(), singleDecision.getToCryptaId());

        // but all offers with status update
        List<MergeOffer> offers = yield.getRecsByIndex(1);
        assertEquals(2, offers.size());
        Assert.forAll(offers, o -> o.getStatus().equals(MergeOfferStatus.CONFIRMED));
    }

}
