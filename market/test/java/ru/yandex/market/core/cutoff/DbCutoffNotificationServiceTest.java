package ru.yandex.market.core.cutoff;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.cutoff.model.CutoffType;

import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тесты для {@link DbCutoffNotificationService}.
 */
@DbUnitDataSet(before = "csv/DbCutoffNotificationServiceTest.before.csv")
class DbCutoffNotificationServiceTest extends FunctionalTest {
    private static final long DATASOURCE_ID = 10;
    private static final long ACTION_ID = 1;
    private static final int NN_TYPE = 69;

    @Autowired
    DbCutoffNotificationService service;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("Отсутсвуют уровни и для существующих и для нового катофа." +
            "Катофф открываем, уведомления отправляем.")
    @DbUnitDataSet(before = "csv/openCutoffWithNotificationNoLevels.before.csv",
            after = "csv/openCutoffWithNotificationNoLevels.after.csv")
    void openCutoffWithNotificationNoLevels() {
        openCutoff(CutoffType.CPA_QUALITY_OTHER);

        verifySentNotificationType(partnerNotificationClient, 1, NN_TYPE);
    }

    @Test
    @DisplayName("Отсутствуют уровни для нового катофа. Катофф открываем, уведомления не отправляем.")
    @DbUnitDataSet(before = "csv/openCutoffWithNotificationNoLevel.before.csv",
            after = "csv/openCutoffWithNotificationNoLevel.after.csv")
    void openCutoffWithNotificationNoLevel() {
        openCutoff(CutoffType.CPA_QUALITY_OTHER);

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DisplayName("Уровень нового катофа выше чем уровень существующих. Катофф открываем, уведомления отправляем.")
    @DbUnitDataSet(before = "csv/openCutoffWithNotificationHigherLevel.before.csv",
            after = "csv/openCutoffWithNotificationHigherLevel.after.csv")
    void openCutoffWithNotificationHigherLevel() {
        openCutoff(CutoffType.CPA_QUALITY_OTHER);

        verifySentNotificationType(partnerNotificationClient, 1, NN_TYPE);
    }

    @Test
    @DisplayName("механизм проверки уровня и подавления уведомлений работает только для CPA-катофов. " +
            "Кейс: Нового катоффа нет уровня, а для старого есть, но мы все равно его отправляем.")
    @DbUnitDataSet(before = "csv/openCutoffWithNotificationCPCCutoff.before.csv",
            after = "csv/openCutoffWithNotificationCPCCutoff.after.csv")
    void openCutoffWithNotificationCPCCutoff() {
        openCutoff(CutoffType.CPC_PARTNER);

        verifySentNotificationType(partnerNotificationClient, 1, NN_TYPE);
    }

    private void openCutoff(CutoffType cpcCutoffType) {
        transactionTemplate.execute(status -> {
            service.openCutoffWithNotification(DATASOURCE_ID, cpcCutoffType, ACTION_ID, NN_TYPE, List.of(), "comment");
            return null;
        });
    }

}
