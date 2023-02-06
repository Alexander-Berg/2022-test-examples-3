package ru.yandex.market.wms.scheduler.service.clean;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;


@DatabaseSetup("/db/service/clean-skuxloc/before.xml")
class CleanSkuLocServiceTest extends SchedulerIntegrationTest {

    @MockBean
    @Autowired
    private DbConfigService dbConfigService;

    @Autowired
    private CleanSkuLocService cleanSkuLocService;

    @Test
    public void testServiceIsNotEnabled() throws InterruptedException {
        when(dbConfigService.getConfigAsBoolean(eq("DEL_EMPTY_QTY_ROWS_ALLOWED"), anyBoolean()))
                .thenReturn(Boolean.FALSE);

        String expected = "Service not enabled";
        String actual = cleanSkuLocService.execute();

        assertEquals(expected, actual);
    }

    @Test
    @ExpectedDatabase(value = "/db/service/clean-skuxloc/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessExecute() throws InterruptedException {

        when(dbConfigService.getConfigAsBoolean(eq("DEL_EMPTY_QTY_ROWS_ALLOWED"), anyBoolean()))
                .thenReturn(Boolean.TRUE);
        when(dbConfigService.getConfigAsInteger(eq("SELECT_EMPTY_QTY_ROWS_DEF_SIZE"), anyInt()))
                .thenReturn(10);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_BATCH_SIZE"), anyInt()))
                .thenReturn(1);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_SLEEP_TIME"), anyInt()))
                .thenReturn(10000);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_EXEC_TIME"), anyInt()))
                .thenReturn(2);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_ARCHIVE_DAYS"), anyInt()))
                .thenReturn(2);

        String message = cleanSkuLocService.execute();

        String expected = "Deleted 1 rows which contain zero values in skuxloc";
        assertEquals(expected, message);

    }

    @Test
    @ExpectedDatabase(value = "/db/service/clean-skuxloc/after-greater-batch-from-serial-keys-size.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testLessDataFromBatchSizeSuccess() throws InterruptedException {
        when(dbConfigService.getConfigAsBoolean(eq("DEL_EMPTY_QTY_ROWS_ALLOWED"), anyBoolean()))
                .thenReturn(Boolean.TRUE);
        when(dbConfigService.getConfigAsInteger(eq("SELECT_EMPTY_QTY_ROWS_DEF_SIZE"), anyInt()))
                .thenReturn(500000);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_BATCH_SIZE"), anyInt()))
                .thenReturn(10);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_SLEEP_TIME"), anyInt()))
                .thenReturn(10000);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_EXEC_TIME"), anyInt()))
                .thenReturn(2);
        when(dbConfigService.getConfigAsInteger(eq("DEL_EMPTY_QTY_ROWS_ARCHIVE_DAYS"), anyInt()))
                .thenReturn(2);

        String message = cleanSkuLocService.execute();

        String expected = "Deleted 0 rows which contain zero values in skuxloc";
        assertEquals(expected, message);
    }
}