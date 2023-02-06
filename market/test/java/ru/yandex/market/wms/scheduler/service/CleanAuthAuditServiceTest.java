package ru.yandex.market.wms.scheduler.service;

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
import ru.yandex.market.wms.scheduler.dao.CleanAuthAuditDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanAuthAuditServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 10_000;
    private static final int DELETION_ENABLED = 1;
    private static final int DELETION_NOT_ENABLED = 0;
    private static final List<Integer> KEY_LIST_1 = List.of(1, 2);
    private static final List<Integer> KEY_LIST_2 = List.of(3, 4);
    private static final List<Integer> EMPTY_KEY_LIST = List.of();

    @InjectMocks
    private CleanAuthAuditService cleanIdService;

    @Mock
    private CleanAuthAuditDao dao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void executeWhenSomeKeysFoundTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED, 4);
        setUpDaoMock(KEY_LIST_1, KEY_LIST_2, 2);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanIdService.execute();
        String expectedResult = getResult(2, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1);
        verifyDbConfigServiceMock(1);

    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED, 4);
        when(dao.getKeyList(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test")).thenReturn(KEY_LIST_2).thenReturn(EMPTY_KEY_LIST);
        when(dao.deleteByKeyList(anyList(), anyInt())).thenReturn(2).thenReturn(2).thenReturn(2).thenReturn(0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanIdService.execute();
        String expectedResult = getResult(2, 1, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED, 3);
        setUpDaoMock(EMPTY_KEY_LIST, EMPTY_KEY_LIST, 0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanIdService.execute();
        String expectedResult = getResult(0, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDeletionNotAllowedTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_NOT_ENABLED, 3);

        String actualResult = cleanIdService.execute();
        String expectedResult = "Deleting auth audit data is not enabled";
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(0, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(List<Integer> keyList1, List<Integer> keyList2, int deletedRows) {
        when(dao.getKeyList(anyInt(), anyInt(), anyInt()))
                .thenReturn(keyList1).thenReturn(keyList2).thenReturn(EMPTY_KEY_LIST);
        when(dao.deleteByKeyList(anyList(), anyInt()))
                .thenReturn(deletedRows);
    }

    private void verifyDaoMock(int getTimes, int deleteTimes) {
        verify(dao, times(getTimes)).getKeyList(anyInt(), anyInt(), anyInt());
        verify(dao, times(deleteTimes)).deleteByKeyList(anyList(), anyInt());
    }

    private void setUpConfigServiceMock(int deletionEnabled, int partitionSize) {
        final int defaultBatchSize = 6;
        final int defaultSelectTimeout = 30;
        final int defaultDeleteTimeout = 3;
        final int defaultIterationTime = 2000;
        final int defaultSleepTime = 0;
        final int defaultFailSleepTime = 0;
        final int defaultRetryAttempts = 3;

        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_ENABLED"),
                anyInt(), anyInt(), anyInt())).thenReturn(deletionEnabled);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(partitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_ARCHIVE_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(partitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_ITERATION_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultIterationTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_FAILURE_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultFailSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_DELETE_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultDeleteTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_ENABLED"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_ARCHIVE_DAYS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_ITERATION_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_AUTH_AUDIT_DETAIL_FAILURE_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_DELETE_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_AUTH_AUDIT_DETAIL_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
    }

    private String getResult(int deletedRowsCounter, int failedAttemptsNumber, long stopExecutionTime) {
        return String.format("Deleted %s rows in AUTH_AUDIT_DETAIL%s%s", deletedRowsCounter,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
