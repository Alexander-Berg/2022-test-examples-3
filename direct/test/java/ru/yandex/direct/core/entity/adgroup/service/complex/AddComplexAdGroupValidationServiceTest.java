package ru.yandex.direct.core.entity.adgroup.service.complex;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndAdgroupType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultPerformanceAdGroup;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewDynamicBanners.fullDynamicBanner;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class AddComplexAdGroupValidationServiceTest {

    @Autowired
    private AddComplexAdGroupValidationService validationService;
    @Autowired
    private Steps steps;

    private Long campaignId;

    @Before
    public void before() {
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveTextCampaign();
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void validateAdGroupWithBanners_TooManyBanners() {
        AdGroup adGroup = defaultTextAdGroup(campaignId);
        List<BannerWithSystemFields> banners = IntStream.range(0, MAX_BANNERS_IN_ADGROUP + 1).boxed()
                .map(i -> fullTextBanner(campaignId, null))
                .collect(Collectors.toList());

        var vr = validationService.validateBanners(adGroup, banners);
        assertThat(vr, hasDefectDefinitionWith(validationError(path(), maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
    }

    @Test
    public void validateAdGroupWithBanners_MaxBanners() {
        AdGroup adGroup = defaultTextAdGroup(campaignId);
        List<BannerWithSystemFields>  banners = IntStream.range(0, MAX_BANNERS_IN_ADGROUP).boxed()
                .map(i -> fullTextBanner(campaignId, null))
                .collect(Collectors.toList());

        var vr = validationService.validateBanners(adGroup, banners);
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void validateAdGroupWithBanners_BannerTypeNotCorrespondsToAdGroup() {
        AdGroup adGroup = defaultPerformanceAdGroup(campaignId, null);
        var banner = fullTextBanner(campaignId, null);
        var vr = validationService.validateBanners(adGroup, singletonList(banner));
        assertThat(vr, hasDefectDefinitionWith(validationError(path(index(0)),
                inconsistentStateBannerTypeAndAdgroupType())));
    }

    @Test
    public void validateAdGroupWithBanners_TextAdGroupWithDynamicBanner_success() {
        AdGroup adGroup = defaultTextAdGroup(campaignId);
        var banner = fullDynamicBanner(campaignId, null);
        var vr = validationService.validateBanners(adGroup, singletonList(banner));
        assertThat(vr, hasNoDefectsDefinitions());
    }
}
