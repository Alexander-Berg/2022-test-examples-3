package ru.yandex.market.deepmind.tms.executors;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.application.monitoring.MonitoringStatus;
import ru.yandex.market.db.monitoring.DbMonitoring;
import ru.yandex.market.deepmind.common.DeepmindBaseDbTestClass;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.enums.MskuStatusValue;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.MskuStatus;
import ru.yandex.market.deepmind.common.db.jooq.generated.msku.tables.pojos.Season;
import ru.yandex.market.deepmind.common.mocks.ModifyRowsRequestMock;
import ru.yandex.market.deepmind.common.repository.MskuRepository;
import ru.yandex.market.deepmind.common.repository.msku.status.MskuStatusRepository;
import ru.yandex.market.deepmind.common.repository.season.SeasonRepository;
import ru.yandex.market.deepmind.common.utils.TestUtils;
import ru.yandex.market.mbo.taskqueue.TaskQueueHandlerRegistry;
import ru.yandex.market.mbo.taskqueue.TaskQueueRepository;
import ru.yandex.market.mbo.taskqueue.UnsafeTaskQueueExecutor;
import ru.yandex.market.yt.util.table.YtTableRpcApi;

import static org.assertj.core.api.Assertions.assertThat;

public class UploadMskuStatusForLifecycleToYtExecutorTest extends DeepmindBaseDbTestClass {

    @Resource
    private MskuStatusRepository mskuStatusRepository;
    @Resource
    private MskuRepository deepmindMskuRepository;
    @Resource
    private SeasonRepository seasonRepository;
    @Resource
    private DbMonitoring deepmindDbMonitoring;
    @Resource
    private JdbcTemplate jdbcTemplate;
    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource(name = "availabilitiesTaskQueueObjectMapper")
    protected ObjectMapper objectMapper;
    @Resource(name = "availabilitiesTaskQueueHandlerRegistry")
    protected TaskQueueHandlerRegistry taskQueueHandlerRegistry;
    @Resource(name = "availabilitiesTaskQueueRepository")
    protected TaskQueueRepository taskQueueRepository;

    private ModifyRowsRequestMock modifyRowsRequestMock;
    private ModifyRowsRequestMock modifyRowsRequestMockTemp;

    private UploadMskuStatusForLifecycleToYtExecutor executor;
    private UploadMskuStatusForLifecycleToYtExecutor.MonitoringExecutor monitoringExecutor;

    @Before
    public void setUp() throws Exception {
        executor = new UploadMskuStatusForLifecycleToYtExecutor(
            mskuStatusRepository,
            jdbcTemplate,
            null,
            null,
            null
        );

        monitoringExecutor = new UploadMskuStatusForLifecycleToYtExecutor.MonitoringExecutor(
            deepmindDbMonitoring.getOrCreateUnit("unit"),
            mskuStatusRepository
        );

        deepmindMskuRepository.save(TestUtils.newMsku(1));
        deepmindMskuRepository.save(TestUtils.newMsku(2));
        deepmindMskuRepository.save(TestUtils.newMsku(3));

        modifyRowsRequestMock = new ModifyRowsRequestMock();
        var rpcApi = Mockito.mock(YtTableRpcApi.class);
        Mockito.when(rpcApi.createModifyRowRequest()).thenReturn(modifyRowsRequestMock);
        executor.setRpcApi(rpcApi);
    }

    @Test
    public void testUploadOne() {
        mskuStatusRepository.save(mskuStatus(1L, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()));

        executor.execute();

        List<Map<String, ?>> insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("msku_id", 1L),
            Map.entry("msku_status", "NPD"),
        });
        assertThat((Map<String, Object>) insertion.get(0).get("params"))
            .contains(Map.entry("npd_start_date", LocalDate.now().toString()));
    }

    @Test
    public void testSecondUploadWontHappen() {
        seasonRepository.save(new Season().setId(100L).setName("Season"));
        mskuStatusRepository.save(mskuStatus(2L, MskuStatusValue.SEASONAL).setSeasonId(100L));

        executor.execute();

        var insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("msku_id", 2L),
            Map.entry("msku_status", "SEASONAL")
        });
        assertThat((Map<String, Object>) insertion.get(0).get("params"))
            .contains(Map.entry("season_id", 100L));

        // second call
        modifyRowsRequestMock.clear();
        executor.execute();

        insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).isEmpty();
    }

    @Test
    public void testSecondCallWillUploadIfDataHasChanged() {
        var status2 = mskuStatusRepository.save(mskuStatus(2L, MskuStatusValue.REGULAR));
        var status3 = mskuStatusRepository.save(mskuStatus(3L, MskuStatusValue.END_OF_LIFE));

        executor.execute();

        var insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(2);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("msku_id", 2L),
            Map.entry("msku_status", "REGULAR")
        });
        assertThat(insertion.get(1)).contains(new Map.Entry[]{
            Map.entry("msku_id", 3L),
            Map.entry("msku_status", "END_OF_LIFE")
        });

        // second call
        mskuStatusRepository.save(status2.setMskuStatus(MskuStatusValue.IN_OUT));
        modifyRowsRequestMock.clear();
        executor.execute();

        insertion = modifyRowsRequestMock.getInsertion();
        assertThat(insertion).hasSize(1);
        assertThat(insertion.get(0)).contains(new Map.Entry[]{
            Map.entry("msku_id", 2L),
            Map.entry("msku_status", "IN_OUT")
        });
    }

    @Test
    public void testRunNow() {
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()));

        executeWithNowTime(Instant.now());
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testRunAfter1Min() {
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()));

        executeWithNowTime(Instant.now().plus(1, ChronoUnit.MINUTES));
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.OK);
    }

    @Test
    public void testRunAfter50Min() {
        mskuStatusRepository.save(mskuStatus(1, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()));
        mskuStatusRepository.save(mskuStatus(2, MskuStatusValue.NPD).setNpdStartDate(LocalDate.now()));

        executeWithNowTime(Instant.now().plus(50, ChronoUnit.MINUTES));

        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getStatus()).isEqualTo(MonitoringStatus.WARNING);
        Assertions.assertThat(deepmindDbMonitoring.fetchTotalResult().getMessage())
            .contains("msku_status not uploaded to yt in 45 mins msku_ids: 1,2");
    }

    protected void executeTaskQueue() {
        var taskQueueExecutor = new UnsafeTaskQueueExecutor(
            taskQueueHandlerRegistry, transactionTemplate, taskQueueRepository, objectMapper, Duration.ofHours(1));
        while (true) {
            if (!taskQueueExecutor.processNextStep()) {
                return;
            }
        }
    }

    private void executeWithNowTime(Instant checkTime) {
        try {
            monitoringExecutor.setClock(Clock.fixed(checkTime, ZoneOffset.UTC));
            monitoringExecutor.execute();
        } finally {
            monitoringExecutor.setClock(Clock.systemDefaultZone());
        }
    }

    private static MskuStatus mskuStatus(long id, MskuStatusValue status) {
        return new MskuStatus().setMarketSkuId(id).setMskuStatus(status);
    }
}
