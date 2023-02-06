package ru.yandex.market.wms.scheduler.service.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import ru.yandex.market.wms.scheduler.dao.ArchiveInventoryCountLogDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveInventoryCountLogServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;
    private static final int KEY_1 = 1;
    private static final int KEY_2 = 2;
    private static final int KEY_3 = 3;
    private static final List<Integer> KEY_LIST = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3));
    private static final List<Integer> EMPTY_KEY_LIST = new ArrayList<>();

    @InjectMocks
    private ArchiveInventoryCountLogService archiveService;

    @Mock
    private ArchiveInventoryCountLogDao dao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    void executeWhenKeysListSmallerThanPartitionSizeTest(int archivedRowsCount) throws InterruptedException {
        setUpDaoMock(KEY_LIST, EMPTY_KEY_LIST, KEY_LIST, archivedRowsCount, archivedRowsCount, 0, 0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(archivedRowsCount, archivedRowsCount, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysListGreaterThanPartitionSizeTest() throws InterruptedException {
        int arcRowsCnt1 = 2;
        int delRowsCnt1 = 2;
        int arcRowsCnt2 = 1;
        int delRowsCnt2 = 1;
        setUpDaoMock(KEY_LIST, EMPTY_KEY_LIST, KEY_LIST, arcRowsCnt1, delRowsCnt1, arcRowsCnt2, delRowsCnt2);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(2);
        when(dao.archiveAndClean(anyList(), anyInt()))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(arcRowsCnt1, delRowsCnt1))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(arcRowsCnt2, delRowsCnt2))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(0, 0));
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(arcRowsCnt1 + arcRowsCnt2, delRowsCnt1 + delRowsCnt2, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int arcRowsCnt1 = KEY_LIST.size();
        int delRowsCnt1 = KEY_LIST.size();
        setUpDaoMock(KEY_LIST, KEY_LIST, KEY_LIST, arcRowsCnt1, delRowsCnt1, 0, 0);
        when(dao.archiveAndClean(eq(KEY_LIST), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(arcRowsCnt1, delRowsCnt1))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(0, 0));
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(arcRowsCnt1, delRowsCnt1, 1, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        setUpDaoMock(EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, 0, 0, 0, 0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(EMPTY_KEY_LIST.size(), EMPTY_KEY_LIST.size(), 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(
            List<Integer> outputKeyList1,
            List<Integer> outputKeyList2,
            List<Integer> inputKeyList1,
            int arcRowsCnt1,
            int delRowsCnt1,
            int arcRowsCnt2,
            int delRowsCnt2
    ) {
        when(dao.getInventoryLogKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(outputKeyList1).thenReturn(outputKeyList2).thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveAndClean(eq(inputKeyList1), anyInt()))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(arcRowsCnt1, delRowsCnt1))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter(arcRowsCnt2, delRowsCnt2))
                .thenReturn(new ArchiveInventoryCountLogDao.StatCounter());
    }

    private void verifyDaoMock(
            int getInventoryLogKeysTimes,
            int archiveAndCleanTimes
    ) {
        verify(dao, times(getInventoryLogKeysTimes)).getInventoryLogKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(archiveAndCleanTimes)).archiveAndClean(anyList(), anyInt());
    }

    private void setUpConfigServiceMock() {
        int defaultInactivityDaysThreshold = 100;
        int defaultBatchSize = 15_000;
        int defaultTrnTimeoutSec = 5;
        int defaultSelectTimeout = 15;
        int defaultPartitionSize = 1000;
        int defaultSleepTime = 1;
        int defaultRetryAttempts = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_CLEAN_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_CLEAN_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultInactivityDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_CLEAN_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_CLEAN_TRN_TIMEOUT_SEC"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultTrnTimeoutSec);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_SELECT_TIMEOUT_SEC"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultPartitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_CLEAN_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("INVENTORYCOUNT_LOG_CLEAN_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_CLEAN_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_CLEAN_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_CLEAN_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_CLEAN_TRN_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_SELECT_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_CLEAN_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("INVENTORYCOUNT_LOG_CLEAN_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 8))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            int inventoryCountLogDelRowsCnt,
            int inventoryCountLogRowsCnt,
            int failedAttemptsNumber,
            long stopExecutionTime
    ) {
        return String.format("Records clean/copy: INVENTORYCOUNT_LOG: %d/%d%s%s",
                inventoryCountLogDelRowsCnt, inventoryCountLogRowsCnt,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
