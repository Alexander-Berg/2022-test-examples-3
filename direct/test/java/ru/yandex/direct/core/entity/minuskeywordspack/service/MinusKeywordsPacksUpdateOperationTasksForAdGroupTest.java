package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusModerate;
import ru.yandex.direct.core.entity.adgroup.model.StatusShowsForecast;
import ru.yandex.direct.core.entity.adgroup.model.TextAdGroup;
import ru.yandex.direct.core.entity.minuskeywordspack.container.UpdatedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class MinusKeywordsPacksUpdateOperationTasksForAdGroupTest extends MinusKeywordsPacksUpdateOperationBaseTest {

    private static final CompareStrategy COMPARE_STRATEGY = onlyExpectedFields()
            .forFields(newPath(AdGroup.LAST_CHANGE.name())).useMatcher(approximatelyNow());

    @Test
    public void execute_AdGroupWithMinusKeywords_NoChange_AdGroupNotChanged() {
        MinusKeywordsPack minusKeywordsPack =
                createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertActiveAdGroupStatusesIsNotChanged(textAdGroup);
    }

    @Test
    public void execute_AdGroupWithMinusKeywords_ChangeName_AdGroupNotChanged() {
        MinusKeywordsPack minusKeywordsPack =
                createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(nameModelChanges(packIdToUpdate, SECOND_NAME));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertActiveAdGroupStatusesIsNotChanged(textAdGroup);
    }

    @Test
    public void execute_AdGroupWithMinusKeywords_ChangeMinusKeywords_AdGroupChangedCorrectly() {
        MinusKeywordsPack minusKeywordsPack =
                createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertActiveAdGroupStatusesChanged(textAdGroup);
    }

    @Test
    public void execute_CampaignWithMinusKeywords_ChangeMinusKeywords_AdGroupChangedCorrectly() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate,
                textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertActiveAdGroupStatusesOnlyForecastChanged(textAdGroup);
    }

    @Test
    public void execute_MinusKeywordsLinkedToSeveralAdGroups_AdGroupsStatusesChanged() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, packIdToUpdate, textAdGroup.getAdGroupId());
        AdGroupInfo adGroup2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, packIdToUpdate, adGroup2.getAdGroupId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertActiveAdGroupStatusesChanged(textAdGroup);
        assertActiveAdGroupStatusesChanged(adGroup2);
    }

    @Test
    public void execute_MinusKeywordsLinkedToSeveralCampaigns_AdGroupsStatusesChanged() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToCampaign(textAdGroup.getShard(), packIdToUpdate,
                textAdGroup.getCampaignId());
        AdGroupInfo adGroup2 = steps.adGroupSteps().createActiveTextAdGroup(clientInfo);
        testMinusKeywordsPackRepository.linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate,
                adGroup2.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertActiveAdGroupStatusesOnlyForecastChanged(textAdGroup);
        assertActiveAdGroupStatusesOnlyForecastChanged(adGroup2);
    }

    private void assertActiveAdGroupStatusesChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeExpectedActiveAdGroupWithChangedStatuses(new TextAdGroup()));
    }

    private void assertActiveAdGroupStatusesOnlyForecastChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo,
                makeAdGroupActiveAndReadyForTest(new TextAdGroup()).withStatusShowsForecast(StatusShowsForecast.NEW));
    }

    private void assertActiveAdGroupStatusesIsNotChanged(AdGroupInfo adGroupInfo) {
        assertAdGroup(adGroupInfo, makeAdGroupActiveAndReadyForTest(new TextAdGroup()));
    }

    private void assertAdGroup(AdGroupInfo adGroupInfo, AdGroup expectedAdGroup) {
        AdGroup adGroup = adGroupRepository
                .getAdGroups(adGroupInfo.getShard(), singletonList(adGroupInfo.getAdGroupId())).get(0);
        assertThat("состояние группы не соответствует ожидаемому",
                adGroup, beanDiffer(expectedAdGroup).useCompareStrategy(COMPARE_STRATEGY));
    }

    private AdGroup makeAdGroupActiveAndReadyForTest(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.YES)
                .withStatusShowsForecast(StatusShowsForecast.PROCESSED)
                .withStatusModerate(StatusModerate.YES);
    }

    private AdGroup makeExpectedActiveAdGroupWithChangedStatuses(AdGroup adGroup) {
        return adGroup.withStatusBsSynced(StatusBsSynced.NO)
                .withStatusShowsForecast(StatusShowsForecast.NEW)
                .withStatusModerate(StatusModerate.YES);
    }
}
