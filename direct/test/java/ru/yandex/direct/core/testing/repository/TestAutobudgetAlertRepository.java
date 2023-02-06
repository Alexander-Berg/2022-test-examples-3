package ru.yandex.direct.core.testing.repository;

import java.util.Map;

import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.testing.data.TestAutobudgetAlerts;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

public class TestAutobudgetAlertRepository {

    @Autowired
    private AutobudgetHourlyAlertRepository alertRepository;

    public void addAutobudgetAlert(CampaignInfo campaignInfo) {
        addAutobudgetAlert(campaignInfo.getShard(), campaignInfo.getCampaignId());
    }

    public void addAutobudgetAlert(int shard, Long campaignId) {
        HourlyAutobudgetAlert alert = TestAutobudgetAlerts.defaultActiveHourlyAlert(campaignId)
                .withProblems(singleton(AutobudgetHourlyProblem.UPPER_POSITIONS_REACHED))
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE);
        alertRepository.addAlerts(shard, singleton(alert));

        Map<Long, HourlyAutobudgetAlert> actualAlerts =
                alertRepository.getAlerts(shard, singleton(campaignId));
        assumeThat("Статус предупреждение должен соответствовать ожидаемому",
                actualAlerts.get(campaignId).getStatus(), is(AutobudgetCommonAlertStatus.ACTIVE));
    }

    public void assertAutobudgetAlertFrozen(AdGroupInfo adGroupInfo) {
        assertAutobudgetAlertFrozen(adGroupInfo.getShard(), adGroupInfo.getCampaignId());
    }

    public void assertAutobudgetAlertNotFrozen(AdGroupInfo adGroupInfo) {
        assertAutobudgetAlertNotFrozen(adGroupInfo.getShard(), adGroupInfo.getCampaignId());
    }

    public void assertAutobudgetAlertFrozen(int shard, Long campaignId) {
        assertAutobudgetAlertStatus(shard, campaignId, AutobudgetCommonAlertStatus.FROZEN);
    }

    public void assertAutobudgetAlertNotFrozen(int shard, Long campaignId) {
        assertAutobudgetAlertStatus(shard, campaignId, AutobudgetCommonAlertStatus.ACTIVE);
    }

    private void assertAutobudgetAlertStatus(
            int shard, Long campaignId,
            AutobudgetCommonAlertStatus autobudgetCommonAlertStatus) {
        Map<Long, HourlyAutobudgetAlert> actualAlerts =
                alertRepository.getAlerts(shard, singleton(campaignId));
        Assert.assertThat("Неожиданный статус предупреждения",
                actualAlerts.get(campaignId).getStatus(), is(autobudgetCommonAlertStatus));
    }
}
