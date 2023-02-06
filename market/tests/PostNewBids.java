package ru.yandex.autotests.market.bidding.monitoring.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.monitoring.BiddingMonitoringProperties;
import ru.yandex.autotests.market.bidding.monitoring.data.BidMeta;
import ru.yandex.autotests.market.bidding.monitoring.data.ShopInfoFromWiki;
import ru.yandex.autotests.market.bidding.monitoring.steps.BidsSteps;
import ru.yandex.autotests.market.bidding.monitoring.steps.MongoStorageSteps;
import ru.yandex.autotests.market.partner.beans.api.bids.body.OfferBid;
import ru.yandex.autotests.market.partner.beans.api.bids.result.OfferBidSet;
import ru.yandex.autotests.market.report.beans.Offer;
import ru.yandex.autotests.market.report.util.RequestFactory;
import ru.yandex.autotests.market.report.util.offers.OffersParser;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Step;
import ru.yandex.qatools.allure.annotations.Title;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assume.assumeThat;
import static ru.yandex.autotests.market.bidding.monitoring.data.changebid.BidChangesFuntions.getStepFunc;
import static ru.yandex.autotests.market.common.wiki.WikiGridLoadingUtils.loadWikiGrid;
import static ru.yandex.autotests.market.common.wiki.WikiProperties.WIKI_BASE_URL;

/**
 * Created by belmatter on 26.11.14.
 */
@RunWith(Parameterized.class)
@Aqua.Test()
@Title("Задаем ставки")
@Features("post bids")
public class PostNewBids {

    @Parameter("Данные в вики")
    public final String wikiPage = WIKI_BASE_URL + ShopInfoFromWiki.PAGE_URL;

    @Parameterized.Parameter(0)
    @Parameter("campaign id")
    public int campaignId;

    @Parameterized.Parameter(1)
    @Parameter("feed id")
    public int feedId;

    @Parameterized.Parameter(2)
    @Parameter("offers count")
    public int offersCount;

    @Parameterized.Parameter(3)
    @Parameter("region id")
    public String regionId;

    @Parameterized.Parameter(4)
    @Parameter("shop id")
    public String shopId;

    @Parameterized.Parameter(5)
    @Parameter("environment")
    public ShopInfoFromWiki.Stage env;

    @Parameterized.Parameter(6)
    @Parameter("identification type")
    public ShopInfoFromWiki.OfferIdentificationType offerIdentType;

    private BidsSteps bidsSteps = new BidsSteps();
    private MongoStorageSteps mongoStorageSteps;
    private static final BiddingMonitoringProperties PROPERTIES = new BiddingMonitoringProperties();
    private static final Function<Integer, Float> changeBid = getStepFunc();

    private List<BidMeta> uncompletedBids;
    private List<Offer> allOffers;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static List<Object[]> testingData() {
        return loadWikiGrid(ShopInfoFromWiki.PAGE_URL)
                .stream()
                .map(from -> new Object[]{
                        from.get("CampaignId", Integer::parseInt)
                        , from.get("FeedId", Integer::parseInt)
                        , from.get("OffersCount", Integer::parseInt)
                        , from.get("RegionId")
                        , from.get("ShopId")
                        , ShopInfoFromWiki.Stage.valueOf(from.get("stage"))
                        , ShopInfoFromWiki.OfferIdentificationType.valueOf(from.get("Type"))
                })
                .collect(toList());
    }

    @Before
    public void init() {
        assumeThat("Environment is not the same that in test case. Test cases on wiki page",
                PROPERTIES.getStage(),
                equalTo(env));
        mongoStorageSteps = new MongoStorageSteps(env);
        allOffers = new OffersParser().parseOffersFrom(
                RequestFactory
                        .shopOffers(shopId, regionId)
                        .withFeedId(String.valueOf(feedId))
        )
                .getOffers();
        uncompletedBids = mongoStorageSteps.getUncompletedBidsSets();
    }

    @Test
    public void setBidTest() throws IOException {
        OfferBidSet establishedBid = bidsSteps.setOfferBid(
                getFreeOfferBid()
                , campaignId
        );
        mongoStorageSteps.saveUncompletedBid(
                createBidMeta(establishedBid
                        , shopId
                        , regionId
                        , String.valueOf(campaignId)
                        , currentTimeMillis()
                ));
    }

    @Step("Получение свободных офферов")
    private OfferBid getFreeOfferBid() {
        return allOffers.stream()
                .filter(reportOffer -> !isUncompletedBid(reportOffer))
                .map(reportOffer -> reportOfferToBidConverter(changeBid, offerIdentType, reportOffer))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Нет свободных офферов."));
    }

    private boolean isUncompletedBid(Offer offer) {
        switch (offerIdentType) {
            case OFFER_ID:
                return uncompletedBids.stream()
                        .filter(uncompletedBids -> !isBlank(uncompletedBids.getOfferBid().getOfferId()))
                        .anyMatch(
                                uncompletedBid ->
                                        uncompletedBid.getOfferBid().getOfferId().equals(offer.getShopOfferId()));
            case OFFER_NAME:
                return uncompletedBids.stream()
                        .filter(uncompletedBids -> !isBlank(uncompletedBids.getOfferBid().getOfferName()))
                        .anyMatch(
                                uncompletedBid ->
                                        uncompletedBid.getOfferBid().getOfferName().equals(offer.getName()));
            default:
                throw new RuntimeException("Плохой оффер " + offer.getWareMd5() + ". У оффера должен быть name или offer shop id");
        }
    }

    private BidMeta createBidMeta(OfferBidSet offerBidSet, String shopId, String regionId, String campaignId, long timeStamp) {
        BidMeta bidMeta = new BidMeta();
        bidMeta.setOfferBid(offerBidSet);
        bidMeta.setShopId(shopId);
        bidMeta.setRegionId(regionId);
        bidMeta.setCampaignId(campaignId);
        bidMeta.setStartWatchTime(timeStamp);
        return bidMeta;
    }

    private OfferBid reportOfferToBidConverter(Function<Integer, Float> changeBidStrategy,
                                               ShopInfoFromWiki.OfferIdentificationType offerIdentificationType,
                                               Offer from) {
        OfferBid offerBid = new OfferBid()
                .withBid(changeBidStrategy.apply(from.getBid()))
                .withCbid(changeBidStrategy.apply(from.getCbid()))
                .withFeedId(from.getFeedId());
        switch (offerIdentificationType) {
            case OFFER_NAME:
                return offerBid.withOfferName(from.getName());
            case OFFER_ID:
                return offerBid.withOfferId(from.getShopOfferId());
            default:
                throw new RuntimeException("Не могу определить тип индентификации оффера. Доступные типы: " +
                        Stream.of(ShopInfoFromWiki.OfferIdentificationType.values()).map(Enum::name).collect(joining(",")));
        }
    }
}
