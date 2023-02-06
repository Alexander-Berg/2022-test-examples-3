package ru.yandex.direct.core.entity.autobudget.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestAutobudgetAlerts;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.CampaignSteps;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AutobudgetHourlyAlertRepositoryTest {
    @Autowired
    private CampaignSteps campaignSteps;

    @Autowired
    private AutobudgetHourlyAlertRepository alertRepository;

    private CampaignInfo campaignInfo;
    private Long testCampaignId;
    private int shard;

    @Before
    public void prepare() {
        campaignInfo = campaignSteps.createActiveTextCampaign();
        testCampaignId = campaignInfo.getCampaignId();
        shard = campaignInfo.getShard();
    }

    @Test
    public void getAlerts_HasOneAlert() {
        HourlyAutobudgetAlert expectedAlert = TestAutobudgetAlerts.defaultActiveHourlyAlert(testCampaignId);
        alertRepository.addAlerts(shard, singleton(expectedAlert));
        Map<Long, HourlyAutobudgetAlert> alertsFromDb = alertRepository.getAlerts(shard, singleton(testCampaignId));
        assertThat("Из базы должна быть получена одна запись", alertsFromDb.keySet(), hasSize(1));
        assertThat("Запись, полученная из базы, отличается от ожидаемой",
                alertsFromDb.get(testCampaignId), beanDiffer(expectedAlert));
    }

    @Test
    public void getAlerts_HasTwoAlerts() {
        long testCampaignId2 = campaignSteps.createActiveCampaign(campaignInfo.getClientInfo()).getCampaignId();

        HourlyAutobudgetAlert expectedAlert1 = TestAutobudgetAlerts.defaultActiveHourlyAlert(testCampaignId);
        HourlyAutobudgetAlert expectedAlert2 = new HourlyAutobudgetAlert()
                .withCid(testCampaignId2)
                .withLastUpdate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS))
                .withOverdraft(923482L)
                .withProblems(EnumSet.of(AutobudgetHourlyProblem.MAX_BID_REACHED, AutobudgetHourlyProblem.IN_ROTATION))
                .withStatus(AutobudgetCommonAlertStatus.FROZEN);

        alertRepository.addAlerts(shard, asList(expectedAlert1, expectedAlert2));

        Map<Long, HourlyAutobudgetAlert> alertsFromDb =
                alertRepository.getAlerts(shard, asList(testCampaignId, testCampaignId2));
        assertThat("Из базы должно быть получено две записи", alertsFromDb.keySet(), hasSize(2));
        assertThat("Запись для первой кампании, полученная из базы, отличается от ожидаемой",
                alertsFromDb.get(testCampaignId), beanDiffer(expectedAlert1));
        assertThat("Запись для второй кампании, полученная из базы, отличается от ожидаемой",
                alertsFromDb.get(testCampaignId2), beanDiffer(expectedAlert2));
    }

    @Test
    public void getAlert_NoAlert() {
        Map<Long, HourlyAutobudgetAlert> alertsFromDb = alertRepository.getAlerts(shard, singleton(testCampaignId));
        assertThat("Из базы не должно быть получено записей", alertsFromDb.keySet(), hasSize(0));
    }

    @Test
    public void freezeAlerts_HasAlert_AlertIsFrozen() {
        HourlyAutobudgetAlert expectedAlert = TestAutobudgetAlerts.defaultActiveHourlyAlert(testCampaignId);
        alertRepository.addAlerts(shard, singleton(expectedAlert));

        expectedAlert.setStatus(AutobudgetCommonAlertStatus.FROZEN);
        alertRepository.freezeAlerts(shard, singleton(testCampaignId));
        Map<Long, HourlyAutobudgetAlert> alertsFromDb = alertRepository.getAlerts(shard, singleton(testCampaignId));
        assertThat("Из базы должны быть получена одна запись", alertsFromDb.keySet(), hasSize(1));
        // важно не проверять поле lastUpdate, т.к. оно меняется при заморозке
        assertThat("Запись, полученная из базы, отличается от ожидаемой", alertsFromDb.get(testCampaignId),
                beanDiffer(expectedAlert).useCompareStrategy(allFieldsExcept(newPath("lastUpdate"))));
    }

    @Test
    public void freezeAlerts_NoAlert() {
        alertRepository.freezeAlerts(shard, singleton(testCampaignId));
        Map<Long, HourlyAutobudgetAlert> alertsFromDb = alertRepository.getAlerts(shard, singleton(testCampaignId));
        assertThat("Из базы не должно быть получено записей", alertsFromDb.keySet(), hasSize(0));
    }

    // тест на то, что вызов не падает, поэтому нет assert
    @Test
    public void addAlerts_EmptyList() {
        alertRepository.addAlerts(shard, new ArrayList<>());
    }
}
