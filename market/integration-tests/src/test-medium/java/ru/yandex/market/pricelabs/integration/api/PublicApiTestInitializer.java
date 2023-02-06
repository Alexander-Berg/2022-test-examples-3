package ru.yandex.market.pricelabs.integration.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import ru.yandex.market.pricelabs.MockMvcProxy;
import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.exports.ExporterTestContextCsv;
import ru.yandex.market.pricelabs.exports.ExporterTestContextExcel;
import ru.yandex.market.pricelabs.model.ModelbidsPosition;
import ru.yandex.market.pricelabs.model.ModelbidsRecommendation;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.TestControls;
import ru.yandex.market.pricelabs.tms.processing.ExecutorSources;
import ru.yandex.market.pricelabs.tms.processing.GenerationStateCollector;
import ru.yandex.market.pricelabs.tms.processing.categories.CategoriesProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.modelbids.ModelbidsRecommendationSyncProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.integration.api.PublicApiTest.checkResponse;
import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offerVendor;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shopCategory;

@Component
public class PublicApiTestInitializer {

    static final long PL1_SHOP_ID = 49999;
    static final long PL1_USER_ID = 42054;

    static final long USER_ID = 44699999L;

    static final int SHOP_ID = 2289;
    static final int SHOP_ID_2 = 2290;
    static final int SHOP_ID_3 = 2291;
    static final int SHOP_ID_4 = 2292;
    static final int SHOP_ID_5 = 2293;
    static final int SHOP_ID_6 = 2294;

    static final int SHOP_ID_10 = 3456;

    static final int FEED_ID = 3393;
    static final int FEED_ID_2 = 3394;
    static final int FEED_ID_3 = 3395;
    static final int FEED_ID_4 = 3396;
    static final int FEED_ID_5 = 3397;
    static final int FEED_ID_6 = 3398;

    static final int REGION_ID = 123;
    static final int REGION_ID_2 = 234;
    static final int REGION_ID_KZT = 163;
    static final int REGION_ID_3 = 4001;
    static final int REGION_ID_4 = 4002;
    static final int REGION_ID_5 = 4003;
    static final int REGION_ID_6 = 4004;

    static final String KZT_CURRENCY = "KZT";

    static final String OFFER_ID = "4105";

    static final int MODEL_ID_1 = 12631379;
    static final int ZERO_MODEL_ID = 111;

    static final int SHOP_ID_N = 10000;
    static final int SHOP_ID_N2 = 10001;
    static final int FEED_ID_N = 20000;

    static final int UNKNOWN_SHOP = 332211;
    OffersArg args;
    @Autowired
    private TestControls testControls;
    @Autowired
    private ExecutorSources executors;
    @Autowired
    private GenerationStateCollector stateCollector;

    void init() {
        var offersExecutorGen = executors.offersGen();
        var categoriesExecutor = executors.categories();
        var categoriesExecutorBlue = executors.blueCategories();
        var modelbidsExecutor = executors.modelBids();
        var offerVendorsExecutor = executors.offerVendors();
        var offerBlueVendorsExecutor = executors.offerBlueVendor();
        var offerBlueExecutor = executors.offersBlue();

        var offers = getOffers();
        var blueOffers = getBlueOffers();

        var offerVendors = List.of(offerVendor(SHOP_ID, "v1", getInstant()),
                offerVendor(SHOP_ID, "v2", getInstant()),
                offerVendor(SHOP_ID_2, "v3", getInstant()));

        var offerBlueVendors = List.of(offerVendor(SHOP_ID, "bv1", getInstant()),
                offerVendor(SHOP_ID, "bv2", getInstant()),
                offerVendor(SHOP_ID_2, "bv3", getInstant()));

        testControls.executeInParallel(
                () -> testControls.saveShop(shop(SHOP_ID, s -> {
                    s.setFeeds(Set.of((long) FEED_ID));
                    s.setRegion_id(REGION_ID);
                    s.setStatus(ShopStatus.ACTIVE);
                    s.setDomain("test.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_2, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_2));
                    s.setRegion_id(REGION_ID_2);
                    s.setDomain("test2.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_N, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_N, (long) FEED_ID_N + 1, (long) FEED_ID_N + 2));
                    s.setDomain("testn.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_3, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_3));
                    s.setRegion_id(REGION_ID_3);
                    s.setDomain("test3.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_4, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_4));
                    s.setRegion_id(REGION_ID_4);
                    s.setDomain("test4.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_5, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_5));
                    s.setRegion_id(REGION_ID_5);
                    s.setDomain("test5.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_6, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_6));
                    s.setRegion_id(REGION_ID_6);
                    s.setDomain("test6.shop");
                })),
                () -> testControls.saveShop(shop(SHOP_ID_N2, s -> {
                    s.setFeeds(Set.of((long) FEED_ID_N));
                    s.setDomain("testn2.shop");
                })),
                offersExecutorGen::clearSourceTable,
                offersExecutorGen::clearSourceTable,
                categoriesExecutor::clearSourceTable,
                () -> offersExecutorGen.insert(offers),
                () -> categoriesExecutor.insert(initCategoriesWhite()),
                () -> categoriesExecutorBlue.insert(initCategoriesBlue()),
                () -> modelbidsExecutor.insert(initModelbidsRecommendation()),
                () -> offerVendorsExecutor.insert(offerVendors),
                () -> offerBlueVendorsExecutor.insert(offerBlueVendors),
                () -> offerBlueExecutor.insert(blueOffers)
        );

        var state = Objects.requireNonNull(stateCollector.getNewGenerationState());
        args = new OffersArg()
                .setShopId(SHOP_ID)
                .setCluster(state.getClusterName())
                .setIndexer(state.getIndexerName())
                .setGeneration(state.getGenOffersState().getTableName())
                .setCategoriesTable(state.getCategoriesState().getTableName());
    }

    private List<Offer> getBlueOffers() {
        return getOffers().stream().peek(o -> {
            o.setOffer_id(o.getOffer_id() + "_blue");
            o.setName(o.getName() + "_blue");
            o.setShop_id(SHOP_ID_10);
            o.setSsku_offer(true);
            o.setStatus(Status.ACTIVE);
            o.setName_index(o.getName_index() + "blue+");
            o.setOffer_id_index(o.getOffer_id_index() + "blue+");
        }).collect(Collectors.toList());
    }

    private List<Offer> getOffers() {
        var offers = new OffersProcessorBasicTest().readTargetList();
        assertEquals(11, offers.size());
        for (int i = 0; i < offers.size(); i++) {
            Offer offer = offers.get(i);
            offer.setUpdated_at(getInstant());
            if (!"4105".equals(offer.getOffer_id())) {
                if (i <= 5) {
                    offer.setModel_id(1);
                } else {
                    offer.setModel_id(2);
                }
            }
            offer.setMin_bid(1500 + i * 100);
            if (offer.isCard()) {
                offer.setMs_bid_1(2000 + i * 100 + 10);
                offer.setMs_bid_10(2000 + i * 100 + 10 * 10);
            } else {
                offer.setMs_bid_1(1000 + i * 100 + 10);
                offer.setMs_bid_10(1000 + i * 100 + 10 * 10);
                offer.setMs_bid_11(1000 + i * 100 + 11 * 10);
            }
        }

        return offers;
    }

    private List<ShopCategory> initCategoriesWhite() {
        var categories = new ArrayList<>(new CategoriesProcessorTest().readTargetList());
        categories.add(shopCategory(SHOP_ID_N + 1, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 1");
            cat.setOffer_count(1);
            cat.setCurrent_offer_count(1);
            cat.setChildren_count(10);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 2, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 2");
            cat.setOffer_count(21);
            cat.setCurrent_offer_count(2);
            cat.setChildren_count(2);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 1, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 1);
            cat.setParent_category_id(SHOP_ID_N + 1);
            cat.setName("name 1.1");
            cat.setOffer_count(2);
            cat.setCurrent_offer_count(3);
            cat.setChildren_count(20);
            cat.setTree_lvl(2);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 1, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 2);
            cat.setParent_category_id(SHOP_ID_N + 1);
            cat.setName("name 1.2");
            cat.setOffer_count(3);
            cat.setCurrent_offer_count(4);
            cat.setChildren_count(30);
            cat.setTree_lvl(3);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 3, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 3");
            cat.setOffer_count(199);
            cat.setCurrent_offer_count(5);
            cat.setChildren_count(299);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 3, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 1);
            cat.setParent_category_id(SHOP_ID_N + 3);
            cat.setName("name 3");
            cat.setOffer_count(399);
            cat.setCurrent_offer_count(6);
            cat.setChildren_count(499);
            cat.setTree_lvl(2);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 4, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 1);
            cat.setParent_category_id(SHOP_ID_N + 3);
            cat.setName("name 4");
            cat.setOffer_count(1);
            cat.setCurrent_offer_count(7);
            cat.setChildren_count(2);
            cat.setTree_lvl(3);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 4, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 2);
            cat.setParent_category_id(SHOP_ID_N + 1);
            cat.setName("name 4");
            cat.setOffer_count(3);
            cat.setCurrent_offer_count(8);
            cat.setChildren_count(4);
            cat.setTree_lvl(3);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 5, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 2);
            cat.setParent_category_id(SHOP_ID_N + 4);
            cat.setName("name 4.1");
            cat.setOffer_count(19);
            cat.setCurrent_offer_count(9);
            cat.setChildren_count(1);
            cat.setTree_lvl(4);
            cat.setStatus(Status.ACTIVE);
        }));

        // Будет проигнорирована (фид не входит в список текущих фидов магазина)
        categories.add(shopCategory(SHOP_ID_N + 6, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(FEED_ID_N + 3);
            cat.setParent_category_id(SHOP_ID_N + 4);
            cat.setName("name 4.2");
            cat.setOffer_count(29);
            cat.setCurrent_offer_count(99);
            cat.setChildren_count(10);
            cat.setTree_lvl(40);
            cat.setStatus(Status.ACTIVE);
        }));

        // Из другого магазина
        categories.addAll(initCategoriesWhiteShopN2());

        return categories;
    }

    private List<ShopCategory> initCategoriesWhiteShopN2() {
        List<ShopCategory> categories = new ArrayList<>();
        categories.add(shopCategory(SHOP_ID_N + 1, cat -> {
            cat.setShop_id(SHOP_ID_N2);
            cat.setFeed_id(FEED_ID_N);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 1");
            cat.setOffer_count(1);
            cat.setCurrent_offer_count(0);
            cat.setChildren_count(2);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 2, cat -> {
            cat.setShop_id(SHOP_ID_N2);
            cat.setFeed_id(FEED_ID_N);
            cat.setParent_category_id(SHOP_ID_N + 1);
            cat.setName("name 2");
            cat.setOffer_count(1);
            cat.setCurrent_offer_count(1);
            cat.setChildren_count(1);
            cat.setTree_lvl(2);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 3, cat -> {
            cat.setShop_id(SHOP_ID_N2);
            cat.setFeed_id(FEED_ID_N);
            cat.setParent_category_id(SHOP_ID_N + 2);
            cat.setName("name 3");
            cat.setOffer_count(0);
            cat.setCurrent_offer_count(0);
            cat.setChildren_count(0);
            cat.setTree_lvl(3);
            cat.setStatus(Status.ACTIVE);
        }));
        return categories;
    }

    private List<ShopCategory> initCategoriesBlue() {
        var categories = new ArrayList<>(new CategoriesProcessorTest().readTargetList());
        categories.add(shopCategory(SHOP_ID_N + 1, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(ApiConst.VIRTUAL_FEED_BLUE);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 1");
            cat.setOffer_count(1 + 2 + 3);
            cat.setCurrent_offer_count(1 + 3 + 4);
            cat.setChildren_count(10 + 20 + 30);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 2, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(ApiConst.VIRTUAL_FEED_BLUE);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 2");
            cat.setOffer_count(21);
            cat.setCurrent_offer_count(2);
            cat.setChildren_count(2);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 3, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(ApiConst.VIRTUAL_FEED_BLUE);
            cat.setParent_category_id(ShopCategory.NO_CATEGORY);
            cat.setName("name 3");
            cat.setOffer_count(199 + 399);
            cat.setCurrent_offer_count(5 + 6);
            cat.setChildren_count(299 + 499);
            cat.setTree_lvl(1);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 4, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(ApiConst.VIRTUAL_FEED_BLUE);
            cat.setParent_category_id(SHOP_ID_N + 3);
            cat.setName("name 4");
            cat.setOffer_count(1 + 3);
            cat.setCurrent_offer_count(7 + 8);
            cat.setChildren_count(2 + 4);
            cat.setTree_lvl(3);
            cat.setStatus(Status.ACTIVE);
        }));
        categories.add(shopCategory(SHOP_ID_N + 5, cat -> {
            cat.setShop_id(SHOP_ID_N);
            cat.setFeed_id(ApiConst.VIRTUAL_FEED_BLUE);
            cat.setParent_category_id(SHOP_ID_N + 4);
            cat.setName("name 4.1");
            cat.setOffer_count(19);
            cat.setCurrent_offer_count(9);
            cat.setChildren_count(1);
            cat.setTree_lvl(4);
            cat.setStatus(Status.ACTIVE);
        }));
        return categories;
    }

    private List<ModelbidsRecommendation> initModelbidsRecommendation() {
        var modelbidsRecommendation = new ModelbidsRecommendationSyncProcessorTest().readTargetList();
        modelbidsRecommendation.forEach(recom -> {
            if (recom.getModel_id() == MODEL_ID_1) {
                List<ModelbidsPosition> modelbidsPositionList = new ArrayList<>();
                ModelbidsPosition modelbidsPosition = new ModelbidsPosition();
                modelbidsPosition.setPosition(1);
                modelbidsPosition.setCode(0);
                modelbidsPosition.setVbid(recom.getVbid());
                modelbidsPositionList.add(modelbidsPosition);

                ModelbidsPosition modelbidsPosition1 = new ModelbidsPosition();
                modelbidsPosition1.setPosition(2);
                modelbidsPosition1.setCode(0);
                modelbidsPosition1.setVbid(recom.getVbid() + 10);
                modelbidsPositionList.add(modelbidsPosition1);
                recom.setPositions(modelbidsPositionList);
            }
            recom.setModel_updated_at(Instant.now());
        });
        return modelbidsRecommendation;
    }

    <T> void checkResponseIsCsv(Supplier<ResponseEntity<?>> response, Class<T> clazz, String expectCsv) {
        var ret = MockMvcProxy.withMediaType(ApiConst.MIME_CSV, response);
        new ExporterTestContextCsv<>().verify(expectCsv, checkResponse(ret));
    }

    <T> void checkResponseIsExcel(Supplier<ResponseEntity<?>> response, Class<T> clazz, String expectExcel) {
        var ret = MockMvcProxy.withMediaType(ApiConst.MIME_EXCEL, response);
        new ExporterTestContextExcel<>().verify(expectExcel, checkResponse(ret));
    }
}
