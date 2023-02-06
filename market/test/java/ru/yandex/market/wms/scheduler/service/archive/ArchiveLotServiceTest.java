package ru.yandex.market.wms.scheduler.service.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import ru.yandex.market.wms.scheduler.dao.ArchiveLotDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveLotServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;
    private static final int DEFAULT_LOT_ARCH_TIMEOUT = 10;
    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";
    private static final String KEY_3 = "3";
    private static final List<String> KEY_LIST_1 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3));
    private static final List<String> KEY_LIST_2 = new ArrayList<>(Collections.singletonList(KEY_2));
    private static final List<String> KEY_LIST_3 = new ArrayList<>(Collections.singletonList(KEY_3));
    private static final List<String> EMPTY_KEY_LIST = new ArrayList<>();

    @InjectMocks
    private ArchiveLotService archiveService;

    @Mock
    private ArchiveLotDao dao;

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
        setUpDaoMockForGettingKeys(archivedRowsCount, 0,
                KEY_LIST_2, EMPTY_KEY_LIST, KEY_LIST_3, EMPTY_KEY_LIST);
        setUpDaoMockForArchiving(KEY_LIST_2, KEY_LIST_3, DEFAULT_LOT_ARCH_TIMEOUT,
                archivedRowsCount, archivedRowsCount, 0, 0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(archivedRowsCount, archivedRowsCount, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 1, 1, 1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int archivedRowsCount = KEY_LIST_1.size();
        setUpDaoMockForGettingKeys(archivedRowsCount, archivedRowsCount,
                KEY_LIST_2, KEY_LIST_2, KEY_LIST_3, KEY_LIST_3);
        setUpDaoMockForArchiving(KEY_LIST_2, KEY_LIST_3, DEFAULT_LOT_ARCH_TIMEOUT,
                archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount);
        when(dao.archiveLotAndLotAttribute(KEY_LIST_2, KEY_LIST_3, DEFAULT_LOT_ARCH_TIMEOUT))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(new ArchiveLotDao.LotStatCounter(archivedRowsCount, archivedRowsCount))
                .thenReturn(new ArchiveLotDao.LotStatCounter(0, 0));
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(archivedRowsCount, archivedRowsCount, 1, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, 2, 2, 2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        int archivedRowsCount = EMPTY_KEY_LIST.size();
        setUpDaoMockForGettingKeys(EMPTY_KEY_LIST.size(), 0,
                EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST);
        setUpDaoMockForArchiving(EMPTY_KEY_LIST, EMPTY_KEY_LIST, DEFAULT_LOT_ARCH_TIMEOUT,
                archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(archivedRowsCount, archivedRowsCount, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0, 0, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMockForGettingKeys(
            int foundKeysNumber1,
            int foundKeysNumber2,
            List<String> outputKeyList1,
            List<String> outputKeyList2,
            List<String> outputKeyList3,
            List<String> outputKeyList4
    ) {
        when(dao.addLotListFromLot(anyInt(), anyInt(), anyInt()))
                .thenReturn(foundKeysNumber1).thenReturn(foundKeysNumber2).thenReturn(0);
        when(dao.getExcludedLotKeys(anyInt()))
                .thenReturn(outputKeyList1).thenReturn(outputKeyList2).thenReturn(EMPTY_KEY_LIST);
        when(dao.getExcludedLotAttributeKeys(anyInt()))
                .thenReturn(outputKeyList3).thenReturn(outputKeyList4).thenReturn(EMPTY_KEY_LIST);
    }

    private void setUpDaoMockForArchiving(
            List<String> inputKeyList1,
            List<String> inputKeyList2,
            int timeout,
            int lotRowsCnt1,
            int lotAttributeRowsCnt1,
            int lotRowsCnt2,
            int lotAttributeRowsCnt2
    ) {
        when(dao.archiveLotAndLotAttribute(inputKeyList1, inputKeyList2, timeout))
                .thenReturn(new ArchiveLotDao.LotStatCounter(lotRowsCnt1, lotAttributeRowsCnt1))
                .thenReturn(new ArchiveLotDao.LotStatCounter(lotRowsCnt2, lotAttributeRowsCnt2))
                .thenReturn(new ArchiveLotDao.LotStatCounter());
    }

    private void verifyDaoMock(
            int addLotListFromLotTimes,
            int getExcludedLotKeysTimes,
            int getExcludedLotAttributeKeysTimes,
            int archiveLotAndLotAttributeTimes
    ) {
        verify(dao, times(addLotListFromLotTimes)).addLotListFromLot(anyInt(), anyInt(), anyInt());
        verify(dao, times(getExcludedLotKeysTimes)).getExcludedLotKeys(anyInt());
        verify(dao, times(getExcludedLotAttributeKeysTimes)).getExcludedLotAttributeKeys(anyInt());
        verify(dao, times(archiveLotAndLotAttributeTimes)).archiveLotAndLotAttribute(anyList(), anyList(), anyInt());
    }

    private void setUpConfigServiceMock() {
        final int defaultInactivityDaysThreshold = 90;
        final int defaultLotBatchSize = 10_000;
        final int defaultLotPartitionSize = 1000;
        final int defaultSleepTime = 1;
        final int defaultSelectionTimeout = 30;
        final int defaultFailureSleepTime = 1;
        final int defaultRetryAttempts = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultInactivityDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultLotBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_LOT_ARCH_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectionTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultLotPartitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_FAILURE_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultFailureSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("LOT_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_FAILURE_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("LOT_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 9))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(int lotRowsCnt, int lotAttributeRowsCnt, int failedAttemptsNumber,
                             long stopExecutionTime) {
        return String.format("Records moved to SCPRDARC: LOT: %d, LOTATTRIBUTE: %d%s%s",
                lotRowsCnt, lotAttributeRowsCnt,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
