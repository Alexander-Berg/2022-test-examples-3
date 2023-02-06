package ru.yandex.direct.core.entity.adgroup.service.complex;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects.adGroupDoesNotContainThisBanner;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.clientDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.regions.Region.RUSSIA_REGION_ID;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdateComplexAdGroupValidationServiceTest {

    @Autowired
    private UpdateComplexAdGroupValidationService updateValidationService;

    @Autowired
    private Steps steps;

    private AdGroupInfo adGroupInfo;
    private AdGroupInfo dynamicAdGroupInfo;
    private NewTextBannerInfo existingBanner;
    private Long campaignId;
    private Long dynamicCampaignId;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        campaignId = adGroupInfo.getCampaignId();

        dynamicAdGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup();
        dynamicCampaignId = dynamicAdGroupInfo.getCampaignId();

        existingBanner = steps.textBannerSteps().createBanner(
                new NewTextBannerInfo()
                        .withCampaignInfo(adGroupInfo.getCampaignInfo()));
    }

    @Test
    public void validateBanners_TooManyBanners() {
        List<BannerWithSystemFields> banners = IntStream.range(0, MAX_BANNERS_IN_ADGROUP).boxed()
                .map(i -> fullTextBanner(campaignId, adGroupInfo.getAdGroupId()))
                .collect(Collectors.toList());
        banners.add(existingBanner.getBanner());

        Multimap<Long, Long> bannerCount = ArrayListMultimap.create();
        bannerCount.put(adGroupInfo.getAdGroupId(), existingBanner.getBannerId());

        var vr = updateValidationService.validateBanners(adGroupInfo.getAdGroup(), banners, emptyMap(), bannerCount,
                existingBanner.getClientId(), RUSSIA_REGION_ID);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(), maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
    }

    @Test
    public void validateBanners_MaxBanners() {
        List<BannerWithSystemFields> banners = IntStream.range(0, MAX_BANNERS_IN_ADGROUP - 1).boxed()
                .map(i -> fullTextBanner(campaignId, adGroupInfo.getAdGroupId()))
                .collect(Collectors.toList());
        banners.add(existingBanner.getBanner());

        Multimap<Long, Long> bannerCount = ArrayListMultimap.create();
        bannerCount.put(adGroupInfo.getAdGroupId(), existingBanner.getBannerId());

        var vr = updateValidationService.validateBanners(adGroupInfo.getAdGroup(), banners, emptyMap(), bannerCount,
                existingBanner.getClientId(), RUSSIA_REGION_ID);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateBanners_BannersNotCorrespondsToAdGroup() {
        List<BannerWithSystemFields> banners = singletonList(existingBanner.getBanner());

        var vr = updateValidationService.validateBanners(adGroupInfo.getAdGroup(), banners, emptyMap(),
                ArrayListMultimap.create(), existingBanner.getClientId(), RUSSIA_REGION_ID);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)), adGroupDoesNotContainThisBanner())));
    }

    @Test
    public void validateBanners_BannerTypeIsNotCorrespondToAdGroup() {
        var newBanner = clientTextBanner()
                .withCampaignId(dynamicCampaignId)
                .withAdGroupId(dynamicAdGroupInfo.getAdGroupId());
        List<BannerWithSystemFields> banners = asList(existingBanner.getBanner(), newBanner);

        Multimap<Long, Long> bannerCount = ArrayListMultimap.create();
        bannerCount.put(dynamicAdGroupInfo.getAdGroupId(), existingBanner.getBannerId());

        ValidationResult<List<BannerWithSystemFields>, Defect> vr = updateValidationService
                .validateBanners(dynamicAdGroupInfo.getAdGroup(), banners, emptyMap(), bannerCount,
                        existingBanner.getClientId(), RUSSIA_REGION_ID);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(1)),
                inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void validateBanners_TextAdGroupWithDynamicBanner_success() {
        var newBanner = clientDynamicBanner()
                .withCampaignId(adGroupInfo.getCampaignId())
                .withAdGroupId(adGroupInfo.getAdGroupId());
        List<BannerWithSystemFields> banners = asList(existingBanner.getBanner(), newBanner);

        Multimap<Long, Long> bannerCount = ArrayListMultimap.create();
        bannerCount.put(adGroupInfo.getAdGroupId(), existingBanner.getBannerId());

        ValidationResult<List<BannerWithSystemFields>, Defect> vr = updateValidationService
                .validateBanners(adGroupInfo.getAdGroup(), banners, emptyMap(), bannerCount,
                        existingBanner.getClientId(), RUSSIA_REGION_ID);
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
