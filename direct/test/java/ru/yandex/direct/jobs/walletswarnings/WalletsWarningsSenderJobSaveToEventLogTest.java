package ru.yandex.direct.jobs.walletswarnings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.campaign.model.WalletCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.eventlog.model.EventLog;
import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.entity.eventlog.service.EventLogService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.notification.repository.SmsQueueRepository;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.sender.YandexSenderException;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignRepository.STATUS_MAIL_NO_MAIL_SEND;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignRepository.STATUS_MAIL_ONE_DAY_WARN_SEND;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignRepository.STATUS_MAIL_THREE_DAYS_WARN_SEND;
import static ru.yandex.direct.core.entity.dbqueue.DbQueueJobTypes.UPDATE_AGGREGATOR_DOMAINS;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;


@JobsTest
@ExtendWith(SpringExtension.class)
class WalletsWarningsSenderJobSaveToEventLogTest {


    @Autowired
    private Steps steps;
    @Autowired
    private DbQueueSteps dbQueueSteps;
    @Autowired
    private YandexSenderClient senderClient;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private SmsQueueRepository smsQueueRepository;
    @Autowired
    private EventLogRepository eventLogRepository;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private EventLogService eventLogService;
    @Autowired
    private WalletsWarningsMailTemplateResolver walletsWarningsMailTemplateResolver;
    @Autowired
    private DslContextProvider dslContextProvider;

    @Mock
    private OrderStatService orderStatService;

    private WalletsWarningsSenderJob walletsWarningsSenderJob;

    private ClientInfo clientInfo;


    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);

        clientInfo = steps.clientSteps().createDefaultClient();

        dbQueueSteps.registerJobType(UPDATE_AGGREGATOR_DOMAINS);
        dbQueueSteps.clearQueue(UPDATE_AGGREGATOR_DOMAINS);
        walletsWarningsSenderJob = new WalletsWarningsSenderJob(clientInfo.getShard(), senderClient, orderStatService,
                campaignService, campaignRepository, userService, featureService, smsQueueRepository,
                translationService, eventLogService, walletsWarningsMailTemplateResolver);
    }

    static Object[] parameters() {
        return new Object[][]{
                // за 7 дней потратили 637_000, или 91_000 за день (90_000 остаток)
                {"осталось менее дня до окончания средств и до этого не отправляли -> сохраняем событие в eventlog",
                        true, 637_000L, STATUS_MAIL_NO_MAIL_SEND, false, true},
                {"осталось менее дня до окончания средств и до этого отправляли про остаток на 1 день -> не сохраняем" +
                        " событие в eventlog",
                        true, 637_000L, STATUS_MAIL_ONE_DAY_WARN_SEND, false, false},
                {"осталось менее дня до окончания средств и до этого отправляли про остаток на 3 дня -> сохраняем" +
                        " событие в eventlog",
                        true, 637_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, false, true},

                // за 7 дней потратили 217_000, или 31_000 за день (90_000 остаток)
                {"осталось менее 3ех дней и до этого не отправляли -> сохраняем событие в eventlog",
                        true, 217_000L, STATUS_MAIL_NO_MAIL_SEND, false, true},
                {"осталось менее 3ех дней и до этого отправляли про остаток на 1 день -> не сохраняем событие в" +
                        " eventlog",
                        true, 217_000L, STATUS_MAIL_ONE_DAY_WARN_SEND, false, false},
                {"осталось менее 3ех дней и до этого отправляли про остаток на 3 дня -> не сохраняем событие в" +
                        " eventlog",
                        true, 217_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, false, false},

                // за 7 дней потратили 175_000, или 25_000 за день (90_000 остаток)
                {"осталось более 3ех дней до окончания средств -> не сохраняем событие в eventlog",
                        true, 175_000L, STATUS_MAIL_NO_MAIL_SEND, false, false},

                // При выключенной основной фиче new_wallet_warnings_enabled
                {"осталось менее дня до окончания средств и до этого не отправляли " +
                        "и не включена фича new_wallet_warnings_enabled -> не сохраняем событие в eventlog",
                        false, 637_000L, STATUS_MAIL_NO_MAIL_SEND, false, false},
                {"осталось менее дня до окончания средств и до этого отправляли про остаток на 3 дня " +
                        "и не включена фича new_wallet_warnings_enabled -> не сохраняем событие в eventlog",
                        false, 637_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, false, false},
                {"осталось менее 3ех дней и до этого не отправляли " +
                        "и не включена фича new_wallet_warnings_enabled -> не сохраняем событие в eventlog",
                        false, 217_000L, STATUS_MAIL_NO_MAIL_SEND, false, false},

                //При случаях неудачной отправки email'ов
                {"осталось менее дня, до этого не отправляли и email не отправился, было исключение ->" +
                        "не сохраняем событие в eventlog и не ставим новый статус",
                        true, 637_000L, STATUS_MAIL_NO_MAIL_SEND, true, false},
                {"осталось менее 3х дней, до этого не отправляли и email не отправился, было исключение ->" +
                        "не сохраняем событие в eventlog и не ставим новый статус",
                        true, 217_000L, STATUS_MAIL_NO_MAIL_SEND, true, false}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void test(String description, boolean newWalletWarningsEnabled, Long weekSpentSum, Long statusMail,
              boolean hasException, boolean isSent) {
        reset(senderClient);
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.NEW_WALLET_WARNINGS_ENABLED, newWalletWarningsEnabled);
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.SEND_SMS_FOR_NEW_WALLET_WARNINGS, true);

        // остаток 90_000
        CampaignInfo walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo);
        steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, walletInfo.getCampaignId(), BigDecimal.valueOf(300));

        updateCampaignStatusMail(walletInfo.getCampaignId(), statusMail);

        when(orderStatService.getOrdersSumSpent(anyList(), anyList(), any()))
                .thenReturn(Map.of("week", Money.valueOf(BigDecimal.valueOf(weekSpentSum), CurrencyCode.RUB)));

       if (hasException) {
            when(senderClient.sendTemplate(any(), any())).thenThrow(new YandexSenderException("Fail to send"));
        }

        executeJob();

        List<EventLog> campaignEventLogEntries = eventLogRepository.getEventLogsByClientIdAndCampaignId(
                clientInfo.getShard(),
                walletInfo.getCampaign().getClientId(),
                walletInfo.getCampaignId());

        List<Long> campaignId = singletonList(walletInfo.getCampaignId());
        WalletCampaign thisCampaign = campaignRepository
                .getWalletsWithCampaignsByWalletCampaignIds(clientInfo.getShard(), campaignId, false)
                .getWallet(walletInfo.getCampaignId());

        SoftAssertions soft = new SoftAssertions();

        if (!isSent) {
            soft.assertThat(campaignEventLogEntries)
                    .as("нет записи в ивентлоге")
                    .isEmpty();

            //если записи нет, то статус точно не менялся
            soft.assertThat(thisCampaign.getStatusMail().longValue())
                    .as("Статус письма не изменен")
                    .isEqualTo(statusMail);
        } else {
            soft.assertThat(campaignEventLogEntries.size())
                    .as("Есть запись в ивентлоге")
                    .isEqualTo(1);
            if (!campaignEventLogEntries.isEmpty()) {
                soft.assertThat(campaignEventLogEntries.get(0).getType())
                        .as("Тип записи верный")
                        .isEqualTo(EventLogType.MONEY_WARNING_WALLET);
                soft.assertThat(campaignEventLogEntries.get(0).getParams().getDaysLeft())
                        .as("Информация о днях сохранилась")
                        .isNotNull();
            }

            //если запись есть, то статус письма точно изменен
            soft.assertThat(thisCampaign.getStatusMail().longValue())
                    .as("Статус письма изменен")
                    .isNotEqualTo(statusMail);
        }
        soft.assertAll();
        reset(senderClient);
    }

    private void executeJob() {
        Assertions.assertThatCode(() -> walletsWarningsSenderJob.execute())
                .doesNotThrowAnyException();
    }

    private void updateCampaignStatusMail(Long campaignId, Long statusMail) {
        dslContextProvider.ppc(clientInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.STATUS_MAIL, statusMail)
                .where(CAMPAIGNS.CID.eq(campaignId))
                .execute();
    }
}
