package ru.yandex.market.fulfillment.stockstorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.GenericFFJobExecutor;
import ru.yandex.market.fulfillment.stockstorage.service.health.monitoring.jobs.JobWhPair;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.fulfillment.stockstorage.service.tasks.ScheduledTasksGenerationService;
import ru.yandex.market.fulfillment.stockstorage.service.warehouse.WarehouseSyncService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ScheduledTasksGenerationServiceTest {

    @Mock
    private final WarehouseSyncService warehouseSyncService = mock(WarehouseSyncService.class);
    @Mock
    private final GenericFFJobExecutor genericFFJobExecutor = mock(GenericFFJobExecutor.class);
    @Mock
    private final SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);

    private ScheduledTasksGenerationService scheduledTasksGenerationService;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        when(systemPropertyService.getBooleanProperty(SystemPropertyKey.ENABLE_PARALLEL_SCHEDULED_TASKS_GENERATION))
                .thenReturn(true);
    }

    @BeforeEach
    public void init() {
        scheduledTasksGenerationService =
                new ScheduledTasksGenerationService(genericFFJobExecutor, systemPropertyService);
    }

    /**
     * ??????????????????, ?????? ?????? ?????????????????? ???????????? ???? 3-x ???????? ???? ????????????, ?????? ???????? ????????????????.
     */
    @Test
    public void shouldRunThreeJobsSuccessfully() {
        when(warehouseSyncService.getSyncJobWHPairs(anyInt())).thenReturn(createJobWhPairs());
        scheduledTasksGenerationService.execute(createJobWhPairs());

        ArgumentCaptor<JobWhPair> requestArgumentCaptor = ArgumentCaptor.forClass(JobWhPair.class);

        verify(genericFFJobExecutor, times(3))
                .run(requestArgumentCaptor.capture());

        List<JobWhPair> actualJobWhPairs = requestArgumentCaptor.getAllValues();

        verifyJobWhPairs(actualJobWhPairs);
    }

    /**
     * ??????????????????, ?????? ?????? ?????????????????? ?????????????? ???????????? ???????? ???? ????????????, ???? ???????? ?????????? ???? ???????? ????????????????.
     */
    @Test
    public void shouldNotStartAnyJobs() {
        when(warehouseSyncService.getSyncJobWHPairs(anyInt())).thenReturn(Collections.emptyList());
        scheduledTasksGenerationService.execute(Collections.emptyList());

        verify(genericFFJobExecutor, times(0)).run(any(JobWhPair.class));
    }

    private List<JobWhPair> createJobWhPairs() {
        List<JobWhPair> pairs = new ArrayList<>();

        pairs.add(
                new JobWhPair(145, "FullSync")
                        .setBatchSize(200)
        );

        pairs.add(
                new JobWhPair(146, "FullSync")
                        .setBatchSize(150)
        );

        pairs.add(
                new JobWhPair(147, "FullSync")
                        .setBatchSize(100)
        );

        return pairs;
    }

    private void verifyJobWhPairs(List<JobWhPair> jobWhPairs) {
        Assert.assertNotNull(jobWhPairs);
        Assert.assertEquals(3, jobWhPairs.size());

        JobWhPair firstPair = new JobWhPair(145, "FullSync").setBatchSize(200);
        JobWhPair secondPair = new JobWhPair(146, "FullSync").setBatchSize(150);
        JobWhPair thirdPair = new JobWhPair(147, "FullSync").setBatchSize(100);

        Assert.assertTrue(jobWhPairs.containsAll(List.of(firstPair, secondPair, thirdPair)));
    }
}
