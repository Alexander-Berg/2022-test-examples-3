package ru.yandex.direct.jobs.walletswarnings;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junitparams.converters.Nullable;
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
import ru.yandex.direct.core.entity.eventlog.service.EventLogService;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.notification.repository.SmsQueueRepository;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestSmsQueueRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbqueue.steps.DbQueueSteps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.i18n.Translatable;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.sender.YandexSenderException;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.entry;
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
class WalletsWarningsSenderJobTest {


    @Autowired
    private Steps steps;
    @Autowired
    private DbQueueSteps dbQueueSteps;
    @Autowired
    private YandexSenderClient senderClient;
    @Autowired
    private CampaignService campaignService;
    @Autowired
    private EventLogService eventLogService;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FeatureService featureService;
    @Autowired
    private SmsQueueRepository smsQueueRepository;
    @Autowired
    private TranslationService translationService;
    @Autowired
    private WalletsWarningsMailTemplateResolver walletsWarningsMailTemplateResolver;
    @Autowired
    private DslContextProvider dslContextProvider;
    @Autowired
    private TestSmsQueueRepository testSmsQueueRepository;

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
                {"осталось менее дня до окончания средств и до этого не отправляли -> отправляем",
                        true, true, 637_000L, STATUS_MAIL_NO_MAIL_SEND,
                        WalletsWarningsTranslations.INSTANCE.oneDayRemain()},
                {"осталось менее дня до окончания средств и до этого отправляли про остаток на 1 день -> не отправляем",
                        true, true, 637_000L, STATUS_MAIL_ONE_DAY_WARN_SEND, null},
                {"осталось менее дня до окончания средств и до этого отправляли про остаток на 3 дня -> отправляем",
                        true, true, 637_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND,
                        WalletsWarningsTranslations.INSTANCE.oneDayRemain()},
                {"осталось менее дня до окончания средств, но выключена фича -> не отправляем",
                        true, false, 637_000L, STATUS_MAIL_NO_MAIL_SEND, null},

                // за 7 дней потратили 217_000, или 31_000 за день (90_000 остаток)
                {"осталось менее 3ех дней и до этого не отправляли -> отправляем",
                        true, true, 217_000L, STATUS_MAIL_NO_MAIL_SEND,
                        WalletsWarningsTranslations.INSTANCE.treeDaysRemain()},
                {"осталось менее 3ех дней и до этого отправляли про остаток на 1 день -> не отправляем",
                        true, true, 217_000L, STATUS_MAIL_ONE_DAY_WARN_SEND, null},
                {"осталось менее 3ех дней и до этого отправляли про остаток на 3 дня -> не отправляем",
                        true, true, 217_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, null},
                {"осталось менее 3ех дней до окончания средств, но выключена фича -> не отправляем",
                        true, false, 217_000L, STATUS_MAIL_NO_MAIL_SEND, null},

                // за 7 дней потратили 175_000, или 25_000 за день (90_000 остаток)
                {"осталось более 3ех дней до окончания средств -> sms не отправляем",
                        true, true, 175_000L, STATUS_MAIL_NO_MAIL_SEND, null},

                // При выключенной основной фиче new_wallet_warnings_enabled
                {"осталось менее дня до окончания средств и до этого не отправляли " +
                        "и не включена фича new_wallet_warnings_enabled-> не отправляем",
                        false, true, 637_000L, STATUS_MAIL_NO_MAIL_SEND, null},
                {"осталось менее дня до окончания средств и до этого отправляли про остаток на 3 дня " +
                        "и не включена фича new_wallet_warnings_enabled-> не отправляем",
                        false, true, 637_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, null},
                {"осталось менее 3ех дней и до этого не отправляли " +
                        "и не включена фича new_wallet_warnings_enabled-> не отправляем",
                        false, true, 217_000L, STATUS_MAIL_NO_MAIL_SEND, null},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void testSmsSending(String description, boolean newWalletWarningsEnabled, boolean newWalletWarningsSmsEnabled,
                      Long weekSpentSum, Long statusMail, @Nullable Translatable expectTranslatable) {
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.NEW_WALLET_WARNINGS_ENABLED, newWalletWarningsEnabled);
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.SEND_SMS_FOR_NEW_WALLET_WARNINGS, newWalletWarningsSmsEnabled);

        // остаток 90_000
        CampaignInfo walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo);
        steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, walletInfo.getCampaignId(), BigDecimal.valueOf(300));

        updateCampaignStatusMail(walletInfo.getCampaignId(), statusMail);

        when(orderStatService.getOrdersSumSpent(anyList(), anyList(), any()))
                .thenReturn(Map.of("week", Money.valueOf(BigDecimal.valueOf(weekSpentSum), CurrencyCode.RUB)));

        executeJob();

        Map<Long, List<String>> campaignIdToText = testSmsQueueRepository
                .getSmsQueueCampaignIdToText(clientInfo.getShard(), clientInfo.getUid());
        SoftAssertions soft = new SoftAssertions();
        if (expectTranslatable == null) {
            soft.assertThat(campaignIdToText)
                    .as("SMS не отправлена")
                    .isEmpty();
        } else {
            String expectMessage = translationService.translate(expectTranslatable,
                    Locale.forLanguageTag(Language.RU.getLangString()));

            soft.assertThat(campaignIdToText)
                    .as("В очередь на отправку SMS добавлены корректные данные")
                    .hasSize(1)
                    .contains(entry(walletInfo.getCampaignId(), singletonList(expectMessage)));
        }
        soft.assertAll();
    }


    static Object[] parametersForEmail() {
        return new Object[][]{
                {"осталось менее дня, до этого не отправляли и было исключение ->" +
                        "статус не изменился",
                        637_000L, STATUS_MAIL_NO_MAIL_SEND, true},
                {"осталось менее дня, до этого не отправляли и не было исключения ->" +
                        "статус изменился",
                        637_000L, STATUS_MAIL_NO_MAIL_SEND, false},
                {"осталось менее дня, до этого отправляли про остаток на 3 дня, было исключение -> " +
                        "статус не изменился",
                        637_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, true},
                {"осталось менее дня, до этого отправляли про остаток на 3 дня, не было исключения -> " +
                        "статус изменился",
                        637_000L, STATUS_MAIL_THREE_DAYS_WARN_SEND, false},
                {"осталось менее 3х дней, до этого не отправляли и email не отправился, было исключение ->" +
                        "статус не изменился",
                        217_000L, STATUS_MAIL_NO_MAIL_SEND, true},
                {"осталось менее 3х дней, до этого не отправляли не было исключения ->" +
                        "статус изменился",
                        217_000L, STATUS_MAIL_NO_MAIL_SEND, false},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parametersForEmail")
    void testStatusMailChange(String description, Long weekSpentSum, Long statusMail, boolean hasException) {
        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.NEW_WALLET_WARNINGS_ENABLED, true);

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

        List<Long> campaignId = singletonList(walletInfo.getCampaignId());
        WalletCampaign thisCampaign = campaignRepository
                .getWalletsWithCampaignsByWalletCampaignIds(clientInfo.getShard(), campaignId, false)
                .getWallet(walletInfo.getCampaignId());

        SoftAssertions soft = new SoftAssertions();
        if (!hasException) { //исключения при отправке email не было - меняем статус, иначе - наоборот
            soft.assertThat(thisCampaign.getStatusMail().longValue())
                    .as("Флаг statusMail изменен")
                    .isNotEqualTo(statusMail);
        } else {
            soft.assertThat(thisCampaign.getStatusMail().longValue())
                    .as("Флаг statusMail не изменен")
                    .isEqualTo(statusMail);
        }
        reset(senderClient);
        soft.assertAll();
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
