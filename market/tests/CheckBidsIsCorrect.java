package ru.yandex.autotests.market.bidding.monitoring.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.monitoring.BiddingMonitoringProperties;
import ru.yandex.autotests.market.bidding.monitoring.data.BidMeta;
import ru.yandex.autotests.market.bidding.monitoring.steps.BidsSteps;
import ru.yandex.autotests.market.bidding.monitoring.steps.GraphiteSteps;
import ru.yandex.autotests.market.bidding.monitoring.steps.MongoStorageSteps;
import ru.yandex.autotests.market.report.beans.Offer;
import ru.yandex.qatools.allure.annotations.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 * Created by belmatter on 26.11.14.
 */
@Aqua.Test()
@Title("Проверяем применились ли ставки")
@Features("check bids and copy it to stable")
@RunWith(Parameterized.class)
public class CheckBidsIsCorrect {

    static private BiddingMonitoringProperties properties = new BiddingMonitoringProperties();
    static private MongoStorageSteps mongo = new MongoStorageSteps(properties.getStage());

    private BidsSteps bidsSteps = new BidsSteps();
    private GraphiteSteps graphiteSteps =
            new GraphiteSteps(properties.getBiddingGraphiteHost(), properties.getBiddingGraphitePort());

    private static final String METRIC =
            "five_min.market-bidding.quickbids.report." + properties.getStage().name().toLowerCase();

    @Parameterized.Parameter
    public BidMeta bid;

    @Parameter("offer name")
    public String offerName;

    @Parameter("offer id")
    public String offerId;

    @Parameter("feed id")
    public String feedId;

    @Parameter("bid")
    public String bidValue;

    @Parameter("cbid")
    public String cbidValue;

    @Parameter("shop id")
    public String shopId;

    @Parameter("region id")
    public String regionId;

    @Parameter("campaign id")
    public String campaignId;

    @Parameter("время применения ставки")
    public String startWatchTime;

    private Offer offer;

    @Parameterized.Parameters()
    public static Collection<Object[]> testData() {
        List<BidMeta> bids = mongo.getUncompletedBidsSets();
        assertThat("Нет измененных бидов. Возможно, проблема в post pack",
                bids, allOf(notNullValue(), not(empty())));
        return bids.stream()
                .map(bid -> new Object[] { bid })
                .collect(toList());
    }

    @Before
    public void getOffer() {
        offerId = bid.getOfferBid().getOfferId();
        offerName = bid.getOfferBid().getOfferName();
        feedId = String.valueOf(bid.getOfferBid().getFeedId());
        bidValue = String.valueOf(bid.getOfferBid().getBid());
        cbidValue = String.valueOf(bid.getOfferBid().getCbid());
        shopId = bid.getShopId();
        regionId = bid.getRegionId();
        campaignId = bid.getCampaignId();
        startWatchTime = new Date(bid.getStartWatchTime()).toString();
        offer = bidsSteps.getOffersBy(bid);
        assertThat("Не могу найти оффер по информации об изменном bid.", offer, not(nullValue()));
    }

    @Test
    public void testBids() throws IOException {
        if (isBidTimeOut(bid) || isBidCompleted(bid.getOfferBid(), offer)) {
            bid.setEndWatchTime(currentTimeMillis());
        }
    }

    @After
    public void post() throws IOException {
        if (bid.getEndWatchTime() != 0) {
            graphiteSteps.postIntoGraphite(bid, METRIC);
            mongo.saveCompletedBid(bid);
            mongo.removeFromUncompleted(bid);
        }
    }

    @Step("Проверка на превышение timeout")
    @Attachment(value = "Результат проверки", type = "text/plain")
    private boolean isBidTimeOut(BidMeta bid) {
        return currentTimeMillis() - bid.getStartWatchTime() >= properties.getTimeoutDiff();
    }

    @Step("Проверка на применение ставки")
    @Attachment(value = "Результат проверки", type = "text/plain")
    public boolean isBidCompleted(ru.yandex.autotests.market.partner.beans.api.bids.result.OfferBidSet bid,
                                  Offer offer) {
        return isBidEquals(offer.getBid(), bid.getBid()) && isBidEquals(offer.getCbid(), bid.getCbid());
    }

    private boolean isBidEquals(int reportBid, float bid) {
        return Math.round(bid * 100) == reportBid;
    }
}
