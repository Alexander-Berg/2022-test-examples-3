package ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.update;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.container.ComplexContentPromotionAdGroup;
import ru.yandex.direct.core.entity.adgroup.container.ComplexCpmAdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.adgroup.service.complex.contentpromotion.UpdateComplexContentPromotionAdGroupValidationService;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierDemographics;
import ru.yandex.direct.core.entity.bidmodifier.ComplexBidModifier;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.adgroup.ContentPromotionAdGroupInfo;
import ru.yandex.direct.core.testing.info.banner.ContentPromotionBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.testing.data.TestBanners.VALID_CONTENT_PROMOTION_ID;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class UpdateComplexContentPromotionAdGroupValidationServiceTest {

    @Autowired
    private UpdateComplexContentPromotionAdGroupValidationService updateValidationService;
    @Autowired
    private Steps steps;
    @Autowired
    private TestContentPromotionBanners testNewContentPromotionBanners;

    private ClientInfo clientInfo;
    private ComplexContentPromotionAdGroup complexContentPromotionAdGroup;
    private ContentPromotionAdGroupInfo contentPromotionAdGroup;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        contentPromotionAdGroup = steps.contentPromotionAdGroupSteps()
                .createDefaultAdGroup(clientInfo, ContentPromotionAdgroupType.VIDEO);
        ContentPromotionBanner banner = testNewContentPromotionBanners.fullContentPromoBanner(VALID_CONTENT_PROMOTION_ID, "https://www.youtube.com")
                .withCampaignId(contentPromotionAdGroup.getCampaignId())
                .withAdGroupId(contentPromotionAdGroup.getAdGroupId());
        var contentPromotionVideoBannerInfo = steps.contentPromotionBannerSteps()
                .createBanner(new ContentPromotionBannerInfo()
                        .withBanner(banner)
                        .withAdGroupInfo(contentPromotionAdGroup));

        complexContentPromotionAdGroup = new ComplexContentPromotionAdGroup()
                .withAdGroup(contentPromotionAdGroup.getAdGroup())
                .withBanners(singletonList(contentPromotionVideoBannerInfo.getBanner()));

        var keywordInfo =
                steps.newKeywordSteps().createKeyword(contentPromotionAdGroup, defaultKeyword());
        var bidModifierInfo =
                steps.newBidModifierSteps().createDefaultAdGroupBidModifierDemographics(contentPromotionAdGroup);
        ComplexBidModifier complexBidModifier = new ComplexBidModifier()
                .withDemographyModifier((BidModifierDemographics) bidModifierInfo.getBidModifier());

        complexContentPromotionAdGroup
                .withKeywords(singletonList(keywordInfo.getKeyword()))
                .withComplexBidModifier(complexBidModifier);
    }

    @Test
    public void updateKeywordsNoErrorsTest() {
        complexContentPromotionAdGroup.getAdGroup().setName("updated name");
        complexContentPromotionAdGroup.withKeywords(singletonList(defaultKeyword()));

        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(singletonList(complexContentPromotionAdGroup.getAdGroup())),
                        singletonList(complexContentPromotionAdGroup), clientInfo.getClientId());
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void updateBannersLimitErrorTest() {
        List<BannerWithSystemFields> banners = IntStream.range(0, MAX_BANNERS_IN_ADGROUP).boxed()
                .map(i -> testNewContentPromotionBanners.fullContentPromoBanner(VALID_CONTENT_PROMOTION_ID, "https://www.youtube.com")
                        .withAdGroupId(contentPromotionAdGroup.getAdGroupId()))
                .collect(Collectors.toList());
        banners.addAll(complexContentPromotionAdGroup.getBanners());
        complexContentPromotionAdGroup.withBanners(banners);

        ValidationResult<List<AdGroup>, Defect> vr = updateValidationService
                .validateAdGroups(ValidationResult.success(singletonList(complexContentPromotionAdGroup.getAdGroup())),
                        singletonList(complexContentPromotionAdGroup), clientInfo.getClientId());

        assertThat("баннеры валидируются", vr, hasDefectDefinitionWith(
                validationError(path(index(0), field(ComplexCpmAdGroup.BANNERS)),
                        maxBannersInAdGroup(MAX_BANNERS_IN_ADGROUP))));
    }
}
