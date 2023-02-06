package ru.yandex.market.core.samovar;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.samovar.model.SamovarFeedDownloadError;
import ru.yandex.market.core.samovar.model.SamovarFeedDownloadInfo;
import ru.yandex.market.core.samovar.model.SamovarReturnCode;
import ru.yandex.market.core.util.DateTimes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.before.csv")
class SamovarFeedDownloadErrorsServiceTest extends FunctionalTest {

    private static final Set<SamovarFeedDownloadInfo> SAMOVAR_ALL_TYPE_FEED_DOWNLOAD_INFOS = Set.of(
            new SamovarFeedDownloadInfo(CampaignType.SHOP, 1001L),
            new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1002L),
            new SamovarFeedDownloadInfo(CampaignType.SHOP, 1003L),
            new SamovarFeedDownloadInfo(CampaignType.DELIVERY, 1006L),
            new SamovarFeedDownloadInfo(CampaignType.DISTRIBUTION, 1007L)
    );

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SamovarFeedDownloadErrorsService samovarFeedDownloadErrorsService;

    @Autowired
    private SamovarFeedDownloadErrorsDao samovarFeedDownloadErrorsDao;

    @Value("${market.mbi.samovar.problems.notify.threshold}")
    private int feedDownloadProblemsNotifyThreshold;

    //TODO MBI-41957 добавить шаблон и включить отправку уведомлений
    @Test
    void notificationsDontSend() {
        Set<SamovarFeedDownloadInfo> infoSet = Set.of(new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1001L));
        Instant firstTime = null;
        for (int i = 0; i < feedDownloadProblemsNotifyThreshold; i++) {
            Instant updatedAt = Instant.now();
            if (firstTime == null) {
                firstTime = updatedAt;
            }
            SamovarReturnCode returnCode = createReturnCode(500, 0, 1, null,
                    SamovarReturnCode.SamovarReturnStatus.OK);
            assertEquals(6 + i, samovarFeedDownloadErrorsService.insertOrUpdate(infoSet, returnCode, updatedAt, false));

            checkFeedDownloadError(1001L, 6 + i, 3, returnCode, updatedAt, firstTime);
        }

        Instant updatedAt = Instant.now();
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        samovarFeedDownloadErrorsService.clear(infoSet, returnCode, updatedAt);

        checkFeedDownloadError(1001L, 0, 0, returnCode, updatedAt, null);

        verifyZeroInteractions(notificationService);
    }

    @Test
    @DbUnitDataSet(after = "SamovarFeedDownloadErrorsServiceTest.insertOrUpdate.after.csv")
    void insertOrUpdateTest() {
        Instant updatedAt = DateTimes.toInstant(2020, 1, 3);
        Set<SamovarFeedDownloadInfo> infos = Set.of(
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1001L),
                new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1003L)
        );
        SamovarReturnCode returnCode = createReturnCode(500, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertEquals(6, samovarFeedDownloadErrorsService.insertOrUpdate(infos, returnCode,
                updatedAt, false));

        infos = Set.of(new SamovarFeedDownloadInfo(CampaignType.SHOP, 1002L));
        returnCode = createReturnCode(0, 0, 1001, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertEquals(1, samovarFeedDownloadErrorsService.insertOrUpdate(infos, returnCode,
                updatedAt, true));
    }

    @Test
    @DisplayName("Проверка операции обновления информации по ошибке, если список фидов пуст")
    @DbUnitDataSet(after = "SamovarFeedDownloadErrorsServiceTest.before.csv")
    void insertOrUpdate_emptySet_returnNull() {
        Instant updatedAt = Instant.now();
        SamovarReturnCode returnCode = createReturnCode(0, 0, 1001, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertNull(samovarFeedDownloadErrorsService.insertOrUpdate(Collections.emptySet(), returnCode,
                updatedAt, true));
    }

    @Test
    @DisplayName("Чистим существующий статус")
    @DbUnitDataSet(after = "SamovarFeedDownloadErrorsServiceTest.clearExisted.after.csv")
    void clearExistedTest() {
        Instant updatedAt = DateTimes.toInstant(2020, 1, 3);
        Set<SamovarFeedDownloadInfo> infoSet = Set.of(new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1001L));
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, 555L,
                SamovarReturnCode.SamovarReturnStatus.OK);
        samovarFeedDownloadErrorsService.clear(infoSet, returnCode, updatedAt);
    }

    @Test
    @DisplayName("Чистим статус, которого еще нет в базе")
    @DbUnitDataSet(after = "SamovarFeedDownloadErrorsServiceTest.clearNotExisted.after.csv")
    void clearNotExistedTest() {
        Instant updatedAt = DateTimes.toInstant(2020, 1, 3);
        Set<SamovarFeedDownloadInfo> infoSet = Set.of(new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 2000L));
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, 789L,
                SamovarReturnCode.SamovarReturnStatus.OK);
        samovarFeedDownloadErrorsService.clear(infoSet, returnCode, updatedAt);
    }

    @Test
    @DisplayName("Обнуляем информацию об ошибке скачивания фида с временем первой ошибки")
    @DbUnitDataSet(after = "SamovarFeedDownloadErrorsServiceTest.clearExistedTestFirstError.after.csv")
    void clearExistedTest_withFirstErrorTime_nullFirstErrorTime() {
        Instant updatedAt = DateTimes.toInstant(2020, 1, 3);
        Set<SamovarFeedDownloadInfo> infoSet = Set.of(new SamovarFeedDownloadInfo(CampaignType.SHOP, 1006L));
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        samovarFeedDownloadErrorsService.clear(infoSet, returnCode, updatedAt);
    }

    @Test
    @DisplayName("От Самовара пришло сообщение со статусами ОК, но без ссылки на фид")
    @DbUnitDataSet(after = "SamovarFeedDownloadErrorsServiceTest.messageWithoutMdsKeys.after.csv")
    void messageWithoutMdsKeys() {
        Instant updatedAt = DateTimes.toInstant(2020, 1, 3);
        Set<SamovarFeedDownloadInfo> infoSet = Set.of(new SamovarFeedDownloadInfo(CampaignType.SHOP, 1006L));
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.MDS_KEYS_NOT_FOUND);
        samovarFeedDownloadErrorsService.insertOrUpdate(infoSet, returnCode, updatedAt, true);
    }

    @DisplayName("Обновление информацию об ошибке скачивания фида с временем первой ошибки")
    @Test
    void insertOrUpdateExistedTest_withFirstErrorTime_nullFirstErrorTime() {
        Instant updatedAt = Instant.now();
        Set<SamovarFeedDownloadInfo> infoSet = Set.of(new SamovarFeedDownloadInfo(CampaignType.SHOP, 1006L));
        SamovarReturnCode returnCode = createReturnCode(500, 1, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertEquals(6, samovarFeedDownloadErrorsService.insertOrUpdate(infoSet, returnCode, updatedAt, false));

        Optional<SamovarFeedDownloadError> oDownloadError = samovarFeedDownloadErrorsService.get(1006L);
        assertTrue(oDownloadError.isPresent());

        SamovarFeedDownloadError downloadError = oDownloadError.get();
        assertEquals(updatedAt, downloadError.getUpdatedAt());
        assertNotNull(downloadError.getFirstErrorTime());
        assertNotEquals(updatedAt, downloadError.getFirstErrorTime());
    }

    private void checkFeedDownloadError(Long feedId,
                                        long externalErrorCount, long internalErrorCount,
                                        SamovarReturnCode returnCode,
                                        Instant updatedAt, Instant firstErrorTime) {
        Optional<SamovarFeedDownloadError> oDownloadError = samovarFeedDownloadErrorsService.get(feedId);
        assertTrue(oDownloadError.isPresent());

        SamovarFeedDownloadError downloadError = oDownloadError.get();
        assertEquals(feedId, downloadError.getFeedId());
        assertEquals(externalErrorCount, downloadError.getExternalErrorCount());
        assertEquals(internalErrorCount, downloadError.getInternalErrorCount());
        assertEquals(returnCode.getHttpCode(), downloadError.getReturnCode().getHttpCode());
        assertEquals(returnCode.getZoraCode(), downloadError.getReturnCode().getZoraCode());
        assertEquals(returnCode.getFetchCode(), downloadError.getReturnCode().getFetchCode());
        assertEquals(updatedAt, downloadError.getUpdatedAt());
        assertEquals(firstErrorTime, downloadError.getFirstErrorTime());
    }

    @Test
    @DisplayName("Получить последнее обновление по id фида")
    void getExistedTest() {
        Optional<SamovarFeedDownloadError> samovarFeedDownloadError = samovarFeedDownloadErrorsService.get(1000L);

        SamovarFeedDownloadError expected = SamovarFeedDownloadError.builder()
                .feedId(1000L)
                .externalErrorCount(1)
                .internalErrorCount(2)
                .returnCode(createReturnCode(500, 1001, 0, 123L,
                        SamovarReturnCode.SamovarReturnStatus.MDS_KEYS_NOT_FOUND))
                .updatedAt(DateTimes.toInstant(2019, 9, 1))
                .firstErrorTime(DateTimes.toInstant(2019, 12, 12))
                .build();

        assertTrue(samovarFeedDownloadError.isPresent());
        assertEquals(expected, samovarFeedDownloadError.get());
    }

    @Test
    @DisplayName("Получить последнее обновление по id фида. Нет истории")
    void getNotExistedTest() {
        Optional<SamovarFeedDownloadError> samovarFeedDownloadError = samovarFeedDownloadErrorsService.get(2000L);
        assertTrue(samovarFeedDownloadError.isEmpty());
    }

    @DisplayName("Проверка на количество вызовов NotificationService при восстановлении")
    @Test
    @DbUnitDataSet(
            before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv",
            after = "SamovarFeedDownloadErrorsServiceTest.clearShop.after.csv"
    )
    void clear_clear_notificationServiceInvokeTwice() {
        testNotificationForClear(2);
    }

    @DisplayName("Проверка на количество вызовов NotificationService при восстановлении при выключенной настройке")
    @Test
    @DbUnitDataSet(
            before = "SamovarFeedDownloadErrorsServiceTest.disableNtf.before.csv",
            after = "SamovarFeedDownloadErrorsServiceTest.clearShop.after.csv"
    )
    void clear_clearFalseSetting_notificationServiceInvokeZero() {
        testNotificationForClear(0);
    }

    @DisplayName("Проверка на количество вызовов NotificationService при восстановлении без настройке")
    @Test
    @DbUnitDataSet(
            after = "SamovarFeedDownloadErrorsServiceTest.clearShop.after.csv"
    )
    void clear_clearEmptySetting_notificationServiceInvokeZero() {
        testNotificationForClear(0);
    }

    private void testNotificationForClear(int i) {
        var infos = Set.of(
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1001L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1003L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1006L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1007L)
        );
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        samovarFeedDownloadErrorsService.clear(infos, returnCode, Instant.now());
        samovarFeedDownloadErrorsService.clear(infos, returnCode, Instant.now());
        verify(notificationService, times(i)).send(any());
    }


    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при внутренней ошибке")
    @Test
    void insertOrUpdate_externalError_feedNotificationServiceInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        SamovarReturnCode returnCode = createReturnCode(500, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertEquals(6, feedDownloadErrorsService.insertOrUpdate(SAMOVAR_ALL_TYPE_FEED_DOWNLOAD_INFOS, returnCode,
                Instant.now(), false));

        verifyNotificationService(feedDownloadNotificationServiceMap, times(1), times(1),
                never(), never());
    }

    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при внутренней ошибке. " +
            "Только белые")
    @Test
    void insertOrUpdate_externalErrorShopOnly_feedNotificationServiceInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        var infos = Set.of(
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1001L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1003L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1006L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1007L)
        );
        SamovarReturnCode returnCode = createReturnCode(500, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertEquals(6, feedDownloadErrorsService.insertOrUpdate(infos, returnCode,
                Instant.now(), false));

        verifyNotificationService(feedDownloadNotificationServiceMap, times(1), never(),
                never(), never());
    }

    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при внешней ошибке")
    @Test
    void insertOrUpdate_internalError_feedNotificationServiceNotInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        SamovarReturnCode returnCode = createReturnCode(500, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        assertEquals(4, feedDownloadErrorsService.insertOrUpdate(SAMOVAR_ALL_TYPE_FEED_DOWNLOAD_INFOS, returnCode,
                Instant.now(), true));

        verifyNotificationService(feedDownloadNotificationServiceMap, never(), never(),
                never(), never());
    }

    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при восстановлении")
    @Test
    void insertOrUpdate_clear_feedNotificationServiceNotInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        feedDownloadErrorsService.clear(SAMOVAR_ALL_TYPE_FEED_DOWNLOAD_INFOS, returnCode, Instant.now());

        verifyNotificationService(feedDownloadNotificationServiceMap, never(), never(),
                times(1), times(1));
    }

    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при восстановлении. " +
            "Только белые")
    @Test
    void insertOrUpdate_clearShopOnly_feedNotificationServiceNotInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        var infos = Set.of(
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1001L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1003L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1006L),
                new SamovarFeedDownloadInfo(CampaignType.SHOP, 1007L)
        );
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        feedDownloadErrorsService.clear(infos, returnCode, Instant.now());

        verifyNotificationService(feedDownloadNotificationServiceMap, never(), never(),
                times(1), never());
    }

    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при восстановлении. " +
            "Только синие")
    @Test
    void insertOrUpdate_clearSupplierOnly_feedNotificationServiceNotInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        var infos = Set.of(
                new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1001L),
                new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1003L),
                new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1006L),
                new SamovarFeedDownloadInfo(CampaignType.SUPPLIER, 1007L)
        );
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        feedDownloadErrorsService.clear(infos, returnCode, Instant.now());

        verifyNotificationService(feedDownloadNotificationServiceMap, never(), never(),
                never(), times(1));
    }

    @DisplayName("Проверка на количество вызовов SamovarFeedDownloadNotificationService при восстановлении. " +
            "Неизвестые магазины")
    @Test
    void insertOrUpdate_clearUnknownOnly_feedNotificationServiceNotInvoke() {
        var feedDownloadNotificationServiceMap = createFeedDownloadNotificationServiceMock();
        SamovarFeedDownloadErrorsService feedDownloadErrorsService = new SamovarFeedDownloadErrorsServiceImpl(
                samovarFeedDownloadErrorsDao, feedDownloadNotificationServiceMap
        );

        var infos = Set.of(
                new SamovarFeedDownloadInfo(CampaignType.BUSINESS, 1001L),
                new SamovarFeedDownloadInfo(CampaignType.DELIVERY, 1006L),
                new SamovarFeedDownloadInfo(CampaignType.DISTRIBUTION, 1007L)
        );
        SamovarReturnCode returnCode = createReturnCode(200, 0, 1, null,
                SamovarReturnCode.SamovarReturnStatus.OK);
        feedDownloadErrorsService.clear(infos, returnCode, Instant.now());

        verifyNotificationService(feedDownloadNotificationServiceMap, never(), never(),
                never(), never());
    }

    @Nonnull
    private Map<CampaignType, SamovarFeedDownloadNotificationService> createFeedDownloadNotificationServiceMock() {
        Map<CampaignType, SamovarFeedDownloadNotificationService> feedDownloadNotificationServiceMap = new HashMap<>();
        feedDownloadNotificationServiceMap
                .put(CampaignType.SHOP, Mockito.mock(SamovarFeedDownloadNotificationService.class));
        feedDownloadNotificationServiceMap
                .put(CampaignType.SUPPLIER, Mockito.mock(SamovarFeedDownloadNotificationService.class));
        return feedDownloadNotificationServiceMap;
    }

    private void verifyNotificationService(
            @Nonnull Map<CampaignType, SamovarFeedDownloadNotificationService> feedDownloadNotificationServiceMap,
            @Nonnull VerificationMode shopErrorMode, @Nonnull VerificationMode supplierErrorMode,
            @Nonnull VerificationMode shopRecoveryMode, @Nonnull VerificationMode supplierRecoveryMode
    ) {
        verify(feedDownloadNotificationServiceMap.get(CampaignType.SHOP), shopErrorMode)
                .notifyAboutError(any());
        verify(feedDownloadNotificationServiceMap.get(CampaignType.SUPPLIER), supplierErrorMode)
                .notifyAboutError(any());
        verify(feedDownloadNotificationServiceMap.get(CampaignType.SHOP), shopRecoveryMode)
                .notifyAboutRecovery(any());
        verify(feedDownloadNotificationServiceMap.get(CampaignType.SUPPLIER), supplierRecoveryMode)
                .notifyAboutRecovery(any());
    }

    private SamovarReturnCode createReturnCode(int httpCode, int zoraCode, int fetchCode, Long checksum,
                                               SamovarReturnCode.SamovarReturnStatus returnStatus) {
        return SamovarReturnCode.builder()
                .setHttpCode(httpCode)
                .setFetchCode(fetchCode)
                .setZoraCode(zoraCode)
                .setChecksum(checksum)
                .setReturnStatus(returnStatus)
                .build();
    }
}
