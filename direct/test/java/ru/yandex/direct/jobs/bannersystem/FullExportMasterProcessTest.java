package ru.yandex.direct.jobs.bannersystem;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.core.entity.bs.export.queue.service.FullExportQueueService;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

@ParametersAreNonnullByDefault
class FullExportMasterProcessTest {

    private static final int TEST_SHARD = 2;

    @Mock
    private PpcProperty<String> masterType;

    @Mock
    private FullExportQueueService service;

    @Mock
    private FullExportQueueService.Master master;

    private FullExportMasterProcess job;

    @BeforeEach
    void prepare() {
        initMocks(this);
        when(masterType.getOrDefault(anyString())).thenCallRealMethod();
        when(service.getMaster(TEST_SHARD)).thenReturn(master);
        job = new FullExportMasterProcess(TEST_SHARD, service);
    }

    @Test
    void propertyIsNotSet_JobDone() {
        when(masterType.get()).thenReturn(null);

        job.execute();

        verify(service).getMaster(TEST_SHARD);
        verify(master).iteration();
    }

    @Test
    void propertySetToPerl_JobDone() {
        when(masterType.get()).thenReturn("perl");

        job.execute();

        verify(service).getMaster(TEST_SHARD);
        verify(master).iteration();
    }

    @Test
    void propertySetToSomething_JobDone() {
        String value = RandomStringUtils.randomAscii(10);
        when(masterType.get()).thenReturn(value);

        job.execute();


        verify(service).getMaster(TEST_SHARD);
        verify(master).iteration();
    }

    @Test
    void propertySetToJava_JobDone() {
        when(masterType.get()).thenReturn("java");

        job.execute();

        verify(service).getMaster(TEST_SHARD);
        verify(master).iteration();
    }
}
