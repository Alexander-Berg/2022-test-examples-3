//CHECKSTYLE:OFF
package ru.yandex.market.pricelabs.tms.processing.offers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.NonNull;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.Strategy;
import ru.yandex.market.pricelabs.model.types.DaysLimit;
import ru.yandex.market.pricelabs.model.types.FilterMinBidMode;
import ru.yandex.market.pricelabs.model.types.FilterOfferType;
import ru.yandex.market.pricelabs.model.types.FilterPriceType;
import ru.yandex.market.pricelabs.model.types.Shard;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.StrategyObjectType;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;

import static ru.yandex.market.pricelabs.processing.ProcessingUtils.toCents;

public class StrategiesFilterScenarios {
    public static final int TOTAL_SCENARIO_GROUPS = 16;

    public static final int SHOP_ID = 1;
    public static final int SHOP_ID2 = 2;
    public static final int SHOP_ID3 = 3;
    public static final int FEED_ID = 2;
    public static final int REGION_ID = 3;

    private static final AtomicLong ID = new AtomicLong(Shard.SHARD5.getIdPrefix());

    private static final long MODEL_MATCH_AVG = toCents(2 * 1.05);
    private static final int MODEL_MATCH_ID = 1000;
    private static final int MODEL_NON_MATCH_ID = 1001;

    private StrategiesFilterScenarios() {
        //
    }

    public static List<ShopCategory> categories() {
        return Holder.CONTENT.categories;
    }

    public static List<Scenario> scenarios() {
        return Holder.CONTENT.scenarios;
    }

    public static List<Object[][]> junitScenarios() {
        var scenarios = Holder.CONTENT.junitScenarios;
        assert scenarios.size() == TOTAL_SCENARIO_GROUPS :
                "expect exactly " + TOTAL_SCENARIO_GROUPS + " groups, found " + scenarios.size();
        return scenarios;
    }


    private static ScenarioContent scenariosImpl() {
        return new ScenarioContent(new ScenariosBuilder().init());
    }

    private static class Holder {
        private static final ScenarioContent CONTENT = scenariosImpl();
    }

    private static class ScenariosBuilder {
        // Делаем простое 3-х уровневое дерево с двумя вершинами из коря
        //       400
        //      /   \
        //     200   500
        //    /   \
        // 100     300

        private final List<ShopCategory> categories = buildCategories();
        private final List<Scenario> scenarios = new ArrayList<>(1024);

        // Список сценариев один и тот же для проверок в двух режимах - в потоковом и запросах в динтаблицы

        ScenariosBuilder init() {
            this.init1();
            this.init2();
            this.init5();
            this.init6();
            return this;
        }

        //CHECKSTYLE:OFF
        void init1() {

            check("empty", true,
                    filter(Utils.emptyConsumer()),
                    offer("001"));
            //

            check("byFeedId", false,
                    filter(f -> f.setFeed_id(FEED_ID)),
                    offer("002", o -> o.setFeed_id(FEED_ID + 1)));

            check("byFeedId", true,
                    filter(f -> f.setFeed_id(FEED_ID)),
                    offer("003"));
            //

            check("byQueryOnlyId", false,
                    filter(f -> f.set_query_only_id(true)), // Нет списка id для поиска
                    offer("006"));
            //

            check("byQuery", false,
                    filter(f -> f.setQuery("Q1")),
                    offer("007"));
            check("byQuery_name", true,
                    filter(f -> f.setQuery("Q0")),
                    offer("008", o -> o.setName("offer-q0-test")));
            check("byQuery_name", false,
                    filter(f -> f.setQuery("ffer")),
                    offer("008.1", o -> o.setName("offer-q0-test")));
            check("byQuery_name_escape", false,
                    filter(f -> f.setQuery("offer-Q\"0\"")),
                    offer("008.2", o -> o.setName("offer-q0-test")));
            check("byQuery_name_escape", true,
                    filter(f -> f.setQuery("offer-Q\"0\"")),
                    offer("008.3", o -> o.setName("offer-q\"0\"-test")));
            check("byQuery_name_not_unmatched", false,
                    filter(f -> f.setQuery("offer -q88")),
                    offer("008.4", o -> o.setName("offer-q88-test")));
            check("byQuery_name_not_matched", true,
                    filter(f -> f.setQuery("offer -q88")),
                    offer("008.5", o -> o.setName("offer-q99-test")));
            check("byQuery_name_not_unmatched", false,
                    filter(f -> f.setQuery("086 -6")),
                    offer("008.6", o -> o.setName("offer-q00-test")));
            check("byQuery_name_not_matched", false,
                    filter(f -> f.setQuery("086 -6")),
                    offer("008.7", o -> o.setName("offer-q11-test")));
            check("byQuery_name_quotes", true,
                    filter(f -> f.setQuery("Acuvue OASYS -\"1-Day\" -\"Astigmatism \"")),
                    offer("008.8", o -> o
                            .setName("Контактные линзы Acuvue Acuvue Oasys with Hydraclear Plus 6 шт")));
            check("byQuery_name_quotes", false,
                    filter(f -> f.setQuery("Acuvue OASYS -\"1-Day\" -\"Astigmatism \"")),
                    offer("008.9", o -> o
                            .setName("Контактные линзы Acuvue OASYS 1-Day with HydraLuxe Technology, 30 шт (8.5, " +
                                    "-0.50)")));
            check("byQuery_id", true,
                    filter(f -> f.setQuery("F111")),
                    offer("F11111", o -> o.setName("offer-q1-test")));
            check("byQuery_id_numeric", true,
                    filter(f -> f.setQuery("111")),
                    offer("11111", o -> o.setName("offer-q1-test")));
            check("byQuery_withOnlyId", false,
                    filter(f -> {
                        f.setQuery("222");
                        f.set_query_only_id(true);
                    }),
                    offer("F22222", o -> o.setName("offer-q2-test")));
            check("byQuery_withOnlyId2", false,
                    filter(f -> {
                        f.setQuery("333");
                        f.set_query_only_id(true);
                    }),
                    offer("F33333", o -> o.setName("offer-333-test")));
            check("byQuery_withOnlyId", true,
                    filter(f -> {
                        f.setQuery("F44444");
                        f.set_query_only_id(true);
                    }),
                    offer("F44444", o -> o.setName("offer-q4-test")));
            check("byQuery_withOnlyId2", true,
                    filter(f -> {
                        f.setQuery("005,F55,F555,F5555,F55555");
                        f.set_query_only_id(true);
                    }),
                    offer("F55555", o -> o.setName("offer-q5-test")));
            check("byQuery_withOnlyId_escape", false,
                    filter(f -> {
                        f.setQuery("005,F55,F555,F5555,\"F66\"");
                        f.set_query_only_id(true);
                    }),
                    offer("F66", o -> o.setName("offer-F66-test")));
            check("byQuery_withOnlyId_and_other_condition", true,
                    filter(f -> {
                        f.setQuery("F67");
                        f.set_query_only_id(true);
                        f.getBids().setMin_bid_mode(FilterMinBidMode.NOT_MATCHED);
                    }),
                    offer("F67", o -> o.setName("offer-F67-test")));
            check("byQuery_nameAndId", false,
                    filter(f -> f.setQuery("F77 offer")),
                    offer("F77777", o -> o.setName("offer-q77-test")));
            check("byQuery_withNameAsIdSmall", true,
                    filter(f -> {
                        f.setQuery("81"); // будет найден как имя
                    }),
                    offer("F81", o -> o.setName("offer-81-test")));
            check("byQuery_withNameAsIdSmall_uniq3", true, // >15 uniq для включения 'as id'
                    filter(f -> {
                        f.setQuery("80,80,80,80,80,80,80,80,80,80,80,80,80,80,81,82"); // = 3, поиск везде
                    }),
                    offer("F82", o -> o.setName("offer-82-test")));
            check("byQuery_withNameAsIdSmall_uniq15", true,
                    filter(f -> {
                        f.setQuery("1,2,3,4,5,6,7,8,9,10,11,12,13,14,83"); // = 15, поиск везде
                    }),
                    offer("F83", o -> o.setName("offer-83-test")));
            check("byQuery_withNameAsIdSmall_uniq16", false,
                    filter(f -> {
                        f.setQuery("1,2,3,4,5,6,7,8,9,10,11,12,13,14,83,84"); // 15+, поиск только по id
                    }),
                    offer("F84", o -> o.setName("offer-83-test")));
            check("byQuery_longName", true,
                    filter(f -> f.setQuery("actigard|aqua|aquastop|aria|buglock|cachemire|caress|clean|clone" +
                            "|cotton|cover|defence|dry|elite|freedream|fresh|frotte|golf|hit|jaklyn|kapris|kids" +
                            "|light|lira|merino|merinos|microfiber|nika|non-allergenic|plus|protect|protect-a-bed" +
                            "|protection|ronda|season|seasons|simple|standart|stop|stressfree|waterproof|wool" +
                            "|адениум|аква|бамбук|бархатный|барьер|белый|био|боковин|борт|бязь|влагозащитный" +
                            "|влагонепроницаемый|влагостойкий|водонепроницаемый|детский|защит|зима|зима-лето" +
                            "|золотой|кавер|коллекция|крокус|латексированный|лето|лотос|люкс|махра-велюр" +
                            "|медвежонок|медея|мерино|натяжной|нега|непромокаемый|овечий|ода|пвх|периметр|перин" +
                            "|плюс|простын|протект|резинк|сизон|симпл|синтепоне|слим|стандарт|стебель|стеганый" +
                            "|топ|трикотажный|универсал|хлопковый|хлопок|чехол|шерст|широкий|эконом|эко-фикс" +
                            "|экрю|эллегия|эрлан")),
                    offer("014", o -> o.setName("универсальный")));

            check("byQuery_longName", true,
                    filter(f -> f.setQuery("actigard|aqua|aquastop|aria|buglock|cachemire|caress|clean|clone" +
                            "|cotton|cover|defence|dry|elite|freedream|fresh|frotte|golf|hit|jaklyn|kapris|kids" +
                            "|light|lira|merino|merinos|microfiber|nika|non-allergenic|plus|protect|protect-a-bed"
                    )),
                    offer("014.1", o -> o.setName("aquaтест")));

            //
            check("byBidsRecommended-13", true,
                    filter(f -> f.getBids().setRecommended_bid_position(13)), // Будет проигнорировано
                    offer("015"));

            check("byCategories-no-data", false,
                    filter(f -> f.setCategories_by_id(Set.of(1L, 2L, 3L))),
                    offer("020"));
            check("byCategories-exclude-no-data", false, // Означает, что мы требует наличия кода категории
                    filter(f -> f.setExclude_categories_by_id(Set.of(1L, 2L, 3L))),
                    offer("020.1"));
            check("byCategories-exclude-no-data-has-id", true,
                    filter(f -> f.setExclude_categories_by_id(Set.of(1L, 2L, 3L))),
                    offer("020.2", o -> o.setCategory_id(100)));
            check("byPurchasePrice-from-no-purchase", false,
                    filter(f -> f.getPurchasePrice().setPurchase_price_from(1100)),
                    offer("021", o -> o.setPrice(9000)));
            check("byPurchasePrice-to-no-purchase", false,
                    filter(f -> f.getPurchasePrice().setPurchase_price_to(1100)),
                    offer("022", o -> o.setPrice(1000)));
            check("byAutostrategies-only-not-matched", false,
                    filter(f -> f.setWith_autostrategies(true)),
                    offer("023.n", o -> o.setApp_autostrategy_id(0)));
            check("byAutostrategies-only-matched", true,
                    filter(f -> f.setWith_autostrategies(true)),
                    offer("023.m", o -> o.setApp_autostrategy_id(1)));
            check("byAutostrategies-list-not-matched", false,
                    filter(f -> f.setAutostrategies_by_id(Set.of(1, 2, 3))),
                    offer("024.n1", o -> o.setApp_autostrategy_id(0)));
            check("byAutostrategies-list-not-matched", false,
                    filter(f -> f.setAutostrategies_by_id(Set.of(1, 2, 3))),
                    offer("024.n2", o -> o.setApp_autostrategy_id(4)));
            check("byAutostrategies-list-matched", true,
                    filter(f -> f.setAutostrategies_by_id(Set.of(1, 2, 3))),
                    offer("024.m", o -> o.setApp_autostrategy_id(2)));
        }

        private void init2() {

            matchConditions("byIdList", "048",
                    f -> f.setId_list(Set.of("048", "048.0")),
                    Utils.emptyConsumer());
            matchConditions("byIdList", "048.1",
                    f -> {
                        f.set_query_only_id(true);
                        f.setId_list(Set.of("048.1", "048.1.0"));
                    },
                    Utils.emptyConsumer());

            matchConditions("byIdList", "048.2",
                    f -> {
                        f.set_query_only_id(true);
                        f.setId_list(Set.of("048.1", "048.1.0"));
                    },
                    List.of(),
                    List.of(Utils.emptyConsumer()));
            matchConditions("byIdList", "048.3",
                    f -> {
                        f.set_query_only_id(true);
                        f.setId_list(Set.of());
                    },
                    List.of(),
                    List.of(Utils.emptyConsumer()));

            longConditions("byPrice", "049",
                    Filter::setPrice_from, Filter::setPrice_to,
                    Offer::setPrice);

            matchConditions("byVendors", "050",
                    f -> f.setVendors(Set.of("v1", "v2")),
                    o -> o.setVendor_name("v2"));

            matchConditions("byVendors", "050.1",
                    f -> f.setVendors(Set.of("V1", "V2")),
                    o -> o.setVendor_name("V2"));

            matchConditions("byVendors", "050.2",
                    f -> f.setVendors(Set.of("v1", "v2")),
                    o -> o.setVendor_name("V2"));

            matchConditions("byVendors", "050.3",
                    f -> f.setVendors(Set.of("V1", "V2")),
                    o -> o.setVendor_name("v2"));

            matchConditions("byVendors", "050-1",
                    f -> f.setMarket_vendors(Set.of(1, 2)),
                    o -> o.setVendor_id(2));

            matchConditions("byModels", "050-2",
                    f -> f.setModels(Set.of(1, 2)),
                    List.of(o -> o.setModel_id(1),
                            o -> o.setModel_id(2)),
                    List.of(Utils.emptyConsumer(),
                            o -> o.setModel_id(3)));

            matchConditions("byShops", "050-3.1",
                    f -> f.setShops(Set.of(SHOP_ID)),
                    List.of(Utils.emptyConsumer()),
                    List.of());

            matchConditions("byShops", "050-3.2",
                    f -> f.setShops(Set.of(SHOP_ID2)),
                    List.of(),
                    List.of(Utils.emptyConsumer()));

            boolConditions("byAvailable", "051",
                    Filter::setAvailable,
                    (o, b) -> o.setIn_stock_count(b ? 1 : 0));

            boolConditions("byFreeShipping", "052",
                    (f, v) -> f.getShipping().setFree_shipping(v),
                    (o, b) -> o.setShipping_cost(toCents(b ? 0. : 1.)));

            boolConditions("byShippingDiscount", "053",
                    (f, v) -> f.getShipping().setWith_discount(v),
                    (o, b) -> o.setOldprice(toCents(b ? 1. : 0.0)));

            boolConditions("byShippingCutPrice", "054",
                    (f, v) -> f.getShipping().setCut_price(v),
                    Offer::set_cutprice);

            intConditions("byCardPosition", "055",
                    (f, i) -> f.getCardPosition().setCard_position_from(i),
                    (f, i) -> f.getCardPosition().setCard_position_to(i),
                    Offer::setCurrent_pos_all,
                    true,
                    false);

            longConditions("byOwnBids", "056",
                    (f, d) -> f.getBids().setOwn_bid_from(d),
                    (f, d) -> f.getBids().setOwn_bid_to(d),
                    Offer::setBid);

            boolConditions("byBidsDontUp", "057",
                    (f, v) -> f.getBids().setDont_up_to_min(v),
                    Offer::set_dont_up_to_min);

            longConditions("byMinBids", "058",
                    (f, d) -> f.getBids().setMin_bid_from(d),
                    (f, d) -> f.getBids().setMin_bid_to(d),
                    Offer::setMin_bid);

            msBids().forEach((pos, func) ->
                    longConditions("byBidsRecommended", "064-" + pos,
                            (f, d) -> {
                                f.getBids().setRecommended_bid_position(pos);
                                f.getBids().setRecommended_bid_from(d);
                            },
                            (f, d) -> {
                                f.getBids().setRecommended_bid_position(pos);
                                f.getBids().setRecommended_bid_to(d);
                            },
                            func));

            longConditions("byPurchase", "065",
                    (f, d) -> f.getPurchasePrice().setPurchase_price_from(d),
                    (f, d) -> f.getPurchasePrice().setPurchase_price_to(d),
                    (o, d) -> {
                        o.setPrice(d * 2);
                        o.setPurchase_price(d);
                    },
                    false);
            matchConditions("byOfferType-SEARCH", "066",
                    f -> f.setOffer_type(FilterOfferType.SEARCH),
                    Utils.emptyConsumer(),
                    List.of(
                            o -> o.setModel_id(1))
            );
            matchConditions("byOfferType-SEARCH-type", "067",
                    f -> {
                        f.setOffer_type(FilterOfferType.SEARCH);
                        f.getRelativePrices().setPrice_type_from(FilterPriceType.MIN);
                    },
                    List.of(),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setModel_id(1))
            );
            matchConditions("byOfferType-CARDS", "068",
                    f -> f.setOffer_type(FilterOfferType.CARDS),
                    o -> o.setModel_id(1));
            matchConditions("byOfferType-DETACHED", "069",
                    f -> f.setOffer_type(FilterOfferType.DETACHED),
                    o -> o.setLast_model_id(1),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setModel_id(1),
                            o -> {
                                o.setModel_id(2);
                                o.setLast_model_id(1);
                            }));
            matchConditions("byOfferType-DETACHED-type", "070",
                    f -> {
                        f.setOffer_type(FilterOfferType.DETACHED);
                        f.getRelativePrices().setPrice_type_from(FilterPriceType.MIN);
                    },
                    List.of(),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setLast_model_id(1))
            );
            matchConditions("byOfferType-ALL", "071",
                    f -> f.setOffer_type(FilterOfferType.ALL),
                    List.of(
                            o -> o.setModel_id(1),
                            Utils.emptyConsumer(),
                            o -> o.setLast_model_id(1)),
                    List.of());
            matchConditions("byCountOffersInModel", "073",
                    f -> f.setCount_offers_in_model(true),
                    o -> o.setIn_model_count(2),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setIn_model_count(1)
                    ));
            boolConditions("byHiddenOffers", "074",
                    Filter::setHidden_offers,
                    (o, b) -> o.setIs_hide_ttl_hours(b ? 1 : 0));
            boolConditions("byHasStrategy", "075",
                    (f, b) -> f.getStrategies().setHas_strategy(b),
                    (o, b) -> o.setApp_strategy_id(b ? 1L : 0L));
            boolConditions("byReachedStrategy", "076",
                    (f, b) -> {
                        var fs = f.getStrategies();
                        fs.setHas_strategy(true);
                        fs.setReached_strategy(b);
                    },
                    (o, b) -> {
                        o.setApp_strategy_id(1L);
                        o.set_strategy_reach(b);
                    });
            matchConditions("byReachedStrategy-never", "077",
                    f -> f.getStrategies().setReached_strategy(true), // Нет условия на has_strategy
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.set_strategy_reach(true),
                            o -> {
                                o.setApp_strategy_id(1L);
                                o.set_strategy_reach(true);
                            }
                    ),
                    List.of());
            intConditions("byParams", "078",
                    (f, i) -> params(f).computeIfAbsent("p1", s -> new Filter.ParamFilter()).setValue_from(i),
                    (f, i) -> params(f).computeIfAbsent("p1", s -> new Filter.ParamFilter()).setValue_to(i),
                    (o, i) -> params(o).put("p1", i),
                    false,
                    false);
            intConditions("byParams-escaped", "078.1",
                    (f, i) -> params(f).computeIfAbsent("p1\"1\"", s -> new Filter.ParamFilter()).setValue_from(i),
                    (f, i) -> params(f).computeIfAbsent("p1\"1\"", s -> new Filter.ParamFilter()).setValue_to(i),
                    (o, i) -> params(o).put("p1\"1\"", i),
                    false,
                    false);
        }

        private void init5() {

            matchConditions("byCategoryName", "086",
                    f -> f.setCategory("category-500"),
                    o -> o.setCategory_id(500),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(400)
                    ));

            matchConditions("byCategoryName", "087",
                    f -> f.setCategory("category-400"), // Попали на корневую категорию, матчим все
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500),
                            o -> o.setCategory_id(1)), // Нам подойдут любые оффера, у которых есть категория
                    List.of(
                            Utils.emptyConsumer()

                    ));

            matchConditions("byCategoryName", "088",
                    f -> f.setCategory("category-200"),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300)),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500)
                    ));

            //       400
            //      /   \
            //     200   500
            //    /   \
            // 100     300
            matchConditions("byCategoryId", "089",
                    f -> f.setCategories_by_id(Set.of(200L, 500L)),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(500)
                    ),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(0)
                    ));
            matchConditions("byCategoryId-exclude", "089.e",
                    f -> {
                        f.setCategories_by_id(Set.of(200L, 500L));
                        f.setExclude_categories_by_id(Set.of(500L));
                    },
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300)
                    ),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(0),
                            o -> o.setCategory_id(500)
                    ));
            matchConditions("byCategoryId-exclude", "089.f",
                    f -> f.setExclude_categories_by_id(Set.of(500L)),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(300)
                    ),
                    List.of(
                            Utils.emptyConsumer(),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0),
                            o -> o.setCategory_id(500)
                    ));
            matchConditions("byCategoryId", "090",
                    f -> f.setCategories_by_id(Set.of(400L)),  // Попали на корневую категорию, матчим все
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500),
                            o -> o.setCategory_id(1),  // Нам подойдут любые оффера, у которых есть категория
                            o -> o.setCategory_id(0)),
                    List.of(
                            Utils.emptyConsumer()

                    ));
            matchConditions("byCategoryId-exclude", "090.e1",
                    f -> {
                        // Попали на корневую категорию, матчим все
                        f.setCategories_by_id(Set.of(400L));
                        f.setExclude_categories_by_id(Set.of(200L));
                    },
                    List.of(
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500)),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0)
                    ));
            matchConditions("byCategoryId-exclude", "090.e2",
                    f -> {
                        // Попали на корневую категорию, матчим все
                        f.setCategories_by_id(Set.of(400L));
                        // Отменяем выбор от корня
                        f.setExclude_categories_by_id(Set.of(400L));
                    },
                    List.of(),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0)
                    ));
            matchConditions("byCategoryId-exclude", "090.e3",
                    f -> {
                        f.setCategories_by_id(Set.of(200L));
                        f.setExclude_categories_by_id(Set.of(400L));
                    },
                    List.of(),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0)
                    ));
            matchConditions("byCategoryId-exclude", "090.e4",
                    f -> {
                        // Попали на корневую категорию, матчим все
                        f.setCategories_by_id(Set.of(400L));
                        f.setExclude_categories_by_id(Set.of(100L, 300L));
                    },
                    List.of(
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(500)),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0)
                    ));
            matchConditions("byCategoryId-exclude", "090.e5",
                    f -> {
                        // Отменяем выбор от корня
                        f.setExclude_categories_by_id(Set.of(400L));
                    },
                    List.of(),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0)
                    ));
            matchConditions("byCategoryId", "091",
                    f -> f.setCategories_by_id(Set.of(999L)), // Категории не существует
                    List.of(),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(1)
                    ));
            matchConditions("byCategoryId", "092",
                    f -> {
                        f.setShop_id(SHOP_ID2);
                        f.setCategories_by_id(Set.of(999L));  // Категории не существует
                    },
                    List.of(),
                    List.of(
                            o -> offer2(o).setCategory_id(100),
                            o -> offer2(o).setCategory_id(200),
                            o -> offer2(o).setCategory_id(300),
                            o -> offer2(o).setCategory_id(1)
                    ));
            matchConditions("byCategoryId-all", "092.a1",
                    f -> f.setCategories_by_id(Set.of(ShopCategory.NO_CATEGORY)),  // Означает "все категории"
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500),
                            o -> o.setCategory_id(1),  // Нам подойдут любые оффера, у которых есть категория
                            o -> o.setCategory_id(0),
                            Utils.emptyConsumer()),
                    List.of(
                    ));
            matchConditions("byCategoryId-all-exlcude", "092.a2",
                    f -> {
                        f.setCategories_by_id(Set.of(ShopCategory.NO_CATEGORY));
                        f.setExclude_categories_by_id(Set.of(200L));
                    },  // Означает "все категории"
                    List.of(
                            o -> o.setCategory_id(400),
                            o -> o.setCategory_id(500)),
                    List.of(
                            o -> o.setCategory_id(100),
                            o -> o.setCategory_id(200),
                            o -> o.setCategory_id(300),
                            o -> o.setCategory_id(1),
                            o -> o.setCategory_id(0),
                            Utils.emptyConsumer()
                    ));

        }

        private void init6() {

            matchConditions("byPrice-manual-from", "093",
                    f -> f.setPrice_from(0),
                    List.of(o -> o.setPrice(0)),
                    List.of(o -> o.setPrice(-1)));

            matchConditions("byPrice-manual-to", "094",
                    f -> f.setPrice_to(0),
                    List.of(o -> o.setPrice(0)),
                    List.of(o -> o.setPrice(1)));

            matchConditions("byPurchasePrice-manual-from", "095",
                    f -> f.getPurchasePrice().setPurchase_price_from(0),
                    List.of(o -> {
                                o.setPrice(100);
                                o.setPurchase_price(100);
                            },
                            o -> {
                                o.setPrice(100);
                                o.setPurchase_price(99);
                            }),
                    List.of(o -> {
                        o.setPrice(100);
                        o.setPurchase_price(101);
                    }));

            matchConditions("byPurchasePrice-manual-to", "096",
                    f -> f.getPurchasePrice().setPurchase_price_to(0),
                    List.of(o -> {
                                o.setPrice(100);
                                o.setPurchase_price(100);
                            },
                            o -> {
                                o.setPrice(100);
                                o.setPurchase_price(101);
                            }),
                    List.of(o -> {
                        o.setPrice(100);
                        o.setPurchase_price(99);
                    }));

            matchConditions("byCardPosition-manual-from", "097",
                    f -> f.getCardPosition().setCard_position_from(2),
                    List.of(o -> o.setCurrent_pos_all(0),
                            o -> o.setCurrent_pos_all(2)),
                    List.of(o -> o.setCurrent_pos_all(1)));

            matchConditions("byCardPosition-manual-from", "097.1",
                    f -> f.getCardPosition().setCard_position_from(2),
                    List.of(o -> {
                                o.setCurrent_pos_all(0);
                                o.setMs_model_count(1);
                            },
                            o -> {
                                o.setCurrent_pos_all(2);
                                o.setMs_model_count(1);
                            }),
                    List.of(o -> {
                                o.setCurrent_pos_all(1);
                                o.setMs_model_count(0);
                            },
                            o -> {
                                o.setCurrent_pos_all(1);
                                o.setMs_model_count(1);
                            }));

            matchConditions("byCardPosition-manual-from", "097.2",
                    f -> f.getCardPosition().setCard_position_from(1),
                    List.of(o -> {
                                o.setCurrent_pos_all(0);
                                o.setMs_model_count(1);
                            },
                            Utils.emptyConsumer(),
                            o -> {
                                o.setCurrent_pos_all(1);
                                o.setMs_model_count(0);
                            },
                            o -> {
                                o.setCurrent_pos_all(2);
                                o.setMs_model_count(2);
                            }),
                    List.of());

            matchConditions("byCardPosition-manual-to", "098",
                    f -> f.getCardPosition().setCard_position_to(2),
                    List.of(o -> o.setCurrent_pos_all(1),
                            o -> o.setCurrent_pos_all(2)),
                    List.of(o -> o.setCurrent_pos_all(0),
                            o -> o.setCurrent_pos_all(3)));

            matchConditions("byCardPosition-manual-to", "098.1",
                    f -> f.getCardPosition().setCard_position_to(2),
                    List.of(o -> {
                                o.setCurrent_pos_all(1);
                                o.setMs_model_count(1);
                            },
                            o -> {
                                o.setCurrent_pos_all(1);
                                o.setMs_model_count(2);
                            },
                            o -> {
                                o.setCurrent_pos_all(2);
                                o.setMs_model_count(1);
                            }),
                    List.of(o -> {
                                o.setCurrent_pos_all(3);
                                o.setMs_model_count(2);
                            },
                            o -> {
                                o.setCurrent_pos_all(0);
                                o.setMs_model_count(1);
                            }));

            matchConditions("byCardPosition-manual-from-to", "098.2",
                    f -> {
                        f.getCardPosition().setCard_position_from(1);
                        f.getCardPosition().setCard_position_to(3);
                    },
                    List.of(o -> o.setCurrent_pos_all(1),
                            o -> o.setCurrent_pos_all(2),
                            o -> o.setCurrent_pos_all(3)),
                    List.of(o -> o.setCurrent_pos_all(0),
                            o -> o.setCurrent_pos_all(4)));

            matchConditions("byCardPosition-manual-from-to", "098.3",
                    f -> {
                        f.getCardPosition().setCard_position_from(2);
                        f.getCardPosition().setCard_position_to(3);
                    },
                    List.of(o -> o.setCurrent_pos_all(2),
                            o -> o.setCurrent_pos_all(3)),
                    List.of(o -> o.setCurrent_pos_all(0),
                            o -> o.setCurrent_pos_all(1),
                            o -> o.setCurrent_pos_all(4)));

            matchConditions("byBidsOwn-manual-from", "099",
                    f -> f.getBids().setOwn_bid_from(0),
                    List.of(o -> o.setBid(0L)),
                    List.of(o -> o.setBid(-1L)));

            matchConditions("byBidsOwn-manual-to", "100",
                    f -> f.getBids().setOwn_bid_to(0),
                    List.of(o -> o.setBid(0L)),
                    List.of(o -> o.setBid(1L)));

            matchConditions("byBidsMin-manual-from", "101",
                    f -> f.getBids().setMin_bid_from(0),
                    List.of(o -> o.setMin_bid(0)),
                    List.of(o -> o.setMin_bid(-1)));

            matchConditions("byBidsMin-manual-to", "102",
                    f -> f.getBids().setMin_bid_to(0),
                    List.of(o -> o.setMin_bid(0)),
                    List.of(o -> o.setMin_bid(1)));

            matchConditions("byBidsRecommended-manual-from", "103",
                    f -> {
                        f.getBids().setRecommended_bid_position(2);
                        f.getBids().setRecommended_bid_from(0);
                    },
                    List.of(o -> o.setMs_bid_2(0)),
                    List.of(o -> o.setMs_bid_2(-1)));

            matchConditions("byBidsRecommended-manual-to", "104",
                    f -> {
                        f.getBids().setRecommended_bid_position(2);
                        f.getBids().setRecommended_bid_to(0);
                    },
                    List.of(o -> o.setMs_bid_2(0)),
                    List.of(o -> o.setMs_bid_2(1)));
        }
        //CHECKSTYLE:ON

        private void check(String name, boolean match, Filter filter, Offer offer) {
            scenarios.add(new Scenario(name, match, filter, offer));
        }


        private void intConditions(String name, String offerIdPrefix,
                                   BiConsumer<Filter, Integer> filterSetterFrom,
                                   BiConsumer<Filter, Integer> filterSetterTo,
                                   BiConsumer<Offer, Integer> offer,
                                   boolean matchIfEmptyFrom,
                                   boolean matchIfEmptyTo) {
            intConditions0(name + "From", offerIdPrefix + ".from", filterSetterFrom, offer, true);
            intConditions0(name + "To", offerIdPrefix + ".to", filterSetterTo, offer, false);

            var fFrom = filter(f -> filterSetterFrom.accept(f, 3));
            check(name + ".not-equals-from-empty", matchIfEmptyFrom,
                    fFrom,
                    offer(offerIdPrefix + ".4", o -> o.setShop_id(fFrom.getShop_id())));

            var fTo = filter(f -> filterSetterTo.accept(f, 3));
            check(name + ".equals-to-empty", matchIfEmptyTo,
                    fTo,
                    offer(offerIdPrefix + ".5", o -> o.setShop_id(fTo.getShop_id())));
        }

        private void intConditions0(String name, String offerIdPrefix,
                                    BiConsumer<Filter, Integer> filterSetter,
                                    BiConsumer<Offer, Integer> offerSetter,
                                    boolean greaterOrEquals) {
            final int v = 3;

            check(name + ".equals", true,
                    filter(f -> filterSetter.accept(f, v)),
                    offer(offerIdPrefix + ".1", o -> offerSetter.accept(o, v)));
            check(name + ".less-than", !greaterOrEquals,
                    filter(f -> filterSetter.accept(f, v)),
                    offer(offerIdPrefix + ".2", o -> offerSetter.accept(o, v - 1)));
            check(name + ".greater-than", greaterOrEquals,
                    filter(f -> filterSetter.accept(f, v)),
                    offer(offerIdPrefix + ".3", o -> offerSetter.accept(o, v + 1)));
        }

        private void longConditions(String name, String offerIdPrefix,
                                    BiConsumer<Filter, Long> filterSetterFrom,
                                    BiConsumer<Filter, Long> filterSetterTo,
                                    BiConsumer<Offer, Long> offer) {
            longConditions(name, offerIdPrefix, filterSetterFrom, filterSetterTo, offer, true, true);
        }

        private void longConditions(String name, String offerIdPrefix,
                                    BiConsumer<Filter, Long> filterSetterFrom,
                                    BiConsumer<Filter, Long> filterSetterTo,
                                    BiConsumer<Offer, Long> offer,
                                    boolean matchIfEmpty) {
            longConditions(name, offerIdPrefix, filterSetterFrom, filterSetterTo, offer, true, matchIfEmpty);
        }

        private void longConditions(String name, String offerIdPrefix,
                                    BiConsumer<Filter, Long> filterSetterFrom,
                                    BiConsumer<Filter, Long> filterSetterTo,
                                    BiConsumer<Offer, Long> offer,
                                    boolean includeEquals,
                                    boolean matchIfEmpty) {
            longConditions0(name + "From", offerIdPrefix + ".from",
                    filterSetterFrom, offer, includeEquals, true);
            longConditions0(name + "To", offerIdPrefix + ".to",
                    filterSetterTo, offer, includeEquals, false);

            var fFrom = filter(f -> filterSetterFrom.accept(f, 105L));
            check(name + ".not-equals-from-empty", false,
                    fFrom,
                    offer(offerIdPrefix + ".4", o -> o.setShop_id(fFrom.getShop_id())));

            var fTo = filter(f -> filterSetterTo.accept(f, 105L));
            check(name + ".equals-to-empty", matchIfEmpty,
                    fTo,
                    offer(offerIdPrefix + ".5", o -> o.setShop_id(fTo.getShop_id())));
        }

        private void longConditions0(String name, String offerIdPrefix,
                                     BiConsumer<Filter, Long> filterSetter,
                                     BiConsumer<Offer, Long> offerSetter,
                                     boolean includeEquals,
                                     boolean greaterOrEquals) {
            final long v = 105;

            if (includeEquals) {
                check(name + ".equals", true,
                        filter(f -> filterSetter.accept(f, v)),
                        offer(offerIdPrefix + ".1", o -> offerSetter.accept(o, v)));
            }
            check(name + ".less-than", !greaterOrEquals,
                    filter(f -> filterSetter.accept(f, v)),
                    offer(offerIdPrefix + ".2", o -> offerSetter.accept(o, v - 10)));
            check(name + ".greater-than", greaterOrEquals,
                    filter(f -> filterSetter.accept(f, v)),
                    offer(offerIdPrefix + ".3", o -> offerSetter.accept(o, v + 10)));
        }

        private void boolConditions(String name, String offerIdPrefix,
                                    BiConsumer<Filter, Boolean> filterSetter,
                                    BiConsumer<Offer, Boolean> offerSetter) {
            boolConditions(name, offerIdPrefix,
                    f -> filterSetter.accept(f, true),
                    f -> filterSetter.accept(f, false),
                    o -> offerSetter.accept(o, true),
                    o -> offerSetter.accept(o, false));
        }

        private void boolConditions(String name, String offerIdPrefix,
                                    Consumer<Filter> filterOn, Consumer<Filter> filterOff,
                                    Consumer<Offer> matched, Consumer<Offer> unmatched) {

            check(name + ".off-matched", false,
                    filter(filterOff),
                    offer(offerIdPrefix + ".1", matched));
            check(name + ".on-unmatched", false,
                    filter(filterOn),
                    offer(offerIdPrefix + ".2", unmatched));
            check(name + ".on-matched", true,
                    filter(filterOn),
                    offer(offerIdPrefix + ".3", matched));
            check(name + ".off-unmatched", true,
                    filter(filterOff),
                    offer(offerIdPrefix + ".4", unmatched));

        }

        private void matchConditions(String name, String offerIdPrefix,
                                     Consumer<Filter> filterOn,
                                     Consumer<Offer> matched) {
            matchConditions(name, offerIdPrefix, filterOn, matched, List.of(Utils.emptyConsumer()));
        }

        private void matchConditions(String name, String offerIdPrefix,
                                     Consumer<Filter> filterOn,
                                     Consumer<Offer> matched,
                                     Collection<Consumer<Offer>> unmatchedList) {
            matchConditions(name, offerIdPrefix, filterOn, List.of(matched), unmatchedList);
        }

        private void matchConditions(String name, String offerIdPrefix,
                                     Consumer<Filter> filterOn,
                                     Collection<Consumer<Offer>> matchedList,
                                     Collection<Consumer<Offer>> unmatchedList) {
            int i = 0;
            for (var matched : matchedList) {
                check(name + ".matched", true,
                        filter(filterOn),
                        offer(offerIdPrefix + "." + (i++), matched));
            }
            for (var unmatched : unmatchedList) {
                check(name + ".not-matched", false,
                        filter(filterOn),
                        offer(offerIdPrefix + "." + (i++), unmatched));
            }
        }


        private static Offer offer2(Offer offer) {
            offer.setShop_id(SHOP_ID2);
            return offer;
        }

        private static int[] intValues(DaysLimit limit, int value) {
            int[] ret = new int[DaysLimit.latest().getIndex() + 1];
            ret[limit.getIndex()] = value;
            return ret;
        }

        private static long[] longValues(DaysLimit limit, long value) {
            long[] ret = new long[DaysLimit.latest().getIndex() + 1];
            ret[limit.getIndex()] = value;
            return ret;
        }

        private static double[] doubleValues(DaysLimit limit, double value) {
            double[] ret = new double[DaysLimit.latest().getIndex() + 1];
            ret[limit.getIndex()] = value;
            return ret;
        }

        private static Offer offer(String offerId) {
            return offer(offerId, Utils.emptyConsumer());
        }

        private static Offer offer(String offerId, Consumer<Offer> init) {
            return TmsTestUtils.offer(offerId, offer -> {
                offer.setShop_id(SHOP_ID);
                offer.setFeed_id(FEED_ID);
                offer.setStatus(Status.ACTIVE);
                init.accept(offer);
            });
        }

        private static Filter filter(Consumer<Filter> init) {
            return TmsTestUtils.filter(ID.incrementAndGet(), filter -> {
                filter.setShop_id(SHOP_ID);
                init.accept(filter);
            });
        }

        private static Strategy strategy(Consumer<Strategy> init) {
            return TmsTestUtils.strategy(ID.incrementAndGet(), strategy -> {
                strategy.setShop_id(SHOP_ID);
                init.accept(strategy);
            });
        }

        private static ShopCategory category(long categoryId, Consumer<ShopCategory> init) {
            return TmsTestUtils.shopCategory(categoryId, c -> {
                c.setShop_id(SHOP_ID);
                c.setFeed_id(FEED_ID);
                c.setStatus(Status.ACTIVE);
                c.setName("category-" + categoryId);
                init.accept(c);
            });
        }

        private static Map<String, Filter.ParamFilter> params(Filter filter) {
            if (filter.getParams() == null) {
                filter.setParams(new Object2ObjectOpenHashMap<>());
            }
            return filter.getParams();
        }

        private static Map<String, Integer> params(Offer offer) {
            if (offer.getParams_map() == null) {
                offer.setParams_map(new Object2ObjectOpenHashMap<>());
            }
            return offer.getParams_map();
        }

        private static Map<Integer, BiConsumer<Offer, Long>> msBids() {
            Map<Integer, BiConsumer<Offer, Long>> ret = new LinkedHashMap<>();
            ret.put(1, Offer::setMs_bid_1);
            ret.put(2, Offer::setMs_bid_2);
            ret.put(3, Offer::setMs_bid_3);
            ret.put(4, Offer::setMs_bid_4);
            ret.put(5, Offer::setMs_bid_5);
            ret.put(6, Offer::setMs_bid_6);
            ret.put(7, Offer::setMs_bid_7);
            ret.put(8, Offer::setMs_bid_8);
            ret.put(9, Offer::setMs_bid_9);
            ret.put(10, Offer::setMs_bid_10);
            ret.put(11, Offer::setMs_bid_11);
            ret.put(12, Offer::setMs_bid_12);
            return ret;
        }

        private static List<ShopCategory> buildCategories() {
            return List.of(
                    category(400, c -> {
                        c.setTree_left(1);
                        c.setTree_right(5);
                    }),
                    // Первая вершина, 2 уровня
                    category(200, c -> {
                        c.setTree_left(1);
                        c.setTree_right(3);
                        c.setParent_category_id(400);
                        c.setTree_lvl(1);
                    }),
                    category(100, c -> {
                        c.setTree_left(1);
                        c.setTree_right(1);
                        c.setParent_category_id(200);
                        c.setTree_lvl(2);
                    }),
                    category(300, c -> {
                        c.setTree_left(3);
                        c.setTree_right(3);
                        c.setParent_category_id(200);
                        c.setTree_lvl(2);
                    }),

                    // Вторая вершина, 1 уровень
                    category(500, c -> {
                        c.setTree_left(5);
                        c.setTree_right(5);
                        c.setParent_category_id(400);
                        c.setTree_lvl(1);
                    })
            );
        }


    }

    public static class Scenario {
        private final String name;
        private final boolean match;
        private final Filter filter;
        private final Offer offer;
        private final Strategy strategy;

        private Scenario(@NonNull String name, boolean match, @NonNull Filter filter, @NonNull Offer offer) {
            this.match = match;
            this.filter = filter;
            this.offer = offer;
            this.name = offer.getOffer_id() + " -> " + name + ": " + match;
            this.strategy = ScenariosBuilder.strategy(s -> {
                s.setObject_type(StrategyObjectType.FILTER);
                s.setObject_id(String.valueOf(filter.getFilter_id()));
                s.setShop_id(filter.getShop_id());
            });
        }

        public String getName() {
            return name;
        }

        public Filter getFilter() {
            return filter;
        }

        public Offer getOffer() {
            return offer;
        }

        public Strategy getStrategy() {
            return strategy;
        }

        private Object[] toJunitScenario() {
            return new Object[]{name, match, filter, offer, strategy};
        }
    }

    private static class ScenarioContent {
        private final List<Scenario> scenarios;
        private final List<ShopCategory> categories;

        private final List<Object[][]> junitScenarios;

        private ScenarioContent(ScenariosBuilder scenariosBuilder) {
            this.scenarios = Collections.unmodifiableList(scenariosBuilder.scenarios);
            this.categories = Collections.unmodifiableList(scenariosBuilder.categories);

            // Нужно разбить все тесты на 16 наборов
            int splitSize = (scenarios.size() / TOTAL_SCENARIO_GROUPS) + 1;
            this.junitScenarios = Utils.split(scenarios, splitSize).stream()
                    .map(list -> list.stream().map(Scenario::toJunitScenario).toArray(Object[][]::new))
                    .collect(Collectors.toList());
        }
    }
}
