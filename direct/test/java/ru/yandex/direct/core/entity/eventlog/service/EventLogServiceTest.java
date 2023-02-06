package ru.yandex.direct.core.entity.eventlog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.jooq.Field;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.eventlog.model.DaysLeftNotificationType;
import ru.yandex.direct.core.entity.eventlog.model.EventLog;
import ru.yandex.direct.core.entity.eventlog.model.EventLogParams;
import ru.yandex.direct.core.entity.eventlog.model.EventLogType;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.eventlog.service.EventLogService.EMPTY_ID;
import static ru.yandex.direct.dbschema.ppc.Tables.EVENTLOG;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class EventLogServiceTest {

    private Integer shard;
    private Long campaignId;
    private Long clientId;
    private LocalDateTime beforeEventTime;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private EventLogService eventLogService;

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    public Steps steps;

    @Before
    public void initTestData() {
        CampaignInfo campaignInfo = steps.campaignSteps().createDefaultCampaign();
        shard = campaignInfo.getShard();
        campaignId = campaignInfo.getCampaignId();
        clientId = campaignInfo.getClientId().asLong();
        beforeEventTime = LocalDateTime.now().minusSeconds(1);
    }

    private CompareStrategy getEventLogCompareStrategy() {
        return DefaultCompareStrategies.allFields()
                .forFields(BeanFieldPath.newPath("0", "id")).useMatcher(greaterThan(EMPTY_ID))
                .forFields(BeanFieldPath.newPath("0", "eventTime")).useMatcher(greaterThan(beforeEventTime));
    }

    private EventLog getCommonExpectedEventLogData() {
        return new EventLog()
                .withClientId(clientId)
                .withCampaignId(campaignId)
                .withBannerId(EMPTY_ID)
                .withBidsId(EMPTY_ID);
    }


    @Test
    public void checkAddMoneyInEventLog() {
        Money sumPayed = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CurrencyCode.RUB);
        eventLogService.addMoneyInEventLog(campaignId, CampaignType.TEXT, sumPayed, clientId);

        List<EventLog> eventLogs = eventLogRepository.getEventLogsByClientIdAndCampaignId(shard, clientId, campaignId);

        EventLog expectedEventLog = getCommonExpectedEventLogData()
                .withType(EventLogType.MONEY_IN)
                .withParams(new EventLogParams()
                        .withSumPayed(sumPayed.bigDecimalValue().toPlainString())
                        .withCurrency(sumPayed.getCurrencyCode())
                );
        assertThat("в базе сохранили ожидаемые параметры события", eventLogs,
                beanDiffer(singletonList(expectedEventLog)).useCompareStrategy(getEventLogCompareStrategy()));
    }

    @Test
    public void checkAddMoneyInWalletEventLog() {
        Money sumPayed = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CurrencyCode.YND_FIXED);
        eventLogService.addMoneyInEventLog(campaignId, CampaignType.WALLET, sumPayed, clientId);

        List<EventLog> eventLogs = eventLogRepository.getEventLogsByClientIdAndCampaignId(shard, clientId, campaignId);

        EventLog expectedEventLog = getCommonExpectedEventLogData()
                .withType(EventLogType.MONEY_IN_WALLET)
                .withParams(new EventLogParams()
                        .withSumPayed(sumPayed.bigDecimalValue().toPlainString())
                        .withCurrency(sumPayed.getCurrencyCode())
                );
        assertThat("в базе сохранили ожидаемые параметры события", eventLogs,
                beanDiffer(singletonList(expectedEventLog)).useCompareStrategy(getEventLogCompareStrategy()));
    }

    @Test
    public void checkAddMoneyOutEventLog() {
        eventLogService.addMoneyOutEventLog(campaignId, CampaignType.TEXT, clientId);

        List<EventLog> eventLogs = eventLogRepository.getEventLogsByClientIdAndCampaignId(shard, clientId, campaignId);

        EventLog expectedEventLog = getCommonExpectedEventLogData()
                .withType(EventLogType.MONEY_OUT);

        assertThat("в базе сохранили ожидаемые параметры события", eventLogs,
                beanDiffer(singletonList(expectedEventLog)).useCompareStrategy(getEventLogCompareStrategy()));
        assertTrue("в базе поле param - это null", isFieldNull(EVENTLOG.PARAMS, eventLogs.get(0).getId()));
    }

    @Test
    public void checkAddMoneyOutWalletEventLog() {
        eventLogService.addMoneyOutEventLog(campaignId, CampaignType.WALLET, clientId);

        List<EventLog> eventLogs = eventLogRepository.getEventLogsByClientIdAndCampaignId(shard, clientId, campaignId);

        EventLog expectedEventLog = getCommonExpectedEventLogData()
                .withType(EventLogType.MONEY_OUT_WALLET);
        assertThat("в базе сохранили ожидаемые параметры события", eventLogs,
                beanDiffer(singletonList(expectedEventLog)).useCompareStrategy(getEventLogCompareStrategy()));
        assertTrue("в базе поле param - это null", isFieldNull(EVENTLOG.PARAMS, eventLogs.get(0).getId()));
    }

    @Test
    public void checkAddCampFinishedEventLog() {
        LocalDate finishDate = LocalDate.now();
        eventLogService.addCampFinishedEventLog(campaignId, finishDate, clientId);

        List<EventLog> eventLogs = eventLogRepository.getEventLogsByClientIdAndCampaignId(shard, clientId, campaignId);

        EventLog expectedEventLog = getCommonExpectedEventLogData()
                .withType(EventLogType.CAMP_FINISHED)
                .withParams(new EventLogParams()
                        .withFinishDate(finishDate.toString())
                );
        assertThat("в базе сохранили ожидаемые параметры события", eventLogs,
                beanDiffer(singletonList(expectedEventLog)).useCompareStrategy(getEventLogCompareStrategy()));
    }


    @Test
    public void checkAddMoneyWarningForDaysEventLog() {
        Money restSum = Money.valueOf(RandomNumberUtils.nextPositiveBigDecimal(), CurrencyCode.RUB);
        eventLogService.addMoneyWarningForDaysEventLog(campaignId, clientId,
                DaysLeftNotificationType.ONE_DAY_REMAIN, restSum);

        List<EventLog> eventLogs = eventLogRepository.getEventLogsByClientIdAndCampaignId(shard, clientId, campaignId);

        EventLog expectedEventLog = getCommonExpectedEventLogData()
                .withType(EventLogType.MONEY_WARNING_WALLET)
                .withParams(new EventLogParams()
                        .withDaysLeft(DaysLeftNotificationType.ONE_DAY_REMAIN)
                        .withSumRest(restSum.bigDecimalValue())
                        .withCurrency(restSum.getCurrencyCode()));
        assertThat("в базе сохранили ожидаемые параметры события", eventLogs,
                beanDiffer(singletonList(expectedEventLog)).useCompareStrategy(getEventLogCompareStrategy()));
    }


    private <T> boolean isFieldNull(Field<T> field, Long eventLogId) {
        return dslContextProvider.ppc(shard)
                .select(field)
                .from(EVENTLOG)
                .where(EVENTLOG.ID.eq(eventLogId))
                .fetchOptional(field).isEmpty();
    }

}
