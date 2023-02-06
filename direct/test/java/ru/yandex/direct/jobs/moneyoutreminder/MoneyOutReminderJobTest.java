package ru.yandex.direct.jobs.moneyoutreminder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import junitparams.converters.Nullable;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.testing.repository.TestEventLogRepository;
import ru.yandex.direct.core.testing.repository.TestSmsQueueRepository;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.i18n.Language;
import ru.yandex.direct.i18n.Translatable;

import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.MONEY_OUT_WALLET;
import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.MONEY_OUT_WALLET_WITH_AO;
import static ru.yandex.direct.jobs.moneyoutreminder.MoneyOutReminderNotificationType.SEVEN_DAYS_OFF;
import static ru.yandex.direct.jobs.moneyoutreminder.MoneyOutReminderNotificationType.THREE_DAYS_OFF;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

/**
 * проверяет логику отбора счётов + отправку смс
 */
public class MoneyOutReminderJobTest extends MoneyOutReminderJobTestBase {

    @Autowired
    private TestSmsQueueRepository testSmsQueueRepository;
    @Autowired
    private MoneyOutReminderSenderService moneyOutReminderSenderService;
    @Autowired
    private TestEventLogRepository testEventLogRepository;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);

        before();
        walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo);

        MoneyOutReminderService moneyOutReminderService =
                new MoneyOutReminderService(eventLogRepository, orderStatService, clientRepository);
        moneyOutReminderJob = new MoneyOutReminderJob(shard, moneyOutReminderService, campaignService,
                campaignRepository, featureService, ppcPropertiesSupport, moneyOutReminderSenderService, userService);
    }

    @AfterEach
    void after() {
        testEventLogRepository
               .deleteCampaignEvents(shard, clientInfo.getClientId(), walletInfo.getCampaignId());
    }

    static Object[] parameters() {
        return new Object[][]{
                //случаи, когда проперти о работе джобы выключена/джоба не должна ничего обрабатывать
                {"проперти выключена -> ничего не отправляем",
                        false, false, List.of(3), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(11_000L), true,
                        null},
                {"все события обработаны -> ничего не отправляем",
                        true, true, List.of(3), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(11_000L), true,
                        null},

                //автоовердрафта нет, потрачено за неделю 630_000, остаток 9_000
                {"события произошли 7 и 2 дня назад, нет автоовердрафта и лимит достигнут -> не отправляем " +
                        "напоминание",
                        true, false, List.of(2, 7), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(11_000L), true,
                        null},
                {"события произошли 3 и 5 дней назад, нет автоовердрафта и лимит достигнут -> отправляем напоминание",
                        true, false, List.of(3, 5), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(11_000L), true,
                        THREE_DAYS_OFF},
                {"событие произошло 7 дней назад, нет автоовердрафта и лимит достигнут -> отправляем напоминание",
                        true, false, List.of(7), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(11_000L), true,
                        SEVEN_DAYS_OFF},

                //остаток 9_300, лимит не достигнут
                {"событие произошло 7 дней назад, нет автоовердрафта и лимит не достигнут -> не отправляем напоминание",
                        true, false, List.of(7), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(50_000L), true,
                        null},

                //случаи с автоовердрафтом
                {"событие произошло 3 дня назад, лимит достигнут и не было автоовердрафта, но он был подключен позже" +
                        " -> не отправляем напоминание",
                        true, false, List.of(3), MONEY_OUT_WALLET, 1000L, BigDecimal.valueOf(11_000L), true,
                        null},
                {"событие произошло 3 дня назад, был автоовердрафт и лимит не достигнут благодаря автоовердрафту" +
                        " -> не отправляем напоминание",
                        true, false, List.of(3), MONEY_OUT_WALLET_WITH_AO, 10_000L, BigDecimal.valueOf(11_000L), true,
                        null},
                {"событие произошло 7 дней назад, был автоовердрафт, но лимит достигнут все равно" +
                        " -> отправляем напоминание",
                        true, false, List.of(7), MONEY_OUT_WALLET_WITH_AO, 1000L, BigDecimal.valueOf(11_000L), true,
                        SEVEN_DAYS_OFF},

                //когда фича о динамическом пороге выключена
                {"событие произошло 7 дней назад, нет автоовердрафта, нет фичи о дин. пороге, но лимит не достигнут" +
                        " -> не отправляем напоминание",
                        true, false, List.of(7), MONEY_OUT_WALLET, 0L, BigDecimal.valueOf(11_000L), false,
                        null},
                {"событие произошло 3 дня назад с автоовердрафтом, нет фичи о пороге, но лимит достигнут все равно" +
                        " -> отправляем напоминание",
                        true, false, List.of(3), MONEY_OUT_WALLET_WITH_AO, 1000L, BigDecimal.valueOf(9000L), false,
                        THREE_DAYS_OFF},
                {"событие произошло 3 дня назад c автоовердрафтом, нет фичи о пороге," +
                        "лимит не достигнут благодаря автоовердрафту -> не отправляем напоминание",
                        true, false, List.of(3), MONEY_OUT_WALLET_WITH_AO, 1000L, BigDecimal.valueOf(10_004L), false,
                        null},

        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("parameters")
    void test(String description, boolean isJobOn, boolean isDoneProgress, Collection<Integer> daysAgo,
              EventLogType eventLogType, Long autoOverdraftAddition, BigDecimal rest, boolean isDynamicThresholdEnabled,
              @Nullable MoneyOutReminderNotificationType expectedNotification) {
        LocalDateTime today = LocalDateTime.now();
        LocalDate toDate = today.toLocalDate().minusDays(1);
        LocalDate prevDate = toDate.minusDays(1);

        String progressValue = (isDoneProgress ? toDate : prevDate).toString().concat(",done");
        setupProperties(isJobOn, progressValue);

        //устанавливаем остаток счёта(в итоге остаток посчитается как rest - 10_000(= sum - sumSpent))
        createCampaignAndAddSumToWallet(rest);

        //добавляем нужные события
        eventLogRepository.addEventLogs(
                shard,
                mapList(daysAgo, dayAgo -> createEventLogWithDefaults(
                        clientInfo,
                        walletInfo.getCampaignId(),
                        dayAgo,
                        today,
                        eventLogType))
        );

        //за неделю потрачено 637_000 - в день 91_000, таким образом динамический порог равен 9100
        mockWeeksSpendAndAutoOverdraft(BigDecimal.valueOf(DEFAULT_SUM_SPENT_BY_WEEK), BigDecimal.valueOf(autoOverdraftAddition));

        steps.featureSteps().addClientFeature(
                clientInfo.getClientId(), FeatureName.USE_DYNAMIC_THRESHOLD_FOR_SEND_ORDER_WARNINGS,
                isDynamicThresholdEnabled);

        executeJob();

        Map<Long, List<String>> campaignIdToText = testSmsQueueRepository
                .getSmsQueueCampaignIdToText(shard, clientInfo.getUid());
        SoftAssertions soft = new SoftAssertions();
        if (expectedNotification == null) {
            soft.assertThat(campaignIdToText)
                    .as("SMS не отправлена")
                    .isEmpty();
        } else {
            Translatable expectTranslatable = null;
            if (expectedNotification.equals(THREE_DAYS_OFF)) {
                expectTranslatable = MoneyOutReminderSmsTranslations.INSTANCE.threeDaysOff(clientInfo.getLogin());
            } else if (expectedNotification.equals(SEVEN_DAYS_OFF)) {
                expectTranslatable = MoneyOutReminderSmsTranslations.INSTANCE.sevenDaysOff(clientInfo.getLogin());
            }
            String expectMessage = translationService
                    .translate(expectTranslatable, Locale.forLanguageTag(Language.RU.getLangString()));
            soft.assertThat(campaignIdToText)
                    .as("В очередь на отправку SMS добавлены корректные данные")
                    .hasSize(1)
                    .containsEntry(walletInfo.getCampaignId(), Collections.singletonList(expectMessage));
        }
        soft.assertAll();
    }
}
