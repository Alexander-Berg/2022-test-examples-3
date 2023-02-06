package ru.yandex.direct.core.entity.inventori.service.type;

import java.math.BigDecimal;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;

import ru.yandex.direct.core.entity.adgroup.model.CpmIndoorAdGroup;
import ru.yandex.direct.core.entity.banner.model.old.OldCpmIndoorBanner;
import ru.yandex.direct.core.entity.bidmodifier.AgeType;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographicsAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.GenderType;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.creative.model.AdditionalData;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.VideoFormat;
import ru.yandex.direct.core.entity.placements.model1.IndoorPlacement;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.inventori.model.request.BlockSize;
import ru.yandex.direct.inventori.model.request.GroupType;
import ru.yandex.direct.inventori.model.request.PageBlock;
import ru.yandex.direct.inventori.model.request.ProfileCorrection;
import ru.yandex.direct.inventori.model.request.Target;
import ru.yandex.direct.inventori.model.request.VideoCreative;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeCpmIndoorBanner;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmIndoorVideoAddition;
import static ru.yandex.direct.core.testing.data.TestGroups.activeCpmIndoorAdGroup;
import static ru.yandex.direct.core.testing.data.TestPlacements.indoorPlacementWithDefaultBlock;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class CpmIndoorAdGroupDataConverterTest {

    private ClientId clientId = ClientId.fromLong(1L);
    private Long campaignId = 2L;
    private Long adGroupId = 3L;
    private Long pageId = 4L;
    private Long blockId = 5L;
    private Long creativeId = 6L;
    private Long bannerId = 7L;

    private AdGroupDataConverter converter = new AdGroupDataConverter();

    @Test
    public void convertAdGroupDataToInventoriTarget_AllData() {
        AdGroupData adGroupData = defaultAdGroupDataBuilder().build();

        Target target = converter.convertAdGroupDataToInventoriTarget(adGroupData);
        Target expectedTarget = defaultExpectedTarget();
        assertThat(target).is(matchedBy(beanDiffer(expectedTarget)));
    }

    @Test
    public void convertAdGroupDataToInventoriTarget_BidModifierDemographics45Age() {
        AdGroupData adGroupData = defaultAdGroupDataBuilder().build();
        adGroupData.getBidModifierDemographics().getDemographicsAdjustments().get(0).setAge(AgeType._45_);

        Target target = converter.convertAdGroupDataToInventoriTarget(adGroupData);
        Target expectedTarget = defaultExpectedTarget()
                .withProfileCorrections(singletonList(defaultExpectedProfileCorrectionBuilder()
                        .withAge(ProfileCorrection.Age._45_54)
                        .build()));
        assertThat(target).is(matchedBy(beanDiffer(expectedTarget).useCompareStrategy(onlyExpectedFields())));
    }

    private AdGroupData.Builder defaultAdGroupDataBuilder() {
        Campaign campaign = defaultCampaign(clientId, campaignId);
        IndoorPlacement placement = indoorPlacementWithDefaultBlock(pageId, blockId);
        CpmIndoorAdGroup adGroup = defaultAdGroup(campaignId, adGroupId, placement);
        Creative creative = defaultCreative(clientId, creativeId);

        return AdGroupData.builder()
                .withCampaign(campaign)
                .withAdGroup(adGroup)
                .withPageBlocks(adGroup.getPageBlocks())
                .withExcludedPageBlocks(emptyList())
                .withBannerIds(singletonList(bannerId))
                .withCreativesByBannerId(ImmutableMap.of(bannerId, creative))
                .withBidModifierDemographics(defaultBidModifierDemographics())
                .withRetargetingConditions(emptyList())
                .withGoalIdToCryptaGoalMapping(emptyMap());
    }

    private Campaign defaultCampaign(ClientId clientId, Long campaignId) {
        return new Campaign()
                .withId(campaignId)
                .withClientId(clientId.asLong())
                .withType(CampaignType.CPM_BANNER)
                .withGeo(singleton(100))
                .withDisabledDomains(singleton("yandex.ru"));
    }

    private CpmIndoorAdGroup defaultAdGroup(Long campaignId, Long adGroupId, IndoorPlacement placement) {
        return activeCpmIndoorAdGroup(campaignId, placement)
                .withId(adGroupId)
                .withGeo(singletonList(200L));
    }

    private Creative defaultCreative(ClientId clientId, Long creativeId) {
        return defaultCpmIndoorVideoAddition(clientId, creativeId)
                .withDuration(5L)
                .withAdditionalData(new AdditionalData()
                        .withDuration(BigDecimal.valueOf(1.5))
                        .withFormats(asList(
                                new VideoFormat()
                                        .withWidth(1280)
                                        .withHeight(720)
                                        .withType("video/mp4")
                                        .withUrl("http://abc.com/1"),
                                new VideoFormat()
                                        .withWidth(800)
                                        .withHeight(600)
                                        .withType("video/mp4")
                                        .withUrl("http://abc.com/2"))));
    }

    private OldCpmIndoorBanner defaultBanner(Long campaignId, Long adGroupId, Long creativeId, Long bannerId) {
        return activeCpmIndoorBanner(campaignId, adGroupId, creativeId)
                .withId(bannerId);
    }

    private BidModifierDemographics defaultBidModifierDemographics() {
        return new BidModifierDemographics().withDemographicsAdjustments(singletonList(
                new BidModifierDemographicsAdjustment()
                        .withGender(GenderType.MALE)
                        .withAge(AgeType._25_34)
                        .withPercent(110)));
    }

    private Target defaultExpectedTarget() {
        return new Target()
                .withAdGroupId(adGroupId)
                .withGroupType(GroupType.INDOOR)
                .withVideoCreatives(singletonList(new VideoCreative(1500, null, Sets.newSet(
                        new BlockSize(16, 9),
                        new BlockSize(4, 3)
                ))))
                .withPageBlocks(singletonList(new PageBlock(pageId, singletonList(blockId))))
                .withProfileCorrections(singletonList(defaultExpectedProfileCorrectionBuilder().build()));
    }

    private ProfileCorrection.Builder defaultExpectedProfileCorrectionBuilder() {
        return ProfileCorrection.builder()
                .withGender(ProfileCorrection.Gender.MALE)
                .withAge(ProfileCorrection.Age._25_34)
                .withCorrection(110);
    }
}
