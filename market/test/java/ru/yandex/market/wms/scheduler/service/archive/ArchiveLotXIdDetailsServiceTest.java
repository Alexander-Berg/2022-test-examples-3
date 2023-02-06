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
import ru.yandex.market.wms.scheduler.dao.ArchiveLotXIdDao;
import ru.yandex.market.wms.scheduler.exception.JobNoSuccessAttemptsException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveLotXIdDetailsServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;

    @InjectMocks
    private ArchiveLotXIdDetailsService archiveService;

    @Mock
    private ArchiveLotXIdDao dao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5})
    void executeArchiveWhenSomeKeysFoundTest(int rowsToArchive) throws InterruptedException {
        int iterationsNumber = 1;
        int rearcRowsCntDetail = rowsToArchive - 1;
        int arcRowsCntDetail = rowsToArchive;
        int rearcRowsCntHeader = rowsToArchive - 1;
        int arcRowsCntHeader = rowsToArchive;
        int cleanHeadersCnt = rowsToArchive;
        setUpDaoMock(rowsToArchive, rearcRowsCntDetail, arcRowsCntDetail, rearcRowsCntHeader, arcRowsCntHeader,
                cleanHeadersCnt, false);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(rearcRowsCntDetail, rearcRowsCntHeader, arcRowsCntDetail,
                arcRowsCntHeader, 0, stopExecutionTime);
        String actualResult = archiveService.executeArchive();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(iterationsNumber + 1, iterationsNumber, iterationsNumber, iterationsNumber, iterationsNumber, 0);
        verifyDbConfigServiceMockForArchiving(1);
    }

    @Test
    void executeArchiveWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int iterationsNumber = 2;
        int rowsToArchive = 3;
        int rearcRowsCntDetail = rowsToArchive - 1;
        int rearcRowsCntHeader = rowsToArchive - 1;
        setUpDaoMock(rowsToArchive, rearcRowsCntDetail, rowsToArchive, rearcRowsCntHeader, rowsToArchive,
                rowsToArchive, true);
        when(dao.archiveAndDeleteDetails(anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(rowsToArchive)
                .thenReturn(0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(rearcRowsCntDetail + rearcRowsCntDetail, rearcRowsCntHeader, rowsToArchive,
                rowsToArchive, 1, stopExecutionTime);
        String actualResult = archiveService.executeArchive();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(iterationsNumber + 1, iterationsNumber, iterationsNumber, iterationsNumber - 1,
                iterationsNumber - 1, 0);
        verifyDbConfigServiceMockForArchiving(1);
    }

    @Test
    void executeArchiveWhenKeysNotFoundTest() throws InterruptedException {
        int iterationsNumber = 0;
        int rowsToArchive = 0;
        setUpDaoMock(rowsToArchive, rowsToArchive, rowsToArchive, rowsToArchive, rowsToArchive, rowsToArchive, true);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(rowsToArchive, rowsToArchive, rowsToArchive,
                rowsToArchive, 0, stopExecutionTime);
        String actualResult = archiveService.executeArchive();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(iterationsNumber + 1, iterationsNumber, iterationsNumber, iterationsNumber, iterationsNumber, 0);
        verifyDbConfigServiceMockForArchiving(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 5})
    void executeCleanWhenSomeKeysFoundTest(int rowsToClean) throws InterruptedException {
        int iterationsNumber = 1;
        int rearcRowsCntHeader = rowsToClean - 1;
        int arcRowsCntHeader = rowsToClean;
        setUpDaoMock(0, 0, 0, rearcRowsCntHeader, arcRowsCntHeader, rowsToClean, false);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(0, rearcRowsCntHeader, 0,
                arcRowsCntHeader, 0, stopExecutionTime);
        String actualResult = archiveService.executeClean();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(0, 0, 0, iterationsNumber, iterationsNumber, iterationsNumber + 1);
        verifyDbConfigServiceMockForCleaning(1);
    }

    @Test
    void executeCleanWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int iterationsNumber = 2;
        int rowsToClean = 3;
        int rearcRowsCntHeader = rowsToClean - 1;
        setUpDaoMock(0, 0, 0, rearcRowsCntHeader, rowsToClean, rowsToClean, true);
        when(dao.archiveAndDeleteHeaders(anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(rowsToClean)
                .thenReturn(0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(0, rearcRowsCntHeader + rearcRowsCntHeader, 0,
                rowsToClean, 1, stopExecutionTime);
        String actualResult = archiveService.executeClean();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(0, 0, 0, iterationsNumber, iterationsNumber, iterationsNumber + 1);
        verifyDbConfigServiceMockForCleaning(1);
    }

    @Test
    void executeCleanWhenKeysNotFoundTest() throws InterruptedException {
        int iterationsNumber = 0;
        int rowsToClean = 0;
        setUpDaoMock(rowsToClean, rowsToClean, rowsToClean, rowsToClean, rowsToClean, rowsToClean, true);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(rowsToClean, rowsToClean, rowsToClean,
                rowsToClean, 0, stopExecutionTime);
        String actualResult = archiveService.executeClean();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(0, 0, 0, iterationsNumber, iterationsNumber, iterationsNumber + 1);
        verifyDbConfigServiceMockForCleaning(1);
    }

    @Test
    void executeArchiveWhenNoSuccessAttemptsTest() throws InterruptedException {
        when(dao.fillTemporaryTableWithLotXIdKey(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"));
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(10);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(1000);

        Assertions.assertThrows(JobNoSuccessAttemptsException.class, () -> {
            archiveService.executeArchive();
        });
    }

    @Test
    void executeCleanWhenNoSuccessAttemptsTest() throws InterruptedException {
        when(dao.fillTemporaryTableWithLonelyHeaders(anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"));
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(10);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(1000);

        Assertions.assertThrows(JobNoSuccessAttemptsException.class, () -> {
            archiveService.executeClean();
        });
    }

    private void setUpDaoMock(
            int rowsToArchive1,
            int rearcRowsCntDetail1,
            int arcRowsCntDetail1,
            int rearcRowsCntHeader1,
            int arcRowsCntHeader1,
            int cleanHeadersCnt1,
            boolean isRepeatable
    ) {
        when(dao.fillTemporaryTableWithLotXIdKey(anyInt(), anyInt(), anyInt()))
                .thenReturn(rowsToArchive1).thenReturn(isRepeatable ? rowsToArchive1 : 0).thenReturn(0);
        when(dao.clearPreviouslyArchivedDetailRecords(anyInt()))
                .thenReturn(rearcRowsCntDetail1).thenReturn(isRepeatable ? rearcRowsCntDetail1 : 0).thenReturn(0);
        when(dao.archiveAndDeleteDetails(anyInt()))
                .thenReturn(arcRowsCntDetail1).thenReturn(isRepeatable ? arcRowsCntDetail1 : 0).thenReturn(0);
        when(dao.clearPreviouslyArchivedHeaderRecords(anyInt()))
                .thenReturn(rearcRowsCntHeader1).thenReturn(isRepeatable ? rearcRowsCntHeader1 : 0).thenReturn(0);
        when(dao.archiveAndDeleteHeaders(anyInt()))
                .thenReturn(arcRowsCntHeader1).thenReturn(isRepeatable ? arcRowsCntHeader1 : 0).thenReturn(0);
        when(dao.fillTemporaryTableWithLonelyHeaders(anyInt(), anyInt()))
                .thenReturn(cleanHeadersCnt1).thenReturn(isRepeatable ? cleanHeadersCnt1 : 0).thenReturn(0);
    }

    private void verifyDaoMock(
            int fillTemporaryTableWithLotXIdKeyTimes,
            int clearPreviouslyArchivedDetailRecordsTimes,
            int archiveAndDeleteDetailsTimes,
            int clearPreviouslyArchivedHeaderRecordsTimes,
            int archiveAndDeleteHeadersTimes,
            int fillTemporaryTableWithLonelyHeadersTimes
    ) {
        verify(dao, times(fillTemporaryTableWithLotXIdKeyTimes))
                .fillTemporaryTableWithLotXIdKey(anyInt(), anyInt(), anyInt());
        verify(dao, times(clearPreviouslyArchivedDetailRecordsTimes))
                .clearPreviouslyArchivedDetailRecords(anyInt());
        verify(dao, times(archiveAndDeleteDetailsTimes)).archiveAndDeleteDetails(anyInt());
        verify(dao, times(clearPreviouslyArchivedHeaderRecordsTimes))
                .clearPreviouslyArchivedHeaderRecords(anyInt());
        verify(dao, times(archiveAndDeleteHeadersTimes)).archiveAndDeleteHeaders(anyInt());
        verify(dao, times(fillTemporaryTableWithLonelyHeadersTimes))
                .fillTemporaryTableWithLonelyHeaders(anyInt(), anyInt());
    }

    private void setUpConfigServiceMock() {
        final int defaultBatchSize = 500;
        final int defaultInactivityDaysThreshold = 90;
        final int defaultTrnTimeoutSec = 5;
        final int defaultLotxiddetailSelectTimeout = 10;
        final int defaultLotxiddetailCleanTimeout = 5;
        final int defaultSleepTime = 1;
        final int defaultRetryAttempts = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultInactivityDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_TRN_TIMEOUT_SEC"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultTrnTimeoutSec);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultLotxiddetailSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultLotxiddetailCleanTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMockForArchiving(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_TRN_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 12))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private void verifyDbConfigServiceMockForCleaning(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_ARCH_TRN_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOTXIDDETAIL_CLEAN_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 12))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            int rearcRowsCntDetail,
            int rearcRowsCntHeader,
            int arcRowsCntDetail,
            int arcRowsCntHeader,
            int failedAttemptsNumber,
            long stopExecutionTime
    ) {
        return String.format("Records rearc/arc to DETAIL,HEADER: %d,%d / %d,%d%s%s",
                rearcRowsCntDetail, rearcRowsCntHeader,
                arcRowsCntDetail, arcRowsCntHeader,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
