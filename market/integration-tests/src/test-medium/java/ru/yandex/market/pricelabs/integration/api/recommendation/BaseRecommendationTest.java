package ru.yandex.market.pricelabs.integration.api.recommendation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.EnumUtil;
import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.integration.api.AbstractAutostrategiesTestsOffersInitialisation;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.BlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.NewBlueBidsRecommendation;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.recommendation.NewOfferRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.OfferRecommendation;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.AutostrategyType;
import ru.yandex.market.pricelabs.model.types.FilterType;
import ru.yandex.market.pricelabs.model.types.RecommendationType;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.processing.cpa.CpaRecommendation;
import ru.yandex.market.pricelabs.services.database.SequenceService;
import ru.yandex.market.pricelabs.services.database.SequenceServiceImpl;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;

import static ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings.TypeEnum.CPA;
import static ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest.autostrategy;

class BaseRecommendationTest extends AbstractAutostrategiesTestsOffersInitialisation {

    public static final int SHOP1 = 1;
    public static final int SHOP2 = 2;
    public static final int CATEGORY_DEFAULT = 90401;
    protected YtScenarioExecutor<Offer> offersExecutor;
    protected YtSourceTargetScenarioExecutor<NewBlueBidsRecommendation, BlueBidsRecommendation> categoryBidsExecutor;
    protected YtSourceTargetScenarioExecutor<NewOfferRecommendation, OfferRecommendation> offerBidsExecutor;
    protected YtScenarioExecutor<Autostrategy> autostrategiesExecutor;
    protected YtScenarioExecutor<Filter> filterExecutor;
    protected Instant now;
    protected AutostrategyTarget target;
    protected Shop shop;
    @Autowired
    private SequenceService sequenceService;

    protected void init(AutostrategyTarget target) {
        this.target = target;
        now = getInstant();

        var virtualShopId = target.get(SHOP1, ApiConst.VIRTUAL_SHOP_BLUE);

        ((SequenceServiceImpl) sequenceService).resetSequences();

        this.offersExecutor = target.get(executors.offers(), executors.offersBlue());
        this.filterExecutor = executors.filters();

        this.shop = TmsTestUtils.shop(virtualShopId);
        shop.setStatus(ShopStatus.ACTIVE);

        var feeds = target.get(Set.of((long) FEED1, (long) FEED2), Set.of((long) ApiConst.VIRTUAL_FEED_BLUE));
        shop.setFeeds(feeds);

        testControls.initOnce(getClass(), () -> testControls.executeInParallel(() -> testControls.saveShop(shop),
                getSaveOffersRunnable(target.get(false, true), SHOP1, SHOP2, offersExecutor)));

        // перезаливаем офферы, т.к. в некоторых тестах мы добавляем новые.
        var offers =
                List.of(offersSampleShop1(), offersSampleShop2NoAutostrategy())
                        .stream().flatMap(Collection::stream).collect(Collectors.toList());

        autostrategiesExecutor = target.get(executors.whiteAutostrategiesExecutor(),
                executors.blueAutostrategiesExecutor());

        autostrategiesExecutor.insert(List.of());

        offersExecutor.insert(offers);

        categoryBidsExecutor = executors.blueBidsRecommender();
        categoryBidsExecutor.insert(new ArrayList<>(getBlueBidsRecommendations().values()));

        offerBidsExecutor = executors.offersRecommender();
        offerBidsExecutor.insert(getOfferRecommendations());
    }

    @AfterEach
    public void afterEach() {
        autostrategiesExecutor.insert(List.of());
        offersExecutor.insert(List.of());
        categoryBidsExecutor.clearTargetTable();
        offerBidsExecutor.clearTargetTable();
        filterExecutor.clearTargetTable();
    }

    protected Autostrategy createAutostrategy(String name, double drrBid, RecommendationType type) {
        return createAutostrategy(1234, name, drrBid, type);
    }

    protected Autostrategy createAutostrategy(int id, String name, double drrBid, RecommendationType type) {
        String autoName = name + "_" + id;
        Autostrategy autostrategy = new Autostrategy();
        autostrategy.setFilter_id(1L);
        autostrategy.setFilter_type(FilterType.SIMPLE);
        autostrategy.setEnabled(true);
        autostrategy.setName(autoName);
        autostrategy.setType(AutostrategyType.CPA);
        autostrategy.setShop_id((int) shop.getShop_id());
        autostrategy.setRecommendation_type(type);
        autostrategy.setUpdated_at(Instant.now());
        autostrategy.setPriority(id);
        Autostrategy.CpaStrategySettings cpaSettings = new Autostrategy.CpaStrategySettings();
        cpaSettings.setDrr_bid((long) (drrBid * 100L));

        autostrategy.setCpaSettings(cpaSettings);
        autostrategy.setAutostrategy_id(id);
        autostrategiesExecutor.insert(List.of(autostrategy), false);

        List<Autostrategy> autostrategies = autostrategiesExecutor.selectTargetRows();

        return autostrategies.stream().filter(a -> a.getName().equals(autoName))
                .collect(Collectors.toSet())
                .stream()
                .findFirst()
                .get();
    }

    protected List<OfferRecommendation> getOfferRecommendations() {
        return List.of(newOfferRecommendation(1L, "0", 0.15, 0.41, now),
                newOfferRecommendation(1L, "1", 0.25, 0.32,
                        now), newOfferRecommendation(1L, "2", 0.55, 0.33, now),
                newOfferRecommendation(2L, "1", 0.65, 0.63,
                        now));
    }

    protected Map<Integer, BlueBidsRecommendation> getBlueBidsRecommendations() {
        return Map.of(
                CATEGORY_DEFAULT, new BlueBidsRecommendation(CATEGORY_DEFAULT, 0.5, 0.1, 0.22, getInstant()),
                CATEGORY1, new BlueBidsRecommendation(CATEGORY1, 0.6, 0.2, 0.33, getInstant()),
                CATEGORY2, new BlueBidsRecommendation(CATEGORY2, 0.7, 0.3, 0.44, getInstant()));
    }

    protected OfferRecommendation newOfferRecommendation(long partnerId, String offerId, double maxBid, double optBid,
                                                         Instant time) {
        OfferRecommendation offerRecommendation = new OfferRecommendation();
        offerRecommendation.setPartner_id(partnerId);
        offerRecommendation.setOffer_id(offerId);
        offerRecommendation.setOptimal_bid(optBid);
        offerRecommendation.setMaximum_bid(maxBid);
        offerRecommendation.setComputation_datetime(time);
        offerRecommendation.setUpdated_at(time);
        offerRecommendation.setQueries(List.of("Query for " + partnerId, "Query2 for " + partnerId));
        offerRecommendation.setLost_queries(List.of("Lost Query1 for " + partnerId, "Lost Query2 for " + partnerId));
        return offerRecommendation;
    }

    protected Map<Integer, List<Offer>> offersSampleShop1GroupedByCategories() {
        return offersSampleShop1().stream().collect(Collectors.groupingBy(o -> (int) o.getCategory_id()));
    }

    protected List<Offer> offersSampleShop2NoAutostrategy() {
        return offersSampleShopNoAutostrategy(target.get(false, true), SHOP2);
    }

    protected List<Offer> offersSampleShop1() {
        return offersSampleShop(target.get(false, true), SHOP1);
    }


    protected AutostrategyFilter autostrategyFilter(List<String> offerIds) {
        return new AutostrategyFilter().type(AutostrategyFilter.TypeEnum.SIMPLE)
                .simple(new AutostrategyFilterSimple().offerIds(offerIds));
    }


    protected void assertRecommendationPlan(Autostrategy autostrategy, RecommendationType plan) {
        Autostrategy persisted =
                autostrategiesExecutor.selectTargetRows()
                        .stream()
                        .filter(a -> a.getAutostrategy_id() == autostrategy.getAutostrategy_id())
                        .findFirst().get();

        Assertions.assertEquals(plan, persisted.getRecommendation_type());
    }

    protected void addOfferRecommendations(Map<Offer, CpaRecommendation> offersWithBid) {
        List<OfferRecommendation> offerRecommendations = offersWithBid.keySet().stream().map(o -> {
            OfferRecommendation rec = new OfferRecommendation();
            rec.setOffer_id(o.getOffer_id());
            rec.setPartner_id(o.getShop_id());
            rec.setMaximum_bid(offersWithBid.get(o).getMaxDrr());
            rec.setOptimal_bid(offersWithBid.get(o).getOptDrr());
            return rec;
        }).collect(Collectors.toList());

        offerBidsExecutor.insert(offerRecommendations);
    }

    protected void addOfferRecommendations(List<Offer> offers, double maxMaxBid, double maxOptBid) {
        Map<Offer, CpaRecommendation> res = new HashMap<>();
        double step = 0.005;
        double max = maxMaxBid / 100d;
        double opt = maxOptBid / 100d;
        for (Offer offer : offers) {
            CpaRecommendation rec = new CpaRecommendation();
            rec.setMaxDrr(max);
            rec.setOptDrr(opt);
            max -= step;
            opt -= step;
            if (!res.containsKey(offer)) {
                res.put(offer, rec);
            }
        }

        addOfferRecommendations(res);
    }

    protected void addCategoryRecommendations(Map<Long, Double> offersWithBids) {
        List<BlueBidsRecommendation> categoryRecommendations = offersWithBids.keySet().stream().map(o -> {
            BlueBidsRecommendation rec = new BlueBidsRecommendation();
            rec.setCategory_id(o.intValue());
            rec.setCpa_rec_bid(offersWithBids.get(o));
            return rec;
        }).collect(Collectors.toList());
        categoryBidsExecutor.insert(categoryRecommendations);
    }

    protected void addCategoryRecommendations(List<Offer> offers, double maxBid) {
        Map<Long, Double> res = new HashMap<>();
        double bid = maxBid / 100d;
        for (Offer offer : offers) {
            if (!res.containsKey(offer.getCategory_id())) {
                res.put(offer.getCategory_id(), bid);
                bid -= 0.01;
            }
        }

        addCategoryRecommendations(res);
    }

    protected List<Offer> bindOffers(int autostrategyId) {
        return bindOffers(autostrategyId, 100L);
    }

    protected List<Offer> bindOffers(int autostrategyId, long bid) {
        List<Offer> offers = offersExecutor.selectTargetRows();
        offers.forEach(o -> {
            o.setApp_autostrategy_id(autostrategyId);
            o.setShop_id(shop.getShop_id());
            o.setStatus(Status.ACTIVE);
            o.setOffer_id_index("idx_" + o.getOffer_id());
            o.setBid(bid);
        });
        offersExecutor.insert(offers);
        offers = offersExecutor.selectTargetRows();
        Assertions.assertTrue(offers.size() > 4);
        return offers;
    }

    protected AutostrategySave getAutostrategySave(String name, long drr, RecommendationType type) {
        AutostrategySave auto = autostrategy(name, CPA);
        auto.setEnabled(true);
        auto.getSettings().getCpa().setDrrBid(drr);
        auto.recommendationType(
                EnumUtil.findEnum(AutostrategySave.RecommendationTypeEnum.class, String.valueOf(type)));
        return auto;
    }

}
