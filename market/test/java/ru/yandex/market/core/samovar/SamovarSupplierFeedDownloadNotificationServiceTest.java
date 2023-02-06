package ru.yandex.market.core.samovar;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.samovar.model.SamovarFeedDownloadError;
import ru.yandex.market.core.samovar.model.SamovarReturnCode;

import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Date: 04.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarSupplierFeedDownloadNotificationServiceTest extends FunctionalTest {

    public static final SamovarFeedDownloadError DEFAULT_SAMOVAR_FEED_DOWNLOAD_ERROR =
            SamovarFeedDownloadError.builder()
                    .feedId(1L)
                    .returnCode(SamovarReturnCode.builder()
                            .setFetchCode(1)
                            .setZoraCode(1)
                            .setHttpCode(1)
                            .setReturnStatus(SamovarReturnCode.SamovarReturnStatus.OK)
                            .build())
                    .externalErrorCount(1L)
                    .internalErrorCount(1L)
                    .updatedAt(Instant.now())
                    .firstErrorTime(null)
                    .build();

    @Qualifier("samovarSupplierFeedDownloadNotificationService")
    @Autowired
    private SamovarFeedDownloadNotificationService samovarFeedDownloadNotificationService;
    @Autowired
    private NotificationService notificationService;

    //TODO MBI-41957 добавить шаблон и включить отправку уведомлений
    @DisplayName("Проверка на то, что нотификация об ошибке по синим выключена")
    @Test
    void notifyAboutError_any_disableNotification() {
        samovarFeedDownloadNotificationService.notifyAboutRecovery(List.of(DEFAULT_SAMOVAR_FEED_DOWNLOAD_ERROR));

        verifyZeroInteractions(notificationService);
    }

    //TODO MBI-41957 добавить шаблон и включить отправку уведомлений
    @DisplayName("Проверка на то, что нотификация о восстановлении по синим выключена")
    @Test
    void notifyAboutRecovery_any_disableNotification() {
        samovarFeedDownloadNotificationService.notifyAboutError(List.of(DEFAULT_SAMOVAR_FEED_DOWNLOAD_ERROR));

        verifyZeroInteractions(notificationService);
    }
}
