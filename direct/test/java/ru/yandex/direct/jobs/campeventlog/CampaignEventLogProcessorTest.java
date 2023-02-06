package ru.yandex.direct.jobs.campeventlog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.eventlog.model.EventLog;
import ru.yandex.direct.core.entity.eventlog.model.EventLogParams;
import ru.yandex.direct.core.entity.eventlog.repository.EventLogRepository;
import ru.yandex.direct.core.testing.repository.TestEventLogRepository;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.ess.logicobjects.campeventlog.CampaignEventLogLogicObject;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.logicprocessor.common.EssLogicProcessorContext;
import ru.yandex.direct.logicprocessor.processors.campeventlog.CampaignEventLogProcessor;
import ru.yandex.direct.scheduler.hourglass.TaskParametersMap;
import ru.yandex.direct.scheduler.support.DirectShardedJob;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.AVG_CPA_CHANGED;
import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.DAILY_BUDGET_CHANGED;
import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.ROI_COEF_CHANGED;
import static ru.yandex.direct.core.entity.eventlog.model.EventLogType.WEEKLY_BUDGET_CHANGED;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@JobsTest
@ExtendWith(SpringExtension.class)
class CampaignEventLogProcessorTest {
    private static final int SHARD = 1;
    private static final Long CAMPAIGN_ID = 12345L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(54321L);
    public static final BigDecimal VALUE_BEFORE = BigDecimal.valueOf(12.23);
    public static final BigDecimal VALUE_AFTER = BigDecimal.valueOf(13.34);

    @Autowired
    private EventLogRepository eventLogRepository;

    @Autowired
    private TestEventLogRepository testEventLogRepository;

    @Autowired
    private EssLogicProcessorContext essLogicProcessorContext;

    private CampaignEventLogProcessor processor;

    static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {getCampaignEventTemplate()
                        .withCampaignEventType(CampaignEventLogLogicObject.CampaignEventType.WEEKLY_BUDGET_CHANGED)
                        .withBudgetBefore(VALUE_BEFORE)
                        .withBudgetAfter(VALUE_AFTER),

                        getEventLogTtemplate()
                                .withParams(new EventLogParams()
                                        .withBudgetBefore(VALUE_BEFORE)
                                        .withBudgetAfter(VALUE_AFTER))
                                .withType(WEEKLY_BUDGET_CHANGED)
                },

                {getCampaignEventTemplate()
                        .withCampaignEventType(CampaignEventLogLogicObject.CampaignEventType.AVG_CPA_CHANGED)
                        .withAvgCpaBefore(VALUE_BEFORE)
                        .withAvgCpaAfter(VALUE_AFTER),

                        getEventLogTtemplate()
                                .withParams(new EventLogParams()
                                        .withAvgCpaBefore(VALUE_BEFORE)
                                        .withAvgCpaAfter(VALUE_AFTER))
                                .withType(AVG_CPA_CHANGED)
                },

                {getCampaignEventTemplate()
                        .withCampaignEventType(CampaignEventLogLogicObject.CampaignEventType.ROI_COEF_CHANGED)
                        .withRoiCoefBefore(VALUE_BEFORE)
                        .withRoiCoefAfter(VALUE_AFTER),

                        getEventLogTtemplate()
                                .withParams(new EventLogParams()
                                        .withRoiCoefBefore(VALUE_BEFORE)
                                        .withRoiCoefAfter(VALUE_AFTER))
                                .withType(ROI_COEF_CHANGED)
                },

                {getCampaignEventTemplate()
                        .withCampaignEventType(CampaignEventLogLogicObject.CampaignEventType.DAILY_BUDGET_CHANGED)
                        .withBudgetBefore(VALUE_BEFORE)
                        .withBudgetAfter(VALUE_AFTER),

                        getEventLogTtemplate()
                                .withParams(new EventLogParams()
                                        .withBudgetBefore(VALUE_BEFORE)
                                        .withBudgetAfter(VALUE_AFTER))
                                .withType(DAILY_BUDGET_CHANGED)
                },
        });
    }

    private static EventLog getEventLogTtemplate() {
        return new EventLog()
                .withBannerId(0L)
                .withBidsId(0L)
                .withCampaignId(CAMPAIGN_ID)
                .withClientId(CLIENT_ID.asLong());
    }

    private static CampaignEventLogLogicObject getCampaignEventTemplate() {
        return new CampaignEventLogLogicObject()
                .withEventTime(LocalDateTime.now())
                .withCampaignId(CAMPAIGN_ID)
                .withClientId(CLIENT_ID);
    }

    @BeforeEach
    void beforeEach() {
        processor = new CampaignEventLogProcessor(essLogicProcessorContext, eventLogRepository);
        processor.initialize(TaskParametersMap.of(DirectShardedJob.SHARD_PARAM, String.valueOf(SHARD)));
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("parameters")
    void process_CampaignChanged(CampaignEventLogLogicObject campaignEventLogLogicObject, EventLog expectedEventLog) {
        processor.process(singletonList(campaignEventLogLogicObject));

        List<EventLog> eventLogs =
                eventLogRepository.getEventLogsByClientIdAndCampaignId(SHARD, CLIENT_ID.asLong(), CAMPAIGN_ID);
        assumeThat(eventLogs, hasSize(1));
        EventLog eventLog = eventLogs.get(0);

        assertThat(eventLog).is(matchedBy(beanDiffer(expectedEventLog).useCompareStrategy(onlyExpectedFields())));
    }

    @AfterEach
    void afterEach() {
        testEventLogRepository.deleteCampaignEvents(SHARD, CLIENT_ID, CAMPAIGN_ID);
    }
}
