package ru.yandex.market.pricelabs.tms.processing.autostrategies;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import NMarket.NAmore.NAutostrategy.MarketAmoreService.TAutostrategies;
import com.google.protobuf.InvalidProtocolBufferException;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilter;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategyFilterSimple;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySave;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.AutostrategyShopState;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.types.AutostrategyTarget;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.model.types.WithTaskId;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.tms.AbstractTmsSpringConfiguration;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests.MockWebServerControls;
import ru.yandex.market.pricelabs.tms.jobs.AutostrategiesJob;
import ru.yandex.market.pricelabs.tms.jobs.AutostrategiesMonitoringJob;
import ru.yandex.market.pricelabs.tms.jobs.VendorAutostrategiesProcessingJob;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.categories.CategoriesTreeHolderImpl;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorDatasourcesProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorBasicTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyOfferTarget;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyShopState;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.autostrategyState;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shopCategory;

public abstract class AbstractAutostrategiesMonitoringJobTest extends AbstractTmsSpringConfiguration {

    private static final long MIN = TimeUnit.MINUTES.toMillis(1);

    protected static final int SHOP_ID = 2289;
    protected static final int FEED_ID = 3393;
    static final long BUSINESS_ID = 737313;

    static final int SHOP_ID_INACTIVE = 2290;
    private static final int FEED_ID_INACTIVE = 3394;

    static final int SHOP_ID_NON_PL2 = 2291;
    static final int SHOP_ID_NON_ID = 2292;

    protected static final int DATASOURCE_ID = 3391;

    @Autowired
    @Qualifier("autostrategiesMonitoringJobWhite")
    private AutostrategiesMonitoringJob jobWhite;

    @Autowired
    @Qualifier("autostrategiesMetaWhite")
    private AutostrategiesMetaProcessor metaProcessorWhite;

    @Autowired
    @Qualifier("autostrategiesStateWhite")
    private AutostrategiesStateProcessor stateProcessorWhite;

    @Autowired
    @Qualifier("autostrategiesMonitoringJobBlue")
    private AutostrategiesMonitoringJob jobBlue;

    @Autowired
    @Qualifier("autostrategiesMetaBlue")
    private AutostrategiesMetaProcessor metaProcessorBlue;

    @Autowired
    @Qualifier("autostrategiesStateBlue")
    private AutostrategiesStateProcessor stateProcessorBlue;

    @Autowired
    @Qualifier("vendorAutostrategiesProcessingJob")
    private VendorAutostrategiesProcessingJob jobVendorBlue;

    @Autowired
    @Qualifier("autostrategiesMetaVendorBlue")
    private AutostrategiesMetaProcessor metaProcessorVendorBlue;

    @Autowired
    @Qualifier("autostrategiesStateVendorBlue")
    private AutostrategiesStateProcessor stateProcessorVendorBlue;

    @Autowired
    private OffersProcessor offersProcessor;

    @Autowired
    @Qualifier("mockWebServerAmore")
    protected MockWebServerControls mockWebServerAmore;

    private AutostrategiesJob job;
    private AutostrategiesMetaProcessor metaProcessor;
    private AutostrategiesStateProcessor stateProcessor;

    protected YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersExecutor;
    private YtScenarioExecutor<AutostrategyState> autostrategiesStateExecutor;
    private YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateExecutor;
    private YtScenarioExecutor<Offer> offersExecutor;
    private YtScenarioExecutor<ShopCategory> categoryExecutor;

    protected int virtualFeedId;
    protected String offerPrefix;
    private int datasourceId;
    private int shopId;
    private AutostrategyTarget target;

    void init(AutostrategyTarget target) {
        this.target = target;
        this.job = target.get(jobWhite, jobBlue, jobVendorBlue);
        this.metaProcessor = target.get(metaProcessorWhite, metaProcessorBlue, metaProcessorVendorBlue);
        this.stateProcessor = target.get(stateProcessorWhite, stateProcessorBlue, stateProcessorVendorBlue);
        this.datasourceId = target == AutostrategyTarget.vendorBlue ? DATASOURCE_ID : 0;
        this.shopId = SHOP_ID;

        this.autostrategyOffersExecutor = target.get(
                executors.autostrategyOffersWhite(),
                executors.autostrategyOffersBlue(),
                executors.autostrategyOffersVendorTarget());
        this.autostrategiesStateExecutor = target.get(
                executors.autostrategiesStateWhite(),
                executors.autostrategiesStateBlue(),
                executors.autostrategiesStateVendorBlue());
        this.autostrategiesShopStateExecutor = target.get(
                executors.autostrategiesShopStateWhite(),
                executors.autostrategiesShopStateBlue(),
                executors.autostrategiesShopStateVendorBlue());
        this.offersExecutor = target.get(executors.offers(), executors.offersBlue());
        this.categoryExecutor = target.get(executors.categories(), executors.blueCategories());

        var virtualShopId = SHOP_ID;
        virtualFeedId = target.get(FEED_ID, ApiConst.VIRTUAL_FEED_BLUE);
        offerPrefix = target.get("", FEED_ID + ".");

        // Проверим, что фильтр по категориям применяется успешно
        var categories = List.of(
                shopCategory(1L, cat -> {
                    cat.setFeed_id(FEED_ID);
                    cat.setShop_id(virtualShopId);
                    cat.setStatus(Status.ACTIVE);
                }),
                shopCategory(2L, cat -> {
                    cat.setFeed_id(FEED_ID);
                    cat.setShop_id(virtualShopId);
                    cat.setParent_category_id(1L);
                    cat.setStatus(Status.ACTIVE);
                }));

        testControls.initOnce(this.getClass(), () -> {
            testControls.resetShops();

            testControls.executeInParallel(
                    () -> testControls.saveShop(shop(SHOP_ID, FEED_ID, 213, s -> {
                        s.setType(target.isWhite() ? ShopType.DSBS : ShopType.SUPPLIER);
                        s.setBusiness_id(BUSINESS_ID);
                    })),
                    () -> testControls.saveShop(shop(virtualShopId, virtualFeedId, 213, s -> {
                        s.setType(target.isWhite() ? ShopType.DSBS : ShopType.SUPPLIER);
                        s.setBusiness_id(BUSINESS_ID);
                    })),
                    () -> testControls.saveShop(shop(SHOP_ID_INACTIVE, FEED_ID_INACTIVE, 213, s -> {
                        s.setStatus(ShopStatus.INACTIVE);
                        s.setType(target.isWhite() ? ShopType.DSBS : ShopType.SUPPLIER);
                        s.setBusiness_id(BUSINESS_ID);
                    })),
                    () -> testControls.saveShop(shop(SHOP_ID_NON_PL2, FEED_ID, 213, s -> {
                        s.setType(target.isWhite() ? ShopType.DSBS : ShopType.SUPPLIER);
                        s.setBusiness_id(BUSINESS_ID);
                    })),
                    () -> testControls.saveShop(shop(SHOP_ID_NON_ID, SHOP_ID_NON_ID, 213, s -> {
                        s.setType(target.isWhite() ? ShopType.DSBS : ShopType.SUPPLIER);
                        s.setBusiness_id(BUSINESS_ID);
                    }))
            );

            testControls.executeInParallel(
                    () -> {
                        categoryExecutor.clearTargetTable();
                        Long2ObjectMap<ShopCategory> categoriesMap = new Long2ObjectOpenHashMap<>();
                        categories.forEach(cat -> categoriesMap.put(cat.getCategory_id(), cat));
                        new CategoriesTreeHolderImpl(virtualFeedId, categoriesMap, () ->
                                categoryExecutor.insert(categories)).flush();
                    }
            );

            if (target == AutostrategyTarget.vendorBlue) {
                var productId = VendorDatasourcesProcessor.PRODUCT_ID;
                executors.vendorDatasources().insert(List.of(
                        new VendorDatasource(productId, SHOP_ID, DATASOURCE_ID, getInstant()),
                        new VendorDatasource(productId, SHOP_ID_NON_ID, DATASOURCE_ID, getInstant()),
                        new VendorDatasource(productId, SHOP_ID_INACTIVE, DATASOURCE_ID, getInstant()),
                        new VendorDatasource(productId, SHOP_ID_NON_PL2, DATASOURCE_ID, getInstant())
                ));
            }
        });

        testControls.executeInParallel(
                () -> autostrategyOffersExecutor.clearTargetTable(),
                () -> autostrategiesShopStateExecutor.clearTargetTable(),
                () -> testControls.cleanupTasksService(),
                () -> offersExecutor.clearTargetTable(),
                () -> mockWebServerAmore.cleanup(),
                () -> AutostrategiesMetaProcessorWhiteTest.cleanupTables(metaProcessor, stateProcessor, testControls));
    }

    @Test
    void testNothingToDo() {
        testControls.executeJob(job);
        testControls.checkNoScheduledTasks();
    }

    @Test
    void testShopIsNotPL2() throws InvalidProtocolBufferException {
        int id = metaProcessor.create(1, SHOP_ID_NON_PL2, autostrategy());
        TimingUtils.addTime(MIN);

        testNoOffersWereLinkedImpl(id, SHOP_ID_NON_PL2);
    }

    @Test
    void testShopIsNotID() throws InvalidProtocolBufferException {
        int id = metaProcessor.create(1, SHOP_ID_NON_ID, autostrategy());
        TimingUtils.addTime(MIN);

        testNoOffersWereLinkedImpl(id, SHOP_ID_NON_ID);
    }

    @Test
    void testShopIsNotActive() throws InvalidProtocolBufferException {
        // Точно так же считается
        int id = metaProcessor.create(1, SHOP_ID_INACTIVE, autostrategy());
        TimingUtils.addTime(MIN);

        testNoOffersWereLinkedImpl(id, SHOP_ID_INACTIVE);
    }

    @Test
    void testShopIsActiveButNothingToDo() throws InvalidProtocolBufferException {
        var id = metaProcessor.create(1, SHOP_ID, autostrategy());
        TimingUtils.addTime(MIN);

        testNoOffersWereLinkedImpl(id, SHOP_ID);
    }

    @Test
    void testShopExcludeProcessingShops() {
        metaProcessor.create(1, SHOP_ID, autostrategy());
        TimingUtils.addTime(MIN);

        testControls.executeJob(job);
        testControls.startScheduledTask(job.getJobType());
        testControls.checkNoScheduledTasks();

        TimingUtils.addTime(MIN);
        testControls.executeJob(job);
        testControls.checkNoScheduledTasks(); // Новая не запускается - есть невыполненная задача
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 1000})
    void testOffersLinking(int batchSize) throws InvalidProtocolBufferException {
        testOffersLinkingImpl(batchSize);
    }

    private Task testOffersLinkingImpl(int batchSize) throws InvalidProtocolBufferException {
        var offers = new OffersProcessorBasicTest().readTargetList();
        TmsTestUtils.update(offers, offer -> {
            offer.setCategory_id(1L);
            if (offer.getOffer_id().equals("4370") || offer.getOffer_id().equals("4371")) {
                offer.setModel_id(1);
            }
        });

        offersProcessor.setBatchSize(batchSize);
        offersExecutor.insert(offers);

        var auto = autostrategy();
        var id = metaProcessor.create(1, SHOP_ID, auto);
        TimingUtils.addTime(MIN);

        testControls.executeJob(job);

        var task = testControls.startScheduledTask(job.getJobType());
        var now = getInstant();
        acceptAutostrategy(id, now, task);
        testControls.executeTask(task);

        Consumer<WithTaskId> updateTask = a -> {
            a.setJob_id(task.getJob_id());
            a.setTask_id(task.getTask_id());
        };
        Consumer<AutostrategyOfferTarget> updateOffer = o -> {
            updateTask.accept(o);
            o.setDatasource_id(datasourceId);
            o.setShop_id(target.get(shopId, ApiConst.VIRTUAL_SHOP_BLUE));
            acceptTargetOffer(o);
        };

        testControls.executeInParallel(
                () -> assertAutostrategyOffers(id, updateOffer, now),
                () -> autostrategiesStateExecutor.verify(List.of(
                        autostrategyState(id, SHOP_ID, s -> {
                            s.setUpdated_at(now);
                            s.setLinked_at(now);
                            s.setLinked_enabled(true);
                            s.setLinked_count(2);
                            updateTask.accept(s);
                        })
                )),
                () -> autostrategiesShopStateExecutor.verify(List.of(
                        autostrategyShopState(SHOP_ID, a -> {
                            a.setProcess_start(now);
                            a.setProcess_complete(now);
                            a.setUpdated_at(now);
                        })
                )));

        assertWebServerAutostrategySentResponse(id, now);

        testControls.checkNoScheduledTasks();
        testControls.taskMonitoringJob();

        TimingUtils.addTime(MIN);

        testControls.executeJob(job);
        testControls.checkNoScheduledTasks();

        return task;
    }

    protected void assertAutostrategyOffers(int id, Consumer<AutostrategyOfferTarget> updateOffer, Instant now) {
        autostrategyOffersExecutor.verify(List.of(
                autostrategyOfferTarget(SHOP_ID, virtualFeedId, offerPrefix + "4370", SHOP_ID, id, 0, now, BUSINESS_ID,
                        1, 300, updateOffer),
                autostrategyOfferTarget(SHOP_ID, virtualFeedId, offerPrefix + "4371", SHOP_ID, id, 0, now, BUSINESS_ID,
                        1, 300, updateOffer)
        ));
    }

    protected void assertAutostrategyOffersClean(int id, Consumer<AutostrategyOfferTarget> updateOfferBefore,
                                                 Consumer<AutostrategyOfferTarget> updateOffer, Instant before,
                                                 Instant now) {
        autostrategyOffersExecutor.verify(List.of(
                autostrategyOfferTarget(SHOP_ID, virtualFeedId, offerPrefix + "4370", SHOP_ID, id, 0, before,
                        BUSINESS_ID,
                        1, 300, updateOfferBefore),
                autostrategyOfferTarget(SHOP_ID, virtualFeedId, offerPrefix + "4371", SHOP_ID, id, 0, before,
                        BUSINESS_ID,
                        1, 300, updateOfferBefore),
                autostrategyOfferTarget(SHOP_ID, virtualFeedId, offerPrefix + "4370", SHOP_ID, 0, 0, now, BUSINESS_ID,
                        1, 0, o -> {
                            updateOffer.accept(o);
                            o.setType(null);
                        }),
                autostrategyOfferTarget(SHOP_ID, virtualFeedId, offerPrefix + "4371", SHOP_ID, 0, 0, now, BUSINESS_ID,
                        1, 0, o -> {
                            updateOffer.accept(o);
                            o.setType(null);
                        })
        ));
    }

    @Test
    void testOffersLinkingThenCleanAutostrategies() throws InvalidProtocolBufferException {
        var taskBefore = this.testOffersLinkingImpl(1000);

        var strategies = metaProcessor.selectAll();
        assertEquals(1, strategies.size());
        var id = strategies.get(0).getAutostrategy_id();

        metaProcessor.delete(1, SHOP_ID, id);

        // Т.к. мы добавили минуту в предыдущем тесте
        var before = getInstant().minus(1, ChronoUnit.MINUTES);
        TimingUtils.addTime(MIN);
        acceptAutostrategy(id, getInstant(), taskBefore);

        testControls.executeJob(job);
        var task = testControls.startScheduledTask(job.getJobType());
        testControls.executeTask(task);

        Consumer<WithTaskId> updateTaskBefore = o -> {
            o.setJob_id(taskBefore.getJob_id());
            o.setTask_id(taskBefore.getTask_id());
        };
        Consumer<AutostrategyOfferTarget> updateOfferBefore = o -> {
            updateTaskBefore.accept(o);
            o.setDatasource_id(datasourceId);
            o.setShop_id(target.get(shopId, ApiConst.VIRTUAL_SHOP_BLUE));
            acceptTargetOffer(o);
        };

        Consumer<AutostrategyOfferTarget> updateOffer = o -> {
            o.setJob_id(task.getJob_id());
            o.setTask_id(task.getTask_id());
            o.setDatasource_id(datasourceId);
            o.setShop_id(target.get(shopId, ApiConst.VIRTUAL_SHOP_BLUE));
            acceptTargetOffer(o);
            o.setBid(0);
        };

        var now = getInstant();

        testControls.executeInParallel(
                () -> assertAutostrategyOffersClean(id, updateOfferBefore, updateOffer, before, now),
                () -> autostrategiesStateExecutor.verify(List.of(
                        // Состояние привязок автостратегий не поменялось
                        autostrategyState(id, SHOP_ID, s -> {
                            s.setUpdated_at(before);
                            s.setLinked_at(before);
                            s.setLinked_enabled(true);
                            s.setLinked_count(2);
                            updateTaskBefore.accept(s);
                        })
                )),
                () -> autostrategiesShopStateExecutor.verify(List.of(
                        autostrategyShopState(SHOP_ID, a -> {
                            a.setProcess_start(now);
                            a.setProcess_complete(now);
                            a.setUpdated_at(now);
                        })
                )));

        assertEmptyWebServerResponse(now);

        testControls.checkNoScheduledTasks();

        TimingUtils.addTime(MIN);

        testControls.executeJob(job);
        testControls.checkNoScheduledTasks();
    }

    private void testNoOffersWereLinkedImpl(int id, int shopId) throws InvalidProtocolBufferException {
        testControls.executeJob(job);

        testControls.checkActiveJobTypes(job.getJobType());

        var task = testControls.startScheduledTask(job.getJobType());
        testControls.executeTask(task);

        testControls.executeInParallel(
                () -> autostrategyOffersExecutor.verify(List.of()),
                () -> autostrategiesStateExecutor.verify(List.of(
                        autostrategyState(id, shopId, s -> {
                            s.setUpdated_at(getInstant());
                            s.setLinked_at(getInstant());
                            s.setLinked_enabled(true);
                            s.setJob_id(task.getJob_id());
                            s.setTask_id(task.getTask_id());
                        })
                )),
                () -> autostrategiesShopStateExecutor.verify(List.of(
                        autostrategyShopState(shopId, a -> {
                            a.setProcess_start(getInstant());
                            a.setProcess_complete(getInstant());
                            a.setUpdated_at(getInstant());
                        })
                )));

        assertWebServerNoOfferResponse(shopId);

        testControls.checkNoScheduledTasks();
    }

    private AutostrategySave autostrategy() {
        return new AutostrategySave()
                .name("Sample")
                .enabled(true)
                .filter(autostrategyFilter())
                .settings(autostrategySettings());
    }

    protected AutostrategyFilter autostrategyFilter() {
        return new AutostrategyFilter()
                .type(AutostrategyFilter.TypeEnum.SIMPLE)
                .simple(new AutostrategyFilterSimple()
                        .priceFrom(1000000L)
                        .categories(List.of(1L)));
    }

    protected AutostrategySettings autostrategySettings() {
        return new AutostrategySettings()
                .type(AutostrategySettings.TypeEnum.CPA)
                .cpa(new AutostrategySettingsCPA().drrBid(300L));
    }

    protected void expectAmoreSettings(TAutostrategies.AutostrategyParams.Builder params) {
        params.getCpaBuilder().setCpa(300);
    }


    protected void assertWebServerNoOfferResponse(int shopId) throws InvalidProtocolBufferException {
        var request = mockWebServerAmore.getMessage();

        var expectAmore = TAutostrategies.newBuilder();
        expectAmore.addShopsBuilder()
                .setShopId(shopId)
                .setNOffers(0)
                .setTsCreate(timeSource().getMillis());

        var actualAmore = TAutostrategies.parseFrom(request.getBody().readByteArray());
        assertEquals(expectAmore.build(), actualAmore);
    }

    protected void assertWebServerAutostrategySentResponse(int autostrategyId,
                                                           Instant now) throws InvalidProtocolBufferException {
        var request = mockWebServerAmore.getMessage();

        var expectAmore = TAutostrategies.newBuilder();
        expectAmoreSettings(expectAmore.addShopsBuilder()
                .setShopId(SHOP_ID)
                .setNOffers(2)
                .setTsCreate(now.toEpochMilli())
                .addAsParamsBuilder()
                .setUid(autostrategyId));

        var actualAmore = TAutostrategies.parseFrom(request.getBody().readByteArray());
        assertEquals(expectAmore.build(), actualAmore);
    }

    protected void assertEmptyWebServerResponse(Instant now) throws InvalidProtocolBufferException {
        var request = mockWebServerAmore.getMessage();

        var expectAmore = TAutostrategies.newBuilder();
        expectAmore.addShopsBuilder()
                .setShopId(SHOP_ID)
                .setTsCreate(now.toEpochMilli());

        var actualAmore = TAutostrategies.parseFrom(request.getBody().readByteArray());
        assertEquals(expectAmore.build(), actualAmore);
    }

    protected void acceptAutostrategy(int id, Instant now, Task task) {

    }

    protected void acceptTargetOffer(AutostrategyOfferTarget offer) {

    }
}
