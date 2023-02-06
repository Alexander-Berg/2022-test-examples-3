package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.testing.data.TestGroups.activeTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationAdditionalTasksForAdGroupTest extends KeywordsAddOperationBaseTest {

    private static final CompareStrategy COMPARE_STRATEGY = onlyExpectedFields();

    @Test
    public void execute_AdGroupIsDraft_AdGroupIsChangedExceptStatusModerate() {
        createOneDraftAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertDraftAdGroupStatusesChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsActive_AdGroupIsChangedWithStatusModerate() {
        createOneActiveAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertActiveAdGroupStatusesChanged(adGroupInfo1);
    }

    @Test
    public void execute_AdGroupIsRejected_AdGroupIsChangedWithStatusModerateWithoutPostModerate() {
        createOneRejectedAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertRejectedAdGroupStatusesChanged(adGroupInfo1);
    }

    @Test
    public void execute_DraftAndActiveAdGroups_BothAdGroupsIsChangedCorrectly() {
        createActiveAndDraftAdGroups();

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo2, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertActiveAdGroupStatusesChanged(adGroupInfo1);
        assertDraftAdGroupStatusesChanged(adGroupInfo2);
    }

    @Test
    public void execute_AllKeywordsForAdGroupAreInvalid_AdGroupIsNotChanged() {
        createOneActiveAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessful(false));

        assertActiveAdGroupStatusesNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_KeywordForAdGroupIsDuplicatedWithExisting_AdGroupIsNotChanged() {
        createOneActiveAdGroup();

        Keyword keyword = defaultKeyword().withPhrase(PHRASE_1).withNormPhrase(PHRASE_1);
        keywordSteps.createKeyword(adGroupInfo1, keyword);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isNotAdded(PHRASE_1)));

        assertActiveAdGroupStatusesNotChanged(adGroupInfo1);
    }

    @Test
    public void execute_KeywordForAdGroupIsDuplicatedExisting_AdGroupIsNotChanged() {
        createOneActiveAdGroup();

        List<Keyword> keywords = asList(
                clientKeyword(adGroupInfo1, PHRASE_1),
                clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isNotAdded(PHRASE_1)));

        assertActiveAdGroupStatusesChanged(adGroupInfo1);
    }

    @Override
    protected void createOneActiveAdGroup() {
        AdGroup adGroup = makeAdGroupActive(activeTextAdGroup(null));
        adGroupInfo1 = adGroupSteps.createAdGroup(adGroup, clientInfo);
    }

    protected void createOneRejectedAdGroup() {
        AdGroup adGroup = makeAdGroupRejected(activeTextAdGroup(null));
        adGroupInfo1 = adGroupSteps.createAdGroup(adGroup);
        clientInfo = adGroupInfo1.getClientInfo();
    }

    protected void createOneDraftAdGroup() {
        AdGroup adGroup = makeAdGroupDraft(defaultTextAdGroup(null));
        adGroupInfo1 = adGroupSteps.createAdGroup(adGroup, clientInfo);
    }

    protected void createActiveAndDraftAdGroups() {
        createOneActiveAdGroup();

        AdGroup draftAdGroup = makeAdGroupDraft(defaultTextAdGroup(null));
        adGroupInfo2 = adGroupSteps.createAdGroup(draftAdGroup, adGroupInfo1.getCampaignInfo());
    }

    private void assertDraftAdGroupStatusesChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeExpectedDraftAdGroupWithChangedStatuses(new TextAdGroup()));
    }

    private void assertActiveAdGroupStatusesChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeExpectedActiveAdGroupWithChangedStatuses(new TextAdGroup()));
    }

    private void assertRejectedAdGroupStatusesChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeExpectedRejectedAdGroupWithChangedStatuses(new TextAdGroup()));
    }

    private void assertActiveAdGroupStatusesNotChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeAdGroupActive(new TextAdGroup()));
    }

    private void assertAdGroup(AdGroupInfo adGroupInfo, AdGroup expectedAdGroup) {
        AdGroup adGroup = adGroupRepository
                .getAdGroups(adGroupInfo.getShard(), singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat("состояние группы не соответствует ожидаемому",
                adGroup, beanDiffer(expectedAdGroup).useCompareStrategy(COMPARE_STRATEGY));
    }

    private AdGroup makeAdGroupDraft(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.SENDING)
                .withStatusShowsForecast(StatusShowsForecast.ARCHIVED)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.YES);
    }

    private AdGroup makeAdGroupActive(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.SENDING)
                .withStatusShowsForecast(StatusShowsForecast.ARCHIVED)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.YES);
    }

    private AdGroup makeAdGroupRejected(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.SENDING)
                .withStatusShowsForecast(StatusShowsForecast.ARCHIVED)
                .withStatusModerate(StatusModerate.YES)
                .withStatusPostModerate(StatusPostModerate.REJECTED);
    }

    private AdGroup makeExpectedDraftAdGroupWithChangedStatuses(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusModerate(StatusModerate.NEW)
                .withStatusPostModerate(StatusPostModerate.YES);
    }

    private AdGroup makeExpectedActiveAdGroupWithChangedStatuses(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.NO);
    }

    private AdGroup makeExpectedRejectedAdGroupWithChangedStatuses(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusModerate(StatusModerate.READY)
                .withStatusPostModerate(StatusPostModerate.REJECTED);
    }
}
