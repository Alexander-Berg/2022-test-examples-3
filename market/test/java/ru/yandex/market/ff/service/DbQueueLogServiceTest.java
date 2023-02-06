package ru.yandex.market.ff.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ff.model.AppInfo;
import ru.yandex.market.ff.model.entity.DbQueueLog;
import ru.yandex.market.ff.model.enums.DbQueueLogEvent;
import ru.yandex.market.ff.model.enums.DbQueueType;
import ru.yandex.market.ff.repository.DbQueueLogRepository;
import ru.yandex.market.ff.service.implementation.DbQueueLogCacheImpl;
import ru.yandex.market.ff.service.implementation.DbQueueLogServiceImpl;


public class DbQueueLogServiceTest {

    private DbQueueLogService dbQueueLogService;
    private DbQueueLogCache dbQueueLogCache;
    private DbQueueLogRepository dbQueueLogRepository;
    private ConcreteEnvironmentParamService environmentParamService;
    private SoftAssertions assertions;


    @BeforeEach
    public void init() {
        dbQueueLogRepository = Mockito.mock(DbQueueLogRepository.class);
        environmentParamService = Mockito.mock(ConcreteEnvironmentParamService.class);
        AppInfo appInfo = new AppInfo("hostName", "FFWF-test");
        dbQueueLogCache = new DbQueueLogCacheImpl(environmentParamService, dbQueueLogRepository);
        dbQueueLogService = new DbQueueLogServiceImpl(dbQueueLogCache, environmentParamService,
                appInfo);
        assertions = new SoftAssertions();
    }

    @AfterEach
    public void triggerAssertions() {
        assertions.assertAll();
    }

    @Test
    public void createNewEventWorksCorrect() {
        Mockito.when(environmentParamService.enableDbQueueLogWriting()).thenReturn(true);
        Mockito.when(environmentParamService.getDbQueueLogsBatchSize()).thenReturn(50);

        dbQueueLogService.createNewEvent(DbQueueType.VALIDATE_CIS, DbQueueLogEvent.NEW, 10, 20);

        ArgumentCaptor<List<DbQueueLog>> logCaptor = ArgumentCaptor.forClass((Class) List.class);
        dbQueueLogCache.saveToPersistenceStorage();
        Mockito.verify(dbQueueLogRepository).save(logCaptor.capture());

        List<DbQueueLog> capturedLogs = new ArrayList<>(logCaptor.getValue());

        DbQueueLog capturedLog = capturedLogs.get(0);

        assertions.assertThat(capturedLog.getQueueName()).isEqualTo(DbQueueType.VALIDATE_CIS);
        assertions.assertThat(capturedLog.getEvent()).isEqualTo(DbQueueLogEvent.NEW);
        assertions.assertThat(capturedLog.getEntityId()).isEqualTo(10);
        assertions.assertThat(capturedLog.getTaskId()).isEqualTo(20);
        assertions.assertThat(capturedLog.getHostName()).isEqualTo("hostName");
    }

    @Test
    public void createMultipleNewEventAndMultipleBatchesCorrect() {
        int batchSize = 50;
        int secondBatchSize = batchSize - 1;
        int startTaskId = 20;

        Mockito.when(environmentParamService.enableDbQueueLogWriting()).thenReturn(true);
        Mockito.when(environmentParamService.getDbQueueLogsBatchSize()).thenReturn(50);

        int queueLogSize = batchSize + secondBatchSize;

        for (int i = 0; i < queueLogSize; i++) {
            dbQueueLogService.createNewEvent(DbQueueType.VALIDATE_CIS, DbQueueLogEvent.NEW, 10 + i,
                    startTaskId + i);
        }

        ArgumentCaptor<List<DbQueueLog>> logCaptor = ArgumentCaptor.forClass((Class) List.class);
        dbQueueLogCache.saveToPersistenceStorage();

        Mockito.verify(dbQueueLogRepository, Mockito.atLeast(2)).save(logCaptor.capture());

        List<List<DbQueueLog>> logBatches = logCaptor.getAllValues();
        List<DbQueueLog> queueLogs = logBatches.stream().flatMap(Collection::stream).collect(Collectors.toList());

        assertions.assertThat(queueLogs).hasSize(queueLogSize);

        List<Long> uniqueTaskIds =
                queueLogs.stream().map(DbQueueLog::getTaskId).distinct().collect(Collectors.toList());

        assertions.assertThat(uniqueTaskIds).anyMatch(l -> l == startTaskId);
        assertions.assertThat(uniqueTaskIds).anyMatch(l -> l == startTaskId + queueLogSize - 1);
    }

    @Test
    public void saveRemainingRowsTest() {
        Mockito.when(environmentParamService.enableDbQueueLogWriting()).thenReturn(true);
        Mockito.when(environmentParamService.getDbQueueLogsBatchSize()).thenReturn(50);

        dbQueueLogService.createNewEvent(DbQueueType.VALIDATE_CIS, DbQueueLogEvent.NEW, 10, 20);
        dbQueueLogCache.saveToPersistenceStorage();
        ArgumentCaptor<List<DbQueueLog>> logCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(dbQueueLogRepository).save(logCaptor.capture());

        List<DbQueueLog> capturedLogs = new ArrayList<>(logCaptor.getValue());

        assertions.assertThat(capturedLogs).hasSize(1);
    }
}
