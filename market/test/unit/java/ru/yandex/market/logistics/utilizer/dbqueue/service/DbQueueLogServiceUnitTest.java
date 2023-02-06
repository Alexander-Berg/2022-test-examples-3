package ru.yandex.market.logistics.utilizer.dbqueue.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.dbqueue.DbqueueTaskType;
import ru.yandex.market.logistics.utilizer.domain.AppInfo;
import ru.yandex.market.logistics.utilizer.domain.entity.DbQueueLog;
import ru.yandex.market.logistics.utilizer.domain.enums.DbQueueLogEvent;
import ru.yandex.market.logistics.utilizer.repo.DbQueueLogRepository;
import ru.yandex.market.logistics.utilizer.service.system.SystemPropertyService;
import ru.yandex.market.logistics.utilizer.service.system.keys.SystemPropertyBooleanKey;
import ru.yandex.market.logistics.utilizer.service.time.DateTimeService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbQueueLogServiceUnitTest extends SoftAssertionSupport {

    private DbQueueLogService dbQueueLogService;
    private DbQueueLogRepository dbQueueLogRepository;

    private final static String MOC_HOST_NAME = "host";

    @BeforeEach
    public void init() {
        dbQueueLogRepository = Mockito.mock(DbQueueLogRepository.class);
        SystemPropertyService systemPropertyService = mock(SystemPropertyService.class);
        when(systemPropertyService.getProperty(SystemPropertyBooleanKey.WRITE_DBQUEUE_LOG_ENABLED)).thenReturn(true);
        DateTimeService dateTimeService = Mockito.mock(DateTimeService.class);
        AppInfo appInfo = new AppInfo(MOC_HOST_NAME);

        dbQueueLogService = new DbQueueLogServiceImpl(dbQueueLogRepository, systemPropertyService, dateTimeService,
                appInfo);
    }

    @Test
    public void createNewEventWorksCorrect() {

        dbQueueLogService.createNewEvent(DbqueueTaskType.CREATE_TRANSFER, DbQueueLogEvent.NEW, "payload", 10L);
        ArgumentCaptor<DbQueueLog> logCaptor = ArgumentCaptor.forClass(DbQueueLog.class);
        Mockito.verify(dbQueueLogRepository).save(logCaptor.capture());
        DbQueueLog capturedLog = logCaptor.getValue();

        softly.assertThat(capturedLog.getQueueName()).isEqualTo(DbqueueTaskType.CREATE_TRANSFER);
        softly.assertThat(capturedLog.getEvent()).isEqualTo(DbQueueLogEvent.NEW);
        softly.assertThat(capturedLog.getPayload()).isEqualTo("payload");
        softly.assertThat(capturedLog.getTaskId()).isEqualTo(10L);
        softly.assertThat(MOC_HOST_NAME);

    }

}
