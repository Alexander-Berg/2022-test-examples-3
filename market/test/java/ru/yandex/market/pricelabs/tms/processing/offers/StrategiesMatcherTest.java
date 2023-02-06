package ru.yandex.market.pricelabs.tms.processing.offers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;

import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.StrategyPair;
import ru.yandex.market.pricelabs.model.types.StrategyFormType;
import ru.yandex.market.pricelabs.model.types.StrategyObjectType;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;

import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.filter;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.strategy;

class StrategiesMatcherTest {

    static final int SHOP_ID = 1;
    static final int FEED_ID = 2;
    static final int REGION_ID = 3;

    private Instant now;
    private TmsTestUtils.CachedShopContent content;
    private TmsTestUtils.DataSourceContent dsContent;
    private StrategiesMatcher matcher;

    @BeforeEach
    void init() {
        this.now = getInstant();
        this.content = TmsTestUtils.getCachedShopContent(SHOP_ID, FEED_ID, REGION_ID);
        this.dsContent = content.dsContent;
    }

    private static Object[][] strategyOrder() {
        var data = new StrategyOrderData();

        var offer = offer("1", o -> {
            o.setModel_id(1);
            o.setCategory_id(100);
            o.setShop_id(SHOP_ID);
            o.setFeed_id(FEED_ID);
        });

        var offerNoCat = offer("1", o -> {
            o.setModel_id(1);
            o.setShop_id(SHOP_ID);
            o.setFeed_id(FEED_ID);
        });

        var offerNoFeed = offer("1", o -> {
            o.setModel_id(1);
            o.setCategory_id(100);
            o.setShop_id(SHOP_ID);
            o.setFeed_id(FEED_ID + 1);
        });

        return new Object[][]{
                {List.of(data.s1, data.s2, data.s3, data.s4, data.s5), data.filters, offer, data.s5, "By Offer_id"},
                {List.of(data.s1, data.s2, data.s3, data.s4), data.filters, offer, data.s4, "By Filter"},
                {List.of(data.s1, data.s2, data.s3, data.s4, data.s4p1), data.filters, offer,
                        data.s4p1, "By Filter, p1"},
                {List.of(data.s1, data.s2, data.s3, data.s4, data.s4p2), data.filters, offer,
                        data.s4p2, "By Filter, p2"},
                {List.of(data.s1, data.s2, data.s3, data.s4, data.s4p1, data.s4p2), data.filters, offer,
                        data.s4p2, "By Filter, p2"},
                {List.of(data.s1, data.s2, data.s3), data.filters, offer, data.s3, "By Category"},
                {List.of(data.s1, data.s2, data.s3), data.filters, offerNoCat, data.s2, "By Feed (no category)"},
                {List.of(data.s1, data.s2), data.filters, offer, data.s2, "By Feed"},
                {List.of(data.s1, data.s2), data.filters, offerNoFeed, data.s1, "By Shop)"},
                {List.of(data.s2), data.filters, offerNoFeed, null, "Not Matched (no feed, no shop)"},
        };
    }

    private static class StrategyOrderData {
        private final StrategyPair s1; // По магазину
        private final StrategyPair s2; // По фиду
        private final StrategyPair s3; // По категории
        private final StrategyPair s4; // По фильтру
        private final StrategyPair s4p1; // По фильтру
        private final StrategyPair s4p2; // По фильтру
        private final StrategyPair s5; // По офферу

        private final List<Filter> filters = new ArrayList<>();

        private StrategyOrderData() {
            // Стратегия по shop
            this.s1 = StrategyPair.fromCardSearch(
                    strategy(1, s -> {
                        s.setObject_type(StrategyObjectType.SHOP);
                        s.setShop_id(SHOP_ID);
                    }),
                    strategy(100, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(1);
                    }));

            // Стратегия по shop_feed
            this.s2 = StrategyPair.fromCardSearch(
                    strategy(2, s -> {
                        s.setObject_type(StrategyObjectType.FILTER);
                        s.setObject_id("20");
                        s.setShop_id(SHOP_ID);
                    }),
                    strategy(200, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(2);
                    }));
            filters.add(filter(20, f -> {
                f.setShop_id(SHOP_ID);
                f.setFeed_id(FEED_ID);
            }));

            // Стратегия по категории
            this.s3 = StrategyPair.fromCardSearch(
                    strategy(3, s -> {
                        s.setObject_type(StrategyObjectType.CATEGORY);
                        s.setObject_id("100");
                        s.setShop_id(SHOP_ID);
                    }),
                    strategy(300, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(3);
                    }));

            // Стратегия по фильтру
            this.s4 = StrategyPair.fromCardSearch(
                    strategy(4, s -> {
                        s.setObject_type(StrategyObjectType.FILTER);
                        s.setObject_id("40");
                        s.setShop_id(SHOP_ID);
                    }),
                    strategy(400, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(4);
                    }));

            filters.add(filter(40, f -> {
                f.setShop_id(SHOP_ID);
                f.setCategories_by_id(Set.of(100L));
            }));

            this.s4p1 = StrategyPair.fromCardSearch(
                    strategy(41, s -> {
                        s.setObject_type(StrategyObjectType.FILTER);
                        s.setObject_id("401");
                        s.setShop_id(SHOP_ID);
                    }),
                    strategy(4001, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(41);
                    }));

            filters.add(filter(401, f -> {
                f.setShop_id(SHOP_ID);
                f.setCategories_by_id(Set.of(100L));
                f.setSort_order(1);
            }));

            this.s4p2 = StrategyPair.fromCardSearch(
                    strategy(42, s -> {
                        s.setObject_type(StrategyObjectType.FILTER);
                        s.setObject_id("402");
                        s.setShop_id(SHOP_ID);
                    }),
                    strategy(4002, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(42);
                    }));

            // Какой же приоритет, как и 401, но имеет бОльший номер - т.е. он важнее
            filters.add(filter(402, f -> {
                f.setShop_id(SHOP_ID);
                f.setCategories_by_id(Set.of(100L));
                f.setSort_order(1);
            }));

            // Стратегия по офферу
            this.s5 = StrategyPair.fromCardSearch(
                    strategy(5, s -> {
                        s.setObject_type(StrategyObjectType.OFFER);
                        s.setObject_id("1");

                        s.setShop_id(1);
                    }),
                    strategy(500, s -> {
                        s.setShop_id(SHOP_ID);
                        s.setType(StrategyFormType.SEARCH);
                        s.setParent_strategy_id(5);
                    }));
        }
    }

}
