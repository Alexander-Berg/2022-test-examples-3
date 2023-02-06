package ru.yandex.market.wms.scheduler.service.clean;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.QueryTimeoutException;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.QuartzHistoryDao;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CleanQuartzHistoryServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 10_000;
    private static final int DELETION_ENABLED = 1;
    private static final int DELETION_NOT_ENABLED = 0;

    @Autowired
    private CleanQuartzHistoryService service;

    @SpyBean
    @Autowired
    private QuartzHistoryDao dao;

    @MockBean
    @Autowired
    private DbConfigService dbConfigService;

    @BeforeEach
    void setUp() {
        Mockito.reset(dbConfigService);
        Mockito.reset(dao);
    }

    @Test
    void executeWhenSomeKeysFoundTestWithMockDaoTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED);
        setUpDaoMock(3, 3);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(6, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(3);
        verifyDbConfigServiceMock(1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-quartz-history/before.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-quartz-history/after.xml",
            connection = "schedulerConnection", assertionMode = NON_STRICT_UNORDERED)
    void executeWhenSomeKeysFoundTestWithRealMethodDaoTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED);
        when(dao.deleteHistoryByDays(anyInt(), anyInt())).thenCallRealMethod();

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(2, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() {
        setUpConfigServiceMock(DELETION_ENABLED);
        when(dao.deleteHistoryByDays(anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Test")).thenReturn(0);

        assertThrows(
                QueryTimeoutException.class,
                () -> service.execute(),
                "Test"
        );

        verifyDaoMock(1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundWithMockDaoTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED);
        setUpDaoMock(0, 0);

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/clean/clean-quartz-history/after.xml", connection = "schedulerConnection")
    @ExpectedDatabase(value = "/db/dao/clean/clean-quartz-history/after.xml",
            connection = "schedulerConnection", assertionMode = NON_STRICT_UNORDERED)
    void executeWhenKeysNotFoundWithRealMethodDaoTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_ENABLED);
        when(dao.deleteHistoryByDays(anyInt(), anyInt())).thenCallRealMethod();

        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        String actualResult = service.execute();
        String expectedResult = getResult(0, stopExecutionTime);
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDeletionNotAllowedTest() throws InterruptedException {
        setUpConfigServiceMock(DELETION_NOT_ENABLED);

        String actualResult = service.execute();
        String expectedResult = "Deleting old history records in QRTZ_HISTORY not enabled";
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(0);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(int deletedRows1, int deletedRows2) {
        when(dao.deleteHistoryByDays(anyInt(), anyInt()))
                .thenReturn(deletedRows1).thenReturn(deletedRows2).thenReturn(0);
    }

    private void verifyDaoMock(int deleteTimes) {
        verify(dao, times(deleteTimes)).deleteHistoryByDays(anyInt(), anyInt());
    }

    private void setUpConfigServiceMock(int deletionEnabled) {
        final int defaultBatchSize = 30_000;
        final int defaultDaysThreshold = 90;

        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_ENABLED"),
                anyInt(), anyInt(), anyInt())).thenReturn(deletionEnabled);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_ENABLED"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_QRTZ_HISTORY_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 4))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(int deletedRowsCounter, long stopExecutionTime) {
        return String.format("Deleted %s old history records in QRTZ_HISTORY%s", deletedRowsCounter,
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
    }
}
