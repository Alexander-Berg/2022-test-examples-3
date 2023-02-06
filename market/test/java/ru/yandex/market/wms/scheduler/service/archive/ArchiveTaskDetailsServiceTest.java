package ru.yandex.market.wms.scheduler.service.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.QueryTimeoutException;

import ru.yandex.market.wms.common.service.DbConfigService;
import ru.yandex.market.wms.scheduler.config.SchedulerIntegrationTest;
import ru.yandex.market.wms.scheduler.dao.ArchiveTaskDetailsDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveTaskDetailsServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;
    private static final int KEY_1 = 1;
    private static final int KEY_2 = 2;
    private static final int KEY_3 = 3;
    private static final int KEY_4 = 4;
    private static final List<Integer> KEY_LIST_1 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4));
    private static final List<Integer> KEY_LIST_2 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3));
    private static final List<Integer> KEY_LIST_3 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2));
    private static final List<Integer> EMPTY_KEY_LIST = new ArrayList<>();

    @InjectMocks
    private ArchiveTaskDetailsService archiveService;

    @Mock
    private ArchiveTaskDetailsDao dao;

    @Mock
    private DbConfigService dbConfigService;

    private static Stream<Arguments> provideListsWhenKeysListSmallerThanPartitionSize() {
        return Stream.of(
                Arguments.of(KEY_LIST_1, KEY_LIST_2, 1, 0),
                Arguments.of(KEY_LIST_2, KEY_LIST_3, 2, 0),
                Arguments.of(KEY_LIST_1, KEY_LIST_3, 0, 0)
        );
    }

    private static Stream<Arguments> provideListsWhenKeysListGreaterThanPartitionSize() {
        return Stream.of(
                Arguments.of(KEY_LIST_1, 2, KEY_LIST_2, 3, 1, 0),
                Arguments.of(KEY_LIST_2, 3, KEY_LIST_1, 1, 2, 0)
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @ParameterizedTest
    @MethodSource("provideListsWhenKeysListSmallerThanPartitionSize")
    void executeWhenKeysListSmallerThanPartitionSizeTest(
            List<Integer> taskDetailSerialKeys,
            List<Integer> userActivitySerialKeys,
            int userActivityRowsCnt1,
            int taskDetailContainerIdsRowsCnt
    ) throws InterruptedException {
        setUpDaoMock(taskDetailSerialKeys, userActivitySerialKeys, userActivityRowsCnt1,
                taskDetailSerialKeys.size(), userActivitySerialKeys.size(), false, taskDetailContainerIdsRowsCnt);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(taskDetailSerialKeys.size(),
                userActivitySerialKeys.size() + userActivityRowsCnt1, 0, stopExecutionTime, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1, 1, 1);
        verifyDbConfigServiceMock(1);
    }

    @ParameterizedTest
    @MethodSource("provideListsWhenKeysListGreaterThanPartitionSize")
    void executeWhenKeysListGreaterThanPartitionSizeTest(
            List<Integer> taskDetailSerialKeys,
            int taskDetailRowsCnt1,
            List<Integer> userActivitySerialKeys,
            int userActivityRowsCnt1,
            int userActivityRowsCnt2,
            int taskDetailContainerIdsRowsCnt
    ) throws InterruptedException {
        when(dao.archiveTaskDetailAndUserActivityBySerialKeys(anyList(), anyInt()))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(taskDetailRowsCnt1, userActivityRowsCnt1,
                        taskDetailContainerIdsRowsCnt))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(taskDetailRowsCnt1, userActivityRowsCnt1,
                        taskDetailContainerIdsRowsCnt))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(0, 0, 0));
        when(dao.archiveAdditionalUserActivityBySerialKeys(anyList(), anyInt()))
                .thenReturn(userActivityRowsCnt2).thenReturn(userActivityRowsCnt2).thenReturn(0);
        setUpDaoMock(taskDetailSerialKeys, userActivitySerialKeys, userActivityRowsCnt1,
                taskDetailSerialKeys.size(), userActivitySerialKeys.size(), false, taskDetailContainerIdsRowsCnt);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(2);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(taskDetailRowsCnt1 + taskDetailRowsCnt1,
                userActivityRowsCnt1 + userActivityRowsCnt1 + userActivityRowsCnt2 + userActivityRowsCnt2,
                0, stopExecutionTime, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2, 2, 2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        int archivedRowsCount = EMPTY_KEY_LIST.size();
        setUpDaoMock(EMPTY_KEY_LIST, EMPTY_KEY_LIST, archivedRowsCount, archivedRowsCount, archivedRowsCount, false,
                archivedRowsCount);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(archivedRowsCount, archivedRowsCount, 0, stopExecutionTime, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(1, 1, 0, 0);
        verifyDbConfigServiceMock(1);
    }

    @ParameterizedTest
    @MethodSource("provideListsWhenKeysListSmallerThanPartitionSize")
    void executeWhenDaoThrowsQueryTimeoutExceptionTest(
            List<Integer> taskDetailSerialKeys,
            List<Integer> userActivitySerialKeys,
            int userActivityRowsCnt1,
            int taskDetailContainerIdsRowsCnt
    ) throws InterruptedException {
        setUpDaoMock(taskDetailSerialKeys, userActivitySerialKeys, userActivityRowsCnt1,
                taskDetailSerialKeys.size(), userActivitySerialKeys.size(), true, taskDetailContainerIdsRowsCnt);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(2);
        when(dao.archiveTaskDetailAndUserActivityBySerialKeys(anyList(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(taskDetailSerialKeys.size(), userActivityRowsCnt1,
                        taskDetailContainerIdsRowsCnt))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(0, 0, 0));
        when(dao.archiveAdditionalUserActivityBySerialKeys(eq(userActivitySerialKeys), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(userActivitySerialKeys.size())
                .thenReturn(0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        String expectedResult = getResult(taskDetailSerialKeys.size(),
                userActivitySerialKeys.size() + userActivityRowsCnt1, 2, stopExecutionTime, 0);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMock(2, 2, 2, 2);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMock(
            List<Integer> taskDetailSerialKeys,
            List<Integer> userActivitySerialKeys,
            int userActivityRowsCnt1,
            int taskDetailRowsCnt,
            int userActivityRowsCnt2,
            boolean isRepeatable,
            int taskDetailContainerIdsRowsCnt
    ) {
        when(dao.getTaskDetailSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(taskDetailSerialKeys)
                .thenReturn(isRepeatable ? taskDetailSerialKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.getUserActivitySerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(userActivitySerialKeys)
                .thenReturn(isRepeatable ? userActivitySerialKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveTaskDetailAndUserActivityBySerialKeys(eq(taskDetailSerialKeys), anyInt()))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(taskDetailRowsCnt, userActivityRowsCnt1,
                        taskDetailContainerIdsRowsCnt))
                .thenReturn(isRepeatable ?
                        new ArchiveTaskDetailsDao.StatCounter(taskDetailRowsCnt, userActivityRowsCnt1,
                                taskDetailContainerIdsRowsCnt) :
                        new ArchiveTaskDetailsDao.StatCounter(0, 0, 0))
                .thenReturn(new ArchiveTaskDetailsDao.StatCounter(0, 0, 0));
        when(dao.archiveAdditionalUserActivityBySerialKeys(eq(userActivitySerialKeys), anyInt()))
                .thenReturn(userActivityRowsCnt2)
                .thenReturn(isRepeatable ? userActivityRowsCnt2 : 0)
                .thenReturn(0);
    }

    private void verifyDaoMock(
            int getTaskDetailSerialKeysTimes,
            int getUserActivitySerialKeysTimes,
            int archiveTaskDetailAndUserActivityBySerialKeysTimes,
            int archiveAdditionalUserActivityBySerialKeysTimes
    ) {
        verify(dao, times(getTaskDetailSerialKeysTimes)).getTaskDetailSerialKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(getUserActivitySerialKeysTimes)).getUserActivitySerialKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(archiveTaskDetailAndUserActivityBySerialKeysTimes))
                .archiveTaskDetailAndUserActivityBySerialKeys(anyList(), anyInt());
        verify(dao, times(archiveAdditionalUserActivityBySerialKeysTimes))
                .archiveAdditionalUserActivityBySerialKeys(anyList(), anyInt());
    }

    private void setUpConfigServiceMock() {
        final int defaultBatchSize = 15000;
        final int defaultInactivityDaysThreshold = 30;
        final int defaultPartitionSize = 1000;
        final int defaultSelectTimeout = 20;
        final int defaultArchiveTimeout = 3;
        final int defaultSleepTime = 1;
        final int defaultRetryAttempts = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultInactivityDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultArchiveTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_SELECT_TIMEOUT_SEC"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultPartitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_SELECT_TIMEOUT_SEC"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("TASKDETAIL_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes * 8))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            int taskDetailRowsCnt,
            int userActivityRowsCnt,
            int failedAttemptsNumber,
            long stopExecutionTime,
            int taskDetailContainerIdsRowsCnt) {
        if (taskDetailRowsCnt > 0 || userActivityRowsCnt > 0 || taskDetailContainerIdsRowsCnt > 0) {
            return String.format(
                    "Records moved to SCPRDARC: TASKDETAIL: %d, USERACTIVITY: %d, TASKDETAILCONTAINERIDS: %d%s%s",
                    taskDetailRowsCnt, userActivityRowsCnt, taskDetailContainerIdsRowsCnt,
                    failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                    System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]");
        }
        return "No tasks";
    }
}
