package ru.yandex.direct.jobs.autobudget;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.util.streamex.StreamEx;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.common.db.PpcPropertyType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignDayBudgetOptions;
import ru.yandex.direct.core.entity.campaign.model.DayBudgetNotificationStatus;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.strategy.model.StrategyWithDayBudget;
import ru.yandex.direct.core.entity.strategy.repository.StrategyModifyRepository;
import ru.yandex.direct.core.entity.strategy.repository.StrategyTypedRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


/**
 * Тесты на джобу UpdateBudgetJob с использованием базы из докера.
 */
@JobsTest
@ExtendWith(SpringExtension.class)
class UpdateBudgetJobTest {

    private static final long DEFAULT_DAY_BUDGET_CHANGE_COUNT = 0L;

    @Autowired
    private Steps steps;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private StrategyModifyRepository strategyModifyRepository;

    @Autowired
    private StrategyTypedRepository strategyTypedRepository;

    private UpdateDayBudgetJob job;
    private long cid;
    private int shard;
    private Campaign campaignDayBudgetOptions;
    private Map<Long, CampaignDayBudgetOptions> expectedMap;

    private PpcPropertiesSupport mockPpcPropertiesSupport() {
        PpcPropertiesSupport ppcPropertiesSupport = mock(PpcPropertiesSupport.class);
        LocalDate propValue = LocalDate.now()
                .minusDays(1);
        PpcPropertyName<LocalDate> propertyName =
                new PpcPropertyName<>(String.format(UpdateDayBudgetJob.LAST_RUN_DATE_PROPERTY, shard),
                        PpcPropertyType.LOCAL_DATE);
        @SuppressWarnings("unchecked")
        PpcProperty<LocalDate> property = mock(PpcProperty.class);
        doReturn(propValue).when(property).get();
        doReturn(property).when(ppcPropertiesSupport).get(eq(propertyName));
//        doReturn(propValue).when(ppcPropertiesSupport)
//                .get(String.format(UpdateDayBudgetJob.LAST_RUN_DATE_PROPERTY, shard));
        return ppcPropertiesSupport;
    }

    @BeforeEach
    void before() {
        CampaignInfo defaultCampaign = steps.campaignSteps().createDefaultCampaign();
        cid = defaultCampaign.getCampaignId();
        shard = defaultCampaign.getShard();

        campaignDayBudgetOptions = new Campaign()
                .withId(cid)
                .withDayBudgetDailyChangeCount(1L)
                .withDayBudgetNotificationStatus(DayBudgetNotificationStatus.SENT)
                .withDayBudgetStopTime(LocalDateTime.now().minusMinutes(1));
        expectedMap = new HashMap<>();

        job = new UpdateDayBudgetJob(shard, campaignRepository, strategyTypedRepository, strategyModifyRepository, mockPpcPropertiesSupport());
    }

    private void executeJob() {
        assertThatCode(() -> job.execute())
                .doesNotThrowAnyException();
    }

    @Test
    void checkUpdateBudgetJobResetAllDayBudgetCampOptions() {
        campaignRepository.updateDayBudgetOptions(shard, campaignDayBudgetOptions);

        executeJob();
        Map<Long, CampaignDayBudgetOptions> dayBudgetCampOptionsMap =
                campaignRepository.getDayBudgetOptions(shard, singletonList(cid));

        expectedMap.put(cid, campaignDayBudgetOptions
                .withDayBudgetDailyChangeCount(DEFAULT_DAY_BUDGET_CHANGE_COUNT)
                .withDayBudgetNotificationStatus(DayBudgetNotificationStatus.READY)
                .withDayBudgetStopTime(null));
        assertThat("должны сброситься все поля дневного бюджета т.к. у кампании day_budget_stop_time < now()",
                dayBudgetCampOptionsMap,
                beanDiffer(expectedMap).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    void checkUpdateBudgetJobResetOnlyDayBudgetDailyChangeCount() {
        //Отрезаем nano секунды т.к. в базе они не хранятся а при сравнении(beandiffer-ом) будут мешать
        campaignDayBudgetOptions.setDayBudgetStopTime(LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS));
        campaignRepository.updateDayBudgetOptions(shard, campaignDayBudgetOptions);

        executeJob();
        Map<Long, CampaignDayBudgetOptions> dayBudgetCampOptionsMap =
                campaignRepository.getDayBudgetOptions(shard, singletonList(cid));

        expectedMap.put(cid, campaignDayBudgetOptions.withDayBudgetDailyChangeCount(DEFAULT_DAY_BUDGET_CHANGE_COUNT));
        assertThat(
                "должен сброситься только day_budget_daily_change_count т.к. у кампании day_budget_stop_time > now()",
                dayBudgetCampOptionsMap,
                beanDiffer(expectedMap).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields()));
    }

    @Test
    void checkUpdateBudgetJobResetOnlyDayBudgetDailyChangeCountForStrategies() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();

        CampaignInfo defaultCampaign = steps.textCampaignSteps().createDefaultCampaign(clientInfo);

        cid = defaultCampaign.getCampaignId();
        shard = defaultCampaign.getShard();

        var strategyId = getStrategyIdForCampaign(cid);

        StrategyWithDayBudget strategy = getStrategyById(strategyId);

        campaignDayBudgetOptions = new Campaign()
                .withId(cid)
                .withDayBudgetDailyChangeCount(1L)
                .withDayBudgetNotificationStatus(DayBudgetNotificationStatus.SENT)
                .withDayBudgetStopTime(LocalDateTime.now().minusMinutes(1));


        job = new UpdateDayBudgetJob(shard, campaignRepository, strategyTypedRepository, strategyModifyRepository, mockPpcPropertiesSupport());

        //Отрезаем nano секунды т.к. в базе они не хранятся а при сравнении(beandiffer-ом) будут мешать
        campaignDayBudgetOptions.setDayBudgetStopTime(LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.SECONDS));
        campaignRepository.updateDayBudgetOptions(shard, campaignDayBudgetOptions);

        final int nonZeroDayBudgetDailyChangeCount = 3;

        strategyModifyRepository.updateStrategiesTable(shard, List.of(
                ModelChanges.build(strategy, StrategyWithDayBudget.DAY_BUDGET_DAILY_CHANGE_COUNT, nonZeroDayBudgetDailyChangeCount).applyTo(strategy)
        ));

        checkDayBudgetDailyCountForStrategy(strategyId, nonZeroDayBudgetDailyChangeCount);

        executeJob();

        checkDayBudgetDailyCountForStrategy(strategyId, 0);
    }

    private void checkDayBudgetDailyCountForStrategy(long strategyId, int dayBudgetDailyChangeCount) {
        StrategyWithDayBudget updatedStrategy = getStrategyById(strategyId);
        Assertions.assertThat(updatedStrategy.getDayBudgetDailyChangeCount()).isEqualTo(dayBudgetDailyChangeCount);
    }

    private StrategyWithDayBudget getStrategyById(long strategyId) {
        return StreamEx.of(strategyTypedRepository.getTyped(shard, List.of(strategyId)))
                .select(StrategyWithDayBudget.class)
                .toList()
                .get(0);
    }

    private long getStrategyIdForCampaign(Long cid) {
        return campaignRepository.getStrategyIdsByCampaignIds(shard, List.of(cid)).get(cid);
    }
}
