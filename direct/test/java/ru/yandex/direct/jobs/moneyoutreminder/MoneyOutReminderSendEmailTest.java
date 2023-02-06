package ru.yandex.direct.jobs.moneyoutreminder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.notification.repository.SmsQueueRepository;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.core.testing.repository.TestUserRepository;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.sender.YandexSenderClient;
import ru.yandex.direct.sender.YandexSenderTemplateParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.jobs.moneyoutreminder.MoneyOutReminderJob.PROGRESS_DONE;
import static ru.yandex.direct.jobs.moneyoutreminder.MoneyOutReminderNotificationType.THREE_DAYS_OFF;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MoneyOutReminderSendEmailTest extends MoneyOutReminderJobTestBase {

    @Autowired
    private TestCampaignRepository testCampaignRepository;
    @Autowired
    private MoneyOutReminderMailTemplateResolver moneyOutReminderMailTemplateResolver;
    @Autowired
    private SmsQueueRepository smsQueueRepository;
    @Autowired
    private TestUserRepository testUserRepository;

    @Mock
    private YandexSenderClient senderClient;

    private String campaignSlug;

    private Map<String, String> templateArgs;

    private long walletId;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
        before();

        MoneyOutReminderService moneyOutReminderService =
                new MoneyOutReminderService(eventLogRepository, orderStatService, clientRepository);
        MoneyOutReminderSenderService moneyOutReminderSenderService =
                new MoneyOutReminderSenderService(moneyOutReminderMailTemplateResolver, senderClient,
                        translationService, smsQueueRepository);
        moneyOutReminderJob = new MoneyOutReminderJob(shard, moneyOutReminderService, campaignService,
                campaignRepository, featureService, ppcPropertiesSupport, moneyOutReminderSenderService, userService);

        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.USE_DYNAMIC_THRESHOLD_FOR_SEND_ORDER_WARNINGS,
                true);

        //кошелек с общим остатком в 1000
        createCampaignAndAddSumToWallet(BigDecimal.valueOf(DEFAULT_REST_SUM));
        walletId = walletInfo.getCampaignId();

        LocalDateTime today = LocalDateTime.now();
        LocalDate prevDate = today.toLocalDate().minusDays(2);

        String progressValue = moneyOutReminderService.progressToString(prevDate,PROGRESS_DONE);
        setupProperties(true, progressValue);

        //дефолтное событие, по которому точно отправится уведомление
        eventLogRepository.addEventLog(
                shard,
                createEventLogWithDefaults(clientInfo, walletId, THREE_DAYS_OFF.days, today,
                                EventLogType.MONEY_OUT_WALLET));

        //за неделю потрачено 637_000 - в день 91_000, таким образом динамический порог равен 9100
        mockWeeksSpendAndAutoOverdraft(BigDecimal.valueOf(DEFAULT_SUM_SPENT_BY_WEEK), BigDecimal.ZERO);

        campaignSlug = moneyOutReminderMailTemplateResolver
                .resolveTemplateId(clientInfo.getChiefUserInfo().getUser().getLang(), THREE_DAYS_OFF);

        templateArgs = Map.of(
                MoneyOutReminderSenderService.LOGIN, clientInfo.getLogin(),
                MoneyOutReminderSenderService.CLIENT_ID, clientInfo.getClientId().toString());
    }

    @AfterEach
    void after() {
        testCampaignRepository.deleteCampaign(shard, walletId);
    }

    /**
     * У кошелька указан валидный email в camp_options -> напоминание отправляется туда
     */
    @Test
    void checkNotificationToWalletEmail() {
        String email = "email123@yandex.ru";

        testCampaignRepository.updateEmail(shard, walletId, email);

        executeJob();

        var expectTemplateParams = new YandexSenderTemplateParams.Builder()
                .withToEmail(email)
                .withCampaignSlug(campaignSlug)
                .withArgs(templateArgs)
                .build();
        var argument = ArgumentCaptor.forClass(YandexSenderTemplateParams.class);

        verify(senderClient).sendTemplate(argument.capture(), any());
        assertThat(argument.getValue())
                .as("Параметры шаблона")
                .is(matchedBy(beanDiffer(expectTemplateParams).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * У кошелька не валидный email в camp_options -> напоминание отправляется на email из users
     */
    @Test
    void checkSendNotificationToUserEmail() {
        String email = clientInfo.getChiefUserInfo().getUser().getEmail();

        testCampaignRepository.updateEmail(shard, walletId, "");

        executeJob();

        var expectTemplateParams = new YandexSenderTemplateParams.Builder()
                .withToEmail(email)
                .withCampaignSlug(campaignSlug)
                .withArgs(templateArgs)
                .build();
        var argument = ArgumentCaptor.forClass(YandexSenderTemplateParams.class);

        verify(senderClient).sendTemplate(argument.capture(), any());
        assertThat(argument.getValue())
                .as("Параметры шаблона")
                .is(matchedBy(beanDiffer(expectTemplateParams).useCompareStrategy(onlyExpectedFields())));
    }

    /**
     * Ни у кошелька, ни у пользователя нет валидного email -> напоминание не отправляется
     */
    @Test
    void checkDoNotSendNotificationWithBadEmails() {
        testCampaignRepository.updateEmail(shard, walletId, "");
        testUserRepository.setUnvalidatedUserEmail(shard, clientInfo.getUid());

        executeJob();

        verify(senderClient, never()).sendTemplate(any(), any());
    }
}
