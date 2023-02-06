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
import ru.yandex.market.wms.scheduler.dao.ArchiveItrnDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveItrnServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;
    private static final int DEFAULT_COPY_TIMEOUT = 5;
    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";
    private static final String KEY_3 = "3";
    private static final List<String> KEY_LIST = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3));
    private static final List<String> EMPTY_KEY_LIST = new ArrayList<>();

    @InjectMocks
    private ArchiveItrnService archiveService;

    @Mock
    private ArchiveItrnDao dao;

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
        setUpDaoMockForGettingKeys(KEY_LIST, EMPTY_KEY_LIST);
        setUpDaoMock(KEY_LIST, archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount, false);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(stopExecutionTime,
                archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysListGreaterThanPartitionSizeTest() throws InterruptedException {
        int itrnRowsCnt1 = 2;
        int itrnserialRowsCnt1 = 2;
        int itrnDelRowsCnt1 = 2;
        int itrnserialDelRowsCnt1 = 2;
        int itrnRowsCnt2 = 1;
        int itrnserialRowsCnt2 = 1;
        int itrnDelRowsCnt2 = 1;
        int itrnserialDelRowsCnt2 = 1;
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(2);
        when(dao.getItrnKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(KEY_LIST).thenReturn(EMPTY_KEY_LIST).thenReturn(EMPTY_KEY_LIST);
        when(dao.copyItrnByItrnKeys(anyList(), anyInt()))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(
                        itrnRowsCnt1, itrnserialRowsCnt1, itrnDelRowsCnt1, itrnserialDelRowsCnt1))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(
                        itrnRowsCnt2, itrnserialRowsCnt2, itrnDelRowsCnt2, itrnserialDelRowsCnt2))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(0, 0, 0, 0));
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(stopExecutionTime, itrnRowsCnt1 + itrnRowsCnt2,
                itrnserialRowsCnt1 + itrnserialRowsCnt2, itrnDelRowsCnt1 + itrnDelRowsCnt2,
                itrnserialDelRowsCnt1 + itrnserialDelRowsCnt2, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int archivedRowsCount = KEY_LIST.size();
        setUpDaoMockForGettingKeys(KEY_LIST, KEY_LIST);
        setUpDaoMock(EMPTY_KEY_LIST, archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount, true);
        when(dao.copyItrnByItrnKeys(eq(KEY_LIST), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(archivedRowsCount, archivedRowsCount, archivedRowsCount,
                        archivedRowsCount))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(0, 0, 0, 0));
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(stopExecutionTime,
                archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount, 1);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        int archivedRowsCount = EMPTY_KEY_LIST.size();
        setUpDaoMockForGettingKeys(EMPTY_KEY_LIST, EMPTY_KEY_LIST);
        setUpDaoMock(EMPTY_KEY_LIST, archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount, true);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(stopExecutionTime,
                archivedRowsCount, archivedRowsCount, archivedRowsCount, archivedRowsCount, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMockForGettingKeys(List<String> outputKeyList1, List<String> outputKeyList2) {
        when(dao.getItrnKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(outputKeyList1).thenReturn(outputKeyList2).thenReturn(EMPTY_KEY_LIST);
    }

    private void setUpDaoMock(
            List<String> inputKeyList1,
            int itrnRowsCnt1,
            int itrnserialRowsCnt1,
            int itrnDelRowsCnt1,
            int itrnserialDelRowsCnt1,
            boolean isRepeatable
    ) {
        when(dao.copyItrnByItrnKeys(eq(inputKeyList1), anyInt()))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(itrnRowsCnt1, itrnserialRowsCnt1, itrnDelRowsCnt1,
                        itrnserialDelRowsCnt1))
                .thenReturn(isRepeatable ? new ArchiveItrnDao
                        .ItrnStatCounter(itrnRowsCnt1, itrnserialRowsCnt1, itrnDelRowsCnt1, itrnserialDelRowsCnt1) :
                        new ArchiveItrnDao.ItrnStatCounter(0, 0, 0, 0))
                .thenReturn(new ArchiveItrnDao.ItrnStatCounter(0, 0, 0, 0));
    }

    private void verifyDaoMock(
            int getItrnKeysTimes,
            int copyItrnByItrnKeysTimes
    ) {
        verify(dao, times(getItrnKeysTimes)).getItrnKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(copyItrnByItrnKeysTimes)).copyItrnByItrnKeys(anyList(), anyInt());
    }

    private void setUpConfigServiceMock() {
        final int defaultBatchSize = 15000;
        final int defaultItrnCopyDaysDelta = 0;
        final int defaultPartitionSize = 1000;
        final int defaultSelectTimeout = 10;
        final int defaultCopySleepTime = 1;
        final int defaultRetryAttempts = 3;

        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_DAYS_DELTA"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultItrnCopyDaysDelta);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_COPY_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultPartitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultCopySleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_FAILURE_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultCopySleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITRN_COPY_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_DAYS_DELTA"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_FAILURE_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("ITRN_COPY_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 9))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            long stopExecutionTime,
            int itrnRowsCnt,
            int itrnserialRowsCnt,
            int itrnDelRowsCnt,
            int itrnserialDelRowsCnt,
            int failedAttemptsNumber
    ) {
        if (itrnRowsCnt > 0 || itrnserialRowsCnt > 0 || itrnDelRowsCnt > 0 || itrnserialDelRowsCnt > 0) {
            return String.format("Records copy/delete to SCPRDARC: ITRN: %d/%d, itrnserial: %d/%d%s%s",
                    itrnRowsCnt, itrnDelRowsCnt, itrnserialRowsCnt, itrnserialDelRowsCnt,
                    failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                    System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
        } else {
            return "No tasks";
        }
    }
}
