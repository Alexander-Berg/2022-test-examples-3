package ru.yandex.market.wms.scheduler.service.archive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import ru.yandex.market.wms.scheduler.dao.ArchiveReceiptDetailDao;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveReceiptDetailServiceTest extends SchedulerIntegrationTest {

    private static final int DEFAULT_TIME_LIMIT = 5_000;
    private static final String KEY_1 = "1";
    private static final String KEY_2 = "2";
    private static final String KEY_3 = "3";
    private static final String KEY_4 = "4";
    private static final String KEY_5 = "5";
    private static final String KEY_6 = "6";
    private static final List<String> KEY_LIST_1 =
            new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4, KEY_5, KEY_6));
    private static final List<String> KEY_LIST_2 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4, KEY_5));
    private static final List<String> KEY_LIST_3 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4));
    private static final List<String> KEY_LIST_4 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3));
    private static final List<String> KEY_LIST_5 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2));
    private static final List<String> KEY_LIST_6 = new ArrayList<>(Collections.singletonList(KEY_1));
    private static final List<String> KEY_LIST_7 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3));
    private static final List<String> KEY_LIST_8 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4));
    private static final List<String> KEY_LIST_9 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4, KEY_5,
            KEY_6));
    private static final List<String> KEY_LIST_10 = new ArrayList<>(Arrays.asList(KEY_1, KEY_2, KEY_3, KEY_4, KEY_5,
            KEY_6));
    private static final List<String> EMPTY_KEY_LIST = new ArrayList<>();

    @InjectMocks
    private ArchiveReceiptDetailService archiveService;

    @Mock
    private ArchiveReceiptDetailDao dao;

    @Mock
    private DbConfigService dbConfigService;

    private static Stream<Arguments> provideLists() {
        return Stream.of(
                Arguments.of(KEY_LIST_1, KEY_LIST_2, KEY_LIST_3, KEY_LIST_4, KEY_LIST_5, KEY_LIST_6, KEY_LIST_7,
                        KEY_LIST_8, KEY_LIST_9, KEY_LIST_10),
                Arguments.of(KEY_LIST_3, KEY_LIST_6, KEY_LIST_1, KEY_LIST_4, KEY_LIST_2, KEY_LIST_5, KEY_LIST_7,
                        KEY_LIST_8, KEY_LIST_3, KEY_LIST_3),
                Arguments.of(KEY_LIST_3, KEY_LIST_3, KEY_LIST_3, KEY_LIST_3, KEY_LIST_3, KEY_LIST_3, KEY_LIST_3,
                        KEY_LIST_3, KEY_LIST_3, KEY_LIST_3),
                Arguments.of(KEY_LIST_1, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST,
                        EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST, EMPTY_KEY_LIST)
        );
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        setUpConfigServiceMock();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    @ParameterizedTest
    @MethodSource("provideLists")
    void executeWhenThereAreSomeKeysTest(
            List<String> keyList1,
            List<String> keyList2,
            List<String> keyList3,
            List<String> keyList4,
            List<String> keyList5,
            List<String> keyList6,
            List<String> keyList7,
            List<String> keyList8,
            List<String> keyList9,
            List<String> keyList10
    ) throws InterruptedException {
        setUpAddReceiptKeysMockMethods(keyList1.size(), 0, 0, 0,
                0, 0, 0, 0, 0, false);
        setUpArchiveReceiptKeysMockMethods(keyList1, keyList2, keyList3, keyList4, keyList5, keyList6, keyList7,
                keyList8, false);
        setUpCleanReceiptPrioritiesMethod(keyList9);
        setUpCleanInboundBookingsMethod(keyList10);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        ArchiveReceiptDetailService.StatCounter statCounter = new ArchiveReceiptDetailService.StatCounter(
                keyList1.size(), keyList2.size(), keyList3.size(), keyList4.size(), keyList5.size(), keyList6.size(),
                keyList7.size(), keyList8.size(), keyList9.size(), keyList10.size());
        String expectedResult = getResult(statCounter, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);
        verifyAddReceiptKeysMockMethods(2, 1, 1, 1, 1, 1, 1, 1);
        verifyArchiveReceiptKeysMockMethods(1, 1, 1, 1, 1, 1, 1, 1);
        verifyArchiveReceiptStatusHistoryKeysMockMethods(
                1, 1, 1, 1);
        verifyCleanReceiptPrioritiesMockMethods(1);
        verifyCleanInboundContainerBookingMockMethods(1);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenDaoThrowsQueryTimeoutExceptionTest() throws InterruptedException {
        setUpAddReceiptKeysMockMethods(
                KEY_LIST_1.size(), 0, 0, 0, 0,
                0, 0, 0, 0, true);
        setUpArchiveReceiptKeysMockMethods(
                KEY_LIST_1, KEY_LIST_2, KEY_LIST_3, KEY_LIST_4, KEY_LIST_5, KEY_LIST_6, KEY_LIST_7, KEY_LIST_8, true);
        setUpCleanReceiptPrioritiesMethod(KEY_LIST_9);
        setUpCleanInboundBookingsMethod(KEY_LIST_10);
        when(dao.archiveReceiptByReceiptKeys(anyList(), anyInt()))
                .thenThrow(new QueryTimeoutException("Message"))
                .thenReturn(KEY_LIST_1.size())
                .thenReturn(0);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        ArchiveReceiptDetailService.StatCounter statCounter = new ArchiveReceiptDetailService.StatCounter(
                KEY_LIST_1.size(), KEY_LIST_2.size(), KEY_LIST_3.size(), KEY_LIST_4.size(), KEY_LIST_5.size(),
                KEY_LIST_6.size(), KEY_LIST_7.size(), KEY_LIST_8.size(), KEY_LIST_9.size(), KEY_LIST_10.size());
        String expectedResult = getResult(statCounter, 1, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyAddReceiptKeysMockMethods(3, 1, 1, 1, 1, 1, 1, 1);
        verifyArchiveReceiptKeysMockMethods(2, 2, 1,
                1, 1, 1,
                1, 1);
        verifyArchiveReceiptStatusHistoryKeysMockMethods(1,
                1, 1, 1);
        verifyCleanReceiptPrioritiesMockMethods(1);
        verifyCleanInboundContainerBookingMockMethods(2);
        verifyDbConfigServiceMock(1);
    }

    @Test
    void executeWhenKeysNotFoundTest() throws InterruptedException {
        setUpAddReceiptKeysMockMethods(0, 0, 0, 0,
                0, 0, 0, 0, 0, false);
        long stopExecutionTime = System.currentTimeMillis() + DEFAULT_TIME_LIMIT;

        ArchiveReceiptDetailService.StatCounter statCounter = new ArchiveReceiptDetailService.StatCounter(
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        String expectedResult = getResult(statCounter, 0, stopExecutionTime);
        String actualResult = archiveService.execute();
        Assertions.assertEquals(expectedResult, actualResult);

        verifyAddReceiptKeysMockMethods(1, 1, 1, 1, 1, 1, 1, 1);
        verifyArchiveReceiptKeysMockMethods(0, 0, 0, 0, 0, 0, 0, 0);
        verifyArchiveReceiptStatusHistoryKeysMockMethods(
                0, 0, 0, 0);
        verifyCleanReceiptPrioritiesMockMethods(0);
        verifyCleanInboundContainerBookingMockMethods(0);
        verifyDbConfigServiceMock(1);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void setUpAddReceiptKeysMockMethods(
            int receiptKeysCounter1,
            int receiptKeysCounter2,
            int receiptKeysCounter3,
            int receiptKeysCounter4,
            int receiptKeysCounter5,
            int receiptKeysCounter6,
            int receiptKeysCounter7,
            int receiptKeysCounter8,
            int receiptKeysCounter9,
            boolean isRepeatable
    ) {
        when(dao.addReceiptKeysFromReceiptDetail(anyInt(), anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter1).thenReturn(isRepeatable ? receiptKeysCounter1 : 0).thenReturn(0);
        when(dao.addAdditionalReceiptKeysFromReceiptDetail(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter2).thenReturn(isRepeatable ? receiptKeysCounter2 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptDetailUit(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter3).thenReturn(isRepeatable ? receiptKeysCounter3 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptDetailIdentity(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter4).thenReturn(isRepeatable ? receiptKeysCounter4 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptStatusHistory(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter5).thenReturn(isRepeatable ? receiptKeysCounter5 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptDetailStatusHistory(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter6).thenReturn(isRepeatable ? receiptKeysCounter6 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptDetailService(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter7).thenReturn(isRepeatable ? receiptKeysCounter7 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptDetailItem(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter8).thenReturn(isRepeatable ? receiptKeysCounter8 : 0).thenReturn(0);
        when(dao.addReceiptKeysFromReceiptDetailItem(anyInt(), anyInt()))
                .thenReturn(receiptKeysCounter9).thenReturn(isRepeatable ? receiptKeysCounter9 : 0).thenReturn(0);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void verifyAddReceiptKeysMockMethods(
            int addReceiptKeysFromReceiptDetailTimes,
            int addAdditionalReceiptKeysFromReceiptDetailTimes,
            int addReceiptKeysFromReceiptDetailUitTimes,
            int addReceiptKeysFromReceiptDetailIdentityTimes,
            int addReceiptKeysFromReceiptStatusHistoryTimes,
            int addReceiptKeysFromReceiptDetailStatusHistoryTimes,
            int addReceiptKeysFromReceiptDetailServiceTimes,
            int addReceiptKeysFromReceiptDetailItemTimes
    ) {
        verify(dao, times(addReceiptKeysFromReceiptDetailTimes))
                .addReceiptKeysFromReceiptDetail(anyInt(), anyInt(), anyInt());
        verify(dao, times(addAdditionalReceiptKeysFromReceiptDetailTimes))
                .addAdditionalReceiptKeysFromReceiptDetail(anyInt(), anyInt());
        verify(dao, times(addReceiptKeysFromReceiptDetailUitTimes))
                .addReceiptKeysFromReceiptDetailUit(anyInt(), anyInt());
        verify(dao, times(addReceiptKeysFromReceiptDetailIdentityTimes))
                .addReceiptKeysFromReceiptDetailIdentity(anyInt(), anyInt());
        verify(dao, times(addReceiptKeysFromReceiptStatusHistoryTimes))
                .addReceiptKeysFromReceiptStatusHistory(anyInt(), anyInt());
        verify(dao, times(addReceiptKeysFromReceiptDetailStatusHistoryTimes))
                .addReceiptKeysFromReceiptDetailStatusHistory(anyInt(), anyInt());
        verify(dao, times(addReceiptKeysFromReceiptDetailServiceTimes))
                .addReceiptKeysFromReceiptDetailService(anyInt(), anyInt());
        verify(dao, times(addReceiptKeysFromReceiptDetailItemTimes))
                .addReceiptKeysFromReceiptDetailItem(anyInt(), anyInt());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void setUpArchiveReceiptKeysMockMethods(
            List<String> receiptKeyList,
            List<String> receiptDetailKeyList,
            List<String> receiptDetailUitKeyList,
            List<String> receiptDetailIdentityKeyList,
            List<String> receiptStatusHistoryKeyList,
            List<String> receiptDetailStatusHistoryKeysList,
            List<String> receiptDetailServiceKeyList,
            List<String> receiptDetailItemKeyList,
            boolean isRepeatable
    ) {
        when(dao.getExcludedReceiptKeys(anyInt())).thenReturn(receiptKeyList)
                .thenReturn(isRepeatable ? receiptKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveReceiptByReceiptKeys(eq(receiptKeyList), anyInt())).thenReturn(receiptKeyList.size())
                .thenReturn(isRepeatable ? receiptKeyList.size() : EMPTY_KEY_LIST.size())
                .thenReturn(0);
        when(dao.getExcludedReceiptDetailKeys(anyInt())).thenReturn(receiptDetailKeyList)
                .thenReturn(isRepeatable ? receiptDetailKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.getExcludedReceiptDetailUitKeys(anyInt())).thenReturn(receiptDetailUitKeyList)
                .thenReturn(isRepeatable ? receiptDetailUitKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.getExcludedReceiptDetailIdentityKeys(anyInt())).thenReturn(receiptDetailIdentityKeyList)
                .thenReturn(isRepeatable ? receiptDetailIdentityKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.getExcludedReceiptDetailServiceKeys(anyInt())).thenReturn(receiptDetailServiceKeyList)
                .thenReturn(isRepeatable ? receiptDetailServiceKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.getExcludedReceiptDetailItemKeys(anyInt())).thenReturn(receiptDetailItemKeyList)
                .thenReturn(isRepeatable ? receiptDetailItemKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveReceiptDetails(
                eq(receiptDetailKeyList), eq(receiptDetailUitKeyList), eq(receiptDetailIdentityKeyList),
                eq(receiptDetailServiceKeyList), eq(receiptDetailItemKeyList), anyInt()))
                .thenReturn(new ArchiveReceiptDetailDao.ReceiptStatCounter(receiptDetailKeyList.size(),
                        receiptDetailUitKeyList.size(), receiptDetailIdentityKeyList.size(),
                        receiptDetailServiceKeyList.size(), receiptDetailItemKeyList.size(), 0, 0))
                .thenReturn(isRepeatable ?
                        new ArchiveReceiptDetailDao.ReceiptStatCounter(receiptDetailKeyList.size(),
                                receiptDetailUitKeyList.size(), receiptDetailIdentityKeyList.size(),
                                receiptDetailServiceKeyList.size(), receiptDetailItemKeyList.size(),
                                0, 0) :
                        new ArchiveReceiptDetailDao.ReceiptStatCounter())
                .thenReturn(new ArchiveReceiptDetailDao.ReceiptStatCounter());

        when(dao.getExcludedReceiptStatusHistoryKeys(anyInt())).thenReturn(receiptStatusHistoryKeyList)
                .thenReturn(isRepeatable ? receiptStatusHistoryKeyList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveReceiptStatusHistory(eq(receiptStatusHistoryKeyList), anyInt()))
                .thenReturn(receiptStatusHistoryKeyList.size())
                .thenReturn(isRepeatable ? receiptStatusHistoryKeyList.size() : EMPTY_KEY_LIST.size())
                .thenReturn(0);

        when(dao.getExcludedReceiptDetailStatusHistoryKeys(anyInt())).thenReturn(receiptDetailStatusHistoryKeysList)
                .thenReturn(isRepeatable ? receiptDetailStatusHistoryKeysList : EMPTY_KEY_LIST)
                .thenReturn(EMPTY_KEY_LIST);
        when(dao.archiveReceiptDetailStatusHistory(eq(receiptDetailStatusHistoryKeysList), anyInt()))
                .thenReturn(receiptDetailStatusHistoryKeysList.size())
                .thenReturn(isRepeatable ? receiptDetailStatusHistoryKeysList.size() : EMPTY_KEY_LIST.size())
                .thenReturn(0);
    }

    private void setUpCleanReceiptPrioritiesMethod(List<String> keyList9) {
        when(dao.cleanReceiptToPrioritiesByReceiptKeys(anyList(), anyInt()))
                .thenReturn(keyList9.size());
    }

    private void setUpCleanInboundBookingsMethod(List<String> keyList10) {
        when(dao.cleanInboundContainerBookingsByReceiptKeys(anyList(), anyInt()))
                .thenReturn(keyList10.size());
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private void verifyArchiveReceiptKeysMockMethods(
            int getExcludedReceiptKeysTimes,
            int archiveReceiptByReceiptKeysTimes,
            int getExcludedReceiptDetailKeysTimes,
            int getExcludedReceiptDetailUitKeysTimes,
            int getExcludedReceiptDetailIdentityKeysTimes,
            int getExcludedReceiptDetailServiceKeysTimes,
            int getExcludedReceiptDetailItemKeysTimes,
            int archiveReceiptDetailAndReceiptDetailUitAndReceiptDetailIdentityTimes
    ) {
        verify(dao, times(getExcludedReceiptKeysTimes)).getExcludedReceiptKeys(anyInt());
        verify(dao, times(archiveReceiptByReceiptKeysTimes)).archiveReceiptByReceiptKeys(anyList(), anyInt());
        verify(dao, times(getExcludedReceiptDetailKeysTimes)).getExcludedReceiptDetailKeys(anyInt());
        verify(dao, times(getExcludedReceiptDetailUitKeysTimes)).getExcludedReceiptDetailUitKeys(anyInt());
        verify(dao, times(getExcludedReceiptDetailIdentityKeysTimes)).getExcludedReceiptDetailIdentityKeys(anyInt());
        verify(dao, times(getExcludedReceiptDetailServiceKeysTimes)).getExcludedReceiptDetailServiceKeys(anyInt());
        verify(dao, times(getExcludedReceiptDetailItemKeysTimes)).getExcludedReceiptDetailItemKeys(anyInt());
        verify(dao, times(archiveReceiptDetailAndReceiptDetailUitAndReceiptDetailIdentityTimes))
                .archiveReceiptDetails(
                        anyList(), anyList(), anyList(), anyList(), anyList(), anyInt());
    }

    private void verifyArchiveReceiptStatusHistoryKeysMockMethods(
            int getExcludedReceiptStatusHistoryKeysTimes,
            int archiveReceiptStatusHistoryTimes,
            int getExcludedReceiptDetailStatusHistoryKeysTimes,
            int archiveReceiptDetailStatusHistoryTimes
    ) {
        verify(dao, times(getExcludedReceiptStatusHistoryKeysTimes)).getExcludedReceiptStatusHistoryKeys(anyInt());
        verify(dao, times(archiveReceiptStatusHistoryTimes)).archiveReceiptStatusHistory(anyList(), anyInt());
        verify(dao, times(getExcludedReceiptDetailStatusHistoryKeysTimes))
                .getExcludedReceiptDetailStatusHistoryKeys(anyInt());
        verify(dao, times(archiveReceiptDetailStatusHistoryTimes))
                .archiveReceiptDetailStatusHistory(anyList(), anyInt());
    }

    private void verifyCleanReceiptPrioritiesMockMethods(int cleanReceiptPrioritiesTimes) {
        verify(dao, times(cleanReceiptPrioritiesTimes)).cleanReceiptToPrioritiesByReceiptKeys(anyList(), anyInt());
    }

    private void verifyCleanInboundContainerBookingMockMethods(int cleanInboundContainerBookingsTimes) {
        verify(dao, times(cleanInboundContainerBookingsTimes)).cleanInboundContainerBookingsByReceiptKeys(anyList(),
                anyInt());
    }

    private void setUpConfigServiceMock() {
        final int defaultInactivityDaysThreshold = 7;
        final int defaultReceiptdetailBatchSize = 10000;
        final int defaultReceiptdetailPartitionSize = 1000;
        final int defaultSleepTime = 1;
        final int defaultFailureSleepTime = 1;
        final int defaultReceiptdetailArchTimeout = 10;
        final int defaultReceiptstatushistoryArchTimeout = 20;
        final int defaultSelectionTimeout = 30;
        final int defaultRetryAttempts = 1;

        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_DAYS_THRESHOLD"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultInactivityDaysThreshold);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_BATCH_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultReceiptdetailBatchSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultReceiptdetailArchTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTSTATUSHISTORY_ARCH_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultReceiptstatushistoryArchTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_SELECT_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSelectionTimeout);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_TIME_LIMIT"),
                anyInt(), anyInt(), anyInt())).thenReturn(DEFAULT_TIME_LIMIT);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_PART_SIZE"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultReceiptdetailPartitionSize);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_FAILURE_SLEEP_TIME"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultFailureSleepTime);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_RETRY_ATTEMPTS"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
        when(dbConfigService.getConfigAsIntegerBetween(eq("RECEIPTS_TO_PRIORITIES_CLEANING_TIMEOUT"),
                anyInt(), anyInt(), anyInt())).thenReturn(defaultRetryAttempts);
    }

    private void verifyDbConfigServiceMock(int getConfigTimes) {
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_DAYS_THRESHOLD"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_BATCH_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTSTATUSHISTORY_ARCH_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_SELECT_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_TIME_LIMIT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_PART_SIZE"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_FAILURE_SLEEP_TIME"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTDETAIL_ARCH_RETRY_ATTEMPTS"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("RECEIPTS_TO_PRIORITIES_CLEANING_TIMEOUT"), anyInt(), anyInt(), anyInt());
        verify(dbConfigService, times(getConfigTimes))
                .getConfigAsIntegerBetween(eq("INBOUND_CONTAINER_BOOKINGS_CLEANING_TIMEOUT"), anyInt(), anyInt(),
                        anyInt());
        verify(dbConfigService, times(12))
                .getConfigAsIntegerBetween(anyString(), anyInt(), anyInt(), anyInt());
    }

    private String getResult(
            ArchiveReceiptDetailService.StatCounter statCounter,
            int failedAttemptsNumber,
            long stopExecutionTime
    ) {
        return String.format("Records moved to SCPRDARC: RECEIPT: %d, RECEIPTDETAIL: %d, RECEIPTDETAILUIT: %d, " +
                        "RECEIPTSTATUSHISTORY: %d, RECEIPTDETAILSTATUSHISTORY: %d, RECEIPDETAILIDENTITY: %d, " +
                        "RECEIPTDETAILSERVICE: %d, RECEIPTDETAILITEM: %d. " +
                        "Also cleaned %d RECEIPTS_TO_PRIORITIES, %d INBOUND_CONTAINER_BOOKINGS records %s%s",
                statCounter.getReceiptRowsCnt(), statCounter.getReceiptDetailRowsCnt(),
                statCounter.getReceiptDetailUitRowsCnt(), statCounter.getReceiptStatusHistoryRowsCnt(),
                statCounter.getReceiptDetailStatusHistoryRowsCnt(), statCounter.getReceiptDetailIdentityRowsCnt(),
                statCounter.getReceiptDetailServiceRowsCnt(), statCounter.getReceiptDetailItemRowsCnt(),
                statCounter.getReceiptToPriorityCnt(),
                statCounter.getInboundContainerBookingsCnt(),
                failedAttemptsNumber > 0 ? ", number of failed attempts: " + failedAttemptsNumber : "",
                System.currentTimeMillis() < stopExecutionTime ? "" : " [overtime]"
        );
    }
}
