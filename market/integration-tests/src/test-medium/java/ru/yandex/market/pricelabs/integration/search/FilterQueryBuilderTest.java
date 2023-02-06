package ru.yandex.market.pricelabs.integration.search;

import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import ru.yandex.inside.yt.kosher.impl.ytree.object.annotation.YTreeObject;
import ru.yandex.market.pricelabs.cache.CachedCategories;
import ru.yandex.market.pricelabs.integration.AbstractIntegrationSpringConfiguration;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.processing.ShopFeedsArg;
import ru.yandex.market.pricelabs.processing.categories.CategoriesLoader;
import ru.yandex.market.pricelabs.search.FilterQueryBuilder;
import ru.yandex.market.pricelabs.search.FilterQueryBuilder.InclusionMode;
import ru.yandex.market.pricelabs.search.FilterQueryBuilder.QueryParameters.QueryParametersBuilder;
import ru.yandex.market.pricelabs.tms.TestControls;
import ru.yandex.market.pricelabs.tms.cache.CachedData;
import ru.yandex.market.pricelabs.tms.cache.CachedDataSource;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;
import ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios;
import ru.yandex.market.yt.binding.YTBinder;
import ru.yandex.market.yt.client.YtClientProxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.FEED_ID;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.REGION_ID;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.SHOP_ID;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.SHOP_ID2;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.SHOP_ID3;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.categories;
import static ru.yandex.market.pricelabs.tms.processing.offers.StrategiesFilterScenarios.scenarios;

// Задача этого класса - гарантировать, что все фильтры, которые мы проверяем в потоковом режиме,
// работают точно так же при выборках из YT
@Slf4j
public class FilterQueryBuilderTest extends AbstractIntegrationSpringConfiguration {

    private static final YTBinder<OfferIdObject> ID_BINDER = YTBinder.getBinder(OfferIdObject.class);

    private final Random random = new Random();

    @Autowired
    private TestClassInitializer initializer;

    @Autowired
    @Qualifier("filterQueryBuilderWhite")
    private FilterQueryBuilder filterQueryBuilder;

    // Все сценарии одним куском (т.к. это medium тесты)
    static Object[][] junitScenarios() {
        return StrategiesFilterScenarios.junitScenarios().stream().flatMap(Stream::of).toArray(Object[][]::new);
    }


    // Проверять тесты полностью независимо (т.е. вставлять офферы, фильтр, чистить и т.д.) - дорого
    // Вместо этого выполним вставку всех данных разово, а потом просто проверим результат выполнения запросов
    // См. StrategyInitializer ниже

    @BeforeEach
    void init() {
        initializer.initOnce();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("junitScenarios")
    void checkScenarioInSQL(String testName, boolean expectTrue, Filter filter, Offer offer) {
        int shopId = (int) offer.getShop_id();
        var cachedData = initializer.loadShopCache(shopId);

        var actualFiler = cachedData.getFilterMap().get(filter.getFilter_id());
        assertNotNull(actualFiler, "Unable to find filter with id " + filter.getFilter_id());

        // Результат выборки не должен отличаться для разных параметров join-ов
        var params = new QueryParametersBuilder()
                .includeModels(random.nextBoolean() ? InclusionMode.LEFT_JOIN : null)
                .includeStatsOffer(random.nextBoolean() ? InclusionMode.LEFT_JOIN : null)
                .includeCategories(random.nextBoolean() ? InclusionMode.LEFT_JOIN : null)
                .includeShops(random.nextBoolean() ? InclusionMode.JOIN : null)
                .includeStatsDays(random.nextBoolean() ? InclusionMode.LEFT_JOIN : null)
                .select("offers.offer_id as offer_id")
                .orderBy(random.nextBoolean() ? "offers.shop_id, offers.feed_id, offers.offer_id" : null)
                .limit(initializer.offersCount + random.nextInt(100))
                .build();

        var shop = cachedData.getShop();

        var categories = initializer.cachedCategoriesMap.get(shopId);
        assertNotNull(shop, "Unable to find categories with id " + shopId);

        var filters = Map.of(shop, categories.matchWithCategories(actualFiler));
        var query = filterQueryBuilder.buildQuery(filters, params);
        assertFalse(query.isEmpty());

        log.info("Filter: {}", filter);
        log.info("Query: {}", query);

        assertFalse(query.isAllowJoinWithoutIndex());

        var offers = initializer.ytClient.selectRows(query.getQuery(), ID_BINDER).stream()
                .map(OfferIdObject::getOffer_id)
                .collect(Collectors.toSet());
        log.info("Found offers: {}", offers);

        assertEquals(expectTrue, offers.contains(offer.getOffer_id()),
                "Unmatched query result for " + offer.getOffer_id());

    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    @YTreeObject
    static class OfferIdObject {
        private String offer_id;

        public String getOffer_id() {
            return offer_id;
        }
    }

    @Component
    public static class TestClassInitializer {

        @Autowired
        private CachedDataSource cachedDataSource;

        @Autowired
        private TestControls testControls;

        @Autowired
        private ExecutorSources executors;

        @Autowired
        @Qualifier("categoriesLoaderWhite")
        private CategoriesLoader categoriesLoader;

        private YtClientProxy ytClient;

        private Int2ObjectMap<CachedCategories> cachedCategoriesMap;

        private Int2ObjectMap<CachedData> cachedShopData;

        private int offersCount;

        private boolean configured;

        void initOnce() {
            if (configured) {
                return; // ---
            }

            // Вставим все данные в таблицы
            var scenarios = scenarios();

            var offersExecutor = executors.offers();
            var filtersExecutor = executors.filters();
            var categoriesExecutor = executors.categories();

            var offers = offersExecutor.asCreated(scenarios.stream()
                    .map(StrategiesFilterScenarios.Scenario::getOffer)
                    .collect(Collectors.toList()));
            this.offersCount = offers.size();

            var filters = filtersExecutor.asCreated(scenarios.stream()
                    .map(StrategiesFilterScenarios.Scenario::getFilter)
                    .collect(Collectors.toList()));
            var categories = categoriesExecutor.asUpdated(categories());

            testControls.executeInParallel(
                    () -> offersExecutor.insert(offers),
                    () -> filtersExecutor.insert(filters),
                    () -> testControls.saveShop(shop(SHOP_ID, FEED_ID, REGION_ID)),
                    () -> testControls.saveShop(shop(SHOP_ID2, FEED_ID, REGION_ID)),
                    () -> testControls.saveShop(shop(SHOP_ID3, FEED_ID, REGION_ID)),
                    () -> categoriesExecutor.insert(categories)
            );

            cachedCategoriesMap = new Int2ObjectOpenHashMap<>();
            IntStream.of(SHOP_ID, SHOP_ID2, SHOP_ID3).forEach(shopId ->
                    cachedCategoriesMap.put(shopId, new CachedCategories(() ->
                            categoriesLoader.loadCategories(new ShopFeedsArg(shopId, (long) FEED_ID))))
            );

            cachedShopData = new Int2ObjectOpenHashMap<>();

            ytClient = offersExecutor.getCfg().getClient();
            configured = true;
        }

        CachedData loadShopCache(int shopId) {
            return cachedShopData.computeIfAbsent(shopId,
                    key -> cachedDataSource.loadShopCache(shopId));
        }
    }
}
