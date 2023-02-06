package ru.yandex.market.wms.scheduler.service.archive;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.dao.QueryTimeoutException;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.dao.ArchiveAssignmentsDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveAssignmentsServiceTest {
    private static final int DEFAULT_TIME_LIMIT = 120_000;
    @Mock
    private ArchiveAssignmentsDao dao;
    @Mock
    private DbConfigService dbConfigService;
    @InjectMocks
    private ArchiveAssignmentsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void setUpConfigServiceMock(int batchSize, int partitionSize) {
        final int defaultSelectTimeout = 20;
        final int defaultArchiveTimeout = 3;
        final int defaultRetryAttempts = 3;
        final int defaultSleepTime = 10_000;

        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(batchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(partitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_SEL_TIMEOUT_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_ARC_TIMEOUT_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultArchiveTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
        when(dbConfigService.getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
    }

    @Test
    public void testArchiveStandard() throws InterruptedException {
        setUpConfigServiceMock(10_000, 1000);
        List<String> assignmentNumbers = List.of("A11", "A12", "A13");
        when(dao.findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(assignmentNumbers);
        when(dao.moveToArchive(anyList(), anyInt()))
                .thenAnswer((InvocationOnMock invocationOnMock) -> {
                    List<String> list = invocationOnMock.getArgument(0);
                    return list.size();
                });
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(assignmentNumbers.size(), 0, stopExecutionTime);
        String actualResult = service.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verify(dao, times(1)).findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt());
        verify(dao, times(1)).moveToArchive(anyList(), anyInt());
        verifyDbConfigServiceMock();
    }

    @Test
    public void testArchiveNoTasks() throws InterruptedException {
        setUpConfigServiceMock(10_000, 1000);
        when(dao.findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(Collections.emptyList());
        when(dao.moveToArchive(anyList(), anyInt()))
                .thenAnswer((InvocationOnMock invocationOnMock) -> {
                    List<String> list = invocationOnMock.getArgument(0);
                    return list.size();
                });
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(0, 0, stopExecutionTime);
        String actualResult = service.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verify(dao, times(1)).findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt());
        verify(dao, times(0)).moveToArchive(anyList(), anyInt());
        verifyDbConfigServiceMock();
    }

    @Test
    public void testArchiveMultiplePartitions() throws InterruptedException {
        setUpConfigServiceMock(100, 3);
        List<String> assignmentNumbers = List.of("A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10");
        when(dao.findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt()))
                .thenReturn(assignmentNumbers)
                .thenReturn(Collections.emptyList());
        when(dao.moveToArchive(anyList(), anyInt()))
                .thenAnswer((InvocationOnMock invocationOnMock) -> {
                    List<String> list = invocationOnMock.getArgument(0);
                    return list.size();
                });
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(assignmentNumbers.size(), 0, stopExecutionTime);
        String actualResult = service.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verify(dao, times(2)).findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt());
        verify(dao, times(4)).moveToArchive(anyList(), anyInt());
        verifyDbConfigServiceMock();
    }

    @Test
    public void testArchiveWithExceptions() throws InterruptedException {
        setUpConfigServiceMock(100, 30);
        List<String> assignmentNumbers = List.of("A01", "A02", "A03", "A04", "A05", "A06", "A07", "A08", "A09", "A10");
        when(dao.findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt()))
                .thenThrow(new QueryTimeoutException("Read Timeout"))
                .thenReturn(assignmentNumbers)
                .thenReturn(assignmentNumbers)
                .thenReturn(Collections.emptyList());
        when(dao.moveToArchive(anyList(), anyInt()))
                .thenThrow(new QueryTimeoutException("Write Timeout"))
                .thenAnswer((InvocationOnMock invocationOnMock) -> {
                    List<String> list = invocationOnMock.getArgument(0);
                    return list.size();
                });
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(assignmentNumbers.size(), 2, stopExecutionTime);
        String actualResult = service.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verify(dao, times(3)).findAssignmentNumbersToArchive(anyInt(), anyInt(), anyInt());
        verify(dao, times(2)).moveToArchive(anyList(), anyInt());
        verifyDbConfigServiceMock();
    }

    private String getResult(
            int assignmentsMovedCnt,
            int failedAttemptsNumber,
            long stopExecutionTime) {
        if (assignmentsMovedCnt > 0) {
            return String.format("Records moved to SCPRDARC: ASSIGNMENTS: %d%s%s",
                    assignmentsMovedCnt,
                    failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                    System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
        }
        return "No tasks";
    }

    private void verifyDbConfigServiceMock() {
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_SEL_TIMEOUT_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_ARC_TIMEOUT_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(1))
                .getConfigAsIntegerBetween(eq("ARC_ASSIGNMENTS_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(8))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }
}
