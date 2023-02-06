package ru.yandex.market.wms.scheduler.service.archive;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.ArchiveRequestToteHistoryDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveRequestToteHistoryServiceTest extends SchedulerIntegrationTest {

    private static final int TIME_LIMIT = 10_000;
    private static final List<Long> ID_LIST_1 = List.of(1L, 2L, 3L);
    private static final List<Long> ID_LIST_2 = List.of(4L, 5L, 6L);
    private static final List<Long> EMPTY_LIST = Collections.emptyList();

    @InjectMocks
    private ArchiveRequestToteHistoryService service;

    @Mock
    private DbConfigService dbConfigService;

    @Mock
    private ArchiveRequestToteHistoryDao dao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void executeWhenSomeIdsFoundTest() throws InterruptedException {
        int archiveTimes = 2;
        int counter = 3;
        setUpConfigServiceMock(3);
        setUpDaoMock(ID_LIST_1, ID_LIST_2, counter);

        long stopExecutionTime = System.currentTimeMillis() + TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(counter * archiveTimes, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, archiveTimes);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int archiveTimes = 1;
        int counter = 3;

        setUpConfigServiceMock(6);
        when(dao.getRequestToteEventIds(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test")).thenReturn(ID_LIST_1).thenReturn(EMPTY_LIST);
        when(dao.archive(anyList(), anyInt())).thenReturn(counter);

        long stopExecutionTime = System.currentTimeMillis() + TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(counter * archiveTimes, 1, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, archiveTimes);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        setUpConfigServiceMock(3);
        setUpDaoMock(EMPTY_LIST, EMPTY_LIST, 0);

        long stopExecutionTime = System.currentTimeMillis() + TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(0, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(
            List<Long> keyList1,
            List<Long> keyList2,
            int counter
    ) {
        when(dao.getRequestToteEventIds(anyInt(), anyInt(), anyInt()))
                .thenReturn(keyList1).thenReturn(keyList2).thenReturn(EMPTY_LIST);
        when(dao.archive(anyList(), anyInt())).thenReturn(counter);
    }

    private void verifyDaoMock(int getTimes, int archiveTimes) {
        verify(dao, times(getTimes)).getRequestToteEventIds(anyInt(), anyInt(), anyInt());
        verify(dao, times(archiveTimes)).archive(anyList(), anyInt());
    }

    private void setUpConfigServiceMock(int partitionSize) {
        final int daysThreshold = 30;
        final int failureSleepTime = 0;
        final int archivingTimeout = 3;
        final int selectionTimeout = 3;
        final int maxRetryAttemptsNumber = 3;

        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(daysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_PARTITION_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(partitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_FAIL_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(failureSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(archivingTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(selectionTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(maxRetryAttemptsNumber);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_PARTITION_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_FAIL_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TOTE_REQ_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 7))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            int counter,
            int failedAttemptsNumber,
            long stopExecutionTime
    ) {
        return String.format("Records moved to SCPRDARC: TOTE_REQUEST_HISTORY: %d%s%s",
                counter,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
