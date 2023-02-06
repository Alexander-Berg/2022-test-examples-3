package ru.yandex.market.mbo.statistic;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.dbcp2.BasicDataSource;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Strings;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.catalogue.category.CategoryManagersManager;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.model.CategoryStatistics;
import ru.yandex.market.mbo.statistic.model.CategoryWorkload;
import ru.yandex.market.mbo.statistic.model.OperatorStatistics;
import ru.yandex.market.mbo.statistic.model.RankAndCount;
import ru.yandex.market.mbo.statistic.model.StatisticsFilter;
import ru.yandex.market.mbo.statistic.model.TaskType;
import ru.yandex.market.mbo.statistic.model.Workload;
import ru.yandex.market.mbo.user.MboUser;
import ru.yandex.market.mbo.user.UserManager;
import ru.yandex.market.mbo.utils.BaseDbTest;
import ru.yandex.market.mbo.utils.MboAssertions;
import ru.yandex.market.mbo.utils.TestClock;
import ru.yandex.market.mbo.yt.TestYt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author kravchenko-aa
 * @date 13/03/2019
 */
@SuppressWarnings("checkstyle:magicNumber")
public class StatisticsServiceImplTest extends BaseDbTest {
    private static final Long HID_1 = 10859406L;
    private static final Long HID_2 = 278342L;
    private static final Long UID_1 = 299171538L;
    private static final Long UID_2 = 545803222L;
    private static final String TEST_DATA = "ru/yandex/market/mbo/statistics/dao/statistics_test_data.json";
    private static final int TEST_WORKLOAD = 10;

    @Autowired
    private NamedParameterJdbcTemplate postgresJdbcTemplate;
    @Autowired
    private TransactionTemplate postgresTransactionTemplate;

    private StatisticsServiceImpl statisticsServiceImpl;
    private NamedParameterJdbcTemplate contentTemplate;

    private YangLogStorageServiceImpl yangLogStorageService;
    private YangLogStoreRequestHelper requestHelper;
    private YangTaskInspectionService yangTaskInspectionService;

    @Before
    public void init() throws IOException, URISyntaxException {
        UserManager userManager = Mockito.mock(UserManager.class);
        doAnswer(invocation -> {
            long uid = invocation.getArgument(0);
            return new MboUser("login", uid, "fullname", "email", "staff");
        }).when(userManager).getUserInfo(anyLong());

        String dbName = getClass().getSimpleName() + UUID.randomUUID().toString() + "content";
        BasicDataSource contentDataSource = new BasicDataSource();
        contentDataSource.setDriverClassName("org.h2.Driver");
        contentDataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:ru/yandex/market/mbo/statistics/dao/market_content.sql'" +
                ";MODE=PostgreSQL"
        );
        contentTemplate = new NamedParameterJdbcTemplate(contentDataSource);

        UserWorkloadService userWorkloadService = Mockito.mock(UserWorkloadService.class);
        when(userWorkloadService.getCategoryWorkload(anyLong())).thenReturn(getCategoryWorkload());
        when(userWorkloadService.getUserWorkload(anyLong(), anyBoolean(), any()))
            .thenReturn(Workload.DEFAULT_WORKLOAD);

        statisticsServiceImpl = new StatisticsServiceImpl(postgresJdbcTemplate, contentTemplate, contentTemplate,
            userManager, userWorkloadService, Mockito.mock(CategoryManagersManager.class));
        statisticsServiceImpl.init();

        yangTaskInspectionService = new YangTaskInspectionService(
            new YangTaskInspectionDao(postgresJdbcTemplate), statisticsServiceImpl, postgresTransactionTemplate);

        ModelAuditStatisticsService modelAuditStatisticsService = Mockito.mock(ModelAuditStatisticsService.class);
        when(modelAuditStatisticsService.computeModelStatsTask(any())).then(
            invocation -> invocation.getArgument(0));


        YangWorkingTimeDao yangWorkingTimeDao = new YangWorkingTimeDao(postgresJdbcTemplate);
        YtStatisticsService ytStatisticsService = new YtStatisticsServiceImpl(new TestYt(),
                YPath.simple("//home/thorinhood/yang/statistic"), yangWorkingTimeDao);
        ytStatisticsService.deferredInit();

        YangStatToYtQueue yangStatToYtQueue = new YangStatToYtQueue(postgresJdbcTemplate, postgresTransactionTemplate);
        YangLogStorageServiceImpl storageService = new YangLogStorageServiceImpl(
            postgresJdbcTemplate,
            postgresTransactionTemplate,
            Mockito.mock(NamedParameterJdbcTemplate.class),
            Mockito.mock(TarifManager.class),
            null,
            modelAuditStatisticsService,
            yangTaskInspectionService,
            ytStatisticsService,
            yangWorkingTimeDao,
            yangStatToYtQueue);
        ReflectionTestUtils.setField(storageService, "clock", new TestClock());

        yangLogStorageService = spy(storageService);
        // Для тестов сойдет и биллинг с фронта
        doAnswer(invocation -> {
            YangLogStorage.YangLogStoreRequest request = invocation.getArgument(0);
            YangLogStorageServiceImpl.TaskRank taskRank = new YangLogStorageServiceImpl.TaskRank();

            taskRank.setOperatorChangesBilling(request.getContractorInfo().getBillingTotal());
            taskRank.setSuperOperatorCorrectionsBilling(request.getInspectorInfo().getBillingTotal());

            return taskRank;
        }).when(yangLogStorageService).getTaskRank(any(), any(), any());

        requestHelper = new YangLogStoreRequestHelper();
    }

    private void loadJsonData() {
        List<String> jsons;
        try {
            jsons = Files.readAllLines(Paths.get(
                StatisticsServiceImplTest.class.getClassLoader().getResource(TEST_DATA).toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }

        jsons.forEach(json -> {
            YangLogStorage.YangLogStoreRequest.Builder builder = YangLogStorage.YangLogStoreRequest.newBuilder();
            try {
                JsonFormat.parser().merge(json, builder);
                yangLogStorageService.yangLogStore(builder.build());
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
                fail("Can't prepared test data: " + e.getMessage());
            }
        });
    }

    @Test
    public void allStatisticsCountTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 8);
    }

    @Test
    public void operatorToCategoryStatisticsCountTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        filter.setGroupByCategory(true);
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 3);
    }

    @Test
    public void managerToCategoryStatisticsCountTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        filter.setGroupByOperator(true);
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 7);
    }

    @Test
    public void globalStatisticsCountTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        filter.setGroupByOverall(true);
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 1);
    }

    @Test
    public void categoryStatisticsTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        filter.setCategoryIds(Collections.singleton(HID_1));
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 1);
        OperatorStatistics statistics = operatorStatistics.get(0);
        CategoryStatistics categoryStatistics = statistics.getCategoryStatistics();
        assertFalse(Strings.isNullOrEmpty(categoryStatistics.getCategoryName()));
        assertEquals(categoryStatistics.getCategoryRank(), 96.58, 0.01);
        assertEquals(categoryStatistics.getCategoryCompleteness(), 95.06, 0.01);
        assertEquals(categoryStatistics.getCreatedMappingCount(), 49.0, 0.01);
        assertEquals(categoryStatistics.getCreatedModelCount(), 5.0, 0.01);
        assertEquals(categoryStatistics.getCreatedSkuCount(), 42.0, 0.01);
        assertEquals(categoryStatistics.getTrashMappingsCount(), 4.0, 0.01);
        assertEquals(categoryStatistics.getProcessingOffersCount(), 53.0, 0.01);
    }

    @Test
    public void filterBySeveralCategoriesTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        filter.setCategoryIds(ImmutableSet.of(HID_1, HID_2));
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 3);
    }

    @Test
    public void filterBySeveralOperatorsTest() {
        loadJsonData();
        StatisticsFilter filter = getDefaultFilter();
        filter.setOperatorIds(ImmutableSet.of(UID_1, UID_2));
        List<OperatorStatistics> operatorStatistics =
            statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();
        assertEquals(operatorStatistics.size(), 4);
    }

    private YangLogStorage.ActionInfo createActionInfo(long entityId) {
        return YangLogStorage.ActionInfo.newBuilder()
                .setEntityId(entityId)
                .build();
    }

    @Test
    public void testNotInspectedStatIsNotUsedInRankCalculations() {
        YangLogStorage.ActionInfo blankActionInfo = YangLogStorage.ActionInfo.newBuilder().build();

        YangLogStorage.YangLogStoreRequest.Builder inspected = requestHelper.newRequest()
            .setContractorInfo(requestHelper.newOperatorInfo(UID_1, 2.0))
            .setInspectorInfo(requestHelper.newOperatorInfo(UID_2, 3.0))
            .addModelStatistic(requestHelper.newModelStatistic()
                .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                        .addAllAliases(Arrays.asList(blankActionInfo, blankActionInfo))
                        .addAllParam(Arrays.asList(createActionInfo(100L), createActionInfo(200L),
                                createActionInfo(300L)))
                        .build())
                .setInspectorActions(YangLogStorage.ModelActions.newBuilder()
                        .addAllParam(Collections.singletonList(createActionInfo(100L)))
                        .build())
                .setCorrectionsActions(YangLogStorage.ModelActions.newBuilder()
                        .addAllAliases(Collections.singletonList(blankActionInfo))
                        .addAllParam(Collections.singletonList(createActionInfo(100L)))
                        .build()))
            .addModelStatistic(requestHelper.newModelStatistic()
                .setType(ModelStorage.ModelType.GURU)
                .setContractorActions(YangLogStorage.ModelActions.newBuilder().build())
                .setInspectorActions(YangLogStorage.ModelActions.newBuilder().build())
                .setCorrectionsActions(YangLogStorage.ModelActions.newBuilder().build()))
            .addMappingStatistic(requestHelper.newMappingStatistic()
                    .setUid(UID_1));

        yangLogStorageService.yangLogStore(inspected.build());

        YangLogStorage.YangLogStoreRequest.Builder notInspected = requestHelper.newRequest()
            .setContractorInfo(requestHelper.newOperatorInfo(UID_1, 5.0)) // Shouldn't be taken into account
            .addModelStatistic(requestHelper.newModelStatistic()
                .setContractorActions(YangLogStorage.ModelActions.newBuilder()
                    .addAllAliases(Arrays.asList(blankActionInfo, blankActionInfo))
                    .addAllParam(Arrays.asList(createActionInfo(100L), createActionInfo(200L),
                            createActionInfo(300L)))
                    .build()))
            .addModelStatistic(requestHelper.newModelStatistic()
                .setType(ModelStorage.ModelType.GURU)
                .setContractorActions(YangLogStorage.ModelActions.newBuilder().build()))
            .addMappingStatistic(requestHelper.newMappingStatistic()
                .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH));

        yangLogStorageService.yangLogStore(notInspected.build());

        List<OperatorStatistics> statistics = statisticsServiceImpl.getStatisticsWithAggregate(
            new StatisticsFilter().setTaskType(TaskType.BLUE_LOGS)).getOperatorStatisticsList();
        Assertions.assertThat(statistics).hasSize(5);
        // Single entry so all have same data
        statistics.forEach(stat ->
            MboAssertions.assertSoftly(a -> {
                a.assertThat(stat.getGlobalRank()).isEqualTo(40.0);

                CategoryStatistics categoryStatistics = stat.getCategoryStatistics();
                a.assertThat(categoryStatistics.getCategoryCompleteness()).isEqualTo(80.0);  // (3 + 1) / (3 + 1 + 1)
                a.assertThat(categoryStatistics.getCategoryRank()).isEqualTo(40.0); // 2 / (2 + 3)

                // Here we do calculate not-inspected
                a.assertThat(categoryStatistics.getCreatedModelCount()).isEqualTo(2);
                a.assertThat(categoryStatistics.getCreatedSkuCount()).isEqualTo(2);
                a.assertThat(categoryStatistics.getCreatedMappingCount()).isEqualTo(1);
                a.assertThat(categoryStatistics.getProcessingOffersCount()).isEqualTo(2);
                a.assertThat(categoryStatistics.getTrashMappingsCount()).isEqualTo(1);
            }));
    }

    @Test
    public void testNotInspectedOnlyStatDontFail() {
        YangLogStorage.YangLogStoreRequest.Builder notInspected = requestHelper.newRequest()
            .setContractorInfo(requestHelper.newOperatorInfo(UID_1, 5.0)) // Shouldn't be taken into account
            .addModelStatistic(requestHelper.newModelStatistic()
                .setContractorChanges(requestHelper.newActionCount()
                    .setAliases(2)
                    .setParam(3)
                    .addAllParamIds(Arrays.asList(100L, 200L, 300L))))
            .addModelStatistic(requestHelper.newModelStatistic()
                .setType(ModelStorage.ModelType.GURU)
                .setContractorChanges(requestHelper.newActionCount()))
            .addMappingStatistic(requestHelper.newMappingStatistic()
                .setOfferMappingStatus(YangLogStorage.MappingStatus.TRASH)
                .setUid(UID_1));

        yangLogStorageService.yangLogStore(notInspected.build());

        List<OperatorStatistics> statistics = statisticsServiceImpl.getStatisticsWithAggregate(
            new StatisticsFilter().setTaskType(TaskType.BLUE_LOGS)).getOperatorStatisticsList();
        Assertions.assertThat(statistics).hasSize(5);
        // Single entry so all have same data
        statistics.forEach(stat ->
            MboAssertions.assertSoftly(a -> {
                a.assertThat(stat.getGlobalRank()).isEqualTo(0);

                CategoryStatistics categoryStatistics = stat.getCategoryStatistics();
                a.assertThat(categoryStatistics.getCategoryCompleteness()).isEqualTo(0);
                a.assertThat(categoryStatistics.getCategoryRank()).isEqualTo(0);

                // Here we do calculate not-inspected
                a.assertThat(categoryStatistics.getCreatedModelCount()).isEqualTo(1);
                a.assertThat(categoryStatistics.getCreatedSkuCount()).isEqualTo(1);
                a.assertThat(categoryStatistics.getCreatedMappingCount()).isEqualTo(0);
                a.assertThat(categoryStatistics.getProcessingOffersCount()).isEqualTo(1);
                a.assertThat(categoryStatistics.getTrashMappingsCount()).isEqualTo(1);
            }));
    }

    @Test
    public void testCategoryWorkloadNotDuplicate() {
        loadJsonData();

        int categoryCount = postgresJdbcTemplate.queryForList(
            "select distinct category_id from " + YangLogStorageServiceImpl.LOG_STATISTICS_TABLE,
            Collections.emptyMap(), Long.class).size();

        StatisticsFilter filter = getDefaultFilter();
        filter.setGroupByOverall(true);
        List<OperatorStatistics> statistics = statisticsServiceImpl.getStatistics(filter).getOperatorStatisticsList();

        assertEquals(statistics.size(), 1);
        CategoryWorkload averageWorkload = statistics.get(0).getCategoryWorkload();
        assertEquals(averageWorkload.getOffersCountFromAliasMaker(), categoryCount * TEST_WORKLOAD, 0.01);
        assertEquals(averageWorkload.getOffersCountFromMarkupWorker(), categoryCount * TEST_WORKLOAD, 0.01);
    }

    @Test
    public void testSingleOperatorRating() {
        RankAndCount rank0 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 10,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank0.getRank()).isEqualTo(0.0);

        for (int i = 0; i <= 20; i++) {
            YangLogStorage.YangLogStoreRequest.Builder inspected = requestHelper.newRequest()
                .setContractorInfo(requestHelper.newOperatorInfo(UID_1, i))
                .setInspectorInfo(requestHelper.newOperatorInfo(UID_2, 100 - i));

            YangLogStorage.YangLogStoreRequest.Builder notInspected = requestHelper.newRequest()
                .setContractorInfo(requestHelper.newOperatorInfo(UID_1, 5.0)); // Shouldn't be taken into account

            yangLogStorageService.yangLogStore(inspected.build());
            yangLogStorageService.yangLogStore(notInspected.build());
        }

        RankAndCount rank1 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 1,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank1.getRank()).isEqualTo(20.0);

        RankAndCount rank10 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 10,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank10.getRank()).isEqualTo((20.0 + 11.0) / 2);

        RankAndCount rank100 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 100,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank100.getRank()).isEqualTo(10.0);
    }

    @Test
    public void testTaskTypesLogsClusterRating() {
        RankAndCount rank0 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 10,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank0.getRank()).isEqualTo(0.0);

        for (int i = 0; i <= 20; i++) {
            YangLogStorage.YangLogStoreRequest.Builder blueLogs = requestHelper.newRequest()
                .setContractorInfo(requestHelper.newOperatorInfo(UID_1, i))
                .setInspectorInfo(requestHelper.newOperatorInfo(UID_2, 100 - i))
                .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS);

            YangLogStorage.YangLogStoreRequest.Builder mskuFromPskuGen = requestHelper.newRequest()
                .setContractorInfo(requestHelper.newOperatorInfo(UID_1, 100 - i))
                .setInspectorInfo(requestHelper.newOperatorInfo(UID_2, i))
                .setTaskType(YangLogStorage.YangTaskType.MSKU_FROM_PSKU_GENERATION);

            yangLogStorageService.yangLogStore(blueLogs.build());
            yangLogStorageService.yangLogStore(mskuFromPskuGen.build());
        }

        RankAndCount rank1 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 1,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank1.getRank()).isEqualTo(80.0);

        RankAndCount rank10 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 10,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank10.getRank()).isEqualTo(50.0);

        RankAndCount rank100 = statisticsServiceImpl.getContractorInspectedRank(UID_1, HID_2, 100,
            TaskType.BLUE_LOGS);
        Assertions.assertThat(rank100.getRank()).isEqualTo(50.0);
    }

    private StatisticsFilter getDefaultFilter() {
        StatisticsFilter filter = new StatisticsFilter();
        filter.setTaskType(TaskType.BLUE_LOGS);
        filter.setGroupByCategory(false);
        filter.setGroupByOperator(false);
        filter.setGroupByOverall(false);
        return filter;
    }

    private CategoryWorkload getCategoryWorkload() {
        CategoryWorkload cw = new CategoryWorkload();
        cw.setOffersCountFromMarkupWorker(TEST_WORKLOAD);
        cw.setOffersCountFromAliasMaker(TEST_WORKLOAD);
        return cw;
    }
}
