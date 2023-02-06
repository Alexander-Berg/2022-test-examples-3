package ru.yandex.market.pricelabs.integration.api.recommendation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApi;
import ru.yandex.market.pricelabs.api.api.PublicAutostrategiesApiInterfaces;
import ru.yandex.market.pricelabs.api.search.AutostrategiesSearch;
import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategiesOfferCount;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategiesOfferFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyLoad;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyOffer;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.recommendation.NewPriceRecommendation;
import ru.yandex.market.pricelabs.model.recommendation.PriceRecommendation;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.FilterClass;
import ru.yandex.market.pricelabs.model.types.FilterType;
import ru.yandex.market.pricelabs.model.types.RecommendationType;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.processing.CoreTables;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.yt.YtConfiguration;
import ru.yandex.market.yt.binding.BindingTable;

public class PriceRecommendationApiTest extends BaseRecommendationTest {

    protected PublicAutostrategiesApiInterfaces publicApi;
    @Autowired
    private PublicAutostrategiesApi publicApiBean;
    @Autowired
    @Qualifier("whiteSearch")
    private AutostrategiesSearch whiteSearch;
    @Autowired
    @Qualifier("blueSearch")
    private AutostrategiesSearch blueSearch;
    @Autowired
    private YtConfiguration ytCfg;
    @Autowired
    private CoreTables coreTables;
    private YtScenarioExecutor<AutostrategyState> statesExecutor;
    private YtScenarioExecutor<PriceRecommendation> priceRecommendationExecutor;

    @AfterEach
    public void clearAfter() {
        priceRecommendationExecutor.clearTargetTable();
        filterExecutor.clearTargetTable();
        autostrategiesExecutor.clearTargetTable();
        statesExecutor.clearTargetTable();
    }

    @BeforeEach
    public void beforeEach() {
        publicApi = MockMvcProxy.buildProxy(PublicAutostrategiesApiInterfaces.class, publicApiBean);

        init(AutostrategyTarget.white);

        var table = target.get(
                coreTables.getAutostrategiesStateHistoryTable(),
                coreTables.getBlueAutostrategiesStateHistoryTable(),
                coreTables.getVendorBlueAutostrategiesStateHistoryTable());
        var search = target.get(whiteSearch, blueSearch);
        this.statesExecutor = target.get(
                executors.autostrategiesStateWhite(),
                executors.autostrategiesStateBlue(),
                executors.autostrategiesStateVendorBlue());
        priceRecommendationExecutor = executors.priceRecommendations();

        cleanUpTables(table, search);

        var virtualShopId = target.get(SHOP1, ApiConst.VIRTUAL_SHOP_BLUE);
        var shop = TmsTestUtils.shop(virtualShopId);
        shop.setStatus(ShopStatus.ACTIVE);

        var feeds = target.get(Set.of((long) FEED1, (long) FEED2), Set.of((long) ApiConst.VIRTUAL_FEED_BLUE));
        shop.setFeeds(feeds);

        priceRecommendationExecutor.clearTargetTable();
        filterExecutor.clearTargetTable();
        autostrategiesExecutor.clearTargetTable();
        statesExecutor.clearTargetTable();

        testControls.initOnce(getClass(), () ->
                testControls.executeInParallel(
                        () -> testControls.saveShop(shop),
                        () -> {
                            if (virtualShopId != ApiConst.VIRTUAL_SHOP_BLUE) {
                                // Эти магазины нужны только для белого
                                Stream.of(SHOP2).forEach(shopId ->
                                        testControls.saveShop(TmsTestUtils.shop(shopId, s -> {
                                            s.setStatus(ShopStatus.ACTIVE);
                                            s.setFeeds(feeds);
                                        })));
                            }
                        },
                        getSaveOffersRunnable(target.get(false, true), SHOP1, SHOP2, offersExecutor)
                ));
    }

    @Test
    void testPriceRecommendationCounterExist() {
        // autostrategy created
        Autostrategy autostrategy = createAutostrategy("WithPriceRec", 0.1, RecommendationType.DEFAULT);
        filterExecutor.insert(List.of(getFilter(autostrategy)));
        autostrategiesExecutor.insert(List.of(autostrategy));
        // cycle recalculated the number of recommendations (recommendedCount)
        AutostrategyState stateWithPriceRecommendations = getAutostrategyState(autostrategy);
        statesExecutor.insert(List.of(stateWithPriceRecommendations));

        ResponseEntity<List<AutostrategyLoad>> load = publicApi.autostrategiesGet((int) autostrategy.getShop_id(),
                target.name());
        Assertions.assertNotNull(load.getBody());
        Assertions.assertFalse(load.getBody().isEmpty());
        AutostrategyLoad autostrategyLoad = load.getBody().get(0);
        Assertions.assertEquals(stateWithPriceRecommendations.getRes_count(),
                (long) autostrategyLoad.getRecommendedCount());
    }

    @Test
    public void testShowOffersWithPriceRecommendations() {
        Autostrategy autostrategy = createAutostrategy("Mock", 6.5, RecommendationType.DEFAULT);
        List<Offer> offers = bindOffers(autostrategy.getAutostrategy_id(), 100L);
        addPriceRecommendations(offers);
        ResponseEntity<List<AutostrategyOffer>> offerList = publicApi.autostrategiesOffersPost(SHOP1,
                new AutostrategiesOfferFilter(), 1, 100, target.name());

        printPriceRecommendations();
        Assertions.assertNotNull(offerList.getBody());
        Assertions.assertFalse(offerList.getBody().isEmpty());
        for (AutostrategyOffer autostrategyOffer : offerList.getBody()) {
            Assertions.assertTrue(autostrategyOffer.getPriceStatus() > 0);
            Assertions.assertTrue(autostrategyOffer.getOrigPrice() > 0);
            Assertions.assertTrue(autostrategyOffer.getRecommendedPrice() > 0.0);
            Assertions.assertTrue(autostrategyOffer.getRecommendedPromocode() > 0.0);
            Assertions.assertTrue(autostrategyOffer.getBid() > 0L);
            Assertions.assertNotNull(autostrategyOffer.getComputationDatetime());
        }
    }

    @Test
    public void testShowOffersWithPriceRecommendationsReturnEmptyBecauseAllHasGoodPrice() {
        Autostrategy autostrategy = createAutostrategy("Mock", 6.5, RecommendationType.DEFAULT);
        List<Offer> offers = bindOffers(autostrategy.getAutostrategy_id(), 100L);
        addPriceRecommendations(offers); // но рекомендации говорят что цена нормальная
        AutostrategiesOfferFilter autostrategiesOfferFilter = new AutostrategiesOfferFilter();
        autostrategiesOfferFilter.priceStatus(0); // ищем те у кого есть рекомендованная цена
        ResponseEntity<List<AutostrategyOffer>> offerList = publicApi.autostrategiesOffersPost(SHOP1,
                autostrategiesOfferFilter, 1, 100, target.name());

        Assertions.assertNotNull(offerList.getBody());
        Assertions.assertTrue(offerList.getBody().isEmpty());
    }

    @Test
    public void testShowOffersWithoutPriceRecommendationsReturnAllOffersBecauseFilterNotSet() {
        Autostrategy autostrategy = createAutostrategy("Mock", 6.5, RecommendationType.DEFAULT);
        List<Offer> offers = bindOffers(autostrategy.getAutostrategy_id(), 100L);
        Assertions.assertFalse(offers.isEmpty());

        ResponseEntity<List<AutostrategyOffer>> offerList = publicApi.autostrategiesOffersPost(SHOP1,
                new AutostrategiesOfferFilter(), 1, 100, target.name());

        Assertions.assertNotNull(offerList.getBody());
        Assertions.assertFalse(offerList.getBody().isEmpty());
        for (AutostrategyOffer autostrategyOffer : offerList.getBody()) {
            Assertions.assertTrue(autostrategyOffer.getPriceStatus() < 0);
        }
    }

    @Test
    public void testShowOffersWithoutPriceRecommendationsReturnOffersWithGoodPrice() {
        Autostrategy autostrategy = createAutostrategy("Mock", 6.5, RecommendationType.DEFAULT);
        List<Offer> offers = bindOffers(autostrategy.getAutostrategy_id(), 100L);
        addPriceRecommendations(offers.subList(0, 3));
        Assertions.assertFalse(offers.isEmpty());

        AutostrategiesOfferFilter autostrategiesOfferFilter = new AutostrategiesOfferFilter();
        autostrategiesOfferFilter.priceStatus(1); // ищем те у кого текущая цена нормальная цена
        ResponseEntity<List<AutostrategyOffer>> offerList = publicApi.autostrategiesOffersPost(SHOP1,
                autostrategiesOfferFilter, 1, 100, target.name());

        Assertions.assertNotNull(offerList.getBody());
        Assertions.assertFalse(offerList.getBody().isEmpty());
        Assertions.assertEquals(3, offerList.getBody().size());
    }

    @Test
    public void testOfferCountWithoutPriceRecommendationsReturnOffersWithGoodPrice() {
        Autostrategy autostrategy = createAutostrategy("Mock", 6.5, RecommendationType.DEFAULT);
        List<Offer> offers = bindOffers(autostrategy.getAutostrategy_id(), 100L);
        addPriceRecommendations(offers.subList(0, 3));
        Assertions.assertFalse(offers.isEmpty());

        AutostrategiesOfferFilter autostrategiesOfferFilter = new AutostrategiesOfferFilter();
        autostrategiesOfferFilter.priceStatus(1); // ищем те у кого текущая цена нормальная цена
        ResponseEntity<AutostrategiesOfferCount> offerCount = publicApi.autostrategiesOfferCountPost(SHOP1,
                autostrategiesOfferFilter, target.name());

        Assertions.assertEquals(3, offerCount.getBody().getOfferCount());
    }

    @Test
    public void testShowOffersWithoutPriceRecommendationsButWeLookingForOffersWithoutRecommendations() {
        Autostrategy autostrategy = createAutostrategy("Mock", 6.5, RecommendationType.DEFAULT);
        List<Offer> offers = bindOffers(autostrategy.getAutostrategy_id(), 100L);
        addPriceRecommendations(List.of(offers.get(0))); // есть одна рекомендация
        Assertions.assertFalse(offers.isEmpty());

        AutostrategiesOfferFilter autostrategiesOfferFilter = new AutostrategiesOfferFilter();
        autostrategiesOfferFilter.priceStatus(-1); // нам нужны те офферы у которых ни одной
        ResponseEntity<List<AutostrategyOffer>> offerList = publicApi.autostrategiesOffersPost(SHOP1,
                autostrategiesOfferFilter, 1, 100, target.name());

        printPriceRecommendations();
        Assertions.assertNotNull(offerList.getBody());
        Assertions.assertFalse(offerList.getBody().isEmpty());
        Assertions.assertEquals(offers.size() - 1, offerList.getBody().size());
    }

    private void addPriceRecommendations(List<Offer> offers) {
        List<PriceRecommendation> recs = new ArrayList<>();
        for (Offer offer : offers) {
            PriceRecommendation p = new PriceRecommendation();
            p.setPrice(11);
            p.setRecommended_price(123);
            p.setRecommended_promocode(456);
            p.setUpdated_at(Instant.now());
            p.setComputation_datetime(p.getUpdated_at());
            p.setStatus(1);
            p.setOffer_id(offer.getOffer_id());
            p.setPartner_id(offer.getShop_id());
            recs.add(p);
        }
        priceRecommendationExecutor.insert(recs);
    }

    private Filter getFilter(Autostrategy autostrategy) {
        Filter filter = new Filter();
        filter.setShop_id((int) autostrategy.getShop_id());
        filter.setFilter_class(FilterClass.WHITE_AUTOSTRATEGY);
        filter.setFilter_id(1011);
        autostrategy.setFilter_id(filter.getFilter_id());
        autostrategy.setFilter_type(FilterType.SIMPLE);
        return filter;
    }

    private AutostrategyState getAutostrategyState(Autostrategy ret) {
        AutostrategyState stateWithPriceRecommendations = new AutostrategyState();
        stateWithPriceRecommendations.setShop_id((int) ret.getShop_id());
        stateWithPriceRecommendations.setLinked_count(3);
        stateWithPriceRecommendations.setAutostrategy_id(ret.getAutostrategy_id());
        stateWithPriceRecommendations.setRes_count(14);
        stateWithPriceRecommendations.setLinked_at(Instant.now());
        stateWithPriceRecommendations.setLinked_enabled(true);
        stateWithPriceRecommendations.setLinked_count(111);
        return stateWithPriceRecommendations;
    }

    private void cleanUpTables(BindingTable<AutostrategyStateHistory> table, AutostrategiesSearch search) {
        AbstractAutostrategiesMetaProcessorTest.cleanupTables(search.getCfg(), search.getHistoryCfg(),
                search.getStateCfg(),
                ytCfg.getProcessorCfg(table),
                search.getFilterCfg(), search.getFilterHistoryCfg(),
                testControls);
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
}
