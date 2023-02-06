package ru.yandex.market.mbo.statistic;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.db.JdbcFactory;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.model.YangWorkingTime;
import ru.yandex.market.mbo.utils.BaseDbTest;
import ru.yandex.market.mbo.yt.TestYtWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author yuramalinov
 * @created 17.04.19
 */
@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(Parameterized.class)
public class YangLogStorageServiceImplTest extends BaseDbTest {
    private static final long CONTRACTOR_ID = 2000L;
    private static final long INSPECTOR_ID = 3000L;
    public static final long CATEGORY_ID = 100L;
    private static final long TOTAL_TIME_SEC = 1L;
    private static final int QUEUE_READ_BATCH_SIZE = 1000;
    private static final String DEFAULT_ASSIGNMENT_ID = "assignment_id";
    private static final String DEFUALT_INSPECTOR_ASSIGNMENT_ID = "inspector_assignment_id";

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Resource(name  = "postgresTransactionTemplate")
    TransactionTemplate transactionTemplate;

    private YangLogStorageServiceImpl yangLogStorageService;
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();
    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Parameter
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public YangLogStorage.YangTaskType taskType;
    private NamedParameterJdbcTemplate scatTemplate;
    private BasicDataSource siteCatalogOracleDataSource;
    private TestYtWrapper yt;
    private YPath logsPath = YPath.simple("//home/thorinhood/yang/statistics");
    private YangWorkingTimeDao yangWorkingTimeDao;
    private YangStatToYtQueue yangStatToYtQueue;

    @Parameters
    public static Object[] data() {
        return new Object[]{
            YangLogStorage.YangTaskType.BLUE_LOGS,
            YangLogStorage.YangTaskType.WHITE_LOGS,
            YangLogStorage.YangTaskType.DEEPMATCHER_LOGS
        };
    }

    @Before
    public void setup() {
        ModelAuditStatisticsService modelAuditStatisticsService = Mockito.mock(ModelAuditStatisticsService.class);
        when(modelAuditStatisticsService.computeModelStatsTask(any())).then(
            invocation -> invocation.getArgument(0));
        siteCatalogOracleDataSource = JdbcFactory.createH2DataSource(JdbcFactory.Mode.ORACLE,
            "classpath:site_catalog/yang_task_info.sql");
        scatTemplate = new NamedParameterJdbcTemplate(siteCatalogOracleDataSource);
        yt = new TestYtWrapper();

        yangWorkingTimeDao = new YangWorkingTimeDao(jdbcTemplate);
        YtStatisticsService ytStatisticsService = new YtStatisticsServiceImpl(yt, logsPath, yangWorkingTimeDao);
        ytStatisticsService.deferredInit();
        yangStatToYtQueue = new YangStatToYtQueue(jdbcTemplate, transactionTemplate);
        yangLogStorageService = Mockito.spy(new YangLogStorageServiceImpl(jdbcTemplate, transactionTemplate,
                scatTemplate,
                Mockito.mock(TarifManager.class), new CategoryMappingServiceMock(),
                modelAuditStatisticsService, Mockito.mock(YangTaskInspectionService.class),
                ytStatisticsService, yangWorkingTimeDao, yangStatToYtQueue));
    }

    @After
    public void shutdown() throws SQLException {
        siteCatalogOracleDataSource.close();
    }

    @Test
    public void testWorkingTimePingInit() {
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(100L));
        YangWorkingTime yangWorkingTime = yangWorkingTimeDao.getYangWorkingTime(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime).isNull();
        YangLogStorage.YangWorkingTimePingResponse response = yangLogStorageService
                .yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 1000L, false));
        assertThat(response.getSuccess()).isTrue();
        yangWorkingTime = yangWorkingTimeDao.getYangWorkingTime(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime).isNotNull();
        assertThat(yangWorkingTime.getAssignmentId()).isEqualTo(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime.getTotalTime()).isEqualTo(0L);
        assertThat(yangWorkingTime.isFinished()).isFalse();
        assertThat(yangWorkingTime.getLastPing()).isEqualTo(new Timestamp(100L));
    }

    @Test
    public void testWorkingTimePingTotalTime() {
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(0L));
        yangLogStorageService.yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 60000L, false));
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(1000L));
        YangLogStorage.YangWorkingTimePingResponse response = yangLogStorageService
                .yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 60000L, true));
        assertThat(response.getSuccess()).isTrue();
        YangWorkingTime yangWorkingTime = yangWorkingTimeDao.getYangWorkingTime(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime).isNotNull();
        assertThat(yangWorkingTime.getAssignmentId()).isEqualTo(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime.getTotalTime()).isEqualTo(1L);
        assertThat(yangWorkingTime.isFinished()).isTrue();
        assertThat(yangWorkingTime.getLastPing()).isEqualTo(new Timestamp(1000L));
    }

    @Test
    public void testWorkingTimePingLate() {
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(0L));
        yangLogStorageService.yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 1L, false));
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(2550L));
        YangLogStorage.YangWorkingTimePingResponse response = yangLogStorageService
                .yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 1L, true));
        assertThat(response.getSuccess()).isTrue();
        YangWorkingTime yangWorkingTime = yangWorkingTimeDao.getYangWorkingTime(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime).isNotNull();
        assertThat(yangWorkingTime.getAssignmentId()).isEqualTo(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime.getTotalTime()).isEqualTo(0L);
        assertThat(yangWorkingTime.isFinished()).isTrue();
        assertThat(yangWorkingTime.getLastPing()).isEqualTo(new Timestamp(2550L));
    }

    @Test
    public void testWorkingTimeNotUpdateAfterFinalPing() {
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(0L));
        yangLogStorageService.yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 10000L, false));
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(1000L));
        yangLogStorageService.yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 10000L, true));
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(2000L));
        YangLogStorage.YangWorkingTimePingResponse response =
                yangLogStorageService.yangWorkingTimePing(pingRequest(DEFAULT_ASSIGNMENT_ID, 10000L, true));
        assertThat(response.getSuccess()).isTrue();
        YangWorkingTime yangWorkingTime = yangWorkingTimeDao.getYangWorkingTime(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime).isNotNull();
        assertThat(yangWorkingTime.getAssignmentId()).isEqualTo(DEFAULT_ASSIGNMENT_ID);
        assertThat(yangWorkingTime.getTotalTime()).isEqualTo(1L);
        assertThat(yangWorkingTime.isFinished()).isTrue();
        assertThat(yangWorkingTime.getLastPing()).isEqualTo(new Timestamp(1000L));
    }

    @Test
    public void testStoreWorksFine() {
        addDefaultWorkingTime(DEFAULT_ASSIGNMENT_ID, TOTAL_TIME_SEC * 1000, TOTAL_TIME_SEC * 5);
        addDefaultWorkingTime(DEFUALT_INSPECTOR_ASSIGNMENT_ID, TOTAL_TIME_SEC * 1000, TOTAL_TIME_SEC * 5);
        yangLogStorageService.yangLogStore(defaultRequest().build());

        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.TABLE + " order by id", Collections.emptyMap());
        assertThat(result).hasSize(1);
        Map<String, Object> row = result.get(0);
        assertThat(row.get("id")).isEqualTo("id");
        assertThat(row.get("contractor_uid")).isEqualTo(CONTRACTOR_ID);
        assertThat(row.get("inspector_uid")).isEqualTo(INSPECTOR_ID);
        assertThat(row.get("task_type")).isEqualTo(taskType.toString());

        List<Map<String, Object>> oracleData = scatTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.ORACLE_YANG_TASK_INFO + " order by id",
            Collections.emptyMap());
        verifyOracleData(result, oracleData);

        List<Map<String, Object>> stat = jdbcTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.LOG_STATISTICS_TABLE, Collections.emptyMap());
        assertThat(stat).hasSize(1);
        Map<String, Object> statRow = stat.get(0);
        assertThat(statRow).contains(
            entry("category_id", CATEGORY_ID),
            entry("contractor_uid", CONTRACTOR_ID),
            entry("inspected", true),
            entry("id", "id"),
            entry("task_type", taskType.toString())
        );

        assertOneRowInQueue("id");
        assertThat(yt.tables().read(logsPath, YTableEntryTypes.YSON)).toIterable().hasSize(0);
    }

    @Test
    public void testSubsequentStoreWorksFine() {
        YangLogStorage.YangLogStoreResponse result =
            yangLogStorageService.yangLogStore(defaultRequest().build());

        assertThat(result.getSuccess()).isTrue();

        YangLogStorage.YangLogStoreResponse subsequentResult =
            yangLogStorageService.yangLogStore(defaultRequest().build());

        assertThat(subsequentResult.getSuccess()).isTrue();

        List<Map<String, Object>> pg = jdbcTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.TABLE + " order by id", Collections.emptyMap());
        assertThat(pg).hasSize(1);

        List<Map<String, Object>> oracleData = scatTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.ORACLE_YANG_TASK_INFO + " order by id",
            Collections.emptyMap());
        verifyOracleData(pg, oracleData);

        List<String> idsFromQueue = yangStatToYtQueue.getIdsFromQueue(QUEUE_READ_BATCH_SIZE);

        assertThat(idsFromQueue).hasSize(1);

        assertThat(yt.tables().read(logsPath, YTableEntryTypes.YSON)).toIterable().hasSize(0);
    }

    private void addDefaultWorkingTime(String assignmentId, Long duration, Long pingInterval) {
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(0L));
        yangLogStorageService.yangWorkingTimePing(pingRequest(assignmentId, pingInterval, false));
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(duration));
        yangLogStorageService.yangWorkingTimePing(pingRequest(assignmentId, pingInterval, true));
    }

    @Test
    public void testNoInspectorStoreWorksFine() {
        addDefaultWorkingTime(DEFAULT_ASSIGNMENT_ID, TOTAL_TIME_SEC * 1000, TOTAL_TIME_SEC * 5);
        yangLogStorageService.yangLogStore(YangLogStorage.YangLogStoreRequest.newBuilder()
            .setId("id")
            .setHitmanId(200L)
            .setTaskType(taskType)
            .setCategoryId(CATEGORY_ID)
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setAssignmentId(DEFAULT_ASSIGNMENT_ID)
                .setBillingTotal(10)
                .setPoolId("pool_id")
                .setTaskId("task_id")
                .setUid(CONTRACTOR_ID)
                .build())
            .build());

        List<Map<String, Object>> result = jdbcTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.TABLE + " order by id", Collections.emptyMap());
        assertThat(result).hasSize(1);
        Map<String, Object> row = result.get(0);
        assertThat(row.get("id")).isEqualTo("id");
        assertThat(row.get("contractor_uid")).isEqualTo(CONTRACTOR_ID);
        assertThat(row.get("inspector_uid")).isNull();
        assertThat(row.get("task_type")).isEqualTo(taskType.toString());

        List<Map<String, Object>> oracleData = scatTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.ORACLE_YANG_TASK_INFO + " order by id",
            Collections.emptyMap());
        verifyOracleData(result, oracleData);

        List<Map<String, Object>> stat = jdbcTemplate.queryForList(
            "select * from " + YangLogStorageServiceImpl.LOG_STATISTICS_TABLE, Collections.emptyMap());
        assertThat(stat).hasSize(1);
        Map<String, Object> statRow = stat.get(0);
        assertThat(statRow).contains(
            entry("category_id", CATEGORY_ID),
            entry("contractor_uid", CONTRACTOR_ID),
            entry("inspected", false),
            entry("id", "id"),
            entry("task_type", taskType.toString())
        );
        assertOneRowInQueue("id");
        assertThat(yt.tables().read(logsPath, YTableEntryTypes.YSON)).toIterable().hasSize(0);
    }

    private void assertOneRowInQueue(String id) {
        List<String> idsFromQueue = yangStatToYtQueue.getIdsFromQueue(QUEUE_READ_BATCH_SIZE);
        assertThat(idsFromQueue).containsExactly(id);
    }

    private YangLogStorage.YangWorkingTimePingRequest pingRequest(String assignmentId, Long pingInterval,
                                                                  boolean isFinal) {
        return YangLogStorage.YangWorkingTimePingRequest.newBuilder()
                .setAssignmentId(assignmentId)
                .setPingIntervalSec(pingInterval)
                .setFinal(isFinal)
                .build();
    }

    private YangLogStorage.YangLogStoreRequest.Builder defaultRequest() {
        return YangLogStorage.YangLogStoreRequest.newBuilder()
            .setId("id")
            .setHitmanId(200L)
            .setTaskType(taskType)
            .setCategoryId(CATEGORY_ID)
            .setContractorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setAssignmentId(DEFAULT_ASSIGNMENT_ID)
                .setBillingTotal(10)
                .setPoolId("pool_id")
                .setTaskId("task_id")
                .setPoolName("Some contractor pool")
                .setUid(CONTRACTOR_ID)
                .build())
            .setInspectorInfo(YangLogStorage.OperatorInfo.newBuilder()
                .setAssignmentId(DEFUALT_INSPECTOR_ASSIGNMENT_ID)
                .setBillingTotal(10)
                .setPoolId("pool_id")
                .setTaskId("task_id")
                .setPoolName("Some inspector pool")
                .setUid(INSPECTOR_ID)
                .build());
    }

    private void verifyOracleData(List<Map<String, Object>> result, List<Map<String, Object>> oracleData) {
        assertThat(oracleData).hasSize(result.size());
        for (int i = 0; i < result.size(); i++) {
            Map<String, Object> taskRow = result.get(i);
            Map<String, Object> oracleRow = oracleData.get(i);
            oracleRow.forEach((key, oracleValue) -> {
                Object pgValue = taskRow.get(key);
                // Hack for oracle absense of bigint and default interpretation of number(19) -> BigDecimal
                if (pgValue instanceof Long && oracleValue instanceof Number) {
                    oracleValue = ((Number) oracleValue).longValue();
                }
                if ("".equals(pgValue) && oracleValue == null) {
                    return; // It's Oracle, only wimps store empty strings, big guys force it to nulls
                }
                assertThat(oracleValue)
                    .describedAs("Key %s for pg row %s, oracle row %s", key, taskRow, oracleRow)
                    .isEqualTo(pgValue);
            });
        }
    }

}
