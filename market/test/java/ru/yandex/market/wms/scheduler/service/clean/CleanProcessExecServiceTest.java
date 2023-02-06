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
import ru.yandex.market.wms.scheduler.dao.CleanProcessExecAttrDao;
import ru.yandex.market.wms.scheduler.dao.CleanProcessExecDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CleanProcessExecServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_ATTR_BATCH_SIZE = 10000;
    private static final int DEFAULT_ATTR_PARTITION_SIZE = 1000;
    private static final int DEFAULT_ARCHIVE_INACTIVITY_DAYS_THRESHOLD = 7;
    private static final int DEFAULT_DELETE_INACTIVITY_DAYS_THRESHOLD = 7;
    private static final int DEFAULT_TIME_LIMIT = 180_000;
    private static final int DEFAULT_SELECT_TIMEOUT = 30;
    private static final int DEFAULT_INSERT_TIMEOUT = 5;
    private static final int DEFAULT_DELETE_TIMEOUT = 3;
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;

    private static final List<Integer> KEY_LIST_EMPTY_INT = List.of();
    private static final List<String> KEY_LIST_EMPTY_STRING = List.of();
    private static final List<Integer> KEY_LIST_1_INT = List.of(1, 2, 3, 4);
    private static final List<Integer> KEY_LIST_2_INT = List.of(5, 6, 7, 8);
    private static final List<Integer> KEY_LIST_3_INT = List.of(9, 10, 11);
    private static final List<String> KEY_LIST_1_STRING = List.of("1", "2", "3", "4");
    private static final List<String> KEY_LIST_2_STRING = List.of("5", "6", "7", "8");
    private static final List<String> KEY_LIST_3_STRING = List.of("9", "10", "11");

    private static final List<Integer> KEY_LIST_1_TO_DELETE_INT = List.of(1, 2);
    private static final List<Integer> KEY_LIST_2_TO_DELETE_INT = List.of(3, 4);
    private static final List<Integer> KEY_LIST_3_TO_DELETE_INT = List.of(5, 6);
    private static final List<Integer> KEY_LIST_4_TO_DELETE_INT = List.of(7, 8);
    private static final List<Integer> KEY_LIST_5_TO_DELETE_INT = List.of(9, 10);
    private static final List<Integer> KEY_LIST_6_TO_DELETE_INT = List.of(11);
    private static final List<String> KEY_LIST_1_TO_DELETE_STRING  = List.of("1", "2");
    private static final List<String> KEY_LIST_2_TO_DELETE_STRING  = List.of("3", "4");
    private static final List<String> KEY_LIST_3_TO_DELETE_STRING  = List.of("5", "6");
    private static final List<String> KEY_LIST_4_TO_DELETE_STRING  = List.of("7", "8");
    private static final List<String> KEY_LIST_5_TO_DELETE_STRING  = List.of("9", "10");
    private static final List<String> KEY_LIST_6_TO_DELETE_STRING  = List.of("11");

    @InjectMocks
    CleanProcessExecService cleanProcessExecService;

    @Mock
    private CleanProcessExecAttrDao cleanProcessExecAttrDao;

    @Mock
    private CleanProcessExecDao cleanProcessExecDao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_ATTR_PARTITION_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_ATTR_PARTITION_SIZE);

        when(cleanProcessExecDao.getProcessExecArchiveSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(KEY_LIST_EMPTY_INT);
        when(cleanProcessExecDao.deleteProcessExecHistoryByDaysThreshold(anyInt(), anyInt()))
                .thenReturn(0);
        when(cleanProcessExecAttrDao.getProcessExecAttrSerialKeys(anyInt(), anyInt()))
                .thenReturn(KEY_LIST_EMPTY_INT);
        when(cleanProcessExecAttrDao.getProcessExecAttrProcessHandleIds(anyInt(), anyInt()))
                .thenReturn(KEY_LIST_EMPTY_STRING);

        when(cleanProcessExecAttrDao.cleanProcessExecAttrHistory(anyInt())).thenReturn(0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanProcessExecService.execute();
        String expectedResult = getResult(0, 0, stopExecutionTime, "ARCHIVED", "PROCESSEXEC") + "; " +
                getResult(0, 0, stopExecutionTime, "CLEAN", "PROCESSEXECHISTORY") + "; " +
                getResult(0, 0, stopExecutionTime, "ARCHIVED", "PROCESSEXECATTR") + "; " +
                getResult(0, 0, stopExecutionTime, "CLEAN", "PROCESSEXECATTRHISTORY") + ";";

        Assertions.assertEquals(expectedResult, actualResult);
        verifyDaoMock(1, 0, 1, 1, 0, 1);
    }

    @Test
    void executeWhenSomeKeysFoundTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(2);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(2);

        when(cleanProcessExecDao.getProcessExecArchiveSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(KEY_LIST_1_INT)
                .thenReturn(KEY_LIST_2_INT)
                .thenReturn(KEY_LIST_3_INT)
                .thenReturn(KEY_LIST_EMPTY_INT);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_1_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_2_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_3_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_4_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_5_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_6_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(1);

        when(cleanProcessExecDao.deleteProcessExecHistoryByDaysThreshold(anyInt(), anyInt())).thenReturn(2);

        when(cleanProcessExecAttrDao.getProcessExecAttrSerialKeys(anyInt(), anyInt()))
                .thenReturn(KEY_LIST_1_INT)
                .thenReturn(KEY_LIST_2_INT)
                .thenReturn(KEY_LIST_3_INT)
                .thenReturn(KEY_LIST_EMPTY_INT);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_1_TO_DELETE_INT), anyInt(), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_2_TO_DELETE_INT), anyInt(),  anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_3_TO_DELETE_INT), anyInt(),  anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_4_TO_DELETE_INT), anyInt(),  anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_5_TO_DELETE_INT), anyInt(),  anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_6_TO_DELETE_INT), anyInt(),  anyInt())).thenReturn(1);

        when(cleanProcessExecAttrDao.getProcessExecAttrProcessHandleIds(anyInt(), anyInt()))
                .thenReturn(KEY_LIST_1_STRING)
                .thenReturn(KEY_LIST_2_STRING)
                .thenReturn(KEY_LIST_3_STRING)
                .thenReturn(KEY_LIST_EMPTY_STRING);

        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_1_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_2_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_3_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_4_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_5_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_6_TO_DELETE_STRING), anyInt())).thenReturn(1);

        when(cleanProcessExecAttrDao.cleanProcessExecAttrHistory(anyInt())).thenReturn(0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanProcessExecService.execute();
        String expectedResult = getResult(11, 0, stopExecutionTime, "ARCHIVED", "PROCESSEXEC") + "; " +
                getResult(2, 0, stopExecutionTime, "CLEAN", "PROCESSEXECHISTORY") + "; " +
                getResult(11, 0, stopExecutionTime, "ARCHIVED", "PROCESSEXECATTR") + "; " +
                getResult(0, 0, stopExecutionTime, "CLEAN", "PROCESSEXECATTRHISTORY") + ";";

        Assertions.assertEquals(expectedResult, actualResult);
        verifyDaoMock(4, 6, 1, 4, 6, 1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(4);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(4);
        when(dbConfigService.getConfigAsIntegerBetween(eq("PROCESSEXECATTR_ARCH_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt()))
                .thenReturn(1000);
        when(dbConfigService.getConfigAsIntegerBetween(eq("PROCESSEXEC_ARCH_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt()))
                .thenReturn(1000);

        when(cleanProcessExecDao.getProcessExecArchiveSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test timeout"))
                .thenThrow(new QueryTimeoutException("Test timeout"))
                .thenReturn(KEY_LIST_1_INT)
                .thenReturn(KEY_LIST_EMPTY_INT);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_1_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);
        when(cleanProcessExecDao.archiveProcessExecBySerialKeys(eq(KEY_LIST_2_TO_DELETE_INT), anyInt(), anyInt()))
                .thenReturn(2);

        when(cleanProcessExecDao.deleteProcessExecHistoryByDaysThreshold(anyInt(), anyInt())).thenReturn(0);

        when(cleanProcessExecAttrDao.getProcessExecAttrSerialKeys(anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test timeout"))
                .thenReturn(KEY_LIST_1_INT)
                .thenReturn(KEY_LIST_EMPTY_INT);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_1_TO_DELETE_INT), anyInt(), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.archiveProcessExecAttrBySerialKeys(
                eq(KEY_LIST_2_TO_DELETE_INT), anyInt(), anyInt())).thenReturn(2);

        when(cleanProcessExecAttrDao.getProcessExecAttrProcessHandleIds(anyInt(), anyInt()))
                .thenReturn(KEY_LIST_1_STRING)
                .thenReturn(KEY_LIST_EMPTY_STRING);

        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_1_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttr(eq(KEY_LIST_2_TO_DELETE_STRING), anyInt())).thenReturn(2);
        when(cleanProcessExecAttrDao.cleanProcessExecAttrHistory(anyInt())).thenReturn(0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = cleanProcessExecService.execute();
        String expectedResult = getResult(4, 2, stopExecutionTime, "ARCHIVED", "PROCESSEXEC") + "; " +
                getResult(0, 0, stopExecutionTime, "CLEAN", "PROCESSEXECHISTORY") + "; " +
                getResult(4, 1, stopExecutionTime, "ARCHIVED", "PROCESSEXECATTR") + "; " +
                getResult(0, 0, stopExecutionTime, "CLEAN", "PROCESSEXECATTRHISTORY") + ";";

        Assertions.assertEquals(expectedResult, actualResult);
        verifyDaoMock(4, 2, 1, 3, 2, 1);
    }

    private void setUpConfigServiceMock() {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_ATTR_BATCH_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_SLEEP_TIME"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_TIME_LIMIT / 2);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_SELECT_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_INSERT_TIMEOUT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_INSERT_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_DELETE_TIMEOUT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_DELETE_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXECATTR_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_RETRY_ATTEMPTS);

        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_ATTR_BATCH_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_SLEEP_TIME"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_TIME_LIMIT / 2);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_SELECT_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_INSERT_TIMEOUT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_INSERT_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_DELETE_TIMEOUT"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_DELETE_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_RETRY_ATTEMPTS);

        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_ARCHIVE_INACTIVITY_DAYS_THRESHOLD);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("PROCESSEXEC_DEL_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_DELETE_INACTIVITY_DAYS_THRESHOLD);
    }

    private void verifyDaoMock(int getSerialKeys, int archiveBySerialKeys, int deleteByTrashHold, int getSerialKeysAttr,
                               int archiveBySerialKeysAttr, int cleanProcessExecAttrHistory) {
        verify(cleanProcessExecDao, times(getSerialKeys))
                .getProcessExecArchiveSerialKeys(anyInt(), anyInt(), anyInt());
        verify(cleanProcessExecDao, times(archiveBySerialKeys))
                .archiveProcessExecBySerialKeys(anyList(), anyInt(), anyInt());

        verify(cleanProcessExecDao, times(deleteByTrashHold))
                .deleteProcessExecHistoryByDaysThreshold(anyInt(), anyInt());

        verify(cleanProcessExecAttrDao, times(getSerialKeysAttr))
                .getProcessExecAttrSerialKeys(anyInt(), anyInt());
        verify(cleanProcessExecAttrDao, times(archiveBySerialKeysAttr))
                .archiveProcessExecAttrBySerialKeys(anyList(), anyInt(), anyInt());

        verify(cleanProcessExecAttrDao, times(cleanProcessExecAttrHistory)).cleanProcessExecAttrHistory(anyInt());
    }

    private String getResult(
            int deleteRowsCount, int failedAttemptsNumber, long stopExecutionTime, String action, String table) {
        return String.format("Records %s: %s %d%s%s", action, table, deleteRowsCount,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
