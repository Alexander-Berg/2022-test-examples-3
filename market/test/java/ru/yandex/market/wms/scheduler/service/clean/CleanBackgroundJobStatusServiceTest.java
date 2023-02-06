package ru.yandex.market.wms.scheduler.service.clean;

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
import ru.yandex.market.wms.scheduler.dao.CleanBackgroundJobStatusDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanBackgroundJobStatusServiceTest extends SchedulerIntegrationTest {

    private static final int DELETION_ALLOWED = 1;
    private static final int DELETION_NOT_ALLOWED = 0;
    private static final int DEFAULT_ARCHIVE_DAYS = 7;
    private static final int DEFAULT_SAMPLE_SIZE = 6;
    private static final int DEFAULT_TIME_LIMIT = 5000;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_DELETE_TIMEOUT = 3;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;

    private static final List<Integer> KEY_LIST_1 = List.of(1, 2, 3, 4, 5, 6);
    private static final List<Integer> KEY_LIST_2 = List.of(7, 8, 9, 10, 11, 12);
    private static final List<Integer> KEY_LIST_3 = List.of(13, 14);
    private static final List<Integer> KEY_LIST_EMPTY = List.of();

    @InjectMocks
    CleanBackgroundJobStatusService cleanBackgroundJobStatusService;

    @Mock
    private CleanBackgroundJobStatusDao dao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @Test
    void executeWhenSomeKeysFoundTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(3);
        when(dao.getSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(KEY_LIST_1)
                .thenReturn(KEY_LIST_2)
                .thenReturn(KEY_LIST_3)
                .thenReturn(KEY_LIST_EMPTY);

        when(dao.deleteBySerialKeys(anyList(), anyInt()))
                .thenReturn(3);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanBackgroundJobStatusService.execute();
        String expectedResult = getResult(12, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, 4);
        verifyDbConfigServiceMock(1);

    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(4);

        when(dao.getSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test timeout"))
                .thenReturn(KEY_LIST_1)
                .thenReturn(KEY_LIST_EMPTY);

        when(dao.deleteBySerialKeys(anyList(), anyInt()))
                .thenReturn(2);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanBackgroundJobStatusService.execute();
        String expectedResult = getResult(6, 1, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, 3);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(4);
        when(dao.getSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(KEY_LIST_EMPTY);
        when(dao.deleteBySerialKeys(eq(KEY_LIST_EMPTY), anyInt()))
                .thenReturn(0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanBackgroundJobStatusService.execute();
        String expectedResult = getResult(0, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDeletionNotAllowedTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_ALLOWED"),
                anyInt(), anyInt(), anyInt())).thenReturn(DELETION_NOT_ALLOWED);

        String actualResult = cleanBackgroundJobStatusService.execute();
        String expectedResult = "Deletion in BACKGROUNDJOBSTATUS not allowed";
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(0, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpConfigServiceMock() {
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_ALLOWED"),
                anyInt(), anyInt(), anyInt())).thenReturn(DELETION_ALLOWED);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_ARCHIVE_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_ARCHIVE_DAYS);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_SAMPLE_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SAMPLE_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_EXEC_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_TIME_LIMIT / 2);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_SELECT_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_DELETE_TIMEOUT"),
                anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_DELETE_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_BACKGROUND_JOB_STATUS_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_RETRY_ATTEMPTS);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_ARCHIVE_DAYS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_SAMPLE_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_EXEC_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_DELETE_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes)).getConfigAsIntegerBetween(
                eq("DEL_BACKGROUND_JOB_STATUS_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
    }

    private void verifyDaoMock(
            int getTimes,
            int deleteTimes
    ) {
        verify(dao, times(getTimes)).getSerialKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(deleteTimes)).deleteBySerialKeys(anyList(), anyInt());
    }

    private String getResult(int deleteRowsCount, int failedAttemptsNumber, long stopExecutionTime) {
        return String.format("Records clean: BACKGROUNDJOBSTATUS %d%s%s", deleteRowsCount,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
