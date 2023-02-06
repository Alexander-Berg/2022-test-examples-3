package ru.yandex.market.wms.scheduler.service.archive;

import java.util.ArrayList;
import java.util.Arrays;
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
import ru.yandex.market.wms.scheduler.dao.ArchiveLogisticUnitDao;
import ru.yandex.market.wms.scheduler.utils.LogMessages;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveLogisticUnitServiceTest extends SchedulerIntegrationTest {

    private static final int ARCHIVING_IS_ALLOWED = 1;
    private static final int DEFAULT_SAMPLE_SIZE = 50000;
    private static final int DEFAULT_PARTITION_SIZE = 1000;
    private static final int DEFAULT_ARCHIVE_DAYS = 90;
    private static final int DEFAULT_SELECTION_TIMEOUT = 20;
    private static final int DEFAULT_ARCHIVING_TIMEOUT = 3;
    private static final int MAX_PARTITION_SIZE = 2100;
    private static final int MIN_PARTITION_SIZE = 2;
    private static final int DEFAULT_EXECUTION_TIME_LIMIT = 5_000;
    private static final int DEFAULT_SLEEP_TIME = 1;
    private static final int DEFAULT_RETRY_ATTEMPTS = 1;
    private static final List<Integer> SERIAL_KEY_LIST = new ArrayList<>(Arrays.asList(1, 2, 3));
    private static final List<Integer> EMPTY_KEY_LIST = new ArrayList<>();

    @InjectMocks
    private ArchiveLogisticUnitService service;

    @Mock
    private ArchiveLogisticUnitDao archiveLogisticUnitDao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Положительный кейс, размер списка serialKey меньше размера партиции
     */
    @Test
    void executeWhenThereIsDataToArchiveTest() throws InterruptedException {
        int archivedRowsCount = SERIAL_KEY_LIST.size();
        setUpConfigServiceMock();
        when(archiveLogisticUnitDao.getSerialKeysForArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(SERIAL_KEY_LIST).thenReturn(EMPTY_KEY_LIST);
        when(archiveLogisticUnitDao.archiveBySerialKeys(eq(SERIAL_KEY_LIST), anyInt())).thenReturn(archivedRowsCount);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_EXECUTION_TIME_LIMIT;
        String expectedResult = getResult(archivedRowsCount, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, service.execute());

        verify(archiveLogisticUnitDao, times(1))
                .getSerialKeysForArchive(DEFAULT_SAMPLE_SIZE, DEFAULT_ARCHIVE_DAYS, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveLogisticUnitDao, times(1))
                .archiveBySerialKeys(SERIAL_KEY_LIST, DEFAULT_ARCHIVING_TIMEOUT);
        verifyDbConfigServiceMock();
    }

    /**
     * Положительный кейс, размер списка serialKey больше размера партиции
     */
    @Test
    void executeWhenThereIsDataToArchiveAndSmallPartitionSizeTest() throws InterruptedException {
        int archivedRowsCount1 = 2;
        int archivedRowsCount2 = 1;
        setUpConfigServiceMock();
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(2);
        when(archiveLogisticUnitDao.getSerialKeysForArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(SERIAL_KEY_LIST).thenReturn(EMPTY_KEY_LIST);
        when(archiveLogisticUnitDao.archiveBySerialKeys(anyList(), anyInt()))
                .thenReturn(archivedRowsCount1).thenReturn(archivedRowsCount2).thenReturn(0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_EXECUTION_TIME_LIMIT;
        String expectedResult = getResult(archivedRowsCount1 + archivedRowsCount2, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, service.execute());

        verify(archiveLogisticUnitDao, times(2))
                .getSerialKeysForArchive(DEFAULT_SAMPLE_SIZE, DEFAULT_ARCHIVE_DAYS, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveLogisticUnitDao, times(2))
                .archiveBySerialKeys(anyList(), eq(DEFAULT_ARCHIVING_TIMEOUT));
        verifyDbConfigServiceMock();
    }

    /**
     * Положительный кейс, список serialKey пуст
     */
    @Test
    void executeWhenThereIsNoDataToArchiveTest() throws InterruptedException {
        int archivedRowsCount = EMPTY_KEY_LIST.size();
        setUpConfigServiceMock();
        when(archiveLogisticUnitDao.getSerialKeysForArchive(anyInt(), anyInt(), anyInt())).thenReturn(EMPTY_KEY_LIST);
        when(archiveLogisticUnitDao.archiveBySerialKeys(eq(EMPTY_KEY_LIST), anyInt())).thenReturn(archivedRowsCount);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_EXECUTION_TIME_LIMIT;
        String expectedResult = getResult(archivedRowsCount, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, service.execute());
        verify(archiveLogisticUnitDao, times(1))
                .getSerialKeysForArchive(DEFAULT_SAMPLE_SIZE, DEFAULT_ARCHIVE_DAYS, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveLogisticUnitDao, times(0))
                .archiveBySerialKeys(anyList(), anyInt());
        verifyDbConfigServiceMock();
    }

    /**
     * Положительный кейс, архивация не разрешена
     */
    @Test
    void executeWhenArchivingIsNotAllowedTest() throws InterruptedException {
        when(dbConfigService.getConfigAsBoolean(eq("ARC_LGST_UNIT_ROWS_ALLOWED"), anyBoolean()))
                .thenReturn(false);

        String expectedResult = LogMessages.LOG_I001;
        Assertions.assertEquals(expectedResult, service.execute());
        verify(archiveLogisticUnitDao, times(0))
                .getSerialKeysForArchive(anyInt(), anyInt(), anyInt());
        verify(archiveLogisticUnitDao, times(0))
                .archiveBySerialKeys(anyList(), anyInt());
        verifyDbConfigServiceMock();
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int archivedRowsCount = SERIAL_KEY_LIST.size();
        setUpConfigServiceMock();
        when(archiveLogisticUnitDao.getSerialKeysForArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(SERIAL_KEY_LIST)
                .thenReturn(SERIAL_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(archiveLogisticUnitDao.archiveBySerialKeys(eq(SERIAL_KEY_LIST), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(archivedRowsCount)
                .thenReturn(0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_EXECUTION_TIME_LIMIT;
        String expectedResult = getResult(archivedRowsCount, 1, stopExecutionTime);
        Assertions.assertEquals(expectedResult, service.execute());

        verify(archiveLogisticUnitDao, times(2))
                .getSerialKeysForArchive(DEFAULT_SAMPLE_SIZE, DEFAULT_ARCHIVE_DAYS, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveLogisticUnitDao, times(2))
                .archiveBySerialKeys(SERIAL_KEY_LIST, DEFAULT_ARCHIVING_TIMEOUT);
        verifyDbConfigServiceMock();
    }

    private void setUpConfigServiceMock() {
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_ALLOWED"),
                anyInt(), anyInt(), anyInt())).thenReturn(ARCHIVING_IS_ALLOWED);
        when(dbConfigService.getConfigAsIntegerBetween("ARC_LGST_UNIT_ROWS_BATCH_SIZE",
                DEFAULT_PARTITION_SIZE, MIN_PARTITION_SIZE, MAX_PARTITION_SIZE)).thenReturn(DEFAULT_PARTITION_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_SAMPLE_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SAMPLE_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_ARCHIVE_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_ARCHIVE_DAYS);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_ARCHIVE_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_ARCHIVING_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SELECTION_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_EXECUTION_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_EXECUTION_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SLEEP_TIME);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_LGST_UNIT_ROWS_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_RETRY_ATTEMPTS);
    }

    private void verifyDbConfigServiceMock() {
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_ALLOWED"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_SAMPLE_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_ARCHIVE_DAYS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_ARCHIVE_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_EXECUTION_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ARC_LGST_UNIT_ROWS_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(9))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(int archivedRowsCount, int failedAttemptsNumber, long stopExecutionTime) {
        return String.format("%d records were moved to archive in logistic_unit%s%s",
                archivedRowsCount,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
