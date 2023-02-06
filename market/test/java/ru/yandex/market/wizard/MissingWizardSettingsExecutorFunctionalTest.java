package ru.yandex.market.wizard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.ds.DatasourceService;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.ParamValue;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.protocol.ProtocolService;
import ru.yandex.market.core.wizard.WizardService;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.notification.service.PartnerNotificationApiServiceTest.verifySentNotificationType;

/**
 * Тесты {@link MissingWizardSettingsExecutor}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@DbUnitDataSet(before = "csv/Executor.before.csv")
class MissingWizardSettingsExecutorFunctionalTest extends FunctionalTest {

    @Autowired
    private WizardService wizardService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private ProtocolService protocolService;

    @Autowired
    private DatasourceService datasourceService;

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;

    private MissingWizardSettingsExecutor missingWizardSettingsExecutor;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        missingWizardSettingsExecutor = Mockito.spy(new MissingWizardSettingsExecutor(
                wizardService,
                paramService,
                protocolService,
                datasourceService,
                notificationService,
                environmentService,
                campaignService,
                partnerTypeAwareService
        ));

        Instant timeToSend = LocalDate.of(2017, 5, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        when(missingWizardSettingsExecutor.getTimeToSend()).thenReturn(timeToSend);
    }

    @Test
    @DisplayName("Параметр отправки уведомления отсутствует")
    void testExecutorWithoutNotificationParam() {
        missingWizardSettingsExecutor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "csv/Executor.testExecutorWithEmptyNotificationParam.before.csv")
    @DisplayName("Параметр отправки уведомления присутствует, но сброшен в false")
    void testExecutorWithEmptyNotificationParam() {
        final ParamType paramType = ParamType.NEVER_MISSING_WIZARD_SETTINGS_CHECK;
        ParamValue paramValue = new BooleanParamValue(paramType, 1L, false);
        paramService.setParam(paramValue, 100500L);

        missingWizardSettingsExecutor.doJob(null);

        // Параметр должен быть удален из БД
        List<ParamValue> paramValues = paramService.listParams(paramType);
        assertThat(paramValues).isEmpty();
        verifyNoInteractions(partnerNotificationClient);
    }

    @Test
    @DbUnitDataSet(before = "csv/Executor.all.before.csv")
    @DisplayName("Все настройки не заполнены")
    void testExecutorAll() {
        missingWizardSettingsExecutor.doJob(null);

        verifySentNotificationType(
                partnerNotificationClient, 2,
                MissingWizardSettingsExecutor.NN_TYPE,
                MissingWizardSettingsExecutor.SMB_NN_TYPE
        );
    }

    @Test
    @DbUnitDataSet(before = "csv/Executor.without_legal.before.csv")
    @DisplayName("Юр.инфо не заполнено")
    void testExecutorWithoutLegal() {
        missingWizardSettingsExecutor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 1, MissingWizardSettingsExecutor.NN_TYPE);
    }

    @Test
    @DbUnitDataSet(
            before = {"csv/Executor.all_filled.before.csv", "csv/Executor.without_common_info_for_smb.before.csv"}
    )
    @DisplayName("Общая информация не заполнена у SMB-магазина")
    void testExecutorWithoutCommonInfoForSmb() {
        missingWizardSettingsExecutor.doJob(null);

        verifySentNotificationType(partnerNotificationClient, 1, MissingWizardSettingsExecutor.SMB_NN_TYPE);
    }

    @Test
    @DbUnitDataSet(before = "csv/Executor.all_filled.before.csv")
    @DisplayName("Все заполнено")
    void testExecutorAllFilled() {
        missingWizardSettingsExecutor.doJob(null);

        verifyNoInteractions(partnerNotificationClient);
    }
}
