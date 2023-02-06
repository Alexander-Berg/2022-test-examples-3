package ru.yandex.direct.core.entity.minuskeywordspack.service;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.old.OldPerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldTextBanner;
import ru.yandex.direct.core.entity.banner.repository.old.OldBannerRepository;
import ru.yandex.direct.core.entity.minuskeywordspack.container.UpdatedMinusKeywordsPackInfo;
import ru.yandex.direct.core.entity.minuskeywordspack.model.MinusKeywordsPack;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestBanners;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerInfo;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultPerformanceCreative;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;

@CoreTest
@RunWith(SpringRunner.class)
public class MinusKeywordsPacksUpdateOperationTasksForBannerTest extends MinusKeywordsPacksUpdateOperationBaseTest {
    @Autowired
    OldBannerRepository bannerRepository;

    @Test
    public void execute_DynamicCampaignWithMinusKeywords_NoChange_BannerNotChanged() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(dynamicAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(dynamicAdGroup);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, dynamicAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertBannerStatusesIsNotChanged(bannerInfo);
    }

    @Test
    public void execute_TextCampaignWithMinusKeywords_ChangeMinusKeywords_BannerNotChanged() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(textAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(textAdGroup);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, textAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertBannerStatusesIsNotChanged(bannerInfo);
    }

    @Test
    public void execute_DynamicCampaignWithMinusKeywords_ChangeMinusKeywords_BannerChanged() {
        MinusKeywordsPack minusKeywordsPack = createMinusKeywordsPackInAdGroup(dynamicAdGroup, MINUS_WORD);
        Long packIdToUpdate = minusKeywordsPack.getId();
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(dynamicAdGroup);
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, dynamicAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertBannerStatusesIsChanged(bannerInfo);
    }

    @Test
    public void execute_DynamicAndPerformanceCampaignsWithMinusKeywords_ChangeMinusKeywords_BannersChanged() {
        Long packIdToUpdate = createLibraryMinusKeywordsPack(MINUS_WORD).getMinusKeywordPackId();
        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, dynamicAdGroup.getCampaignId());
        TextBannerInfo bannerInfo = steps.bannerSteps().createActiveTextBanner(dynamicAdGroup);

        Long feedId = steps.feedSteps().createDefaultFeed().getFeedId();
        AdGroupInfo performanceAdGroup = steps.adGroupSteps().createActivePerformanceAdGroup(feedId);
        CreativeInfo creative = steps.creativeSteps()
                .createCreative(defaultPerformanceCreative(clientInfo.getClientId(), null), clientInfo);
        BannerInfo performanceBanner = steps.bannerSteps().createBanner(TestBanners
                .activePerformanceBanner(performanceAdGroup.getCampaignId(), performanceAdGroup.getAdGroupId(),
                        creative.getCreativeId()), performanceAdGroup);

        testMinusKeywordsPackRepository
                .linkLibraryMinusKeywordPackToCampaign(shard, packIdToUpdate, performanceAdGroup.getCampaignId());

        List<ModelChanges<MinusKeywordsPack>> modelChanges =
                singletonList(minusKeywordsModelChanges(packIdToUpdate, MINUS_WORD_2));
        MassResult<UpdatedMinusKeywordsPackInfo> result = executePartial(modelChanges);
        UpdatedMinusKeywordsPackInfo expectedResult = expectedResult(packIdToUpdate, singletonList(MINUS_WORD_2));
        assumeThat(result, isSuccessfulWithMatchers(beanDiffer(expectedResult)));

        assertBannerStatusesIsChanged(bannerInfo);
        OldBanner expectedPerformanceBanner = new OldPerformanceBanner();
        setChangedBannerStatuses(expectedPerformanceBanner);
        assertBanner(performanceBanner, expectedPerformanceBanner);
    }

    private void assertBannerStatusesIsNotChanged(AbstractBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = new OldTextBanner()
                .withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.YES);
        assertBanner(bannerInfo, expectedBanner);
    }

    private void assertBannerStatusesIsChanged(AbstractBannerInfo bannerInfo) {
        OldTextBanner expectedBanner = new OldTextBanner();
        setChangedBannerStatuses(expectedBanner);
        assertBanner(bannerInfo, expectedBanner);
    }

    private void setChangedBannerStatuses(OldBanner banner) {
        banner.withStatusModerate(OldBannerStatusModerate.YES)
                .withStatusBsSynced(StatusBsSynced.NO);
    }

    private void assertBanner(AbstractBannerInfo bannerInfo, OldBanner expectedBanner) {
        OldBanner banner =
                bannerRepository.getBanners(bannerInfo.getShard(), singletonList(bannerInfo.getBannerId())).get(0);
        assertThat("состояние баннера не соответствует ождидаемому",
                banner, beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields()));
    }
}
