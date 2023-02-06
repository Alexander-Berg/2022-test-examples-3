package ru.yandex.direct.core.entity.keyword.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.client.model.ClientLimits;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAddedToAdGroup;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationIgnoreOversizeTest extends KeywordsAddOperationBaseTest {

    @Before
    public void before() {
        super.before();

        createTwoActiveAdGroups();

        steps.clientSteps().updateClientLimits(clientInfo
                .withClientLimits((ClientLimits) new ClientLimits().withClientId(clientInfo.getClientId())
                        .withKeywordsCountLimit(1L)));
    }

    @Test
    public void prepare_NotIgnoreOversize_ValidationResultHasErrors() {
        KeywordsAddOperation operation = createOperation(Applicability.FULL,
                asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = operation.prepare();
        assertThat(prepareResult.isPresent(), is(true));
    }

    @Test
    public void prepare_IgnoreOversize_ValidationResultSuccessful() {
        KeywordsAddOperation ignoreOversizeOperation = createOperationWithIgnoreOversize(
                asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = ignoreOversizeOperation.prepare();
        assertThat(prepareResult.isPresent(), is(false));
    }

    @Test
    public void oversizeKeywordsIndexesByAdGroupId_TwoKeywordsInAdGroup_OversizeContainsOneItem() {
        KeywordsAddOperation ignoreOversizeOperation = createOperationWithIgnoreOversize(
                asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = ignoreOversizeOperation.prepare();
        assumeThat(prepareResult.isPresent(), is(false));

        assertThat(ignoreOversizeOperation.getOversizeKeywordsIndexesByAdGroupId(),
                beanDiffer(singletonMap(adGroupInfo1.getAdGroupId(), singletonList(1))));
    }

    @Test
    public void oversizeKeywordsIndexesByAdGroupId_OneKeywordsInAdGroup_NoOversize() {
        KeywordsAddOperation ignoreOversizeOperation = createOperationWithIgnoreOversize(
                singletonList(validClientKeyword1(adGroupInfo1)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = ignoreOversizeOperation.prepare();
        assumeThat(prepareResult.isPresent(), is(false));

        assertThat(ignoreOversizeOperation.getOversizeKeywordsIndexesByAdGroupId(), beanDiffer(emptyMap()));
    }

    @Test
    public void oversizeKeywordsIndexesByAdGroupId_TwoEqualsKeywords_OversizeKeywordsIndexesContainsOnlyOne() {
        KeywordsAddOperation operation = createOperationWithIgnoreOversize(
                asList(validClientKeyword1(adGroupInfo1),
                        validClientKeyword2(adGroupInfo1),
                        validClientKeyword2(adGroupInfo1)));
        Optional<MassResult<AddedKeywordInfo>> prepareResult = operation.prepare();
        assumeThat(prepareResult.isPresent(), is(false));

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assertThat(oversizeKeywordsIndexes, beanDiffer(singletonMap(adGroupInfo1.getAdGroupId(), singletonList(1))));
    }

    @Test(expected = IllegalStateException.class)
    public void setNewAdGroupIdsByIndex_NewMapIndexesContainsNotExistIndex_CheckStateFailed() {
        KeywordsAddOperation operation = createAndPrepareWithOversizeByOneKeyword();

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.size(), is(1));

        operation.setNewAdGroupIdsByIndex(singletonMap(10, adGroupInfo1.getAdGroupId()));
    }

    @Test(expected = IllegalStateException.class)
    public void apply_setOldAdGroupId_CheckStateNoOversizeByAdGroupsFailed() {
        KeywordsAddOperation operation = createAndPrepareWithOversizeByOneKeyword();

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.size(), is(1));

        operation.setNewAdGroupIdsByIndex(singletonMap(0, adGroupInfo1.getAdGroupId()));
        operation.apply();
    }

    @Test
    public void apply_OversizeWithOneKeyword_SucccessResult() {
        KeywordsAddOperation operation = createAndPrepareWithOversizeByOneKeyword();

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.size(), is(1));

        operation.setNewAdGroupIdsByIndex(singletonMap(1, adGroupInfo2.getAdGroupId()));

        MassResult<AddedKeywordInfo> result = operation.apply();
        assertThat(result, isSuccessfulWithMatchers(
                isAddedToAdGroup(PHRASE_1, adGroupInfo1.getAdGroupId()),
                isAddedToAdGroup(PHRASE_2, adGroupInfo2.getAdGroupId())));
    }

    @Test
    public void apply_OversizeWithTwoEqualsKeywords_SuccessResult() {
        KeywordsAddOperation operation = createOperationWithIgnoreOversize(
                asList(validClientKeyword1(adGroupInfo1),
                        validClientKeyword2(adGroupInfo1),
                        validClientKeyword2(adGroupInfo1)));
        Optional<MassResult<AddedKeywordInfo>> prepareResult = operation.prepare();
        assumeThat(prepareResult.isPresent(), is(false));

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes, beanDiffer(singletonMap(adGroupInfo1.getAdGroupId(), singletonList(1))));

        operation.setNewAdGroupIdsByIndex(singletonMap(1, adGroupInfo2.getAdGroupId()));

        MassResult<AddedKeywordInfo> result = operation.apply();
        assertThat(result, isSuccessfulWithMatchers(
                isAddedToAdGroup(PHRASE_1, adGroupInfo1.getAdGroupId()),
                isAddedToAdGroup(PHRASE_2, adGroupInfo2.getAdGroupId()),
                isNotAdded(PHRASE_2)));
    }

    @Test
    public void apply_WithoutOversize_SuccessResult() {
        KeywordsAddOperation operation =
                createOperationWithIgnoreOversize(singletonList(validClientKeyword1(adGroupInfo1)));
        operation.prepare();

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.isEmpty(), is(true));

        MassResult<AddedKeywordInfo> result = operation.apply();
        assertThat(result, isSuccessfulWithMatchers(isAddedToAdGroup(PHRASE_1, adGroupInfo1.getAdGroupId())));
    }

    @Test(expected = IllegalStateException.class)
    public void apply_AdGroupsIdsInOversizeKeywordsNotChanged_CheckStateFailed() {
        KeywordsAddOperation operation = createAndPrepareWithOversizeByOneKeyword();

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.size(), is(1));

        operation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_NotEmptyNewAdGroup_CheckStateFailed() {
        steps.keywordSteps().createKeyword(adGroupInfo2);

        KeywordsAddOperation operation = createAndPrepareWithOversizeByOneKeyword();

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.size(), is(1));

        operation.setNewAdGroupIdsByIndex(singletonMap(1, adGroupInfo2.getAdGroupId()));
        operation.apply();
    }

    @Test(expected = IllegalStateException.class)
    public void apply_NewAdGroupOversize_CheckStateFailed() {
        KeywordsAddOperation operation = createOperationWithIgnoreOversize(asList(
                validClientKeyword1(adGroupInfo1),
                validClientKeyword2(adGroupInfo1),
                validClientKeyword3(adGroupInfo1)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = operation.prepare();
        assumeThat(prepareResult.isPresent(), is(false));

        Map<Long, List<Integer>> oversizeKeywordsIndexes = operation.getOversizeKeywordsIndexesByAdGroupId();
        assumeThat(oversizeKeywordsIndexes.size(), is(1));

        operation.setNewAdGroupIdsByIndex(ImmutableMap.of(
                1, adGroupInfo2.getAdGroupId(),
                2, adGroupInfo2.getAdGroupId()));
        operation.apply();
    }

    private KeywordsAddOperation createAndPrepareWithOversizeByOneKeyword() {
        KeywordsAddOperation ignoreOversizeOperation = createOperationWithIgnoreOversize(
                asList(validClientKeyword1(adGroupInfo1), validClientKeyword2(adGroupInfo1)));

        Optional<MassResult<AddedKeywordInfo>> prepareResult = ignoreOversizeOperation.prepare();
        assumeThat(prepareResult.isPresent(), is(false));

        return ignoreOversizeOperation;
    }
}
