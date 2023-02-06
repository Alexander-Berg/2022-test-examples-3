package ru.yandex.market.core.samovar;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.samovar.model.SamovarFeedDownloadError;
import ru.yandex.market.core.samovar.model.SamovarFullFeedDownloadInfoError;
import ru.yandex.market.core.samovar.model.SamovarReturnCode;

import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Date: 04.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "SamovarShopFeedDownloadNotificationServiceTest.before.csv")
class SamovarShopFeedDownloadNotificationServiceTest extends FunctionalTest {

    @Qualifier("samovarShopFeedDownloadNotificationService")
    @Autowired
    private SamovarFeedDownloadNotificationService samovarFeedDownloadNotificationService;
    @Value("${market.mbi.samovar.problems.notify.threshold:5}")
    private int feedDownloadProblemsNotifyThreshold;

    @DisplayName("Проверка на то, что по всем фидам с market.mbi.samovar.problems.notify.threshold ошибками " +
            "скачивания было отправлено сообщение об ошибке")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutError_listOfElement_twoNotification() {
        samovarFeedDownloadNotificationService.notifyAboutError(getListWithNotification());

        verifySentNotificationType(partnerNotificationClient, 2, 1599037840);
    }

    @DisplayName("Проверка на то, что по всем фидам с market.mbi.samovar.problems.notify.threshold ошибками " +
            "скачивания было отправлено сообщение о восстановлении")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutRecovery_listOfElement_threeNotification() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(getListWithNotification());

        verifySentNotificationType(partnerNotificationClient, 3, 1599037840);
    }

    @DisplayName("Проверка на то, что по всем фидам с market.mbi.samovar.problems.notify.threshold ошибками " +
            "скачивания не было отправлено сообщение о восстановлении из-за выключенного функционала")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutRecovery_listOfElementFalseSetting_threeNotification() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(getListWithNotification());

        verifySentNotificationType(partnerNotificationClient, 3, 1599037840);
    }

    @DisplayName("Проверка на то, что по всем фидам с market.mbi.samovar.problems.notify.threshold ошибками " +
            "скачивания не было отправлено сообщение о восстановлении из-за отсуствия настройки")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutRecovery_listOfElementEmptySetting_threeNotification() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(getListWithNotification());

        verifySentNotificationType(partnerNotificationClient, 3, 1599037840);
    }

    @Nonnull
    private List<SamovarFeedDownloadError> getListWithNotification() {
        return List.of(
                createSamovarFeedDownloadError(1001L, feedDownloadProblemsNotifyThreshold - 2L, 0L, 400, 3, 0, SamovarReturnCode.SamovarReturnStatus.OK),
                createSamovarFeedDownloadError(1002L, feedDownloadProblemsNotifyThreshold, 4L, 400, 1, 1, SamovarReturnCode.SamovarReturnStatus.OK),
                createSamovarFeedDownloadError(1003L, feedDownloadProblemsNotifyThreshold + 5L, 3L, 400, 100, 100, SamovarReturnCode.SamovarReturnStatus.OK),
                createSamovarFeedDownloadError(1004L, feedDownloadProblemsNotifyThreshold, 2L, 400, 0, 5, SamovarReturnCode.SamovarReturnStatus.OK)
        );
    }

    @DisplayName("Проверка на то, что по всем фидам с меньше чем market.mbi.samovar.problems.notify.threshold " +
            "ошибками скачивания не было отправлено сообщение об ошибке")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutError_listWithoutErrorElement_zeroNotification() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(getListWithoutNotification());

        verifyNoInteractions(partnerNotificationClient);
    }

    @DisplayName("Проверка на то, что по всем фидам с меньше чем market.mbi.samovar.problems.notify.threshold " +
            "ошибками скачивания не было отправлено сообщение о восстановлении")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutRecovery_listWithoutErrorElement_zeroNotification() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(getListWithoutNotification());

        verifyNoInteractions(partnerNotificationClient);
    }

    @Nonnull
    private List<SamovarFeedDownloadError> getListWithoutNotification() {
        return List.of(
                createSamovarFeedDownloadError(1005L, feedDownloadProblemsNotifyThreshold - 1L, 0L, 400, 3, 0, SamovarReturnCode.SamovarReturnStatus.OK),
                createSamovarFeedDownloadError(1006L, feedDownloadProblemsNotifyThreshold - 2L, 4L, 400, 1, 1, SamovarReturnCode.SamovarReturnStatus.OK),
                createSamovarFeedDownloadError(1007L, 0L, 3L, 200, 100, 100, SamovarReturnCode.SamovarReturnStatus.OK)
        );
    }

    @DisplayName("Проверка текста сообщения для notifyAboutError")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutError_element_correctText() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(getOneElementList(1005L));

        verifySentNotificationType(partnerNotificationClient, 1, 1599037840L);
    }

    @DisplayName("Проверка текста сообщения для информирование об отключении магазина")
    @Test
    void notifyAboutCutoff_element_correctText() {
        samovarFeedDownloadNotificationService.notifyAboutCutoff(1005L,
                Collections.singletonList(new SamovarFullFeedDownloadInfoError(1005L, 500L, "http://test.me/shop"))
        );

        verifySentNotificationType(partnerNotificationClient, 1, 36L);
    }

    @DisplayName("Проверка текста сообщения для notifyAboutRecovery")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutRecovery_element_correctText() {
        samovarFeedDownloadNotificationService.notifyAboutError(getOneElementList(1005L));

        verifySentNotificationType(partnerNotificationClient, 1, 1599037840L);
    }

    @DisplayName("Проверка текста сообщения для notifyAboutRecovery")
    @Test
    @DbUnitDataSet(before = "SamovarFeedDownloadErrorsServiceTest.enableNtf.before.csv")
    void notifyAboutError_elementWithoutCampaign_correctText() {
        samovarFeedDownloadNotificationService.notifyAboutError(getOneElementList(1006L));

        verifySentNotificationType(partnerNotificationClient, 1, 1599037840L);
    }

    @Nonnull
    private List<SamovarFeedDownloadError> getOneElementList(long feedId) {
        return List.of(
                createSamovarFeedDownloadError(feedId, feedDownloadProblemsNotifyThreshold, 0L, 400, 3, 0,
                        SamovarReturnCode.SamovarReturnStatus.OK)
        );
    }

    private static SamovarFeedDownloadError createSamovarFeedDownloadError(
            long feedId,
            long externalErrorCount,
            long internalErrorCount,
            int lastHttpCode,
            int lastZoraCode,
            int lastFetchCode,
            SamovarReturnCode.SamovarReturnStatus lastReturnStatus
    ) {
        return SamovarFeedDownloadError.builder()
                .feedId(feedId)
                .externalErrorCount(externalErrorCount)
                .internalErrorCount(internalErrorCount)
                .returnCode(SamovarReturnCode.builder()
                        .setHttpCode(lastHttpCode)
                        .setZoraCode(lastZoraCode)
                        .setFetchCode(lastFetchCode)
                        .setReturnStatus(lastReturnStatus)
                        .build())
                .updatedAt(Instant.now())
                .firstErrorTime(null)
                .build();
    }

}
