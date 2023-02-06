package ru.yandex.market.logistics.utilizer.solomon.scheduler;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import ru.yandex.market.logistics.utilizer.base.SoftAssertionSupport;
import ru.yandex.market.logistics.utilizer.dbqueue.DbqueueTaskType;
import ru.yandex.market.logistics.utilizer.dbqueue.enums.NumberOfRetriesInterval;
import ru.yandex.market.logistics.utilizer.dbqueue.state.DbqueueState;
import ru.yandex.market.logistics.utilizer.service.time.DateTimeService;
import ru.yandex.market.logistics.utilizer.solomon.client.SolomonPushClient;
import ru.yandex.market.logistics.utilizer.solomon.repository.DbqueueRepository;
import ru.yandex.market.logistics.utilizer.util.FileContentUtils;

public class SolomonMonitoringDbqueueJobSchedulerUnitTest extends SoftAssertionSupport {

    private SolomonPushClient solomonPushClient;
    private DateTimeService dateTimeService;
    private DbqueueRepository dbqueueRepository;
    private SolomonMonitoringDbqueueJobScheduler scheduler;

    @BeforeEach
    public void init() {
        solomonPushClient = Mockito.mock(SolomonPushClient.class);
        dateTimeService = Mockito.mock(DateTimeService.class);
        dbqueueRepository = Mockito.mock(DbqueueRepository.class);
        scheduler = new SolomonMonitoringDbqueueJobScheduler(solomonPushClient, dbqueueRepository, dateTimeService);

        Mockito.when(dateTimeService.localDateTimeNow()).thenReturn(LocalDateTime.of(2020, 12, 14, 17, 0));
    }

    @Test
    public void calculateAndSend() throws Exception {
        Mockito.when(dbqueueRepository.getStateByQueue()).thenReturn(
                Map.of(DbqueueTaskType.CREATE_TRANSFER, createState(2, 1, 0, 1),
                        DbqueueTaskType.SKU_STOCKS_EVENT, createState(1, 0, 1, 0))
        );
        scheduler.calculateAndSend();
        String expected = FileContentUtils.getFileContent("fixtures/dbqueue-solomon-monitoring.json");
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(solomonPushClient).push(captor.capture());
        assertJsonCorrect(expected, captor.getValue());
    }

    private DbqueueState createState(int elementsInQueue,
                                     long elementsWithoutRetries,
                                     long elementsWithFewRetries,
                                     long elementsWithManyRetries) {
        return new DbqueueState(elementsInQueue, Map.of(
                NumberOfRetriesInterval.NO_RETRIES, elementsWithoutRetries,
                NumberOfRetriesInterval.FEW_RETRIES, elementsWithFewRetries,
                NumberOfRetriesInterval.MANY_RETRIES, elementsWithManyRetries
        ));
    }

    private void assertJsonCorrect(String expected, String actual) throws Exception {
        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }
}
