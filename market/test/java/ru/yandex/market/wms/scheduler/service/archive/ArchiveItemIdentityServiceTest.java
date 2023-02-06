package ru.yandex.market.wms.scheduler.service.archive;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.ArchiveItemIdentityDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArchiveItemIdentityServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_BATCH_SIZE = 1000;
    private static final int DEFAULT_PARTITION_SIZE = 1000;
    private static final int DEFAULT_DAYS_THRESHOLD = 90;
    private static final int DEFAULT_TIME_LIMIT = 120_000;
    private static final int DEFAULT_ARCHIVING_TIMEOUT = 10;
    private static final int DEFAULT_SELECTION_TIMEOUT = 30;

    private final List<String> serialKeyList = List.of("1", "2", "3");
    private final List<String> emptyKeyList = Collections.emptyList();

    @Mock
    private ArchiveItemIdentityDao archiveItemIdentityDao;

    @Mock
    private DbConfigService dbConfigService;

    @InjectMocks
    private ArchiveItemIdentityService archiveItemIdentityService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Положительный кейс, размер списка serialKey меньше размера партиции
     */
    @Test
    void executeWhenThereIsDataToArchiveTest() throws InterruptedException {
        setUpMocks();
        when(archiveItemIdentityDao.getRowsForArchive(anyInt(), anyInt(), anyInt())).thenReturn(serialKeyList);
        when(archiveItemIdentityDao.archiveBySerialKeys(eq(serialKeyList), anyInt())).thenReturn(3);

        String expectedResult = "3 records were moved to archive in ITEM_IDENTITY";
        Assertions.assertEquals(expectedResult, archiveItemIdentityService.execute());
        verify(archiveItemIdentityDao, times(1))
                .getRowsForArchive(DEFAULT_BATCH_SIZE, DEFAULT_DAYS_THRESHOLD, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveItemIdentityDao, times(1))
                .archiveBySerialKeys(serialKeyList, DEFAULT_ARCHIVING_TIMEOUT);
        verifyMocks();
    }

    /**
     * Положительный кейс, размер списка serialKey больше размера партиции
     */
    @Test
    void executeWhenThereIsDataToArchiveAndSmallPartitionSizeTest() throws InterruptedException {
        setUpMocks();
        final int batchSize = 2;
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(batchSize);
        when(archiveItemIdentityDao.getRowsForArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(serialKeyList).thenReturn(emptyKeyList);
        when(archiveItemIdentityDao.archiveBySerialKeys(anyList(), anyInt()))
                .thenReturn(batchSize);

        String expectedResult = "2 records were moved to archive in ITEM_IDENTITY";
        Assertions.assertEquals(expectedResult, archiveItemIdentityService.execute());
        verify(archiveItemIdentityDao, times(1))
                .getRowsForArchive(batchSize, DEFAULT_DAYS_THRESHOLD, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveItemIdentityDao, times(1))
                .archiveBySerialKeys(anyList(), eq(DEFAULT_ARCHIVING_TIMEOUT));
        verifyMocks();
    }

    /**
     * Положительный кейс, список serialKey пуст
     */
    @Test
    void executeWhenThereIsNoDataToArchiveTest() throws InterruptedException {
        setUpMocks();
        when(archiveItemIdentityDao.getRowsForArchive(anyInt(), anyInt(), anyInt())).thenReturn(emptyKeyList);
        when(archiveItemIdentityDao.archiveBySerialKeys(eq(emptyKeyList), anyInt())).thenReturn(0);

        String expectedResult = "Nothing to archive in ITEM_IDENTITY table";
        Assertions.assertEquals(expectedResult, archiveItemIdentityService.execute());
        verify(archiveItemIdentityDao, times(1))
                .getRowsForArchive(DEFAULT_BATCH_SIZE, DEFAULT_DAYS_THRESHOLD, DEFAULT_SELECTION_TIMEOUT);
        verify(archiveItemIdentityDao, times(0))
                .archiveBySerialKeys(anyList(), anyInt());
        verifyMocks();
    }

    private void setUpMocks() {
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_SAMPLE_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_PARTITION_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_BATCH_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_ARCHIVE_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_DAYS_THRESHOLD);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_ARCHIVE_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_ARCHIVING_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SELECTION_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_EXECUTION_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
    }

    private void verifyMocks() {
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_SAMPLE_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ITEM_IDENTITY_ARCH_ARCHIVE_DAYS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ITEM_IDENTITY_ARCH_ARCHIVE_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ITEM_IDENTITY_ARCH_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1)).getConfigAsIntegerBetween(
                eq("ITEM_IDENTITY_ARCH_EXECUTION_TIME"), anyInt(), anyInt(), anyInt());
    }
}
