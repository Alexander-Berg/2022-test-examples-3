package ru.yandex.market.pricelabs.tms.programs;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import NMarket.NAmore.NAutostrategy.MarketAmoreService;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendation;
import ru.yandex.market.pricelabs.model.types.OfferBidType;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.processing.ShopArg;
import ru.yandex.market.pricelabs.processing.monetization.model.ActionEnum;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaign;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersCheckers;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferTarget;

public class ProgramsAutostrategyApplyTest extends AbstractProgramsTmsTest {

    private OffersCheckers checkers;

    @Autowired
    @Qualifier("mockWebServerAmore")
    protected ConfigurationForTests.MockWebServerControls mockWebServerAmore;

    @BeforeEach
    void initApply() {
        mockWebServerAmore.cleanup();
        this.checkers = new OffersCheckers(this);
    }

    @AfterEach
    void afterApply() {
        executor.clearTargetTable();
        executor.clearSourceTable();
        filterExectuor.clearTargetTable();
        offersExecutor.clearTargetTable();
        advCampaignYtScenarioExecutor.clearTargetTable();
        advCampaignHistoryYtScenarioExecutor.clearTargetTable();
        advOfferBidsYtScenarioExecutor.clearTargetTable();
        autostrategyOffersExecutor.clearTargetTable();
    }

    @Test
    void testFetchPlAutostrategiesAndMonetizationCampaignsAndApplyBothSendToBidding()
            throws InvalidProtocolBufferException {
        int campaignId1 = 1001;
        int campaignId2 = 1002;

        String offerId1 = "111";
        String offerId2 = "112";
        String offerId3 = "113";
        String offerId4 = "114";

        Instant now = Instant.now();
        executors.priceRecommendations().clearTargetTable();
        executors.priceRecommendations().insert(List.of(
                new PriceRecommendation(SHOP_ID, offerId1, 0, 0.06, 0.11, 0.21, now, now),
                new PriceRecommendation(SHOP_ID, offerId2, 0, 0.07, 0.12, 0.22, now, now),
                new PriceRecommendation(SHOP_ID, offerId4, 0, 0.09, 0.14, 0.24, now, now),
                new PriceRecommendation(SHOP_ID, "444444", 1, 0.1, 0.15, 0.25, now, now)
        ));

        // 4 оффера
        List<Offer> offers = getOffersWhite1To1CategoryMapping();

        executor.insert(offers, o -> o.setPrice(10000));


        // 2 стандартных АС с фильтрами настроенными на все офферы
        filterExectuor.insert(List.of(
                getFilter(23, List.of(offerId1, offerId2, offerId3, offerId4)),
                getFilter(24, List.of(offerId1, offerId2, offerId3, offerId4))
        ));
        int autoId1 = 100; // вот эта выбирается
        int autoId2 = 200;
        autostrategyExecutor.insert(List.of(
                getAutostrategy(autoId1, SHOP_ID, " пользовательская1", 2, 23),
                getAutostrategy(autoId2, SHOP_ID, " пользовательская2", 1, 24)
        ));

        // две активных кампании монетизации
        AdvCampaign advCampaign = advCampaign(SHOP_ID, campaignId1, 50, true);
        AdvCampaign advCampaign1 = advCampaign(SHOP_ID, campaignId2, 30, true);
        AdvCampaign advCampaign2 = advCampaign(SHOP_ID, 3000, 40, false);
        advCampaignYtScenarioExecutor.insert(List.of(advCampaign, advCampaign1, advCampaign2));
        advCampaignHistoryYtScenarioExecutor.insert(List.of(
                advCampaignHistory(advCampaign, ActionEnum.CREATED),
                advCampaignHistory(advCampaign1, ActionEnum.CREATED),
                advCampaignHistory(advCampaign2, ActionEnum.REMOVED)
        ));

        // ставки только на офферы в этих кампаниях
        advOfferBidsYtScenarioExecutor.insert(List.of(
                advOfferBid(SHOP_ID, campaignId1, offerId1, 100),
                advOfferBid(SHOP_ID, campaignId1, offerId2, 200),
                advOfferBid(SHOP_ID, campaignId2, offerId1, 300),
                advOfferBid(SHOP_ID, 111111, offerId1, 400),
                advOfferBid(SHOP_ID, 222222, offerId1, 500)
        ));


        testControls.resetShops();
        var newShop = initShop(SHOP_ID, s -> {
            s.setFeeds(Set.of((long) FEED_ID));
            s.setBusiness_id(BUSINESS_ID);
        });
        testControls.saveShop(newShop);

        processorRouter.syncAutostrategiesWhite(
                new ShopArg()
                        .setShopId(SHOP_ID)
                        .setType(ShopType.DSBS),
                new TaskInfo(getInstant(), taskWriter)
        );

        printTables();

        // надо проверить, что
        // к offerId1 и к offerId2 привязалась campaignId1
        // к offerId3 и offerId4 привязался к autoId1

        // В Амур отправляем только автостратегии, кампании он сам подхватит из таблицы
        checkers.checkAmore("add_proto", expectAmore ->
                expectAmore.addShopsBuilder()
                        .setShopId(SHOP_ID)
                        .setNOffers(4)
                        .setTsCreate(timeSource().getMillis())
                        .addAsParams(
                                MarketAmoreService.TAutostrategies.AutostrategyParams.newBuilder()
                                        .setUid(100)
                                        .setCpa(MarketAmoreService.TAutostrategies.CpaParams.newBuilder().setCpa(999))
                        )
        );
        mockWebServerAmore.checkNoMessages();

        Consumer<AutostrategyOfferTarget> updateType = o -> {
            o.setType(OfferBidType.CAMPAIGN);
        };

        autostrategyOffersExecutor.verify(List.of(
                autostrategyOfferTarget(SHOP_ID, FEED_ID, offerId3, SHOP_ID, autoId1, 0, getInstant(), BUSINESS_ID, 0,
                        999),
                autostrategyOfferTarget(SHOP_ID, FEED_ID, offerId4, SHOP_ID, autoId1, 0, getInstant(), BUSINESS_ID, 0,
                        999),
                autostrategyOfferTarget(SHOP_ID, FEED_ID, offerId1, SHOP_ID, campaignId1, 0, getInstant(),
                        BUSINESS_ID, 0,
                        100, updateType),
                autostrategyOfferTarget(SHOP_ID, FEED_ID, offerId2, SHOP_ID, campaignId1, 0, getInstant(),
                        BUSINESS_ID, 0,
                        200, updateType)
        ));

        advCampaignYtScenarioExecutor.verify(
                List.of(
                        advCampaign(SHOP_ID, campaignId1, 50, true, c -> {
                            c.setOffer_count(2);
                            c.setMin_bid(100);
                            c.setMax_bid(200);
                            c.setRec_count(2);
                        }),
                        advCampaign(SHOP_ID, campaignId2, 30, true, c -> {
                            c.setOffer_count(0);
                            c.setMin_bid(0);
                            c.setMax_bid(0);
                        }),
                        advCampaign2
                )
        );
    }
}
