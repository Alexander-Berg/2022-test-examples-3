package ru.yandex.market.pricelabs.tms.processing.offers;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.pricelabs.apis.ApiConst;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettings;
import ru.yandex.market.pricelabs.generated.server.pub.model.AutostrategySettingsCPA;
import ru.yandex.market.pricelabs.model.AutostrategyOfferSource;
import ru.yandex.market.pricelabs.model.AutostrategyOfferTarget;
import ru.yandex.market.pricelabs.model.AutostrategyShopState;
import ru.yandex.market.pricelabs.model.AutostrategyState;
import ru.yandex.market.pricelabs.model.AutostrategyStateHistory;
import ru.yandex.market.pricelabs.model.BrandBusiness;
import ru.yandex.market.pricelabs.model.Filter;
import ru.yandex.market.pricelabs.model.MarketCategory;
import ru.yandex.market.pricelabs.model.NewOfferGen;
import ru.yandex.market.pricelabs.model.NewShopCategory;
import ru.yandex.market.pricelabs.model.NewShopsDat;
import ru.yandex.market.pricelabs.model.NewVendorDatasource;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.OfferVendor;
import ru.yandex.market.pricelabs.model.Shop;
import ru.yandex.market.pricelabs.model.ShopCategory;
import ru.yandex.market.pricelabs.model.ShopsDat;
import ru.yandex.market.pricelabs.model.User;
import ru.yandex.market.pricelabs.model.VendorDatasource;
import ru.yandex.market.pricelabs.model.VendorModelBid;
import ru.yandex.market.pricelabs.model.types.ShopStatus;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.processing.autostrategies.AutostrategiesMetaProcessor;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaign;
import ru.yandex.market.pricelabs.processing.monetization.model.AdvCampaignHistory;
import ru.yandex.market.pricelabs.processing.monetization.model.OfferBids;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.ConfigurationForTests.MockWebServerControls;
import ru.yandex.market.pricelabs.tms.jobs.ShopLoopFullJob;
import ru.yandex.market.pricelabs.tms.processing.AbstractSourceTargetProcessorConfiguration;
import ru.yandex.market.pricelabs.tms.processing.TaskChildrenWriter;
import ru.yandex.market.pricelabs.tms.processing.TaskInfo;
import ru.yandex.market.pricelabs.tms.processing.TasksController;
import ru.yandex.market.pricelabs.tms.processing.TmsTestUtils;
import ru.yandex.market.pricelabs.tms.processing.YtScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.YtSourceTargetScenarioExecutor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AbstractAutostrategiesMetaProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesStateProcessor;
import ru.yandex.market.pricelabs.tms.processing.autostrategies.AutostrategiesVendorContextSource;
import ru.yandex.market.pricelabs.tms.processing.categories.CategoriesProcessorTest;
import ru.yandex.market.pricelabs.tms.processing.cleanup.OffersCleanupProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.ShopsProcessor;
import ru.yandex.market.pricelabs.tms.processing.offers.Expect.ExpectBuilder;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.filter;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.shop;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class AbstractOffersProcessorTest
        extends AbstractSourceTargetProcessorConfiguration<NewOfferGen, Offer> implements OffersProcessorSources {

    static final int UID = 1;

    static final int SHOP_ID = 2289;
    public static final int FEED_ID = 3393;
    static final int CAMPAIGN_ID = 88995;
    static final int LARGE_BATCH = 1000;
    static final int SMALL_BATCH = 2;

    static final long BUSINESS_ID = 737313;
    static final int VENDOR_ID = 5001;
    static final int VENDOR_ID2 = 5002;
    static final int VENDOR_ID3 = 5003;

    static final int DATASOURCE_ID = 6001;
    static final int DATASOURCE_ID2 = 6002;

    User user;
    protected Shop shop;
    OffersArg args;

    @Autowired
    protected OffersProcessor processor;

    @Autowired
    OffersProcessorRouter processorRouter;

    @Autowired
    ShopOffersProcessor shopOffersProcessor;

    @Autowired
    ShopLoopFullJob job;

    @Autowired
    protected TasksController controller;

    @Autowired
    TasksService tasksService;

    @Autowired
    @Qualifier("mockWebServerPartnerApi")
    MockWebServerControls mockWebServerPartnerApi;

    @Autowired
    @Qualifier("mockWebServerMarketReport")
    MockWebServerControls mockWebServerMarketReport;

    @Autowired
    @Qualifier("mockWebServerMarketReportLowLatency")
    MockWebServerControls mockWebServerMarketReportLowLatency;

    @Autowired
    @Qualifier("mockWebServerAmore")
    MockWebServerControls mockWebServerAmore;

    @Autowired
    @Qualifier("autostrategiesMetaWhite")
    AutostrategiesMetaProcessor metaProcessor;

    @Autowired
    @Qualifier("autostrategiesStateWhite")
    AutostrategiesStateProcessor stateProcessor;

    @Autowired
    @Qualifier("autostrategiesMetaBlue")
    AutostrategiesMetaProcessor metaProcessorBlue;

    @Autowired
    @Qualifier("autostrategiesMetaVendorBlue")
    AutostrategiesMetaProcessor metaProcessorVendorBlue;

    @Autowired
    @Qualifier("autostrategiesStateBlue")
    AutostrategiesStateProcessor stateProcessorBlue;

    @Autowired
    @Qualifier("autostrategiesStateVendorBlue")
    AutostrategiesStateProcessor stateProcessorVendorBlue;

    @Autowired
    ShopsProcessor shopProcessor;

    @Autowired
    protected OffersCleanupProcessor offersCleanupProcessor;

    @Autowired
    AutostrategiesVendorContextSource autostrategiesVendorContextSource;

    protected YtSourceTargetScenarioExecutor<NewOfferGen, Offer> executor;
    YtScenarioExecutor<Offer> executorBlue;

    CategoriesProcessorTest categoriesProcessorTest;
    YtSourceTargetScenarioExecutor<NewShopCategory, ShopCategory> categoriesExecutor;
    YtSourceTargetScenarioExecutor<MarketCategory, ShopCategory> marketCategoriesExecutor;
    YtScenarioExecutor<ShopCategory> blueCategoriesExecutor;

    YtScenarioExecutor<OfferVendor> vendorsExecutor;
    YtScenarioExecutor<OfferVendor> vendorsExecutorBlue;
    YtScenarioExecutor<BrandBusiness> brandBusiness;
    YtScenarioExecutor<VendorModelBid> vendorModelBid;

    YtScenarioExecutor<Filter> filtersExecutor;

    YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersExecutor;
    YtScenarioExecutor<AutostrategyState> autostrategiesStateExecutor;
    YtScenarioExecutor<AutostrategyStateHistory> autostrategiesStateHistoryExecutor;
    YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateExecutor;

    YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersExecutorBlue;
    YtScenarioExecutor<AutostrategyState> autostrategiesStateExecutorBlue;
    YtScenarioExecutor<AutostrategyStateHistory> autostrategiesStateHistoryExecutorBlue;
    YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateExecutorBlue;

    YtScenarioExecutor<AutostrategyOfferSource> autostrategyOffersExecutorVendorSource;
    YtScenarioExecutor<AutostrategyOfferTarget> autostrategyOffersExecutorVendorTarget;
    YtScenarioExecutor<AutostrategyState> autostrategiesStateExecutorVendorBlue;
    YtScenarioExecutor<AutostrategyStateHistory> autostrategiesStateHistoryExecutorVendorBlue;
    YtScenarioExecutor<AutostrategyShopState> autostrategiesShopStateExecutorVendorBlue;

    protected YtSourceTargetScenarioExecutor<NewShopsDat, ShopsDat> shopsDatExecutor;

    YtSourceTargetScenarioExecutor<NewVendorDatasource, VendorDatasource> vendorDatasourceExecutor;

    protected YtScenarioExecutor<AdvCampaign> advCampaignYtScenarioExecutor;
    protected YtScenarioExecutor<AdvCampaignHistory> advCampaignHistoryYtScenarioExecutor;
    protected YtScenarioExecutor<OfferBids> advOfferBidsYtScenarioExecutor;

    OffersCheckers checkers;

    @Mock
    TaskChildrenWriter taskWriter;

    public static AutostrategySettings autostrategySettings(long bid) {
        return new AutostrategySettings()
                .type(AutostrategySettings.TypeEnum.CPA)
                .cpa(new AutostrategySettingsCPA()
                        .drrBid(bid));
    }

    void saveAsVirtualShop() {
        saveAsVirtualShop(shop);
    }

    public static Shop initShop(long shopId, Consumer<Shop> initShop) {
        return shop(shopId, s -> {
            s.setStatus(ShopStatus.ACTIVE);

            initShop.accept(s);
        });
    }

    @Override
    public String getSourceCsvPrefix() {
        return "tms/processing/offers/offers_source_gen";
    }

    @Override
    protected String getSourceCsv() {
        return getSourceCsvPrefix() + ".csv";
    }

    @Override
    protected Class<NewOfferGen> getSourceClass() {
        return NewOfferGen.class;
    }

    @Override
    protected String getTargetCsv() {
        return "tms/processing/offers/offers_target.csv";
    }

    @Override
    protected Class<Offer> getTargetClass() {
        return Offer.class;
    }
    //

    @Override
    protected Consumer<NewOfferGen> getSourceUpdate() {
        return offer -> {
            if (offer.getSupplier_id_int() == 0) {
                offer.setSupplier_id(null);
            }
        };
    }

    @Override
    protected Consumer<Offer> getTargetUpdate() {
        return Offer::normalizeFields;
    }

    //

    ExpectBuilder expectBuilder() {
        return new ExpectBuilder();
    }

    void checkTaskResult(TaskStatus status, String message) {
        checkTaskResult(status, message, 1);
    }

    void checkTaskResult(int shopId, TaskStatus status, String message) {
        checkTaskResult(shopId, status, message, 1);
    }

    void checkTaskResult(TaskStatus status, String message, int expectCount) {
        checkTaskResult(SHOP_ID, status, message, expectCount);
    }

    void saveAsVirtualShop(Shop shop) {
        shop.setShop_id(ApiConst.VIRTUAL_SHOP_BLUE);
        shop.setType(ShopType.SUPPLIER);
        shopProcessor.saveShop(shop);
    }

    void checkTaskResult(long taskId, TaskStatus status, String message) {
        var task = testControls.getTask(taskId);
        log.info("Found task: {}", task);
        assertEquals(status, task.getStatus());
        assertEquals(message, task.getInfo());
    }

    void test(Expect expect) {
        test(expect, OffersCheckers::resetWareAndCurrency);
    }

    void checkTaskResult(long shopId, TaskStatus status, String message, int expectCount) {
        var tasks = testControls.getShopTasks((int) shopId);
        log.info("Tasks: {}", tasks);
        assertEquals(expectCount, tasks.size());
        assertTrue(expectCount > 0);

        for (Task task : tasks) {
            assertEquals(status, task.getStatus());
            assertEquals(message, task.getInfo());
        }
    }

    protected void assertAutostrategyStates(Expect expect) {
        testControls.executeInParallel(
                () -> autostrategiesStateExecutor.verify(expect.getAutostrategyStates()),
                () -> autostrategiesStateHistoryExecutor.verify(expect.getAutostrategyStatesHistory()),
                () -> autostrategiesShopStateExecutor.verify(expect.getAutostrategyShopStates()),
                () -> autostrategyOffersExecutor.verify(expect.getAutostrategyOffers())
        );
    }

    List<Offer> readActiveOffers() {
        return filter(readTargetList(), o -> o.getStatus() == Status.ACTIVE);
    }

    protected Task startScheduledTask(JobType expectType) {
        return testControls.startScheduledTask(expectType);
    }

    protected void checkNoScheduledTasks() {
        testControls.checkNoScheduledTasks();
    }

    protected void doFullJob() {
        testControls.executeJob(job);
    }

    @BeforeEach
    void init() {
        processor.setBatchSize(LARGE_BATCH);

        var generation = Objects.requireNonNull(executors.getSourceGenerationTable());
        var table = Objects.requireNonNull(executors.getSourceOffersTable());
        args = new OffersArg()
                .setShopId(SHOP_ID)
                .setType(ShopType.DSBS)
                .setCluster(testControls.getCurrentCluster())
                .setIndexer(testControls.getCurrentIndexer())
                .setGeneration(generation)
                .setCategoriesTable(generation);

        user = TmsTestUtils.user(1000, 2000);

        shop = initShop(args.getShopId(), s -> {
            s.setBusiness_id(BUSINESS_ID);
            s.setFeeds(Set.of((long) FEED_ID));
            s.setDomain("domain");
        });

        this.executor = executors.offersGen();
        this.executorBlue = executors.offersBlue();

        this.categoriesProcessorTest = new CategoriesProcessorTest();
        this.categoriesExecutor = executors.categories();

        this.marketCategoriesExecutor = executors.marketCategories();
        this.blueCategoriesExecutor = executors.blueCategories();

        this.vendorsExecutor = executors.offerVendors();
        this.vendorsExecutorBlue = executors.offerBlueVendor();
        this.brandBusiness = executors.brandBusiness();

        this.vendorModelBid = executors.vendorModelBid();

        this.filtersExecutor = executors.filters();

        this.autostrategyOffersExecutor = executors.autostrategyOffersWhite();
        this.autostrategiesStateExecutor = executors.autostrategiesStateWhite();
        this.autostrategiesStateHistoryExecutor = executors.autostrategiesStateHistoryWhite();
        this.autostrategiesShopStateExecutor = executors.autostrategiesShopStateWhite();

        this.autostrategyOffersExecutorBlue = executors.autostrategyOffersBlue();
        this.autostrategiesStateExecutorBlue = executors.autostrategiesStateBlue();
        this.autostrategiesStateHistoryExecutorBlue = executors.autostrategiesStateHistoryBlue();
        this.autostrategiesShopStateExecutorBlue = executors.autostrategiesShopStateBlue();

        this.autostrategyOffersExecutorVendorSource = executors.autostrategyOffersVendorSource();
        this.autostrategyOffersExecutorVendorTarget = executors.autostrategyOffersVendorTarget();
        this.autostrategiesStateExecutorVendorBlue = executors.autostrategiesStateVendorBlue();
        this.autostrategiesStateHistoryExecutorVendorBlue = executors.autostrategiesStateHistoryVendorBlue();
        this.autostrategiesShopStateExecutorVendorBlue = executors.autostrategiesShopStateVendorBlue();

        this.vendorDatasourceExecutor = executors.vendorDatasources();
        this.shopsDatExecutor = executors.shopsDat();

        this.advCampaignYtScenarioExecutor = executors.advCampaignExecutor();
        this.advCampaignHistoryYtScenarioExecutor = executors.advCampaignHistoryExecutor();
        this.advOfferBidsYtScenarioExecutor = executors.advOfferBidsYtScenarioExecutor();

        testControls.executeInParallel(
                () -> shopsDatExecutor.clearSourceTable(),
                () -> {
                    testControls.resetShopDat();
                    testControls.saveShopDat(SHOP_ID, BUSINESS_ID, false);
                },
                () -> executors.offersGen().clearSourceTable(),
                () -> executor.clearTargetTable(),
                () -> {
                    marketCategoriesExecutor.clearSourceTable();
                    marketCategoriesExecutor.clearTargetTable();
                },
                () -> categoriesExecutor.clearSourceTable(),
                () -> categoriesExecutor.clearTargetTable(),
                () -> blueCategoriesExecutor.clearTargetTable(),
                () -> blueCategoriesExecutor.clearTargetTable(),
                () -> testControls.cleanupTasksService(),
                () -> testControls.cleanupTableRevisions(),
                () -> {
                    testControls.resetShops();
                    testControls.saveShop(shop);
                },
                () -> mockWebServerMarketReport.cleanup(),
                () -> mockWebServerMarketReportLowLatency.cleanup(),
                () -> mockWebServerPartnerApi.cleanup(),
                () -> mockWebServerAmore.cleanup(),
                () -> filtersExecutor.clearTargetTable(),
                () -> autostrategyOffersExecutor.clearTargetTable(),
                () -> autostrategiesShopStateExecutor.clearTargetTable(),
                () -> AbstractAutostrategiesMetaProcessorTest.cleanupTables(metaProcessor, stateProcessor,
                        testControls),
                () -> testControls.resetCaches(),
                () -> testControls.resetExportService(),
                () -> advCampaignYtScenarioExecutor.clearTargetTable(),
                () -> advCampaignHistoryYtScenarioExecutor.clearTargetTable(),
                () -> advOfferBidsYtScenarioExecutor.clearTargetTable()
        );

        this.checkers = new OffersCheckers(this);
        autostrategiesVendorContextSource.resetCachedContext();
    }

    public static NewShopsDat createNewShopDat(int shopId, boolean dsbs, boolean enabled) {
        NewShopsDat dat = new NewShopsDat();
        dat.set_dsbs(dsbs);
        dat.setShop_id(shopId);
        dat.setShop_currency("RUR");
        dat.setShopname("name_" + shopId);
        dat.setDomain("www." + dat.getShopname() + ".ru");
        dat.set_enabled(enabled);
        return dat;
    }

    void test(Expect expect, Consumer<Offer> expectUpdate) {
        expect.getNewRows().forEach(s -> {
            if (args.getType() == ShopType.DSBS) {
                s.setShop_id_int((int) args.getShopId());
            }
        });
        expect.getExistingRows().forEach(s -> s.setShop_id(args.getShopId()));
        expect.getExpectRows().forEach(s -> s.setShop_id(args.getShopId()));
        expect.getAutostrategyOffers().forEach(s -> s.setShop_id((int) args.getShopId()));
        expect.getAutostrategyStates().forEach(s -> s.setShop_id((int) args.getShopId()));
        expect.getAutostrategyStatesHistory().forEach(s -> s.setShop_id((int) args.getShopId()));
        expect.getAutostrategyShopStates().forEach(s -> s.setShop_id((int) args.getShopId()));
        expect.getStrategyStates().forEach(s -> s.setShop_id(args.getShopId()));

        if (expect.getFeedId() > 0) {
            expect.getExistingRows().forEach(s -> s.setFeed_id(expect.getFeedId()));
            expect.getExpectRows().forEach(s -> s.setFeed_id(expect.getFeedId()));
            expect.getAutostrategyOffers().forEach(s -> s.setFeed_id(expect.getFeedId()));
            expect.getStrategyStates().forEach(s -> s.setFeed_id(expect.getFeedId()));
        }

        var exec = expect.isBlue() ? executors.blueOffersGen() : executor;
        exec.test(() -> {
                    if (expect.getAction() != null) {
                        expect.getAction().run();
                    } else {
                        var task = new TaskInfo(getInstant(), taskWriter);
                        var response = shopOffersProcessor.syncOffers(args, task);
                        assertEquals(expect.getResponse(), response.getMessage());
                    }
                },
                expect.getNewRows(), expect.getExistingRows(),
                expect.getExpectRows(),
                expectUpdate);

        assertAutostrategyStates(expect);
    }
}
