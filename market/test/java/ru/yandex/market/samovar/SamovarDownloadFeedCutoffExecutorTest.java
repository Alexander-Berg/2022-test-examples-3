package ru.yandex.market.samovar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Функциональные тесты для {@link SamovarDownloadFeedCutoffExecutor}
 * Date: 25.09.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
class SamovarDownloadFeedCutoffExecutorTest extends FunctionalTest {

    @Autowired
    private SamovarDownloadFeedCutoffExecutor samovarDownloadFeedCutoffExecutor;

    @DisplayName("Проверка на корректное выполнение задачи на отключение магазинов")
    @DbUnitDataSet(
            before = {
                    "SamovarDownloadFeedCutoffExecutorTest.doJob.before.csv",
                    "SamovarDownloadFeedCutoffExecutorTest.env.enabled.csv"
            },
            after = "SamovarDownloadFeedCutoffExecutorTest.doJob.after.csv"
    )
    @Test
    void doJob_enabledTask_twoCutOff() {
        samovarDownloadFeedCutoffExecutor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 4, 36L);
    }

    @DisplayName("Проверка на выключенность задачи на отключение магазинов")
    @DbUnitDataSet(
            before = {
                    "SamovarDownloadFeedCutoffExecutorTest.doJob.before.csv",
                    "SamovarDownloadFeedCutoffExecutorTest.env.disabled.csv"
            },
            after = "SamovarDownloadFeedCutoffExecutorTest.disabled.after.csv"
    )
    @Test
    void doJob_disabledTask_zero() {
        samovarDownloadFeedCutoffExecutor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }
}
