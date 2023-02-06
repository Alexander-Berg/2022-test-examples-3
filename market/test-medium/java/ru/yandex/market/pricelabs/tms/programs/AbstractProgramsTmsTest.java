package ru.yandex.market.pricelabs.tms.programs;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.program.AdvProgramBonus;
import ru.yandex.market.pricelabs.model.program.PartnerBusiness;
import ru.yandex.market.pricelabs.model.program.PartnerProgram;
import ru.yandex.market.pricelabs.model.program.ProgramRecommendationSettings;
import ru.yandex.market.pricelabs.model.program.ProgramService;
import ru.yandex.market.pricelabs.model.recommendation.NewPriceRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendation;
import ru.yandex.market.pricelabs.model.types.AutostrategyType;
import ru.yandex.market.pricelabs.model.types.FilterClass;
import ru.yandex.market.pricelabs.model.types.FilterType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.processing.monetization.model.ActionEnum;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaign;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaignHistory;
import ru.yandex.market.pricelabs.processing.monetization.model.Color;
import ru.yandex.market.pricelabs.processing.monetization.model.OfferBids;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;
import ru.yandex.market.pricelabs.tms.processing.TaskChildrenWriter;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorRouter;
import ru.yandex.market.pricelabs.tms.processing.offers.ShopOffersProcessor;

import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;

public class AbstractProgramsTmsTest extends AbstractOffersProcessorTest {

    public static final Integer BUSINESS_ID = 187361;
    public static final Integer SHOP_ID = 2289;
    public static final int FEED1 = 3393;

    public static final int SHOP1 = SHOP_ID;

    public static final int CATEGORY1 = 2001;
    public static final int CATEGORY2 = 2002;
    public static final int CATEGORY3 = 2003;

    public static final int CATEGORY4 = 2004;
    public static final int CATEGORY5 = 2005;
    public static final int CATEGORY6 = 2006;
    public static final int CATEGORY7 = 2007;

    protected YtScenarioExecutor<Offer> offersExecutor;
    protected YtScenarioExecutor<Offer> blueOffersExecutor;
    protected YtScenarioExecutor<Autostrategy> autostrategyExecutor;
    protected YtScenarioExecutor<Autostrategy> blueAutostrategyExecutor;
    protected YtScenarioExecutor<ShopCategory> categoryExecutor;
    protected YtScenarioExecutor<ProgramRecommendationSettings> categoryRecommendationExecutor;
    protected YtScenarioExecutor<Filter> filterExectuor;
    protected YtScenarioExecutor<PartnerBusiness> partnerBusinessExecutor;
    protected YtScenarioExecutor<PartnerProgram> partnerTypeBusinessExecutor;
    protected YtScenarioExecutor<Shop> shopExecutor;
    protected YtScenarioExecutor<AdvProgramBonus> bonusExecutor;
    protected YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersExecutor;

    @Autowired
    protected ShopOffersProcessor shopOffersProcessor;

    @Autowired
    protected ProgramService programService;

    @Autowired
    protected ExecutorSources executors;

    @Autowired
    OffersProcessorRouter processorRouter;

    @Mock
    TaskChildrenWriter taskWriter;

    @BeforeEach
    void beforeEachParent() {
        shopExecutor = executors.shop();
        offersExecutor = executors.offers();
        blueOffersExecutor = executors.offersBlue();
        autostrategyExecutor = executors.whiteAutostrategiesExecutor();
        blueAutostrategyExecutor = executors.blueAutostrategiesExecutor();
        categoryExecutor = executors.categories();
        categoryRecommendationExecutor = executors.categoryRecommendations();
        filterExectuor = executors.filters();
        partnerBusinessExecutor = executors.partnerBusinessExecutor();
        partnerTypeBusinessExecutor = executors.partnerTypeBusinessExecutor();
        bonusExecutor = executors.bonusExecutor();
        autostrategyOffersExecutor = executors.autostrategyOffersWhite();
    }


    protected void printTables() {
        System.out.println("\n\n\n####  shopExecutor #### " + shopExecutor.getTable());
        for (Shop shop : shopExecutor.selectTargetRows()) {
            System.out.printf("#%s. status=%s %n",
                    shop.getShop_id(),
                    shop.getStatus()
            );
        }

        // белые магазинные категории
        System.out.println("\n####  магазинные категории #### " + categoryExecutor.getTable());
        for (ShopCategory offer : categoryExecutor.selectTargetRows()) {
            System.out.printf("#%s. feed_id=%s status=%s name=%s shop_id=%s offer_count=%s%n",
                    offer.getCategory_id(),
                    offer.getFeed_id(),
                    offer.getStatus(),
                    offer.getName(),
                    offer.getShop_id(),
                    offer.getOffer_count()
            );
        }

        // рекомендации
        System.out.println("\n#### categoryRecommendations #### " + categoryRecommendationExecutor.getTable());
        for (ProgramRecommendationSettings offer : categoryRecommendationExecutor.selectTargetRows()) {
            System.out.printf("#%s. bid=%s%n",
                    offer.getCategory_hyper_id(),
                    offer.getRecommended_bid());
        }

        //
        System.out.println("\n#### Partner business #### " + partnerBusinessExecutor.getTable());
        for (PartnerBusiness item : partnerBusinessExecutor.selectTargetRows()) {
            System.out.printf("Shop_id=%s. getName=%s getBusiness_id=%s getOrganization_name=%s getCreated_at=%s " +
                            "getUpdated_at=%s%n",
                    item.getShop_id(),
                    item.getName(),
                    item.getBusiness_id(),
                    item.getOrganization_name(),
                    item.getCreated_at(),
                    item.getUpdated_at()
            );
        }

        //
        System.out.println("\n#### Partner Type #### " + partnerTypeBusinessExecutor.getTable());
        for (PartnerProgram item : partnerTypeBusinessExecutor.selectTargetRows()) {
            System.out.printf("Shop_id=%s. getProgram=%s getStatus=%s getCreated_at=%s getUpdated_at=%s " +
                            "isEver_activated=%s%n",
                    item.getShop_id(),
                    item.getProgram(),
                    item.getStatus(),
                    item.getCreated_at(),
                    item.getUpdated_at(),
                    item.isEver_activated()
            );
        }

        //
        System.out.println("\n#### Bonus #### " + bonusExecutor.getTable());
        for (AdvProgramBonus item : bonusExecutor.selectTargetRows()) {
            System.out.printf("getProgram_type=%s. getPartner_id=%s getBonus_sum=%s isEnabled=%s " +
                            "getTimestamp=%s%n",
                    item.getProgram_type(),
                    item.getPartner_id(),
                    item.getBonus_sum(),
                    item.isEnabled(),
                    item.getTimestamp()
            );
        }

        printOffers(offersExecutor);
        printOffers(blueOffersExecutor);

        printAutostrategies(autostrategyExecutor);
        printAutostrategies(blueAutostrategyExecutor);

        printAutostrategiesOfferShop(executors.autostrategyOffersBlue());

        printCampaigns();
        printAdvOfferBids();
        printPriceRecommendations();
        printAutostrategyState(executors.autostrategiesStateWhite());
        printAutostrategyState(executors.autostrategiesStateBlue());

        System.out.println("\n\n\n\n");
    }

    private void printAutostrategyState(YtScenarioExecutor<AutostrategyState> executor) {
        System.out.println("\n#### AutostrategyState #### " + executor.getTable());
        for (AutostrategyState item : executor.selectTargetRows()) {
            System.out.printf("shop_id=%s. autostrategy_id=%s linked_count=%s rec_count=%s ssku_linked_count=%s%n",
                    item.getShop_id(),
                    item.getAutostrategy_id(),
                    item.getLinked_count(),
                    item.getRes_count(),
                    item.getSsku_linked_count()
            );
        }
    }

    private void printPriceRecommendations() {
        YtSourceTargetScenarioExecutor<NewPriceRecommendation, PriceRecommendation> executor =
                executors.priceRecommendations();
        System.out.println("\n#### PriceRecommendations #### " + executor.getTable());
        for (PriceRecommendation item : executor.selectTargetRows()) {
            System.out.printf("shop_id=%s. offer_id=%s price=%s recommended_price=%s recommended_promocode=%s " +
                            "status=%s %n",
                    item.getPartner_id(),
                    item.getOffer_id(),
                    item.getPrice(),
                    item.getRecommended_price(),
                    item.getRecommended_promocode(),
                    item.getStatus()
            );
        }
    }

    private void printAdvOfferBids() {
        YtScenarioExecutor<OfferBids> executor = executors.advOfferBidsYtScenarioExecutor();
        System.out.println("\n#### AutostrategyOfferTarget #### " + executor.getTable());
        for (OfferBids item : executor.selectTargetRows()) {
            System.out.printf("shop_id=%s. campaign_id=%s sku=%s bid=%s%n",
                    item.getPartner_id(),
                    item.getAdv_campaign_id(),
                    item.getSku(),
                    item.getBid()
            );
        }
    }

    private void printAutostrategiesOfferShop(YtScenarioExecutor<AutostrategyOfferTarget> executor) {
        System.out.println("\n#### AutostrategyOfferTarget #### " + executor.getTable());
        for (AutostrategyOfferTarget item : executor.selectTargetRows()) {
            System.out.printf("auto_id=%s. shop_id=%s offer_id=%s datasource=%s target=%s%n",
                    item.getAutostrategy_id(),
                    item.getShop_id(),
                    item.getOffer_id(),
                    item.getDatasource_id(),
                    item.getTarget_id()
            );
        }
    }

    private void printAutostrategies(YtScenarioExecutor<Autostrategy> executor) {
        System.out.println("\n#### Autostrategy #### " + executor.getTable());
        for (Autostrategy item : executor.selectTargetRows()) {
            System.out.printf("id=%s. name=%s shop_id=%s priority=%s isEnabled=%s filter_id=%s%n",
                    item.getAutostrategy_id(),
                    item.getName(),
                    item.getShop_id(),
                    item.getPriority(),
                    item.isEnabled(),
                    item.getFilter_id()
            );
        }
    }

    private void printOffers(YtScenarioExecutor<Offer> executor) {
        System.out.println("\n\n\n####  офферы #### " + executor.getTable());
        for (Offer offer : executor.selectTargetRows()) {
            System.out.printf("#%s. shop_id=%s auto_id=%s status=%s feed_id=%s market_id=%s category_id=%s%n",
                    offer.getOffer_id(),
                    offer.getShop_id(),
                    offer.getApp_autostrategy_id(),
                    offer.getStatus(),
                    offer.getFeed_id(),
                    offer.getMarket_category_id(),
                    offer.getCategory_id());
        }
    }

    private void printCampaigns() {
        System.out.println("\n#### AdvCampaign #### " + advCampaignYtScenarioExecutor.getTable());

        for (AdvCampaign row : advCampaignYtScenarioExecutor.selectTargetRows()) {
            System.out.printf("AdvCampaign: id=%s name=%s color=%s partner_id=%s state=%s priority=%s offer_count=%s " +
                            "state=%s min_bid=%s max_bid=%s  %n",
                    row.getId(), row.getName(), row.getColor(), row.getPartner_id(), row.getStatus(), row.getPriority(),
                    row.getOffer_count(), row.getStatus(), row.getMin_bid(), row.getMax_bid()
            );
        }
    }

    protected List<Offer> getOffersWhite1To1CategoryMapping() {
        Instant instant = getInstant();

        return List.of(
                offer("111", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setCategory_id(CATEGORY4);
                    o.setMarket_category_id(CATEGORY1);
                    o.setUpdated_at(instant);
                }),
                offer("112", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setCategory_id(CATEGORY5);
                    o.setMarket_category_id(CATEGORY2);
                    o.setUpdated_at(instant);
                }),
                offer("113", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setCategory_id(CATEGORY6);
                    o.setMarket_category_id(CATEGORY3);
                    o.setUpdated_at(instant);
                }),
                offer("114", o -> {
                    o.setShop_id(SHOP1);
                    o.setFeed_id(FEED1);
                    o.setStatus(Status.ACTIVE);
                    o.setCategory_id(CATEGORY7);
                    o.setMarket_category_id(CATEGORY3);
                    o.setUpdated_at(instant);
                })
        );
    }


    protected AdvCampaignHistory advCampaignHistory(AdvCampaign advCampaign, ActionEnum action) {
        AdvCampaignHistory history = new AdvCampaignHistory();
        history.setName(advCampaign.getName());
        history.setPriority(advCampaign.getPriority());
        history.setActivation(advCampaign.isActivation());
        history.setStatus(advCampaign.getStatus());
        history.setId(advCampaign.getId());
        history.setUpdated_at(getInstant());
        history.setPartner_id(advCampaign.getPartner_id());
        history.setAction(action);
        history.setMax_bid(advCampaign.getMax_bid());
        history.setMin_bid(advCampaign.getMin_bid());
        history.setType(advCampaign.getType());
        return history;
    }

    protected Filter getFilter(int id, List<String> offerId1) {
        Filter f = new Filter();
        f.setShop_id(SHOP_ID);
        f.setFilter_id(id);
        f.setId_list(new HashSet<>(offerId1));
        f.setUpdated_at(getInstant());
        f.setFilter_class(FilterClass.WHITE_AUTOSTRATEGY);
        return f;
    }

    protected OfferBids advOfferBid(Integer shopId, long campaignId1, String sku, int bid) {
        OfferBids b = new OfferBids();
        b.setBid(bid);
        b.setPartner_id(shopId);
        b.setUpdated_at(getInstant());
        b.setAdv_campaign_id(campaignId1);
        b.setSku(sku);
        return b;
    }

    protected AdvCampaign advCampaign(Integer shopId,
                                      int id,
                                      int priority,
                                      boolean enabled
    ) {
        return advCampaign(shopId, id, priority, enabled, Utils.emptyConsumer());
    }

    protected AdvCampaign advCampaign(Integer shopId,
                                      int id,
                                      int priority,
                                      boolean enabled,
                                      Consumer<AdvCampaign> init
    ) {
        AdvCampaign campaign = new AdvCampaign();
        campaign.setId(id);
        campaign.setName("Рекламная кампания");
        campaign.setType("EXCEL");
        campaign.setActivation(false);
        campaign.setStatus(enabled ? ru.yandex.market.pricelabs.processing.monetization.model.Status.ACTIVE :
                ru.yandex.market.pricelabs.processing.monetization.model.Status.INACTIVE);
        campaign.setColor(Color.WHITE);
        campaign.setMax_bid(100);
        campaign.setPriority(priority);
        campaign.setMin_bid(10);
        campaign.setOffer_count(44);
        campaign.setPartner_id(shopId);
        campaign.setUpdated_at(getInstant());
        init.accept(campaign);
        return campaign;
    }

    protected Autostrategy getAutostrategy(int id, int shopId, String name,
                                           int priority, int filterId) {
        return TmsTestUtils.autostrategy(id, shopId, a -> {
            a.setName(name);
            a.setPriority(priority);
            a.setEnabled(true);
            a.setFilter_type(FilterType.SIMPLE);
            a.setFilter_id(filterId);
            a.setUpdated_at(getInstant());
            a.setType(AutostrategyType.CPA);
            Autostrategy.CpaStrategySettings cpaStrategySettings = new Autostrategy.CpaStrategySettings();
            cpaStrategySettings.setDrr_bid(999L);
            a.setCpaSettings(cpaStrategySettings);
        });
    }

}
