package ru.yandex.direct.core.entity.keyword.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.autobudget.model.AutobudgetCommonAlertStatus;
import ru.yandex.direct.core.entity.autobudget.model.AutobudgetHourlyProblem;
import ru.yandex.direct.core.entity.autobudget.model.HourlyAutobudgetAlert;
import ru.yandex.direct.core.entity.autobudget.repository.AutobudgetHourlyAlertRepository;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationAdditionalTasksForFreezeAlertsTest extends KeywordsAddOperationBaseTest {

    @Autowired
    private AutobudgetHourlyAlertRepository alertRepository;

    @Test
    public void execute_OneCampaign_CampaignIsChanged() {
        createOneActiveAdGroup();
        addAlert(adGroupInfo1.getCampaignInfo());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertAlertFrozen(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_TwoCampaigns_BothCampaignsAreChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        addAlert(adGroupInfo1.getCampaignInfo());
        addAlert(adGroupInfo2.getCampaignInfo());

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertAlertFrozen(adGroupInfo1.getCampaignInfo());
        assertAlertFrozen(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsValidAndForSecondIsNot_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        addAlert(adGroupInfo1.getCampaignInfo());
        addAlert(adGroupInfo2.getCampaignInfo());

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));

        assertAlertFrozen(adGroupInfo1.getCampaignInfo());
        assertAlertNotFrozen(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsAddedAndForSecondIsDuplicated_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        addAlert(adGroupInfo1.getCampaignInfo());
        addAlert(adGroupInfo2.getCampaignInfo());

        keywordSteps.createKeyword(adGroupInfo2, defaultKeyword().withPhrase(PHRASE_2));

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_2)));

        assertAlertFrozen(adGroupInfo1.getCampaignInfo());
        assertAlertNotFrozen(adGroupInfo2.getCampaignInfo());
    }

    private void addAlert(CampaignInfo campaignInfo) {
        HourlyAutobudgetAlert alert = new HourlyAutobudgetAlert()
                .withCid(campaignInfo.getCampaignId())
                .withLastUpdate(LocalDateTime.now())
                .withOverdraft(10L)
                .withStatus(AutobudgetCommonAlertStatus.ACTIVE)
                .withProblems(ImmutableSet.of(AutobudgetHourlyProblem.MAX_BID_REACHED));
        alertRepository.addAlerts(adGroupInfo1.getShard(), singletonList(alert));
    }

    private void assertAlertFrozen(CampaignInfo campaignInfo) {
        Map<Long, HourlyAutobudgetAlert> alerts = alertRepository
                .getAlerts(campaignInfo.getShard(), singleton(campaignInfo.getCampaignId()));
        assertThat(alerts.get(campaignInfo.getCampaignId()).getStatus(),
                equalTo(AutobudgetCommonAlertStatus.FROZEN));
    }

    private void assertAlertNotFrozen(CampaignInfo campaignInfo) {
        Map<Long, HourlyAutobudgetAlert> alerts = alertRepository
                .getAlerts(campaignInfo.getShard(), singleton(campaignInfo.getCampaignId()));
        assertThat(alerts.get(campaignInfo.getCampaignId()).getStatus(),
                equalTo(AutobudgetCommonAlertStatus.ACTIVE));
    }
}
