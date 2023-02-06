package ru.yandex.market.pricelabs.tms.processing;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.pricelabs.misc.EmptyArg;
import ru.yandex.market.pricelabs.misc.ToJsonString;
import ru.yandex.market.pricelabs.model.types.ShopType;
import ru.yandex.market.pricelabs.processing.ShopArg;
import ru.yandex.market.pricelabs.services.database.model.JobType;
import ru.yandex.market.pricelabs.services.database.model.Task;
import ru.yandex.market.pricelabs.services.database.model.TaskStatus;
import ru.yandex.market.pricelabs.tms.cache.CachedDataSource;
import ru.yandex.market.pricelabs.tms.processing.categories.recommendations.CategoryRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.cleanup.OffersBlueCleanupProcessor;
import ru.yandex.market.pricelabs.tms.processing.cleanup.OffersCleanupProcessor;
import ru.yandex.market.pricelabs.tms.processing.daily.DailyProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.BusinessProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.MbiContactInfoProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.PartnerBusinessProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorBrandMapProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.VendorDatasourcesProcessor;
import ru.yandex.market.pricelabs.tms.processing.imports.program.PartnerProgramProcessor;
import ru.yandex.market.pricelabs.tms.processing.modelbids.ModelbidsRecommendationImportProcessor;
import ru.yandex.market.pricelabs.tms.processing.modelbids.ModelbidsRecommendationSyncProcessor;
import ru.yandex.market.pricelabs.tms.processing.modelbids.ModelsRangeArg;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersArg;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersProcessorRouter;
import ru.yandex.market.pricelabs.tms.processing.offers.ShopOffersProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.OffersRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.fee.FeeRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.fee.NewFeeRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.price.NewPriceRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.recommendations.price.PriceRecommendationsProcessor;
import ru.yandex.market.pricelabs.tms.processing.stats.BlueBidsRecommenderProcessor;
import ru.yandex.market.pricelabs.tms.services.database.TasksService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
public class ProcessingRouterTest {

    static final long NOW = 1580472798579L;


    private static final TestData<OffersArg> SHOP_LOOP_FULL =
            new TestData<>(JobType.SHOP_LOOP_FULL,
                    TmsTestUtils.defaultOffersArg(1),
                    (t, d) -> verify(t.shopOffersProcessor).syncOffers(eq(d.args), eq(d.taskInfo)));

    private static final TestData<OffersArg> SHOP_LOOP_FULL_LARGE =
            new TestData<>(JobType.SHOP_LOOP_FULL_LARGE,
                    TmsTestUtils.defaultOffersArg(1),
                    (t, d) -> verify(t.shopOffersProcessor).syncOffers(eq(d.args), eq(d.taskInfo)));

    private static final TestData<ModelsRangeArg> MODELBIDS_RECOMMENDATION =
            new TestData<>(JobType.MODELBIDS_RECOMMENDATION,
                    new ModelsRangeArg(1, 2, NOW - 1, NOW + 1),
                    (t, d) -> verify(t.modelbidsRecommendationSyncProcessor).syncModels(eq(d.args)));

    private static final TestData<EmptyArg> MODELBIDS_IMPORT =
            new TestData<>(JobType.MODELBIDS_IMPORT,
                    new EmptyArg(),
                    (t, d) -> verify(t.modelbidsRecommendationImportProcessor).sync());

    private static final TestData<OffersArg> SHOP_LOOP_FULL_PRIORITY =
            new TestData<>(JobType.SHOP_LOOP_FULL_PRIORITY,
                    TmsTestUtils.defaultOffersArg(1),
                    (t, d) -> verify(t.shopOffersProcessor).syncOffers(eq(d.args), eq(d.taskInfo)));

    private static final TestData<ShopArg> AUTOSTRATEGIES_WHITE_MAP =
            new TestData<>(JobType.AUTOSTRATEGIES_WHITE_MAP_PRIORITY,
                    new ShopArg(1, ShopType.DSBS),
                    (t, d) -> verify(t.offersProcessorRouter).syncAutostrategiesWhite(eq(d.args), eq(d.taskInfo)));

    private static final TestData<ShopArg> AUTOSTRATEGIES_BLUE_MAP =
            new TestData<>(JobType.AUTOSTRATEGIES_BLUE_MAP_PRIORITY,
                    new ShopArg(1, ShopType.DSBS),
                    (t, d) -> verify(t.offersProcessorRouter).syncAutostrategiesBlue(eq(d.args), eq(d.taskInfo)));

    private static final TestData<EmptyArg> AUTOSTRATEGIES_VENDOR_BLUE_MAP =
            new TestData<>(JobType.AUTOSTRATEGIES_VENDOR_BLUE_MAP_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.offersProcessorRouter).mapVendorBlueAutostrategies(eq(d.taskInfo)));

    private static final TestData<EmptyArg> AUTOSTRATEGIES_VENDOR_PROCESSING =
            new TestData<>(JobType.AUTOSTRATEGIES_VENDOR_PROCESSING_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.offersProcessorRouter).syncVendorAutostrategies(eq(d.taskInfo)));

    private static final TestData<EmptyArg> SYNC_BLUE_BIDS_RECOMMENDER_PRIORITY =
            new TestData<>(JobType.SYNC_BLUE_BIDS_RECOMMENDER_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.blueBidsRecommenderProcessor).sync());

    private static final TestData<EmptyArg> SYNC_VENDOR_BLUE_DATASOURCES_PRIORITY =
            new TestData<>(JobType.SYNC_VENDOR_BLUE_DATASOURCES_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.vendorDatasourcesProcessor).sync());

    private static final TestData<EmptyArg> SYNC_VENDOR_BLUE_MAP_PRIORITY =
            new TestData<>(JobType.SYNC_VENDOR_BLUE_MAP_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.vendorBrandMapProcessor).sync());

    private static final TestData<EmptyArg> SYNC_BUSINESS_PRIORITY =
            new TestData<>(JobType.SYNC_BUSINESS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.businessProcessor).sync());

    private static final TestData<EmptyArg> SYNC_CATEGORY_RECOMMENDATIONS_PRIORITY =
            new TestData<>(JobType.SYNC_CATEGORY_RECOMMENDATIONS,
                    new EmptyArg(),
                    (t, d) -> verify(t.categoryRecommendationsProcessor).sync());

    private static final TestData<EmptyArg> CLEANUP_OFFERS_PRIORITY =
            new TestData<>(JobType.CLEANUP_OFFERS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.offersCleanupProcessor).cleanup());

    private static final TestData<EmptyArg> CLEANUP_OFFERS_BLUE_PRIORITY =
            new TestData<>(JobType.CLEANUP_OFFERS_BLUE_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.offersBlueCleanupProcessor).cleanup());

    private static final TestData<EmptyArg> DAILY_PROCESSING_PRIORITY =
            new TestData<>(JobType.DAILY_PROCESSING_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.dailyProcessor).process());

    private static final TestData<EmptyArg> PARTNER_PROGRAM_PRIORITY =
            new TestData<>(JobType.PARTNER_PROGRAM_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.partnerProgramProcessor).sync());

    private static final TestData<EmptyArg> PARTNER_BUSINESS_PRIORITY =
            new TestData<>(JobType.PARTNER_BUSINESS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.partnerBusinessProcessor).sync());


    private static final TestData<EmptyArg> SYNC_OFFER_RECOMMENDER_PRIORITY =
            new TestData<>(JobType.SYNC_OFFER_RECOMMENDER_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.offersRecommendationsProcessor).sync());

    private static final TestData<EmptyArg> SYNC_MBI_CONTACT_INFO_PRIORITY =
            new TestData<>(JobType.SYNC_MBI_CONTACT_INFO_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.mbiContactInfoProcessor).sync());

    private static final TestData<EmptyArg> SYNC_PRICE_RECOMMENDATIONS_PRIORITY =
            new TestData<>(JobType.SYNC_PRICE_RECOMMENDATIONS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.priceRecommendationsProcessor).sync()
            );

    private static final TestData<EmptyArg> SYNC_FEE_RECOMMENDATIONS_PRIORITY =
            new TestData<>(JobType.SYNC_FEE_RECOMMENDATIONS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.feeRecommendationsProcessor).sync()
            );

    private static final TestData<EmptyArg> SYNC_NEW_PRICE_RECOMMENDATIONS_PRIORITY =
            new TestData<>(JobType.SYNC_NEW_PRICE_RECOMMENDATIONS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.newPriceRecommendationsProcessor).sync()
            );

    private static final TestData<EmptyArg> SYNC_NEW_FEE_RECOMMENDATIONS_PRIORITY =
            new TestData<>(JobType.SYNC_NEW_FEE_RECOMMENDATIONS_PRIORITY,
                    new EmptyArg(),
                    (t, d) -> verify(t.newFeeRecommendationsProcessor).sync()
            );

    private static final TestData<?>[] TEST_ARGS = {
            SHOP_LOOP_FULL,
            SHOP_LOOP_FULL_PRIORITY,
            SHOP_LOOP_FULL_LARGE,

            MODELBIDS_IMPORT,
            MODELBIDS_RECOMMENDATION,

            AUTOSTRATEGIES_WHITE_MAP,
            AUTOSTRATEGIES_BLUE_MAP,
            AUTOSTRATEGIES_VENDOR_BLUE_MAP,
            AUTOSTRATEGIES_VENDOR_PROCESSING,

            SYNC_BLUE_BIDS_RECOMMENDER_PRIORITY,
            SYNC_VENDOR_BLUE_DATASOURCES_PRIORITY,
            SYNC_VENDOR_BLUE_MAP_PRIORITY,

            SYNC_BUSINESS_PRIORITY,

            CLEANUP_OFFERS_PRIORITY,
            CLEANUP_OFFERS_BLUE_PRIORITY,

            DAILY_PROCESSING_PRIORITY,

            SYNC_CATEGORY_RECOMMENDATIONS_PRIORITY,

            PARTNER_PROGRAM_PRIORITY,

            PARTNER_BUSINESS_PRIORITY,

            SYNC_OFFER_RECOMMENDER_PRIORITY,

            SYNC_MBI_CONTACT_INFO_PRIORITY,

            SYNC_PRICE_RECOMMENDATIONS_PRIORITY,

            SYNC_FEE_RECOMMENDATIONS_PRIORITY,

            SYNC_NEW_PRICE_RECOMMENDATIONS_PRIORITY,

            SYNC_NEW_FEE_RECOMMENDATIONS_PRIORITY
    };

    //

    private TasksController tasksController;

    @Mock
    private TasksService tasksService;

    @Mock
    private CachedDataSource dataSource;


    @Mock
    private OffersProcessorRouter offersProcessorRouter;

    @Mock
    private ShopOffersProcessor shopOffersProcessor;

    @Mock
    private ModelbidsRecommendationSyncProcessor modelbidsRecommendationSyncProcessor;

    @Mock
    private ModelbidsRecommendationImportProcessor modelbidsRecommendationImportProcessor;

    @Mock
    private BlueBidsRecommenderProcessor blueBidsRecommenderProcessor;

    @Mock
    private VendorDatasourcesProcessor vendorDatasourcesProcessor;

    @Mock
    private VendorBrandMapProcessor vendorBrandMapProcessor;

    @Mock
    private BusinessProcessor businessProcessor;

    @Mock
    private CategoryRecommendationsProcessor categoryRecommendationsProcessor;

    @Mock
    private OffersCleanupProcessor offersCleanupProcessor;

    @Mock
    private OffersBlueCleanupProcessor offersBlueCleanupProcessor;

    @Mock
    private DailyProcessor dailyProcessor;

    @Mock
    private PartnerProgramProcessor partnerProgramProcessor;

    @Mock
    private PartnerBusinessProcessor partnerBusinessProcessor;

    @Mock
    private OffersRecommendationsProcessor offersRecommendationsProcessor;

    @Mock
    private MbiContactInfoProcessor mbiContactInfoProcessor;

    @Mock
    private PriceRecommendationsProcessor priceRecommendationsProcessor;

    @Mock
    private FeeRecommendationsProcessor feeRecommendationsProcessor;

    @Mock
    private NewPriceRecommendationsProcessor newPriceRecommendationsProcessor;

    @Mock
    private NewFeeRecommendationsProcessor newFeeRecommendationsProcessor;

    private List<? extends JobProcessing> processors;

    @BeforeEach
    void init() {
        this.processors = List.of(
                offersProcessorRouter,
                modelbidsRecommendationSyncProcessor,
                modelbidsRecommendationImportProcessor,
                blueBidsRecommenderProcessor,
                vendorDatasourcesProcessor,
                vendorBrandMapProcessor,
                businessProcessor,
                offersCleanupProcessor,
                offersBlueCleanupProcessor,
                dailyProcessor,
                categoryRecommendationsProcessor,
                shopOffersProcessor,
                partnerProgramProcessor,
                partnerBusinessProcessor,
                offersRecommendationsProcessor,
                mbiContactInfoProcessor,
                priceRecommendationsProcessor,
                feeRecommendationsProcessor,
                newPriceRecommendationsProcessor,
                newFeeRecommendationsProcessor
        );

        var router = new ProcessingRouter(processors);
        this.tasksController = TasksController.singleThreaded(tasksService, router, dataSource);
    }

    @AfterEach
    void done() {
        for (var processor : processors) {
            verifyNoMoreInteractions(processor);
        }
    }

    @Test
    void verifyTestArgs() {
        var allTypes = Stream.of(JobType.values()).collect(Collectors.toSet());
        for (TestData<?> arg : TEST_ARGS) {
            assertTrue(allTypes.remove(arg.type), "Test type " + arg.type + " must be unique");
        }
        assertEquals(Set.of(), allTypes);
    }

    @ParameterizedTest
    @MethodSource("testDataList")
    <T extends ToJsonString> void testExecution(TestData<T> data) {
        tasksController.processTask(data.asTask());
        data.checkSource.accept(this, data);
    }

    private static Object[] testDataList() {
        return TEST_ARGS;
    }

    public static class TestData<T extends ToJsonString> {
        private final JobType type;
        private final String body;
        private final T args;
        private final BiConsumer<ProcessingRouterTest, TestData<T>> checkSource;
        private final TaskInfo taskInfo;

        public TestData(@NonNull JobType type, @NonNull T args,
                        @NonNull BiConsumer<ProcessingRouterTest, TestData<T>> checkSource) {
            this.type = type;
            this.args = args;
            this.body = args.toJsonString();
            this.checkSource = checkSource;
            this.taskInfo = new TaskInfo(type, 1, 2, Instant.ofEpochMilli(NOW), TaskChildrenWriter.BYPASS);
        }

        public Task asTask() {
            var task = new Task();
            task.setStatus(TaskStatus.RUNNING);
            task.setType(type);
            task.setArgs(body);
            task.setJob_id(taskInfo.getJobId());
            task.setTask_id(taskInfo.getTaskId());
            task.setStarted_first(taskInfo.getStartedFirst());
            return task;
        }

        @Override
        public String toString() {
            return type + ": " + body;
        }
    }
}
