package ru.yandex.market.pricelabs.tms.processing;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import Market.DataCamp.External.OfferOuterClass;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

import ru.yandex.market.pricelabs.CoreTestUtils;
import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Autostrategy;
import ru.yandex.market.pricelabs.model.AutostrategyHistory;
import ru.yandex.market.pricelabs.model.AutostrategyOffer;
import ru.yandex.market.pricelabs.model.AutostrategyOfferSource;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.AutostrategyShopState;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.ModelbidsPosition;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.OfferVendor;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.ShopFeed;
import ru.yandex.market.pricelabs.model.ShopsDat;
import ru.yandex.market.pricelabs.model.Strategy;
import ru.yandex.market.pricelabs.model.StrategyState;
import ru.yandex.market.pricelabs.model.StrategyStateHistory;
import ru.yandex.market.pricelabs.model.User;
import ru.yandex.market.pricelabs.model.types.FilterClass;
import ru.yandex.market.pricelabs.model.types.OfferBidType;
import ru.yandex.market.pricelabs.model.types.Shard;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.StrategyFormType;
import ru.yandex.market.pricelabs.model.types.WithShopId;
import ru.yandex.market.pricelabs.processing.SelectInterface;
import ru.yandex.market.pricelabs.processing.SelectInterfacePerShop;
import ru.yandex.market.pricelabs.processing.ShopArg;
import ru.yandex.market.pricelabs.processing.ShopFeedArg;
import ru.yandex.market.pricelabs.processing.autostrategies.SelectInterfaceAutostrategy;
import ru.yandex.market.pricelabs.processing.autostrategies.SelectInterfacePriceRecommendations;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.tms.cache.CachedDataLoader;
import ru.yandex.market.pricelabs.tms.cache.CachedDataSource;
import ru.yandex.market.pricelabs.tms.cache.CachedShopFeedData;
import ru.yandex.market.pricelabs.tms.idx.OfferPush;
import ru.yandex.market.pricelabs.tms.processing.categories.CategoriesTreeHolder;
import ru.yandex.market.pricelabs.tms.processing.categories.CategoriesTreeHolderImpl;
import ru.yandex.market.pricelabs.tms.processing.imports.SelectInterfaceFilter;
import ru.yandex.market.pricelabs.tms.processing.imports.SelectInterfaceShop;
import ru.yandex.market.pricelabs.tms.processing.imports.SelectInterfaceShopsDat;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;

import static ru.yandex.market.pricelabs.misc.TimingUtils.getInstant;

public final class TmsTestUtils {

    public static final Shard SHARD = ApiConst.PRIMARY_SHARD;
    public static final String SHARD_NAME = SHARD.getSqlName();
    public static final String DEFAULT_GENERATION = "20190701_0000";
    public static final String DEFAULT_INDEXER_NAME = "primary";

    private TmsTestUtils() {
        //
    }

    public static <T, T2> List<T2> map(Collection<T> rows, Function<T, T2> copy, Consumer<T2> update) {
        return rows.stream().map(copy).peek(update).collect(Collectors.toList());
    }

    public static <T, T2> List<T2> map(Collection<T> rows, Function<T, T2> update) {
        return rows.stream().map(update).collect(Collectors.toList());
    }

    public static <T> List<T> update(List<T> rows, Consumer<T> updater) {
        rows.forEach(updater);
        return rows;
    }

    public static <T> List<T> filter(List<T> rows, Predicate<T> filter) {
        return rows.stream().filter(filter).collect(Collectors.toList());
    }

    public static <T> SelectInterface<T> selectOf(List<T> list) {
        return new SelectInterface<>() {
            @Override
            public List<T> selectAll() {
                return list;
            }

            @Override
            public String getTitle() {
                return "Static";
            }
        };
    }

    public static <T extends WithShopId> SelectInterfacePerShop<T> selectOfShop(List<T> list) {
        return new SelectInterfacePerShop<>() {

            @Override
            public List<T> selectAll() {
                return list;
            }

            @Override
            public List<T> selectSingle(ShopArg shopId) {
                return list.stream()
                        .filter(t -> t.getShop_id() == shopId.getShopId())
                        .collect(Collectors.toList());
            }

            @Override
            public List<T> selectList(Collection<Long> shopIdList) {
                var set = Set.copyOf(shopIdList);
                return list.stream()
                        .filter(t -> set.contains(t.getShop_id()))
                        .collect(Collectors.toList());
            }

            @Override
            public String getTitle() {
                return "Static";
            }
        };
    }

    public static <T extends WithShopId> SelectInterfaceShopsDat selectOfShopDat(List<ShopsDat> list) {
        return new SelectInterfaceShopsDat() {

            @Override
            public ShopsDat getShopsDat(int shopId) {
                return list.stream()
                        .filter(t -> t.getShop_id() == shopId)
                        .findFirst().orElse(new ShopsDat());
            }

            @Override
            public List<ShopsDat> selectAll() {
                return list;
            }

            @Override
            public String getTitle() {
                return "Static";
            }
        };
    }

    public static SelectInterfaceFilter selectFilter(List<Filter> list) {
        var delegate = selectOfShop(list);
        return new SelectInterfaceFilter() {

            @Override
            public List<Filter> selectAllFiltersBy(FilterClass filterClass) {
                return list.stream()
                        .filter(filter -> filter.getFilter_class() == filterClass)
                        .collect(Collectors.toList());
            }

            @Override
            public List<Filter> selectSingle(ShopArg shopId) {
                return delegate.selectSingle(shopId);
            }

            @Override
            public List<Filter> selectAll() {
                return delegate.selectAll();
            }

            @Override
            public List<Filter> selectList(Collection<Long> shopIdList) {
                return delegate.selectList(shopIdList);
            }

            @Override
            public String getTitle() {
                return delegate.getTitle();
            }
        };
    }

    public static SelectInterfaceShop selectShop(List<Shop> list) {
        var delegate = selectOfShop(list);
        return new SelectInterfaceShop() {

            @Override
            public List<ShopArg> selectActualShopKeys() {
                return selectActualShops().stream()
                        .map(shop -> new ShopArg(shop.getShop_id(), shop.getType()))
                        .collect(Collectors.toList());
            }

            @Override
            public List<ShopArg> selectActualShopKeys(ShopType shopType) {
                return selectActualShops(shopType).stream()
                        .map(shop -> new ShopArg(shop.getShop_id(), shop.getType()))
                        .collect(Collectors.toList());
            }

            @Override
            public List<Shop> selectActualShops() {
                var timeFrom = getInstant().minus(ApiConst.ALIVE_SHOP_DAYS, ChronoUnit.DAYS);
                return list.stream()
                        .filter(s -> s.getStatus() == ShopStatus.ACTIVE ||
                                s.getUpdated_at().isAfter(timeFrom))
                        .collect(Collectors.toList());
            }

            @Override
            public List<Shop> selectActualShops(ShopType shopType) {
                var timeFrom = getInstant().minus(ApiConst.ALIVE_SHOP_DAYS, ChronoUnit.DAYS);
                return list.stream()
                        .filter(s -> s.getType() == shopType && (s.getStatus() == ShopStatus.ACTIVE ||
                                s.getUpdated_at().isAfter(timeFrom)))
                        .collect(Collectors.toList());
            }

            @Override
            public List<Shop> selectSingle(ShopArg shopId) {
                return delegate.selectSingle(shopId);
            }

            @Override
            public List<Shop> selectAll() {
                return delegate.selectAll();
            }

            @Override
            public List<Shop> selectList(Collection<Long> shopIdList) {
                return delegate.selectList(shopIdList);
            }

            @Override
            public String getTitle() {
                return delegate.getTitle();
            }
        };
    }

    public static SelectInterfaceAutostrategy selectAutostrategy(List<Autostrategy> list) {
        var delegate = selectOfShop(list);
        return new SelectInterfaceAutostrategy() {
            @Override
            public List<Autostrategy> getCampaignsByShop(ShopArg shopId, long startingPriority) {
                return List.of();
            }

            @Override
            public boolean hasAutostrategiesInHistory(ShopArg shopId) {
                return false;
            }

            @Override
            public IntSet getAllShopsWithAutostrategiesInHistory() {
                return IntSets.EMPTY_SET;
            }

            @Override
            public List<Autostrategy> selectSingle(ShopArg shopId) {
                return delegate.selectSingle(shopId);
            }

            @Override
            public List<Autostrategy> selectAll() {
                return delegate.selectAll();
            }

            @Override
            public List<Autostrategy> selectList(Collection<Long> shopIdList) {
                return delegate.selectList(shopIdList);
            }

            @Override
            public String getTitle() {
                return delegate.getTitle();
            }
        };
    }

    public static SelectInterfacePriceRecommendations selectPriceRecommendations(Set<String> offers) {
        return shopId -> offers;
    }

    public static SelectInterfaceShopsDat selectShopDat(List<ShopsDat> list) {
        var delegate = selectOfShopDat(list);
        return new SelectInterfaceShopsDat() {
            @Override
            public ShopsDat getShopsDat(int shopId) {
                return delegate.getShopsDat(shopId);
            }

            @Override
            public List<ShopsDat> selectAll() {
                return delegate.selectAll();
            }

            @Override
            public String getTitle() {
                return delegate.getTitle();
            }
        };
    }

    public static Filter filter(long id) {
        return filter(id, Utils.emptyConsumer());
    }

    public static Filter filter(long id, Consumer<? super Filter> init) {
        var f = new Filter();
        f.setFilter_id(id);
        f.setQuery("");
        f.setCategory("");
        init.accept(f);
        return f;
    }

    public static Strategy strategy(long id) {
        return strategy(id, Utils.emptyConsumer());
    }

    public static Strategy strategy(long id, Consumer<? super Strategy> init) {
        var s = new Strategy();
        s.setStrategy_id(id);
        s.setType(StrategyFormType.MAIN);
        init.accept(s);
        return s;
    }

    public static StrategyState strategyState(long id, Consumer<? super StrategyState> init) {
        var s = new StrategyState();
        s.setStrategy_id(id);
        s.setLinked_enabled(true);
        init.accept(s);
        return s;
    }

    public static StrategyStateHistory strategyStateHistory(int shopId, int feedId, long strategyId, long changeId,
                                                            Instant updatedAt) {
        return strategyStateHistory(shopId, feedId, strategyId, changeId, updatedAt, Utils.emptyConsumer());
    }

    public static StrategyStateHistory strategyStateHistory(int shopId, int feedId, long strategyId, long changeId,
                                                            Instant updatedAt,
                                                            Consumer<? super StrategyStateHistory> init) {
        var s = new StrategyStateHistory();
        s.setShop_id(shopId);
        s.setFeed_id(feedId);
        s.setStrategy_id(strategyId);
        s.setChange_id(changeId);
        s.setUpdated_at(updatedAt);
        s.setLinked_enabled(true);
        init.accept(s);
        return s;
    }

    public static StrategyStateHistory strategyStateHistory(long id, Consumer<? super StrategyStateHistory> init) {
        var s = new StrategyStateHistory();
        s.setStrategy_id(id);
        s.setLinked_enabled(true);
        init.accept(s);
        return s;
    }

    public static ShopCategory shopCategory(long id) {
        return shopCategory(id, Utils.emptyConsumer());
    }

    public static ShopCategory shopCategory(long id, Consumer<? super ShopCategory> init) {
        var c = new ShopCategory();
        c.setCategory_id(id);
        c.setName("Category " + id);
        init.accept(c);
        c.normalizeIndexFields();
        return c;
    }

    public static ShopCategory shopCategory(int shopId, int feedId, int id, int parentId, String name, int offerCount) {
        return shopCategory(shopId, feedId, id, parentId, name, offerCount, Utils.emptyConsumer());
    }

    public static ShopCategory shopCategory(int shopId, int feedId, int id, int parentId, String name, int offerCount,
                                            Consumer<? super ShopCategory> init) {
        var c = new ShopCategory();
        c.setShop_id(shopId);
        c.setFeed_id(feedId);
        c.setCategory_id(id);
        c.setParent_category_id(parentId);
        c.setName(name);
        c.setStatus(Status.ACTIVE);
        c.setCreated_at(getInstant());
        c.setUpdated_at(getInstant());
        c.setOffer_count(offerCount);
        init.accept(c);
        c.normalizeIndexFields();
        return c;
    }

    public static Offer offer(String offerId) {
        return offer(offerId, Utils.emptyConsumer());
    }

    public static Offer offer(String offerId, Consumer<? super Offer> init) {
        var o = new Offer();
        o.setOffer_id(offerId);
        o.setCategory_id(ShopCategory.NO_CATEGORY);
        init.accept(o);
        o.normalizeFields();
        o.getOfferState().markChanged();
        return o;
    }

    public static OfferPush offerPush(int shopId, int feedId, String offerId) {
        return offerPush(shopId, feedId, offerId, Utils.emptyConsumer());
    }

    public static OfferPush offerPush(int shopId, int feedId, String offerId,
                                      Consumer<OfferOuterClass.Offer.Builder> init) {
        var builder = OfferOuterClass.Offer.newBuilder();
        builder.setShopId(shopId).setFeedId(feedId).setOfferId(offerId);
        init.accept(builder);
        return new OfferPush(builder.build());
    }

    public static ShopFeed feed(int feedId) {
        return feed(feedId, "");
    }

    public static ShopFeed feed(int feedId, String url) {
        var f = feed(feedId, Status.ACTIVE);
        f.setUrl(url);
        return f;
    }

    public static ShopFeed feed(int feedId, Status status) {
        var f = new ShopFeed();
        f.setFeed_id(feedId);
        f.setUrl("");
        f.setStatus(status);
        f.setDetails("");
        return f;
    }

    public static List<ShopFeed> feeds(int... feedId) {
        return IntStream.of(feedId)
                .mapToObj(TmsTestUtils::feed)
                .collect(Collectors.toList());
    }

    public static List<ShopFeed> feeds(Collection<Integer> feedId) {
        return feedId.stream()
                .map(TmsTestUtils::feed)
                .collect(Collectors.toList());
    }

    public static Shop shop(int shopId) {
        return shop(shopId, Utils.emptyConsumer());
    }

    public static Shop shop(int shopId, int feedId, int regionId) {
        return shop(shopId, feedId, regionId, Utils.emptyConsumer());
    }

    public static Shop shop(long shopId, long feedId, int regionId, Consumer<? super Shop> init) {
        Shop s = shop(shopId, init);
        s.setFeeds(Set.of(feedId));
        s.setRegion_id(regionId);
        return s;
    }

    public static Shop shop(long shopId, Consumer<? super Shop> init) {
        var s = new Shop();
        s.setShop_id(shopId);
        s.setType(ShopType.DSBS);
        s.setStatus(ShopStatus.ACTIVE);
        s.setUpdated_at(getInstant());
        init.accept(s);
        return s;
    }

    public static User user(long userId, long pl1UserId) {
        return user(userId, pl1UserId, Utils.emptyConsumer());
    }

    public static User user(long userId, long pl1UserId, Consumer<? super User> init) {
        var u = new User();
        u.setUser_id(userId);
        u.setPl_id(pl1UserId);
        u.setShard(SHARD_NAME);
        init.accept(u);
        return u;
    }

    public static Autostrategy autostrategy(int id, int shopId) {
        return autostrategy(id, shopId, Utils.emptyConsumer());
    }

    public static AutostrategyHistory autostrategyHistory(int id, int shopId, int priority) {
        AutostrategyHistory h = new AutostrategyHistory();
        h.setAutostrategy_id(id);
        h.setShop_id(shopId);
        h.setName("АС_" + id + "_магазин_" + shopId);
        h.setPriority(priority);
        h.setEnabled(true);
        return h;
    }

    public static Autostrategy autostrategy(int id, int shopId, Consumer<? super Autostrategy> init) {
        var a = new Autostrategy();
        a.setAutostrategy_id(id);
        a.setShop_id(shopId);
        init.accept(a);
        return a;
    }

    public static AutostrategyOffer autostrategyOffer(int id, int shopId, int feedId, String offerId, Instant now) {
        return autostrategyOffer(id, shopId, feedId, offerId, now, Utils.emptyConsumer());
    }

    public static AutostrategyOffer autostrategyOffer(int id, int shopId, int feedId, String offerId, Instant now,
                                                      Consumer<? super AutostrategyOffer> init) {
        var o = new AutostrategyOffer();
        o.setTimestamp(now.toEpochMilli());
        o.setAutostrategy_id(id);
        o.setShop_id(shopId);
        o.setFeed_id(feedId);
        o.setOffer_id(offerId);
        o.setUpdated_at(now);
        init.accept(o);
        return o;
    }


    //CHECKSTYLE:OFF
    public static AutostrategyOfferSource autostrategyOfferSource(int shopId, int feedId, String offerId, int targetId,
                                                                  int autostrategyId, int datasourceId, Instant now,
                                                                  long businessId, long modelId, int bid) {
        //CHECKSTYLE:ON
        return autostrategyOfferSource(shopId, feedId, offerId, targetId, autostrategyId, datasourceId, now, businessId,
                modelId, bid, Utils.emptyConsumer());
    }

    //CHECKSTYLE:OFF
    public static AutostrategyOfferSource autostrategyOfferSource(int shopId, int feedId, String offerId, int targetId,
                                                                  int autostrategyId, int datasourceId, Instant now,
                                                                  long businessId, long modelId, int bid,
                                                                  Consumer<? super AutostrategyOfferSource> init) {
        //CHECKSTYLE:ON
        var o = new AutostrategyOfferSource();
        o.setShop_id(shopId);
        o.setFeed_id(feedId);
        o.setOffer_id(offerId);
        o.setTarget_shop_id(shopId);
        o.setTarget_feed_id(feedId);
        o.setTarget_offer_id(offerId);
        o.setTarget_id(targetId);
        o.setAutostrategy_id(autostrategyId);
        o.setDatasource_id(datasourceId);
        o.setUpdated_at(now);
        o.setBusiness_id(businessId);
        o.setModel_id(modelId);
        o.setBid(bid);
        init.accept(o);
        return o;
    }

    //CHECKSTYLE:OFF
    public static AutostrategyOfferTarget autostrategyOfferTarget(int shopId, int feedId, String offerId, int targetId,
                                                                  int autostrategyId, int datasourceId, Instant now,
                                                                  long businessId, long modelId, int bid) {
        //CHECKSTYLE:ON
        return autostrategyOfferTarget(shopId, feedId, offerId, targetId, autostrategyId, datasourceId, now, businessId,
                modelId, bid, Utils.emptyConsumer());
    }

    //CHECKSTYLE:OFF
    public static AutostrategyOfferTarget autostrategyOfferTarget(int shopId, int feedId, String offerId, int targetId,
                                                                  int autostrategyId, int datasourceId, Instant now,
                                                                  long businessId, long modelId, int bid,
                                                                  Consumer<? super AutostrategyOfferTarget> init) {
        //CHECKSTYLE:ON
        var o = new AutostrategyOfferTarget();
        o.setTimestamp(now.toEpochMilli());
        o.setShop_id(shopId);
        o.setFeed_id(feedId);
        o.setOffer_id(offerId);
        o.setTarget_id(targetId);
        o.setAutostrategy_id(autostrategyId);
        o.setDatasource_id(datasourceId);
        o.setUpdated_at(now);
        o.setBusiness_id(businessId);
        o.setModel_id(modelId);
        o.setBid(bid);
        o.setType(OfferBidType.AUTOSTRATEGY);
        init.accept(o);
        return o;
    }

    public static AutostrategyState autostrategyState(int id, int shopId, Consumer<? super AutostrategyState> init) {
        var s = new AutostrategyState();
        s.setShop_id(shopId);
        s.setAutostrategy_id(id);
        init.accept(s);
        return s;
    }

    public static AutostrategyStateHistory autostrategyStateHistory(int id, int shopId,
                                                                    Consumer<? super AutostrategyStateHistory> init) {
        var s = new AutostrategyStateHistory();
        s.setShop_id(shopId);
        s.setAutostrategy_id(id);
        init.accept(s);
        return s;
    }

    public static AutostrategyShopState autostrategyShopState(int shopId,
                                                              Consumer<? super AutostrategyShopState> init) {
        var a = new AutostrategyShopState();
        a.setShop_id(shopId);
        init.accept(a);
        return a;
    }

    public static OfferVendor offerVendor(int shopId, String vendor, Instant instant) {
        var v = new OfferVendor();
        v.setShop_id(shopId);
        v.setFeed_id(0);
        v.setVendor(vendor);
        v.setUpdated_at(instant);
        return v;
    }

    public static ModelbidsPosition modelbidsPosition(int code, long vbid, int position) {
        var p = new ModelbidsPosition();
        p.setCode(code);
        p.setVbid(vbid);
        p.setPosition(position);
        return p;
    }

    public static DataSourceContent getDataSourceContent() {
        return new DataSourceContent();
    }

    public static CachedShopContent getCachedShopContent(int shopId, int feedId, int regionId) {
        ShopFeedArg args = new ShopFeedArg(shopId, feedId);
        Shop shop = shop(shopId, feedId, regionId);
        return getCachedShopContent(args, shop);
    }

    public static CachedShopContent getCachedShopContent(ShopFeedArg args, Shop shop) {
        return new CachedShopContent(args, shop);
    }

    public static OffersArg defaultOffersArg() {
        return defaultOffersArg(1);
    }

    public static OffersArg defaultOffersArg(long shopId) {
        return new OffersArg()
                .setShopId(shopId)
                .setType(ShopType.DSBS)
                .setCluster("cluster")
                .setIndexer("primary")
                .setGeneration(DEFAULT_GENERATION)
                .setCategoriesTable(DEFAULT_GENERATION);
    }

    public static <T extends Throwable> void assertThrowsWithMessage(
            Class<T> expectedType, Executable executable, String expectErrorMessage) {
        var error = Assertions.assertThrows(expectedType, executable);
        Assertions.assertEquals(expectErrorMessage.trim(), error.getMessage().trim(),
                () -> "Invalid error message for " + expectedType);
    }

    public static class DataSourceContent {
        //CHECKSTYLE:OFF
        public final List<Filter> filters = new ArrayList<>();
        public final List<Strategy> strategies = new ArrayList<>();
        public final List<Shop> shops = new ArrayList<>();
        public final List<Autostrategy> autostrategiesWhite = new ArrayList<>();
        public final List<Autostrategy> autostrategiesBlue = new ArrayList<>();
        public final List<Autostrategy> autostrategiesVendorBlue = new ArrayList<>();
        public final List<ShopsDat> shopDats = new ArrayList<>();
        public final Set<String> priceRecommendationsOfferIds = new HashSet<>();

        public final CachedDataSource dataSource;
        //CHECKSTYLE:ON

        private DataSourceContent() {
            var select = new SelectInterfaces(
                    selectFilter(filters),
                    selectShop(shops),
                    selectAutostrategy(autostrategiesWhite),
                    selectAutostrategy(autostrategiesBlue),
                    selectAutostrategy(autostrategiesVendorBlue),
                    selectShopDat(shopDats),
                    selectPriceRecommendations(priceRecommendationsOfferIds),
                    () -> List.of());
            dataSource = new CachedDataSource(CachedDataLoader.singleThreaded(select));
        }
    }

    public static class CachedShopContent {
        //CHECKSTYLE:OFF
        public final Long2ObjectMap<ShopCategory> categoryMap;
        public final ShopFeedArg arg;
        public final Shop shop;

        public final DataSourceContent dsContent;
        //CHECKSTYLE:ON

        private long updateIndex;

        @Nullable
        private CategoriesTreeHolder treeHolder;

        private CachedShopContent(@NonNull ShopFeedArg arg, @NonNull Shop shop) {
            this.arg = arg;
            this.shop = shop;

            this.dsContent = getDataSourceContent();
            this.dsContent.shops.add(shop);

            this.categoryMap = new Long2ObjectOpenHashMap<>();
        }

        public CachedShopFeedData newShopData() {
            return newShopData(true);
        }

        public CachedShopFeedData newShopData(boolean forceReloadCategories) {
            var task = new Task();
            task.setType(JobType.SHOP_LOOP_FULL);
            task.setJob_id(++updateIndex);

            if (treeHolder == null || forceReloadCategories) {
                treeHolder = new CategoriesTreeHolderImpl(arg.getFeedId(), categoryMap,
                        CoreTestUtils.emptyRunnable());
                treeHolder.flush();
            }

            var cachedData = dsContent.dataSource.loadShopCache(arg.getShopId());

            return new CachedShopFeedData(getInstant(), cachedData, arg,
                    () -> treeHolder);
        }

    }
}
