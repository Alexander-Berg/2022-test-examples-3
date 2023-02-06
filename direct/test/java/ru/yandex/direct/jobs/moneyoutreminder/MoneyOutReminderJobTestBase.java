package ru.yandex.direct.jobs.moneyoutreminder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcPropertyNames;
import ru.yandex.direct.core.entity.campaign.AutoOverdraftUtils;
import ru.yandex.direct.core.entity.campaign.model.WalletCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignService;
import ru.yandex.direct.core.entity.client.repository.ClientRepository;
import ru.yandex.direct.core.entity.eventlog.model.EventLog;
import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.statistics.service.OrderStatService;
import ru.yandex.direct.core.entity.user.service.UserService;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@JobsTest
@ExtendWith(SpringExtension.class)
public class MoneyOutReminderJobTestBase {


    @Autowired
    protected Steps steps;
    @Autowired
    protected CampaignService campaignService;
    @Autowired
    protected UserService userService;
    @Autowired
    protected CampaignRepository campaignRepository;
    @Autowired
    protected EventLogRepository eventLogRepository;
    @Autowired
    protected FeatureService featureService;
    @Autowired
    protected TranslationService translationService;
    @Autowired
    protected ClientRepository clientRepository;
    @Autowired
    protected PpcPropertiesSupport ppcPropertiesSupport;

    @Mock
    protected OrderStatService orderStatService;

    protected static MockedStatic<AutoOverdraftUtils> autoOverdraftUtilsMock;

    protected MoneyOutReminderJob moneyOutReminderJob;

    protected ClientInfo clientInfo;

    protected CampaignInfo walletInfo;

    protected int shard;

    public static final long DEFAULT_SUM_SPENT_BY_WEEK = 637_000L;
    public static final long DEFAULT_REST_SUM = 11_000L;

    @BeforeAll
    static void beforeAll() {
        autoOverdraftUtilsMock = Mockito.mockStatic(AutoOverdraftUtils.class);
    }

    protected void before() {
        MockitoAnnotations.openMocks(this);

        clientInfo = steps.clientSteps().createDefaultClientAnotherShard();
        shard = clientInfo.getShard();
        walletInfo = steps.campaignSteps().createWalletCampaign(clientInfo);
    }

    public void setupProperties(boolean isJobOn, String progressValue) {
        ppcPropertiesSupport.get(PpcPropertyNames.MONEY_OUT_REMINDER_JOB_ON).set(isJobOn);
        ppcPropertiesSupport.get(PpcPropertyNames.moneyOutReminderProgress(shard)).set(progressValue);
    }

    protected void createCampaignAndAddSumToWallet(BigDecimal rest) {
        steps.campaignSteps()
                .createCampaignUnderWallet(clientInfo, walletInfo.getCampaignId(), BigDecimal.valueOf(300));
        steps.campaignSteps().setCampaignProperty(walletInfo, WalletCampaign.SUM, rest);
    }

    protected void mockWeeksSpendAndAutoOverdraft(BigDecimal spentByWeek,
                                                  BigDecimal autoOverdraftAddition) {
        when(orderStatService.getOrdersSumSpent(anyList(), anyList(), any()))
                .thenReturn(Map.of("week", Money.valueOf(spentByWeek, CurrencyCode.RUB)));

        autoOverdraftUtilsMock.when(() -> AutoOverdraftUtils
                .calculateAutoOverdraftAddition(any(), any(), any(), any()))
                .thenReturn(autoOverdraftAddition);
    }

    protected void executeJob() {
        Assertions.assertThatCode(() -> moneyOutReminderJob.execute())
                .doesNotThrowAnyException();
    }

    public static EventLog createEventLogWithDefaults(ClientInfo clientInfo,
                                                      Long walletId,
                                                      Integer daysAgo,
                                                      LocalDateTime today,
                                                      EventLogType eventLogType) {
        return new EventLog()
                .withClientId(clientInfo.getClientId().asLong())
                .withType(eventLogType)
                .withCampaignId(walletId)
                .withBidsId(0L)
                .withBannerId(0L)
                .withEventTime(today.minusDays(daysAgo).withNano(0));
    }
}
