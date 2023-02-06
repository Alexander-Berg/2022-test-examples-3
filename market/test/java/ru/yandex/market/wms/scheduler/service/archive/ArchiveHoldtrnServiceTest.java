package ru.yandex.market.wms.scheduler.service.archive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.ArchiveHoldtrnDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveHoldtrnServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;

    @InjectMocks
    private ArchiveHoldtrnService archiveService;

    @Mock
    private ArchiveHoldtrnDao dao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void executeWhenSomeKeysFoundTest(int foundKeysNumber) throws InterruptedException {
        int iterationsNumber = 1;
        setUpDaoMock(foundKeysNumber, 0, foundKeysNumber, foundKeysNumber);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(foundKeysNumber, foundKeysNumber, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(iterationsNumber + 1, iterationsNumber + 1, iterationsNumber, iterationsNumber);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int foundKeysNumber = 1;
        int arcRowsCnt = 1;
        int rearcRowsCnt = 1;
        setUpDaoMock(foundKeysNumber, foundKeysNumber, arcRowsCnt, rearcRowsCnt);
        when(dao.clearPreviouslyArchivedRecords(anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(arcRowsCnt)
                .thenReturn(0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(arcRowsCnt, rearcRowsCnt, 1, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, 3, 2, 1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        int iterationsNumber = 0;
        int foundKeysNumber = 0;
        int arcRowsCnt = 0;
        int rearcRowsCnt = 0;
        setUpDaoMock(foundKeysNumber, foundKeysNumber, arcRowsCnt, rearcRowsCnt);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(arcRowsCnt, rearcRowsCnt, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(iterationsNumber + 1, iterationsNumber + 1, iterationsNumber, iterationsNumber);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(
            int foundKeysNumber1,
            int foundKeysNumber2,
            int arcRowsCnt,
            int rearcRowsCnt
    ) {
        when(dao.fillTemporaryTable(anyInt(), anyInt(), anyInt()))
                .thenReturn(foundKeysNumber1).thenReturn(foundKeysNumber2).thenReturn(0);
        when(dao.clearPreviouslyArchivedRecords(anyInt())).thenReturn(arcRowsCnt).thenReturn(0);
        when(dao.archiveAndDelete(anyInt())).thenReturn(rearcRowsCnt).thenReturn(0);

    }

    private void verifyDaoMock(
            int cleanTemporaryTableTimes,
            int fillTemporaryTableTimes,
            int clearPreviouslyArchivedRecordsTimes,
            int archiveAndDeleteTimes
    ) {
        verify(dao, times(cleanTemporaryTableTimes)).cleanTemporaryTable(anyInt());
        verify(dao, times(fillTemporaryTableTimes)).fillTemporaryTable(anyInt(), anyInt(), anyInt());
        verify(dao, times(clearPreviouslyArchivedRecordsTimes)).clearPreviouslyArchivedRecords(anyInt());
        verify(dao, times(archiveAndDeleteTimes)).archiveAndDelete(anyInt());
    }

    private void setUpConfigServiceMock() {
        final int defaultInactivityDaysThreshold = 55;
        final int defaultBatchSize = 15_000;
        final int defaultSleepTime = 1;
        final int defaultArchiveTimeout = 5;
        final int defaultSelectTimeout = 30;
        final int defaultRetryAttempts = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultInactivityDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_TRN_TIMEOUT_SEC"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultArchiveTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_SELECT_TRN_TIMEOUT_SEC"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_TRN_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_SELECT_TRN_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("HOLDTRN_CLEAN_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 7))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(int arcRowsCnt, int rearcRowsCnt, int failedAttemptsNumber, long stopExecutionTime) {
        return String.format("Records arc/rearc: HOLDTRN: %d/%d%s%s", arcRowsCnt, rearcRowsCnt,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
