package ru.yandex.direct.core.entity.autobudget.service;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Function;

import one.util.streamex.StreamEx;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContextManager;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestAutobudgetAlerts.defaultActiveHourlyAlert;

@CoreTest
@RunWith(Parameterized.class)
public class AutobudgetAlertServiceFreezingReasonsTest {

    @Parameterized.Parameter
    public EnumSet<AutobudgetHourlyProblem> problem;

    @Parameterized.Parameter(1)
    public Boolean mustBeFrozen;

    @Autowired
    private AutobudgetAlertService autobudgetAlertService;

    @Autowired
    private AutobudgetHourlyAlertRepository alertRepository;

    @Autowired
    private CampaignSteps campaignSteps;

    private CampaignInfo campaignInfo;
    private long campaignId;

    @Parameterized.Parameters(name = "Набор роблем: {0}. Должно быть заморожено: {1}")
    public static Collection<Object[]> data() {
        Function<AutobudgetHourlyProblem, Boolean> freezingDependencyFn =
                problem -> problem == AutobudgetHourlyProblem.MAX_BID_REACHED ||
                        problem == AutobudgetHourlyProblem.MARGINAL_PRICE_REACHED ||
                        problem == AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED;

        return StreamEx.of(AutobudgetHourlyProblem.values())
                .mapToEntry(freezingDependencyFn)
                .mapKeyValue((key, value) -> new Object[]{EnumSet.of(key), value})
                // добавляем кейсов, где одна проблема перекрывает другие
                .append(new Object[]{EnumSet.of(
                        AutobudgetHourlyProblem.WALLET_DAILY_BUDGET_REACHED,
                        AutobudgetHourlyProblem.MAX_BID_REACHED), false})
                .append(new Object[]{EnumSet.of(
                        AutobudgetHourlyProblem.ENGINE_MIN_COST_LIMITED,
                        AutobudgetHourlyProblem.MAX_BID_REACHED), false})
                .toList();
    }

    @Before
    public void before() throws Exception {
        new TestContextManager(getClass()).prepareTestInstance(this);

        campaignInfo = campaignSteps.createActiveTextCampaign();
        campaignId = campaignInfo.getCampaignId();

        HourlyAutobudgetAlert expectedAlert = defaultActiveHourlyAlert(campaignId)
                .withProblems(problem)
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        alertRepository.addAlerts(campaignInfo.getShard(), singleton(expectedAlert));
    }

    @Test
    public void freezeAlertsOnKeywordsChange_StatusIsValidDependingOnProblem() {
        AutobudgetCommonAlertStatus expectedStatus = mustBeFrozen ?
                AutobudgetCommonAlertStatus.FROZEN : AutobudgetCommonAlertStatus.ACTIVE;

        autobudgetAlertService.freezeAlertsOnKeywordsChange(campaignInfo.getClientId(), singleton(campaignId));
        Map<Long, HourlyAutobudgetAlert> actualAlerts =
                alertRepository.getAlerts(campaignInfo.getShard(), singleton(campaignId));
        assertThat("Статус предупреждения не соответствует ожидаемому",
                actualAlerts.get(campaignId).getStatus(), is(expectedStatus));
    }
}
