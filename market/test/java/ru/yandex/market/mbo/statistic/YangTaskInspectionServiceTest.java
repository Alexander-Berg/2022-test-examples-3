package ru.yandex.market.mbo.statistic;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.http.YangLogStorage;
import ru.yandex.market.mbo.http.YangLogStorage.YangResolveNeedInspectionRequest;
import ru.yandex.market.mbo.statistic.model.RankAndCount;
import ru.yandex.market.mbo.utils.BaseDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.statistic.model.TaskType.BLUE_LOGS;

/**
 * @author yuramalinov
 * @created 03.06.19
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class YangTaskInspectionServiceTest extends BaseDbTest {
    @Autowired
    @Qualifier("postgresJdbcTemplate")
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("postgresTransactionTemplate")
    private TransactionTemplate transactionTemplate;

    private YangTaskInspectionDao taskInspectionDao;
    private StatisticsService statisticsService;
    private YangTaskInspectionService taskInspectionService;

    private int taskIdCounter;

    @Before
    public void setup() {
        taskInspectionDao = new YangTaskInspectionDao(jdbcTemplate);
        statisticsService = Mockito.mock(StatisticsService.class);
        taskInspectionService =
            new YangTaskInspectionService(taskInspectionDao, statisticsService, transactionTemplate);
        taskIdCounter = 0;
    }

    @Test
    public void testNotEnoughData() {
        when(statisticsService.getContractorInspectedRank(anyLong(), anyLong(), anyInt(), any()))
            .thenReturn(new RankAndCount(100.0, YangTaskInspectionService.MIN_INSPECTED_TASK_COUNT - 1));

        YangLogStorage.YangResolveNeedInspectionResponse response = taskInspectionService.resolveNeedInspection(
            request(2).build());

        assertThat(response.getNeedInspection()).isTrue();
        assertThat(response.getDebugInformation()).contains("Task count <");
        // Check storage
        assertThat(taskInspectionDao.getAllInspections())
            .hasSize(1) // is recorded
            .allSatisfy(inspection -> {
                assertThat(inspection.getTaskType()).isEqualTo(BLUE_LOGS);
                assertThat(inspection.getCategoryId()).isEqualTo(1L);
                assertThat(inspection.getOffersCount()).isEqualTo(2);
                assertThat(inspection.getContractorUid()).isEqualTo(3L);
                assertThat(inspection.getTaskId()).isEqualTo("task1");
                assertThat(inspection.isInspected()).isEqualTo(true);
                assertThat(inspection.getDebug()).isEqualTo(response.getDebugInformation());
            });
    }

    @Test
    public void testDryRunNotRecorded() {
        when(statisticsService.getContractorInspectedRank(anyLong(), anyLong(), anyInt(), any()))
            .thenReturn(new RankAndCount(100.0, YangTaskInspectionService.MIN_INSPECTED_TASK_COUNT - 1));

        YangLogStorage.YangResolveNeedInspectionResponse response = taskInspectionService.resolveNeedInspection(
            request(10).setDryRun(true).build());

        assertThat(response.getNeedInspection()).isTrue();
        assertThat(response.getDebugInformation()).contains("Task count <");
        assertThat(taskInspectionDao.getAllInspections()).hasSize(0); // is not recorded
    }

    @Test
    public void testChecks() {
        when(statisticsService.getContractorInspectedRank(anyLong(), anyLong(), anyInt(), any()))
            .thenReturn(new RankAndCount(100.0, YangTaskInspectionService.MIN_INSPECTED_TASK_COUNT + 1));

        YangLogStorage.YangResolveNeedInspectionResponse response;

        // Should have 5% rating (capped), so 19-requests should be falsy and 20-th is checked
        for (int i = 0; i < 19; i++) {
            response = taskInspectionService.resolveNeedInspection(request(10).build());
            assertThat(response.getNeedInspection()).isFalse();
            assertThat(response.getDebugInformation()).contains("inspectionPercent = 5.0");
        }
        response = taskInspectionService.resolveNeedInspection(request(10).build());
        assertThat(response.getNeedInspection()).isTrue();
        assertThat(response.getDebugInformation()).contains("notInspectedOffers = 190");
        assertThat(taskInspectionDao.getAllInspections()).hasSize(20);
    }

    @Test
    public void testSmallPacket() {
        when(statisticsService.getContractorInspectedRank(anyLong(), anyLong(), anyInt(), any()))
            .thenReturn(new RankAndCount(100.0, YangTaskInspectionService.MIN_INSPECTED_TASK_COUNT + 1));

        YangLogStorage.YangResolveNeedInspectionResponse response;

        // Should have 5% rating (capped), so 19-requests should be falsy and 20-th is checked
        for (int i = 0; i < 19; i++) {
            response = taskInspectionService.resolveNeedInspection(request(10).build());
            assertThat(response.getNeedInspection()).isFalse();
        }
        response = taskInspectionService.resolveNeedInspection(request(9).build());
        assertThat(response.getNeedInspection()).isFalse();
        response = taskInspectionService.resolveNeedInspection(request(9).build());
        assertThat(response.getNeedInspection()).isFalse(); // No check still

        // Full task
        response = taskInspectionService.resolveNeedInspection(request(10).build());
        assertThat(response.getNeedInspection()).isTrue(); // No we've got inspection

        assertThat(taskInspectionDao.getAllInspections()).hasSize(22);
    }

    @Test
    public void testTooSmallRankIsAlwaysInspected() {
        when(statisticsService.getContractorInspectedRank(anyLong(), anyLong(), anyInt(), any()))
            .thenReturn(new RankAndCount(50.0, YangTaskInspectionService.MIN_INSPECTED_TASK_COUNT + 1));

        YangLogStorage.YangResolveNeedInspectionResponse response =
            taskInspectionService.resolveNeedInspection(request(9).build());

        assertThat(response.getNeedInspection()).isTrue();
        assertThat(response.getDebugInformation()).contains("Rank 50.0 <");

        assertThat(taskInspectionDao.getAllInspections()).hasSize(1);
    }

    private YangResolveNeedInspectionRequest.Builder request(int offersCount) {
        return YangResolveNeedInspectionRequest.newBuilder()
            .setTaskType(YangLogStorage.YangTaskType.BLUE_LOGS)
            .setCategoryId(1)
            .setOffersCount(offersCount)
            .setUid(3)
            .setTaskId("task" + ++taskIdCounter);
    }
}
