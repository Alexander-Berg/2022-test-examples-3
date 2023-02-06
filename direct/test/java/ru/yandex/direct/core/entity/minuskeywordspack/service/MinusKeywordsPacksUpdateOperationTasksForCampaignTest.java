package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignSimple;
import ru.yandex.direct.core.entity.minuskeywordspack.container.UpdatedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.matcher.LocalDateTimeMatcher.approximatelyNow;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class MinusKeywordsPacksUpdateOperationTasksForCampaignTest extends MinusKeywordsPacksUpdateOperationBaseTest {

    private static final CompareStrategy COMPARE_STRATEGY = onlyExpectedFields()
            .forFields(newPath(Campaign.AUTOBUDGET_FORECAST_DATE.name())).useMatcher(approximatelyNow());

    private static final CompareStrategy COMPARE_STRATEGY_RESET_FORECAST = onlyExpectedFields()
            .forFields(newPath(Campaign.AUTOBUDGET_FORECAST_DATE.name())).useMatcher(nullValue());

    @Test
    public void execute_AdGroupWithMinusKeywords_NoChange_CampaignNotChanged() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        testCampaignRepository.setAutobudgetForecastDate(shard, textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesIsNotChanged(textAdGroup.getCampaignInfo());
    }

    @Test
    public void execute_AdGroupWithMinusKeywords_ChangeName_CampaignNotChanged() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        testCampaignRepository.setAutobudgetForecastDate(shard, textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(nameModelChanges(packIdToUpdate, SECOND_NAME));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesIsNotChanged(textAdGroup.getCampaignInfo());
    }

    @Test
    public void execute_AdGroupAndCampaignWithMinusKeywords_ChangeName_CampaignNotChanged() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, textAdGroup.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(shard, textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(nameModelChanges(packIdToUpdate, SECOND_NAME));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD))
                .withName(SECOND_NAME);
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesIsNotChanged(textAdGroup.getCampaignInfo());
    }

    @Test
    public void execute_AdGroupWithMinusKeywords_ChangeMinusKeywords_CampaignChangedCorrectly() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        testCampaignRepository.setAutobudgetForecastDate(shard, textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesOnlyForecastDateChanged(textAdGroup.getCampaignInfo());
    }

    @Test
    public void execute_CampaignWithMinusKeywords_ChangeMinusKeywords_CampaignChangedCorrectly() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, textAdGroup.getCampaignId());
        testCampaignRepository.setAutobudgetForecastDate(shard, textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesChanged(textAdGroup.getCampaignInfo());
    }

    @Test
    public void execute_MinusKeywordsLinkedToSeveralCampaigns_AllCampaignsStatusesUpdated() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, textAdGroup.getCampaignId());
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, dynamicAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesChanged(textAdGroup.getCampaignInfo());
        assertCampaignStatusesChanged(dynamicAdGroup.getCampaignInfo());
    }

    @Test
    public void execute_MinusKeywordsLinkedToSeveralAdGroups_AllCampaignsStatusesUpdated() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, packIdToUpdate, textAdGroup.getAdGroupId());
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToAdGroup(shard, packIdToUpdate, dynamicAdGroup.getAdGroupId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, asList(MINUS_WORD, MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertCampaignStatusesOnlyForecastDateChanged(textAdGroup.getCampaignInfo());
        assertCampaignStatusesOnlyForecastDateChanged(dynamicAdGroup.getCampaignInfo());
    }

    private void assertCampaignStatusesOnlyForecastDateChanged(CampaignInfo campaignInfo) {
        assertCampaign(campaignInfo, makeCampaignActiveAndReadyForTest(new Campaign().withAutobudgetForecastDate(null)),
                COMPARE_STRATEGY_RESET_FORECAST);
    }

    private void assertCampaignStatusesChanged(CampaignInfo campaignInfo) {
        assertCampaign(campaignInfo, makeExpectedActiveCampaignWithChangedStatuses(new Campaign()),
                COMPARE_STRATEGY_RESET_FORECAST);
    }

    private void assertCampaignStatusesIsNotChanged(CampaignInfo campaignInfo) {
        assertCampaign(campaignInfo, makeCampaignActiveAndReadyForTest(new Campaign()), COMPARE_STRATEGY);
    }

    private void assertCampaign(CampaignInfo campaignInfo, Campaign expectedCampaign, CompareStrategy strategy) {
        CampaignSimple campaign = campaignRepository
                .getCampaignsSimple(campaignInfo.getShard(), singletonList(campaignInfo.getCampaignId()))
                .get(campaignInfo.getCampaignId());
        Assert.assertThat("состояние кампании не соответствует ожидаемому",
                campaign, beanDiffer((CampaignSimple) expectedCampaign).useCompareStrategy(strategy));
    }

    private Campaign makeCampaignActiveAndReadyForTest(Campaign campaign) {
        return campaign.withStatusBsSynced(StatusBsSynced.YES)
                .withAutobudgetForecastDate(LocalDateTime.now());
    }

    private Campaign makeExpectedActiveCampaignWithChangedStatuses(Campaign campaign) {
        return campaign.withStatusBsSynced(StatusBsSynced.NO);
    }
}
