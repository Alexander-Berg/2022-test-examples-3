package ru.yandex.market.wms.scheduler.service.archive;

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
import ru.yandex.market.wms.scheduler.dao.ArchiveWaveDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveWaveServiceTest extends SchedulerIntegrationTest {

    private static final int TIME_LIMIT = 10_000;
    private static final List<String> KEY_LIST_1 =
            List.of("WAVE001", "WAVE002", "WAVE003", "WAVE004", "WAVE005", "WAVE006");
    private static final List<String> KEY_LIST_2 = List.of("WAVE006", "WAVE007", "WAVE008");
    private static final List<String> EMPTY_KEY_LIST = List.of();

    @InjectMocks
    private ArchiveWaveService service;

    @Mock
    private ArchiveWaveDao dao;

    @Mock
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void executeWhenSomeKeysFoundTest() throws InterruptedException {
        int archiveTimes = 3;
        int waveCounter = 3;
        int waveDetailCounter = 2;
        setUpConfigServiceMock(3);
        setUpDaoMock(KEY_LIST_1, KEY_LIST_2, waveCounter, waveDetailCounter, waveCounter, waveDetailCounter);

        long stopExecutionTime = System.currentTimeMillis() + TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(waveCounter * archiveTimes, waveDetailCounter * archiveTimes,
                waveCounter * archiveTimes, waveDetailCounter * archiveTimes, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, archiveTimes);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        int archiveTimes = 3;
        int waveCounter = 3;
        int waveDetailCounter = 2;
        setUpConfigServiceMock(4);
        when(dao.getKeyList(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test")).thenReturn(KEY_LIST_1).thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveByKeyList(anyList(), anyInt())).thenReturn(new ArchiveWaveDao.WaveStatCounter(
                waveCounter, waveDetailCounter, waveCounter, waveDetailCounter));

        long stopExecutionTime = System.currentTimeMillis() + TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(waveCounter * archiveTimes, waveDetailCounter * archiveTimes,
                waveCounter * archiveTimes, waveDetailCounter * archiveTimes, 1, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3, archiveTimes);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        setUpConfigServiceMock(3);
        setUpDaoMock(EMPTY_KEY_LIST, EMPTY_KEY_LIST, 0, 0, 0, 0);

        long stopExecutionTime = System.currentTimeMillis() + TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(0, 0, 0, 0, 0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(
            List<String> keyList1,
            List<String> keyList2,
            int waveArcRowsCounter,
            int waveDetailArcRowsCounter,
            int waveDelRowsCounter,
            int waveDetailDelRowsCounter
    ) {
        when(dao.getKeyList(anyInt(), anyInt(), anyInt()))
                .thenReturn(keyList1).thenReturn(keyList2).thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveByKeyList(anyList(), anyInt())).thenReturn(new ArchiveWaveDao.WaveStatCounter(
                        waveArcRowsCounter, waveDetailArcRowsCounter, waveDelRowsCounter, waveDetailDelRowsCounter));
    }

    private void verifyDaoMock(int getTimes, int archiveTimes) {
        verify(dao, times(getTimes)).getKeyList(anyInt(), anyInt(), anyInt());
        verify(dao, times(archiveTimes)).archiveByKeyList(anyList(), anyInt());
    }

    private void setUpConfigServiceMock(int partitionSize) {
        final int daysThreshold = 30;
        final int batchSize = 6;
        final int failureSleepTime = 0;
        final int archivingTimeout = 3;
        final int selectionTimeout = 30;
        final int maxRetryAttemptsNumber = 3;

        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(daysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(batchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(partitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_FAILURE_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(failureSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_ARCHIVE_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(archivingTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(selectionTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("WAVE_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(maxRetryAttemptsNumber);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_FAILURE_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_ARCHIVE_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("WAVE_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 8))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            int waveArcRowsCounter,
            int waveDetailArcRowsCounter,
            int waveDelRowsCounter,
            int waveDetailDelRowsCounter,
            int failedAttemptsNumber,
            long stopExecutionTime
    ) {
        return String.format("Rows in WAVE/WAVEDETAIL moved to SCPRDARC: %d/%d, deleted from SCPRD: %d/%d%s%s",
                waveArcRowsCounter, waveDetailArcRowsCounter, waveDelRowsCounter, waveDetailDelRowsCounter,
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
