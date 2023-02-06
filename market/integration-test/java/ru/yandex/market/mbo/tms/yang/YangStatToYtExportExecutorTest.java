package ru.yandex.market.mbo.tms.yang;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Resource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.market.mbo.billing.tarif.TarifManager;
import ru.yandex.market.mbo.category.mappings.CategoryMappingServiceMock;
import ru.yandex.market.mbo.db.JdbcFactory;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.statistic.ModelAuditStatisticsService;
import ru.yandex.market.mbo.statistic.YangLogStorageServiceImpl;
import ru.yandex.market.mbo.statistic.YangStatToYtQueue;
import ru.yandex.market.mbo.statistic.YangTaskInspectionService;
import ru.yandex.market.mbo.statistic.YangWorkingTimeDao;
import ru.yandex.market.mbo.statistic.YtStatisticsService;
import ru.yandex.market.mbo.statistic.YtStatisticsServiceImpl;
import ru.yandex.market.mbo.utils.BaseDbTest;
import ru.yandex.market.mbo.yt.TestYtWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class YangStatToYtExportExecutorTest extends BaseDbTest {
    private static final long CONTRACTOR_ID = 2000L;
    private static final long INSPECTOR_ID = 3000L;
    public static final long CATEGORY_ID = 100L;
    private static final long TOTAL_TIME_SEC = 1L;
    private static final int QUEUE_READ_BATCH_SIZE = 1000;
    private static final String DEFAULT_ASSIGNMENT_ID = "assignment_id";
    private static final String DEFUALT_INSPECTOR_ASSIGNMENT_ID = "inspector_assignment_id";

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Resource(name = "postgresTransactionTemplate")
    TransactionTemplate transactionTemplate;

    private YangLogStorageServiceImpl yangLogStorageService;
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();
    @Rule
    public final SpringMethodRule smr = new SpringMethodRule();

    @Parameterized.Parameter
    @SuppressWarnings("checkstyle:VisibilityModifier")
    public YangLogStorage.YangTaskType taskType;
    private NamedParameterJdbcTemplate scatTemplate;
    private BasicDataSource siteCatalogOracleDataSource;
    private TestYtWrapper yt;
    private YPath logsPath = YPath.simple("//home/thorinhood/yang/statistics");
    private YangWorkingTimeDao yangWorkingTimeDao;
    private YangStatToYtQueue yangStatToYtQueue;
    private YangStatToYtExportExecutor executor;

    @Parameterized.Parameters
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
        TarifManager tarifManager = Mockito.mock(TarifManager.class);
        yangLogStorageService = Mockito.spy(new YangLogStorageServiceImpl(jdbcTemplate, transactionTemplate,
            scatTemplate,
            tarifManager, new CategoryMappingServiceMock(),
            modelAuditStatisticsService, Mockito.mock(YangTaskInspectionService.class),
            ytStatisticsService, yangWorkingTimeDao, yangStatToYtQueue));

        YangLogStorageServiceImpl yangLogStorageServiceNotWriting =
            new YangLogStorageServiceImpl(jdbcTemplate, transactionTemplate,
                scatTemplate,
                tarifManager, new CategoryMappingServiceMock(),
                modelAuditStatisticsService, Mockito.mock(YangTaskInspectionService.class),
                ytStatisticsService, yangWorkingTimeDao, yangStatToYtQueue);
        yangLogStorageServiceNotWriting.setWriteToPg(false);


        executor = new YangStatToYtExportExecutor(jdbcTemplate,
            yangLogStorageServiceNotWriting, yt, tarifManager, yangStatToYtQueue, logsPath);
    }

    @After
    public void shutdown() throws SQLException {
        siteCatalogOracleDataSource.close();
    }

    @Test
    public void testStoreWorksFine() throws Exception {
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

        executor.doRealJob(null);
        assertOneRowInYt(
                entry("category_id", YTree.longNode(CATEGORY_ID)),
                entry("contractor_uid", YTree.integerNode(CONTRACTOR_ID)),
                entry("inspected", YTree.booleanNode(true)),
                entry("id", YTree.stringNode("id")),
                entry("task_type", YTree.stringNode(taskType.toString())),
                entry("total_time", YTree.longNode(TOTAL_TIME_SEC)),
                entry("total_time_pp", YTree.longNode(TOTAL_TIME_SEC))
        );
        assertThat(yangStatToYtQueue.getIdsFromQueue(QUEUE_READ_BATCH_SIZE)).isEmpty();
    }

    @Test
    public void testSubsequentStoreWorksFine() throws Exception {
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

        executor.doRealJob(null);
        assertThat(yt.tables().read(logsPath, YTableEntryTypes.YSON)).toIterable().hasSize(1);
        assertThat(yangStatToYtQueue.getIdsFromQueue(QUEUE_READ_BATCH_SIZE)).isEmpty();
    }

    private void addDefaultWorkingTime(String assignmentId, Long duration, Long pingInterval) {
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(0L));
        yangLogStorageService.yangWorkingTimePing(pingRequest(assignmentId, pingInterval, false));
        when(yangLogStorageService.currentTimestamp()).thenReturn(new Timestamp(duration));
        yangLogStorageService.yangWorkingTimePing(pingRequest(assignmentId, pingInterval, true));
    }

    @Test
    public void testNoInspectorStoreWorksFine() throws Exception {
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

        executor.doRealJob(null);
        assertOneRowInYt(
                entry("category_id", YTree.longNode(CATEGORY_ID)),
                entry("contractor_uid", YTree.integerNode(CONTRACTOR_ID)),
                entry("inspected", YTree.booleanNode(false)),
                entry("id", YTree.stringNode("id")),
                entry("task_type", YTree.stringNode(taskType.toString())),
                entry("total_time", YTree.longNode(TOTAL_TIME_SEC))
        );
        assertThat(yangStatToYtQueue.getIdsFromQueue(QUEUE_READ_BATCH_SIZE)).isEmpty();
    }

    private void assertOneRowInYt(Map.Entry<String, YTreeNode>... entries) {
        Iterator<YTreeMapNode> ytRows = yt.tables().read(logsPath, YTableEntryTypes.YSON);
        List<YTreeMapNode> list = StreamSupport
            .stream(((Iterable<YTreeMapNode>) () -> ytRows).spliterator(), false)
            .collect(Collectors.toList());
        assertThat(list).hasSize(1);
        assertThat(list.get(0)).contains(entries);
    }

    private void assertOneRowInQueue(String id) {
        List<String> idsFromQueue = yangStatToYtQueue.getIdsFromQueue(QUEUE_READ_BATCH_SIZE);
        assertThat(idsFromQueue).hasSize(1);
        assertThat(idsFromQueue).contains(id);
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
