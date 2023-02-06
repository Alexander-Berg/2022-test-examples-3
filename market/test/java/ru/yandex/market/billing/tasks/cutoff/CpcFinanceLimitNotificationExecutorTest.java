package ru.yandex.market.billing.tasks.cutoff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tasks.CpcFinanceLimitNotificationExecutor;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.cutoff.CutoffService;
import ru.yandex.market.core.cutoff.service.CutoffMessageService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.param.ParamService;

import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

class CpcFinanceLimitNotificationExecutorTest extends FunctionalTest {
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private DatasourceService datasourceService;
    @Autowired
    private ParamService paramService;
    @Autowired
    private CutoffService cutoffService;
    @Autowired
    private CutoffMessageService cutoffMessageService;
    @Autowired
    private CampaignService campaignService;

    private CpcFinanceLimitNotificationExecutor cpcFinanceLimitNotificationExecutor;

    @BeforeEach
    void setUp() {
        cpcFinanceLimitNotificationExecutor = new CpcFinanceLimitNotificationExecutor(
                notificationService,
                datasourceService,
                paramService,
                cutoffService,
                cutoffMessageService,
                campaignService
        );
    }

    /**
     * Проверяем, что инициировалась отправка сообщений и отключения связались с сообщениями.
     * Это выполняется только для магазинов с открытыми отключениями при этом не связанные с сообщениями
     * Т.е. по отключению не было отправлено сообщение
     */
    @Test
    @DbUnitDataSet(
            before = "financeLimitCutoffNotificationTest.before.csv",
            after = "financeLimitCutoffNotificationTest.after.csv"
    )
    void checkOpenCutoff() {
        cpcFinanceLimitNotificationExecutor.doJob(null);

        //проверяем, что создание сообщений инициировалось два раза
        verifySentNotificationType(partnerNotificationClient, 2,
                CpcFinanceLimitNotificationExecutor.CUTOFF_NOTIFICATION_TEMPLATE);
    }
}
