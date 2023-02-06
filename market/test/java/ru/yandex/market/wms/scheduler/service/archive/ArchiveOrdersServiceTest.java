package ru.yandex.market.wms.scheduler.service.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
import ru.yandex.market.wms.scheduler.dao.ArchiveOrdersDao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveOrdersServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;
    private static final String KEY_0 = "0";
    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";
    private static final String KEY_3 = "3";
    private static final String KEY_4 = "4";
    private static final String KEY_5 = "5";
    private static final String KEY_6 = "6";
    private static final String KEY_7 = "7";
    private static final String KEY_8 = "8";
    private static final List<String> KEY_LIST_0 =
            new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7, KEY_8));
    private static final List<String> KEY_LIST_1 =
            new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6, KEY_7));
    private static final List<String> KEY_LIST_2 =
            new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6));
    private static final List<String> KEY_LIST_3 =
            new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, KEY_5));
    private static final List<String> KEY_LIST_4 = new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2, KEY_3, KEY_4));
    private static final List<String> KEY_LIST_5 = new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2, KEY_3));
    private static final List<String> KEY_LIST_6 = new ArrayList<>(Arrays.asList(KEY_0, KEY_1, KEY_2));
    private static final List<String> KEY_LIST_7 = new ArrayList<>(Arrays.asList(KEY_0, KEY_1));
    private static final List<String> KEY_LIST_8 = new ArrayList<>(Collections.singletonList(KEY_0));
    private static final List<String> EMPTY_KEY_LIST = new ArrayList<>();

    @Getter
    @AllArgsConstructor
    public static class SerialKeys {
        List<String> orderKeys;
        List<String> dropIds;
        List<String> serialKeys;
        List<String> unarchivedDropIds;
        List<String> unarchivedDropIdDetails;
        List<String> unarchivedPickDetailKeys;
        List<String> unarchivedOrderDetailKeys;
        List<String> unarchivedProblemOrdersKeys;
        List<String> unarchivedOrderMaxDimensionsKeys;
    }


    @InjectMocks
    private ArchiveOrdersService archiveService;

    @Mock
    private ArchiveOrdersDao dao;

    @Mock
    private DbConfigService dbConfigService;

    private static Stream<Arguments> provideKeyLists() {
        return Stream.of(
                Arguments.of(new SerialKeys(KEY_LIST_0, KEY_LIST_1, KEY_LIST_2, KEY_LIST_3, KEY_LIST_4, KEY_LIST_5,
                        KEY_LIST_6, KEY_LIST_2, KEY_LIST_3)),
                Arguments.of(new SerialKeys(KEY_LIST_2, KEY_LIST_3, KEY_LIST_4, KEY_LIST_5, KEY_LIST_6, KEY_LIST_0,
                        KEY_LIST_1, KEY_LIST_0, KEY_LIST_4)),
                Arguments.of(new SerialKeys(KEY_LIST_2, KEY_LIST_3, KEY_LIST_0, KEY_LIST_1, KEY_LIST_4, KEY_LIST_5,
                        KEY_LIST_6, KEY_LIST_5, KEY_LIST_2))
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @ParameterizedTest
    @MethodSource("provideKeyLists")
    void executeWhenKeysListSmallerThanPartitionSizeTest(SerialKeys serialKeys) throws InterruptedException {
        List<String> unarchivedOrderStatusKeys = KEY_LIST_7;
        List<String> unarchivedOrderKeys = KEY_LIST_8;
        setUpDaoMockForOrders(serialKeys, unarchivedOrderStatusKeys, unarchivedOrderKeys, false);
        setUpDaoMockForDrops(serialKeys.dropIds, serialKeys.unarchivedDropIds, serialKeys.unarchivedDropIdDetails,
                false);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        ArchiveOrdersService.StatCounter statCounter = new ArchiveOrdersService.StatCounter(
                serialKeys.unarchivedPickDetailKeys.size(), serialKeys.unarchivedOrderDetailKeys.size(),
                unarchivedOrderStatusKeys.size(), unarchivedOrderKeys.size(),
                serialKeys.unarchivedDropIdDetails.size(), serialKeys.unarchivedDropIds.size(),
                serialKeys.serialKeys.size(), serialKeys.unarchivedProblemOrdersKeys.size(),
                serialKeys.unarchivedOrderMaxDimensionsKeys.size());
        String expectedResult = getResult(0, stopExecutionTime, statCounter);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMockForOrderRelated(new ExecutionTimes(1, 1, 1, 1, 1, 1, 1, 1, 1));
        verifyDaoMockForDropsAndEmptyOrderStatusHistory(1, 1, 1, 1, 1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        setUpDaoMockForOrders(
                new SerialKeys(EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST,
                        EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST),
                EMPTY_KEY_LIST, EMPTY_KEY_LIST, false);
        setUpDaoMockForDrops(EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, false);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        ArchiveOrdersService.StatCounter statCounter = new ArchiveOrdersService
                .StatCounter(0, 0, 0, 0, 0, 0, 0, 0, 0);
        String expectedResult = getResult(0, stopExecutionTime, statCounter);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMockForOrderRelated(new ExecutionTimes(1, 1, 0, 0, 0, 0, 0, 0, 0));
        verifyDaoMockForDropsAndEmptyOrderStatusHistory(1, 0, 0, 0, 0);
        verifyDbConfigServiceMock(1);
    }

    @ParameterizedTest
    @MethodSource("provideKeyLists")
    void executeWhenDaoThrowsQueryTimeoutExceptionTest(SerialKeys serialKeys) throws InterruptedException {
        List<String> unarchivedOrderStatusKeys = KEY_LIST_7;
        List<String> unarchivedOrderKeys = KEY_LIST_8;
        setUpDaoMockForOrders(serialKeys, unarchivedOrderStatusKeys, unarchivedOrderKeys, true);
        setUpDaoMockForDrops(serialKeys.dropIds, serialKeys.unarchivedDropIds, serialKeys.unarchivedDropIdDetails,
                true);
        when(dao.copyAndDeleteOrderRelated(eq(true), anyList(), any(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(new ArchiveOrdersDao
                        .OrderStatCounter(serialKeys.unarchivedPickDetailKeys.size(),
                        serialKeys.unarchivedOrderDetailKeys.size(), unarchivedOrderStatusKeys.size(),
                        unarchivedOrderKeys.size(), serialKeys.unarchivedProblemOrdersKeys.size(),
                        serialKeys.unarchivedOrderMaxDimensionsKeys.size()))
                .thenReturn(new ArchiveOrdersDao.OrderStatCounter());
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;
        ArchiveOrdersService.StatCounter statCounter = new ArchiveOrdersService.StatCounter(
                serialKeys.unarchivedPickDetailKeys.size(), serialKeys.unarchivedOrderDetailKeys.size(),
                unarchivedOrderStatusKeys.size(), unarchivedOrderKeys.size(),
                serialKeys.unarchivedDropIdDetails.size(), serialKeys.unarchivedDropIds.size(),
                serialKeys.serialKeys.size(), serialKeys.unarchivedProblemOrdersKeys.size(),
                serialKeys.unarchivedOrderMaxDimensionsKeys.size());

        String expectedResult = getResult(1, stopExecutionTime, statCounter);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyDaoMockForOrderRelated(new ExecutionTimes(2, 2, 2, 2, 2, 2, 2, 2, 2));
        verifyDaoMockForDropsAndEmptyOrderStatusHistory(2, 1, 1, 1, 1);
        verifyDbConfigServiceMock(1);
    }

    private void setUpDaoMockForOrders(
            SerialKeys serialKeys,
            List<String> unarchivedOrderStatusKeys,
            List<String> unarchivedOrderKeys,
            boolean isRepeatable
    ) {
        when(dao.prepareOrderKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(serialKeys.orderKeys)
                .thenReturn(isRepeatable ? serialKeys.orderKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareEmptyOrderSerialKeys(anyInt(), anyInt(), anyInt()))
                .thenReturn(serialKeys.serialKeys)
                .thenReturn(isRepeatable ? serialKeys.serialKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedPickDetailKeys(anyList(), anyInt()))
                .thenReturn(serialKeys.unarchivedPickDetailKeys)
                .thenReturn(isRepeatable ? serialKeys.unarchivedPickDetailKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedOrderDetailKeys(anyList(), anyInt()))
                .thenReturn(serialKeys.unarchivedOrderDetailKeys)
                .thenReturn(isRepeatable ? serialKeys.unarchivedOrderDetailKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedOrderStatusHistoryKeys(anyList(), anyInt()))
                .thenReturn(unarchivedOrderStatusKeys)
                .thenReturn(isRepeatable ? unarchivedOrderStatusKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedOrderKeys(anyList(), anyInt()))
                .thenReturn(unarchivedOrderKeys)
                .thenReturn(isRepeatable ? unarchivedOrderKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedOrderKeys(anyList(), anyInt()))
                .thenReturn(serialKeys.unarchivedProblemOrdersKeys)
                .thenReturn(isRepeatable ? serialKeys.unarchivedProblemOrdersKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedOrderKeys(anyList(), anyInt()))
                .thenReturn(serialKeys.unarchivedOrderMaxDimensionsKeys)
                .thenReturn(isRepeatable ? serialKeys.unarchivedOrderMaxDimensionsKeys : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);

        when(dao.copyAndDeleteOrderRelated(
                anyBoolean(), anyList(), any(), anyInt()))
                .thenReturn(new ArchiveOrdersDao.OrderStatCounter(
                        serialKeys.unarchivedPickDetailKeys.size(), serialKeys.unarchivedOrderDetailKeys.size(),
                        unarchivedOrderStatusKeys.size(), unarchivedOrderKeys.size(),
                        serialKeys.unarchivedProblemOrdersKeys.size(),
                        serialKeys.unarchivedOrderMaxDimensionsKeys.size()))
                .thenReturn(isRepeatable ? new ArchiveOrdersDao.OrderStatCounter(
                        serialKeys.unarchivedPickDetailKeys.size(), serialKeys.unarchivedOrderDetailKeys.size(),
                        unarchivedOrderStatusKeys.size(), unarchivedOrderKeys.size(),
                        serialKeys.unarchivedProblemOrdersKeys.size(),
                        serialKeys.unarchivedOrderMaxDimensionsKeys.size())
                        : new ArchiveOrdersDao.OrderStatCounter())
                .thenReturn(new ArchiveOrdersDao.OrderStatCounter());

        when(dao.copyAndDeleteEmptyOrderStatusHistory(anyBoolean(), anyList(), anyInt()))
                .thenReturn(serialKeys.serialKeys.size())
                .thenReturn(isRepeatable ? serialKeys.serialKeys.size() : 0)
                .thenReturn(0);
    }

    private void setUpDaoMockForDrops(
            List<String> dropIds,
            List<String> unarchivedDropIds,
            List<String> unarchivedDropIdDetails,
            boolean isRepeatable
    ) {
        when(dao.prepareDropIds(anyInt(), anyInt(), anyInt()))
                .thenReturn(dropIds)
                .thenReturn(isRepeatable ? dropIds : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedDropIds(anyList(), anyInt()))
                .thenReturn(unarchivedDropIds)
                .thenReturn(isRepeatable ? unarchivedDropIds : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.prepareUnarchivedDropIdDetails(anyList(), anyInt()))
                .thenReturn(unarchivedDropIdDetails)
                .thenReturn(isRepeatable ? unarchivedDropIdDetails : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.copyAndDeleteDrops(anyBoolean(), anyList(), anyList(), anyList(), anyInt()))
                .thenReturn(new ArchiveOrdersDao
                        .DopIdStatCounter(unarchivedDropIdDetails.size(), unarchivedDropIds.size()))
                .thenReturn(isRepeatable ?
                        new ArchiveOrdersDao
                                .DopIdStatCounter(unarchivedDropIdDetails.size(), unarchivedDropIds.size()) :
                        new ArchiveOrdersDao.DopIdStatCounter())
                .thenReturn(new ArchiveOrdersDao.DopIdStatCounter());
    }

    @Getter
    @AllArgsConstructor
    public static class ExecutionTimes {
        int prepareOrderKeysTimes;
        int prepareEmptyOrderSerialKeysTimes;
        int prepareUnarchivedPickDetailKeysTimes;
        int prepareUnarchivedOrderDetailKeysTimes;
        int prepareUnarchivedOrderStatusHistoryKeysTimes;
        int prepareUnarchivedOrderKeysTimes;
        int prepareUnarchivedProblemOrdersKeysTimes;
        int prepareUnarchivedOrderMaxDimensionsKeysTimes;
        int copyAndDeleteOrderRelatedTimes;
    }


    private void verifyDaoMockForOrderRelated(ExecutionTimes executionTimes) {
        verify(dao, times(executionTimes.prepareOrderKeysTimes))
                .prepareOrderKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(executionTimes.prepareEmptyOrderSerialKeysTimes))
                .prepareEmptyOrderSerialKeys(anyInt(), anyInt(), anyInt());
        verify(dao, times(executionTimes.prepareUnarchivedPickDetailKeysTimes))
                .prepareUnarchivedPickDetailKeys(anyList(), anyInt());
        verify(dao, times(executionTimes.prepareUnarchivedOrderDetailKeysTimes))
                .prepareUnarchivedOrderDetailKeys(anyList(), anyInt());
        verify(dao, times(executionTimes.prepareUnarchivedOrderStatusHistoryKeysTimes))
                .prepareUnarchivedOrderStatusHistoryKeys(anyList(), anyInt());
        verify(dao, times(executionTimes.prepareUnarchivedOrderKeysTimes))
                .prepareUnarchivedOrderKeys(anyList(), anyInt());
        verify(dao, times(executionTimes.prepareUnarchivedProblemOrdersKeysTimes))
                .prepareUnarchivedProblemOrdersKeys(anyList(), anyInt());
        verify(dao, times(executionTimes.prepareUnarchivedOrderMaxDimensionsKeysTimes))
                .prepareUnarchivedOrderMaxDimensionsKeys(anyList(), anyInt());

        verify(dao, times(executionTimes.copyAndDeleteOrderRelatedTimes))
                .copyAndDeleteOrderRelated(anyBoolean(), anyList(), any(), anyInt());

    }

    private void verifyDaoMockForDropsAndEmptyOrderStatusHistory(
            int prepareDropIdsTimes,
            int prepareUnarchivedDropIdsTimes,
            int prepareUnarchivedDropIdDetailsTimes,
            int copyAndDeleteDropsTimes,
            int copyAndDeleteEmptyOrderStatusHistoryTimes
    ) {
        verify(dao, times(prepareDropIdsTimes)).prepareDropIds(anyInt(), anyInt(), anyInt());
        verify(dao, times(prepareUnarchivedDropIdsTimes)).prepareUnarchivedDropIds(anyList(), anyInt());
        verify(dao, times(prepareUnarchivedDropIdDetailsTimes)).prepareUnarchivedDropIdDetails(anyList(), anyInt());
        verify(dao, times(copyAndDeleteDropsTimes))
                .copyAndDeleteDrops(anyBoolean(), anyList(), anyList(), anyList(), anyInt());
        verify(dao, times(copyAndDeleteEmptyOrderStatusHistoryTimes))
                .copyAndDeleteEmptyOrderStatusHistory(anyBoolean(), anyList(), anyInt());
    }

    private void setUpConfigServiceMock() {
        final int delArcOrderAllowedDefault = 1;
        final int delArcOrderBatchSize = 30000;
        final int delArcDropidBatchSize = 30000;
        final int delArcOrderPartSize = 1000;
        final int delArcDropidPartSize = 1000;
        final int delArcOrderPeriodDays = 3;
        final int delArcOrderTimeout = 20;
        final int delArcDropidTimeout = 20;
        final int selOrderTimeout = 60;
        final int defaultSleepTime = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_ALLOWED"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcOrderAllowedDefault);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_DROPID_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcDropidBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcOrderBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_PERIOD_DAYS"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcOrderPeriodDays);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_DROPID_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcDropidTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcOrderTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("SEL_ORDER_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(selOrderTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_DROPID_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcDropidPartSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(delArcOrderPartSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_ALLOWED"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_DROPID_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_PERIOD_DAYS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_DROPID_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("SEL_ORDER_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_DROPID_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("DEL_ARC_ORDER_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(11))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            int failedAttemptsNumber,
            long stopExecutionTime,
            ArchiveOrdersService.StatCounter statCounter
    ) {
        StringBuilder result = new StringBuilder("Records moved to SCPRDARC:");
        result.append(" DROPIDDETAIL: ").append(statCounter.getDropIdDetailRowsCnt());
        result.append(" DROPID: ").append(statCounter.getDropIdRowsCnt());
        result.append(" PICKDETAILS: ").append(statCounter.getPickDetailsRowsCnt());
        result.append(" ORDERDETAILS: ").append(statCounter.getOrderDetailsRowsCnt());
        result.append(" ORDERSTATUSHISTORY: ").append(statCounter.getOrderStatusHistoryRowsCnt());
        result.append(" ORDERS: ").append(statCounter.getOrdersRowsCnt());
        result.append(" EMPTY_ORDERSTATUSHISTORY: ").append(statCounter.getEmptyOrderStatusHistoryRowsCnt());
        result.append(" ProblemOrders: ").append(statCounter.getProblemOrdersRowsCnt());
        result.append(" ORDER_MAX_DIMENSIONS: ").append(statCounter.getOrderMaxDimensionsRowsCnt());
        if (failedAttemptsNumber > 0) {
            result.append(" number of failed attempts: ").append(failedAttemptsNumber);
        }
        if (System.currentTimeMillis() > stopExecutionTime) {
            result.append(" [overtime]");
        }
        return result.toString();
    }
}
