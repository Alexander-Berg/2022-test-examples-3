package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksForCampaignsTest extends KeywordsUpdateOperationBaseTest {

    @Autowired
    private TestCampaignRepository testCampaignRepository;

    @Test
    public void execute_NoChange_CampaignNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        ModelChanges<Keyword> keyword = keywordModelChanges(keywordIdToUpdate, PHRASE_1);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(keyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertCampaignIsNotChanged(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangePhrase_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        ModelChanges<Keyword> keyword = keywordModelChanges(keywordIdToUpdate, PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(keyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangePrice_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        ModelChanges<Keyword> keyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(keyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangePriceContext_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        ModelChanges<Keyword> keyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE_CONTEXT);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(keyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangeAutoBudgetPriority_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        ModelChanges<Keyword> keyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(5, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(keyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_SuspendKeyword_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());

        ModelChanges<Keyword> keyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(true, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(keyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1, true)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
    }


    @Test
    public void execute_TwoCampaigns_BothCampaignsAreChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());

        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
        assertCampaignIsChanged(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsValidAndForSecondIsNot_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());

        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2), null));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
        assertCampaignIsNotChanged(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsUpdatedAndForSecondIsDuplicated_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        Long existingKeywordId = createKeyword(adGroupInfo2, PHRASE_2).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo1.getShard(), adGroupInfo1.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(adGroupInfo2.getShard(), adGroupInfo2.getCampaignId());

        List<ModelChanges<Keyword>> keywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isNotUpdated(existingKeywordId, PHRASE_2)));

        assertCampaignIsChanged(adGroupInfo1.getCampaignInfo());
        assertCampaignIsChanged(adGroupInfo2.getCampaignInfo());
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
