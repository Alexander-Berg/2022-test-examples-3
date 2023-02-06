package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;

import org.jooq.Record2;
import org.jooq.Result;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.repository.TestAutoPriceCampQueueRepository;
import ru.yandex.direct.dbschema.ppc.enums.AutoPriceCampQueueStatus;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksForAutoPriceCampQueueTest extends KeywordsUpdateOperationBaseTest {

    @Autowired
    private TestAutoPriceCampQueueRepository testAutoPriceCampQueueRepository;

    @Test
    public void execute_NoChange_CampaignNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_1);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertAutoPriceQueueContainsCampaignInWaitStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangePhrase_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangePrice_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangePriceContext_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE_CONTEXT);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_ChangeAutoBudgetPriority_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(5, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_SuspendKeyword_CampaignIsChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(true, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1, true)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
    }

    @Test
    public void execute_TwoCampaigns_BothCampaignsAreChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo2.getCampaignInfo());

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_2)));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignIsValidAndForSecondIsNot_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo2.getCampaignInfo());

        List<ModelChanges<Keyword>> changesKeywords = asList(
                keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2), null));

        assertAutoPriceQueueContainsCampaignInErrorStatus(adGroupInfo1.getCampaignInfo());
        assertAutoPriceQueueContainsCampaignInWaitStatus(adGroupInfo2.getCampaignInfo());
    }

    @Test
    public void execute_KeywordForOneCampaignisUpdatedAndForSecondIsDuplicated_OneCampaignIsChanged() {
        createOneActiveAdGroup();
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(activeTextAdGroup(), adGroupInfo1.getClientInfo());

        Long existingKeywordId = createKeyword(adGroupInfo2, PHRASE_1).getId();
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2).getId();

        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo1.getCampaignInfo());
        addAutoPriceCampQueueRecordInWaitStatusWithAssume(adGroupInfo2.getCampaignInfo());

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isNotUpdated(existingKeywordId, PHRASE_1)));

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

    private void addAutoPriceCampQueueRecordInWaitStatusWithAssume(CampaignInfo campaignInfo) {
        testAutoPriceCampQueueRepository.addDefaultAutoPriceCampQueueRecordInWaitStatus(campaignInfo);
        assumeAutoPriceQueueContainsCampaignInWaitStatus(campaignInfo);
    }

}
