package ru.yandex.direct.core.entity.bs.export.queue.service;

import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.direct.core.entity.bs.export.BsExportParametersService;
import ru.yandex.direct.core.entity.bs.export.model.WorkerType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class FullExportQueueServiceMasterGetMaximumCampaignsInQueueTest {
    private static final int TEST_SHARD = 2;

    @Mock
    private BsExportParametersService parametersService;

    private FullExportQueueService.Master master;

    @BeforeEach
    void prepareMaster() {
        initMocks(this);

        //noinspection ConstantConditions
        FullExportQueueService service = new FullExportQueueService(null, parametersService, null, null);
        master = service.getMaster(TEST_SHARD);
    }

    @Test
    void chunkPerWorkerIsNotSet_LimitIsNotSet_ReturnsZero() {
        when(parametersService.getFullExportChunkPerWorker()).thenReturn(0);
        when(parametersService.getFullExportMaximumCampaignsInQueue()).thenReturn(0);

        assertEquals(0, master.getMaximumCampaignsInQueue());
    }

    @Test
    void chunkPerWorkerIsNotSet_LimitIsSet_ReturnsLimit() {
        int limit = RandomUtils.nextInt(1, Integer.MAX_VALUE);
        when(parametersService.getFullExportChunkPerWorker()).thenReturn(0);
        when(parametersService.getFullExportMaximumCampaignsInQueue()).thenReturn(limit);

        assertEquals(limit, master.getMaximumCampaignsInQueue());
    }

    @Test
    void productOfChunkAndWorkers_LessThanLimit_ReturnsLimit() {
        when(parametersService.getFullExportChunkPerWorker()).thenReturn(10);
        when(parametersService.getWorkersNum(WorkerType.FULL_LB_EXPORT, TEST_SHARD)).thenReturn(2);
        when(parametersService.getFullExportMaximumCampaignsInQueue()).thenReturn(5);

        assertEquals(5, master.getMaximumCampaignsInQueue());
    }

    @Test
    void productOfChunkAndWorkers_GreaterThanLimit_ReturnsProduct() {
        when(parametersService.getFullExportChunkPerWorker()).thenReturn(10);
        when(parametersService.getWorkersNum(WorkerType.FULL_LB_EXPORT, TEST_SHARD)).thenReturn(2);
        when(parametersService.getFullExportMaximumCampaignsInQueue()).thenReturn(1000);

        assertEquals(20, master.getMaximumCampaignsInQueue());
    }
}
