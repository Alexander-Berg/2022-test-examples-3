package ru.yandex.market.replenishment.autoorder.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.SupplierRequestNotificationInfo;
import ru.yandex.market.replenishment.autoorder.service.SupplierRequestNotificationService.NotificationInfoAndError;

import static org.mockito.Mockito.when;

public class NotifySupplierAboutRequestLoaderTest extends FunctionalTest {

    NotifySupplierAboutRequestLoader notifySupplierAboutRequestLoader;
    SupplierRequestNotificationService supplierRequestNotificationService;

    @Autowired
    private SqlSession batchSqlSession;

    @Autowired
    private AlertsService alertsService;

    @Before
    public void setUp() {
        supplierRequestNotificationService = Mockito.mock(SupplierRequestNotificationService.class);
        notifySupplierAboutRequestLoader = new NotifySupplierAboutRequestLoader(
            batchSqlSession,
            timeService,
            supplierRequestNotificationService,
            alertsService
        );
    }

    @Test
    @DbUnitDataSet(before = "NotifySupplierAboutRequestLoaderTest.before.csv",
        after = "NotifySupplierAboutRequestLoaderTest_useBlackList.after.csv")
    public void testDraftsClose_useBlackList() {
        prepareMocks(false, 3L, 8L);
        notifySupplierAboutRequestLoader.load();
    }

    @Test
    @DbUnitDataSet(before = "NotifySupplierAboutRequestLoaderTest.before.csv",
        after = "NotifySupplierAboutRequestLoaderTest_useWhiteList.after.csv")
    public void testDraftsClose_useWhiteList() {
        prepareMocks(true);
        notifySupplierAboutRequestLoader.load();
    }

    private void prepareMocks(boolean isWhiteList, long... supplierRequestIdsForError) {
        LocalDateTime now = LocalDateTime.of(2021, 4, 20, 15, 0);
        setTestTime(now);
        when(supplierRequestNotificationService.notifySupplierAboutRequest(
            ArgumentMatchers.any(SupplierRequestNotificationInfo.class),
            ArgumentMatchers.any(LocalDate.class),
            ArgumentMatchers.any(LocalDate.class)))
            .thenAnswer(getTheSameOrErrorForRequestId(supplierRequestIdsForError));
        ReflectionTestUtils.setField(notifySupplierAboutRequestLoader,
            "notificationSuppliersIsWhiteList", isWhiteList);
    }

    private static Answer<CompletableFuture<NotificationInfoAndError>> getTheSameOrErrorForRequestId(
        long... supplierRequestIdsForError) {
        return (InvocationOnMock invocation) -> {
            Object argument = invocation.getArgument(0);
            if (argument instanceof SupplierRequestNotificationInfo) {
                SupplierRequestNotificationInfo info = (SupplierRequestNotificationInfo) argument;
                Long requestId = info.getRequestId();
                if (requestId != null
                    && supplierRequestIdsForError != null
                    && Arrays.binarySearch(supplierRequestIdsForError, requestId) >= 0) {
                    return CompletableFuture.completedFuture(NotificationInfoAndError.of(
                        info, "Test error for supplier request id " + requestId));
                }
                return CompletableFuture.completedFuture(NotificationInfoAndError.of(info));
            }
            return CompletableFuture.failedFuture(new IllegalStateException("Null argument in test"));
        };
    }
}
