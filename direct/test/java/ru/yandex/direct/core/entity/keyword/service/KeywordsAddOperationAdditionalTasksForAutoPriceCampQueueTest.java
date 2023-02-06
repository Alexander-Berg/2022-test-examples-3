package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.jooq.Record2;
import org.jooq.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestAutoPriceCampQueueRepository;
import ru.yandex.direct.dbschema.ppc.enums.AutoPriceCampQueueStatus;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationAdditionalTasksForAutoPriceCampQueueTest extends KeywordsAddOperationBaseTest {

    @Autowired
    private TestAutoPriceCampQueueRepository testAutoPriceCampQueueRepository;

    @Test
    public void execute_OneCampaign_CampaignIsChanged() {
        createOneActiveAdGroup();
        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo1.getCampaignInfo());

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_TwoCampaigns_BothCampaignsAreChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo1.getCampaignInfo());
        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo2.getCampaignInfo());

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsValidAndForSecondIsNot_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo1.getCampaignInfo());
        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo2.getCampaignInfo());

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
        assertAutoPriceQueueContainsCampaignInWaitStatus(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsAddedAndForSecondIsDuplicated_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = adGroupSteps.createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo1.getCampaignInfo());
        addAutoPriceCampQueueRecordInWaitStatus(adGroupInfo2.getCampaignInfo());

        keywordSteps.createKeyword(adGroupInfo2, defaultKeyword().withPhrase(PHRASE_2));

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_2)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
        assertAutoPriceQueueContainsCampaignInWaitStatus(adGroupInfo2.getCampaignInfo());
    }

    private void assertAutoPriceQueueContainsCampaignInErrorStatus(CampaignInfo campaignInfo) {
        Result<Record2<Long, AutoPriceCampQueueStatus>> result =
                testAutoPriceCampQueueRepository.getAutoPriceCampQueueRecords(campaignInfo);
        assertThat("auto_price_camp_queue должна содержать запись для кампании", result, hasSize(1));
        assertThat("статус кампании в auto_price_camp_queue должен быть равен Error",
                result.get(0).value2(), is(AutoPriceCampQueueStatus.Error));
    }

    private void assertAutoPriceQueueContainsCampaignInWaitStatus(CampaignInfo campaignInfo) {
        Result<Record2<Long, AutoPriceCampQueueStatus>> result =
                testAutoPriceCampQueueRepository.getAutoPriceCampQueueRecords(campaignInfo);
        assertThat("auto_price_camp_queue должна содержать запись для кампании", result, hasSize(1));
        assertThat("статус кампании в auto_price_camp_queue должен быть равен Wait",
                result.get(0).value2(), is(AutoPriceCampQueueStatus.Wait));
    }

    private void assumeAutoPriceQueueContainsCampaignInWaitStatus(CampaignInfo campaignInfo) {
        Result<Record2<Long, AutoPriceCampQueueStatus>> result =
                testAutoPriceCampQueueRepository.getAutoPriceCampQueueRecords(campaignInfo);
        assumeThat("auto_price_camp_queue должна содержать запись для кампании для проведения теста",
                result, hasSize(1));
        assertThat("статус кампании в auto_price_camp_queue должен быть равен Wait для проведения теста",
                result.get(0).value2(), is(AutoPriceCampQueueStatus.Wait));
    }

    private void addAutoPriceCampQueueRecordInWaitStatus(CampaignInfo campaignInfo) {
        testAutoPriceCampQueueRepository.addDefaultAutoPriceCampQueueRecordInWaitStatus(campaignInfo);

        assumeAutoPriceQueueContainsCampaignInWaitStatus(campaignInfo);
    }
}
