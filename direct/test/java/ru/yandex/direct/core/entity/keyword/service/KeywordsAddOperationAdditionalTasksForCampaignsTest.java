package ru.yandex.direct.core.entity.keyword.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationAdditionalTasksForCampaignsTest extends KeywordsAddOperationBaseTest {

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Test
    public void execute_OneCampaign_CampaignIsChanged() {
        createOneActiveAdGroup();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_TwoCampaigns_BothCampaignsAreChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
        assertCampaignIsChanged(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsValidAndForSecondIsNot_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
        assertCampaignIsNotChanged(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsAddedAndForSecondIsDuplicated_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());

        keywordSteps.createKeyword(adGroupInfo2, defaultKeyword().withPhrase(PHRASE_2));

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_2)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
        assertCampaignIsNotChanged(adGroupInfo2.getCampaignInfo());
    }

    private void assertCampaignIsChanged(CampaignInfo campaignInfo) {
        LocalDateTime autobudgetForecastDate =
                testCampaignRepository.getAutobudgetForecastDate(campaignInfo.getShard(), campaignInfo.getCampaignId());
        assumeThat(autobudgetForecastDate, nullValue());
    }

    private void assertCampaignIsNotChanged(CampaignInfo campaignInfo) {
        LocalDateTime autobudgetForecastDate =
                testCampaignRepository.getAutobudgetForecastDate(campaignInfo.getShard(), campaignInfo.getCampaignId());
        assumeThat(autobudgetForecastDate, approximatelyNow());
    }
}
