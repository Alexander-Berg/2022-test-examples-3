package ru.yandex.market.wms.scheduler.service.clean;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DatabaseSetup("/db/dao/clean-skuxloc/before.xml")
class CleanSkuLocServiceTest extends SchedulerIntegrationTest {

    private static final boolean DELETION_ENABLED = true;
    private static final int DEFAULT_ARCHIVE_DAYS = 2;
    private static final int DEFAULT_BATCH_SIZE = 50_000;
    private static final int DEFAULT_PARTITION_SIZE = 1;
    private static final int MAX_PARTITION_SIZE = 2100;
    private static final int MIN_PARTITION_SIZE = 1;
    private static final int DEFAULT_SLEEP_TIME = 10_000;
    private static final int DEFAULT_DELETION_TIMEOUT = 5;
    private static final int DEFAULT_SELECTION_TIMEOUT = 20;
    private static final int DEFAULT_ITERATION_TIME_LIMIT = 2000;
    private static final int DEFAULT_EXECUTION_TIME_LIMIT = 120_000;

    @MockBean
    @Autowired
    private DbConfigService dbConfigService;

    @Autowired
    private CleanSkuLocService cleanSkuLocService;

    @Test
    public void testServiceIsNotEnabled() throws InterruptedException {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DELETION_ENABLED"), anyInt(), anyInt(), anyInt())).thenReturn(0);
        String expected = "Service not enabled";
        String actual = cleanSkuLocService.execute();
        assertEquals(expected, actual);
    }

    @Test
    @ExpectedDatabase(value = "/db/dao/clean-skuxloc/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    public void testSuccessExecute() throws InterruptedException {
        setMocks();
        String message = cleanSkuLocService.execute();
        String expected = "Records clean: SKUXLOC 1";
        assertEquals(expected, message);

    }

    private void setMocks() {
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DELETION_ENABLED"), anyInt(), anyInt(), anyInt())).thenReturn(1);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_BATCH_SIZE"), anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_BATCH_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_PARTITION_SIZE"), anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_PARTITION_SIZE);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_ARCHIVE_DAYS"), anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_ARCHIVE_DAYS);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_SLEEP_TIME"), anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SLEEP_TIME);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_TIMEOUT"), anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_DELETION_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_SELECTION_TIMEOUT"), anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_SELECTION_TIMEOUT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_ITERATION_TIME"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_ITERATION_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(
                eq("SKU_LOC_DEL_EXECUTION_TIME"), anyInt(), anyInt(), anyInt()))
                .thenReturn(DEFAULT_EXECUTION_TIME_LIMIT);
    }
}
