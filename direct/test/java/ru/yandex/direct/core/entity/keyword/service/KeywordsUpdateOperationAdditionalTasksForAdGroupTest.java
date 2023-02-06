package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.repository.TestBidsRepository;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATE_EVERY_KEYWORD_CHANGE;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksForAdGroupTest extends KeywordsUpdateOperationBaseTest {

    @Autowired
    private TestBidsRepository testBidsRepository;

    private static final CompareStrategy COMPARE_STRATEGY = onlyExpectedFields()
            .forFields(newPath(AdGroup.LAST_CHANGE.name())).useMatcher(approximatelyNow());

    @Test
    public void execute_AdGroupIsActive_NoChange_AdGroupNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveAdGroupStatusesIsNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_KeywordForAdGroupIsDuplicatedWithExisting_AdGroupIsNotChanged() {
        createOneActiveAdGroup();
        Long existingKeywordId = createKeyword(adGroupInfo1, PHRASE_2).getId();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, PHRASE_2)));

        assertActiveAdGroupOnlyStatusBsSyncedChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_NormPhraseNotChange_AdGroupChangedCorrectly() {
        String phrase = "купить слон";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();

        String newPhrase = "купивший слона";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        if (ppcPropertiesSupport.get(MODERATE_EVERY_KEYWORD_CHANGE).get()) {
            assertActiveAdGroupStatusesChanged(adGroupInfo1);
        } else {
            assertActiveAdGroupStatusesChangedExceptStatusModerate(adGroupInfo1);
        }
    }

    @Test
    public void execute_AdGroupIsActive_ChangePhraseInsignificantly_AdGroupChangedCorrectly() {
        String phrase = "купить";
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, phrase).getId();

        String newPhrase = "купить слона";
        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, newPhrase));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, newPhrase)));

        assertActiveAdGroupStatusesChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_ChangePhraseSignificantly_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        assertActiveAdGroupStatusesChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_SuspendKeyword_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(true, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1, true)));

        assertActiveAdGroupOnlyStatusBsSyncedChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_SuspendKeyword_NoConditions_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        // удаляем условие показа фразы
        testBidsRepository.deleteBid(adGroupInfo1.getShard(), keywordIdToUpdate);

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(true, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1, true)));

        assertActiveAdGroupOnlyStatusBsSyncedChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_ChangeAndSuspendKeyword_NoConditions_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();
        // удаляем условие показа фразы
        testBidsRepository.deleteBid(adGroupInfo1.getShard(), keywordIdToUpdate);

        ModelChanges<Keyword> changesKeywords = keywordModelChanges(keywordIdToUpdate, PHRASE_2)
                .process(true, Keyword.IS_SUSPENDED);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2, true)));

        assertAdGroup(adGroupInfo1,
                makeExpectedActiveAdGroupWithChangedStatuses(new TextAdGroup())
                        .withStatusPostModerate(StatusPostModerate.REJECTED)
                        .withStatusBsSynced(StatusBsSynced.NO));
    }

    @Test
    public void execute_AdGroupIsActive_ChangePrice_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveAdGroupStatusesIsNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_ChangePriceContext_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE_CONTEXT);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveAdGroupStatusesIsNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_ChangeAutoBudgetPriority_AdGroupChangedCorrectly() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process(5, Keyword.AUTOBUDGET_PRIORITY);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveAdGroupStatusesIsNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_ChangeHrefParam1_AdGroupStatusBsSyncedNo() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process("newParam1", Keyword.HREF_PARAM2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveAdGroupStatusBsSyncedNo(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_ChangeHrefParam2_AdGroupStatusBsSyncedNo() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeywords = new ModelChanges<>(keywordIdToUpdate, Keyword.class)
                .process("newParam2", Keyword.HREF_PARAM2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeywords));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        assertActiveAdGroupStatusBsSyncedNo(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsDraft_AdGroupNotChanged() {
        createOneDraftAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        assertDraftAdGroupStatusesNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsSentToModeration_ChangePhrase_AdGroupStatusModerateReady() {
        createOneSentModerateAdGroup();
        Long keywordIdToUpdate = createDraftKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = singletonList(keywordModelChanges(keywordIdToUpdate, PHRASE_2));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_2)));

        assertModerateAdGroupStatusesChanged(adGroupInfo1);
    }

    @Test
    public void execute_TwoAdGroups_BothAdGroupsIsChangedCorrectly() {
        createActiveAndDraftAdGroups();
        assumeAdGroupIsActiveAndReadyForTest(adGroupInfo1);
        assumeAdGroupIsDraftAndReadyForTest(adGroupInfo2);
        Long keywordIdToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1).getId();
        Long keywordIdToUpdate2 = createKeyword(adGroupInfo2, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordIdToUpdate1, PHRASE_2),
                keywordModelChanges(keywordIdToUpdate2, PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate1, PHRASE_2),
                isUpdated(keywordIdToUpdate2, PHRASE_3)));

        assertActiveAdGroupStatusesChanged(adGroupInfo1);
        assertDraftAdGroupStatusesNotChanged(adGroupInfo2);
    }

    @Test
    public void execute_AllKeywordsForAdGroupAreInvalid_AdGroupIsNotChanged() {
        createOneActiveAdGroup();
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        List<ModelChanges<Keyword>> changesKeywords =
                singletonList(keywordModelChanges(keywordIdToUpdate, INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessful(false));

        assertActiveAdGroupStatusesIsNotChanged(adGroupInfo1);
    }

    @Override
    protected void createOneActiveAdGroup() {
        AdGroup adGroup = makeAdGroupActiveAndReadyForTest(activeTextAdGroup(null));
        adGroupInfo1 = steps.adGroupSteps().createAdGroup(adGroup, clientInfo);
        assumeAdGroupIsActiveAndReadyForTest(adGroupInfo1);
    }

    protected void createOneDraftAdGroup() {
        AdGroup adGroup = makeAdGroupDraftAndReadyForTest(defaultTextAdGroup(null));
        adGroupInfo1 = steps.adGroupSteps().createAdGroup(adGroup, clientInfo);
        assumeAdGroupIsDraftAndReadyForTest(adGroupInfo1);
    }

    protected void createOneSentModerateAdGroup() {
        AdGroup adGroup = makeAdGroupSentToModerationAndReadyForTest(defaultTextAdGroup(null));
        adGroupInfo1 = steps.adGroupSteps().createAdGroup(adGroup, clientInfo);
        assumeAdGroupIsSentToModerationAndReadyForTest(adGroupInfo1);
    }

    protected void createActiveAndDraftAdGroups() {
        createOneActiveAdGroup();

        AdGroup draftAdGroup = makeAdGroupDraftAndReadyForTest(defaultTextAdGroup(null));
        adGroupInfo2 = steps.adGroupSteps().createAdGroup(draftAdGroup, adGroupInfo1.getCampaignInfo());
    }

    private void assertDraftAdGroupStatusesNotChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeAdGroupDraftAndReadyForTest(new TextAdGroup()));
    }

    private void assertActiveAdGroupOnlyStatusBsSyncedChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo,
                makeAdGroupActiveAndReadyForTest(new TextAdGroup()).withStatusBsSynced(StatusBsSynced.NO));
    }

    private void assertActiveAdGroupStatusesChangedExceptStatusModerate(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo,
                makeExpectedActiveAdGroupWithChangedStatuses(new TextAdGroup()).withStatusModerate(StatusModerate.YES));
    }

    private void assertActiveAdGroupStatusesChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeExpectedActiveAdGroupWithChangedStatuses(new TextAdGroup()));
    }

    private void assertActiveAdGroupStatusesIsNotChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeAdGroupActiveAndReadyForTest(new TextAdGroup()));
    }

    private void assertActiveAdGroupStatusBsSyncedNo(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo,
                makeAdGroupActiveAndReadyForTest(new TextAdGroup()).withStatusBsSynced(StatusBsSynced.NO));
    }

    private void assertModerateAdGroupStatusesChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeAdGroupSentToModerationAndReadyForTest(new TextAdGroup())
                .withStatusModerate(StatusModerate.READY));
    }

    private void assertAdGroup(AdGroupInfo adGroupInfo, AdGroup expectedAdGroup) {
        AdGroup adGroup = adGroupRepository
                .getAdGroups(adGroupInfo.getShard(), singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat("состояние группы не соответствует ожидаемому",
                adGroup, beanDiffer(expectedAdGroup).useCompareStrategy(COMPARE_STRATEGY));
    }

    private void assumeAdGroupIsDraftAndReadyForTest(AdGroupInfo adGroupInfo) {
        assumeAdGroup(adGroupInfo, makeAdGroupDraftAndReadyForTest(new TextAdGroup()));
    }

    private void assumeAdGroupIsSentToModerationAndReadyForTest(AdGroupInfo adGroupInfo) {
        assumeAdGroup(adGroupInfo, makeAdGroupSentToModerationAndReadyForTest(new TextAdGroup()));
    }

    private void assumeAdGroupIsActiveAndReadyForTest(AdGroupInfo adGroupInfo) {
        assumeAdGroup(adGroupInfo, makeAdGroupActiveAndReadyForTest(new TextAdGroup()));
    }

    private void assumeAdGroup(AdGroupInfo adGroupInfo, AdGroup expectedAdGroup) {
        AdGroup adGroup = adGroupRepository
                .getAdGroups(adGroupInfo.getShard(), singletonList(adGroupInfo.getAdGroupId())).get(0);
        assumeThat("невозможно провести тест, так как не выполняется предварительное условие",
                adGroup, beanDiffer(expectedAdGroup).useCompareStrategy(COMPARE_STRATEGY));
    }

    private AdGroup makeAdGroupActiveAndReadyForTest(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.YES)
                .withStatusShowsForecast(StatusShowsForecast.PROCESSED)
                .withStatusModerate(StatusModerate.YES);
    }

    private AdGroup makeAdGroupSentToModerationAndReadyForTest(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusModerate(StatusModerate.SENT);
    }

    private AdGroup makeAdGroupDraftAndReadyForTest(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.ARCHIVED)
                .withStatusModerate(StatusModerate.NEW);
    }

    private AdGroup makeExpectedActiveAdGroupWithChangedStatuses(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusModerate(StatusModerate.READY);
    }
}
