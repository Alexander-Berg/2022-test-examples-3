package ru.yandex.direct.core.entity.banner.type.title;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.ContentPromotionAdgroupType;
import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContent;
import ru.yandex.direct.core.entity.contentpromotion.model.ContentPromotionContentType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.BannerWithTitle.TITLE;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adGroupNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxTextLengthWithoutTemplateMarker;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_CONTENT_PROMOTION_TITLE;
import static ru.yandex.direct.core.entity.banner.type.title.BannerConstantsService.MAX_LENGTH_VIDEO_FRONTPAGE_TITLE;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.frontpageVideoPackage;
import static ru.yandex.direct.core.testing.data.banner.TestContentPromotionBanners.clientContentPromoBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithTitleAddNegativeTest extends BannerNewAdGroupInfoAddOperationTestBase {

    private static final String TITLE_STR =
            "long title long title long title long title long title long title long title long title long title l" +
                    "long title long title long title long title long title long title long title long title long " +
                    "title lo";

    @Test
    public void invalidTitleForContentPromotionBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(content.getId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withTitle(TITLE_STR);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TITLE)),
                maxTextLengthWithoutTemplateMarker(MAX_LENGTH_CONTENT_PROMOTION_TITLE))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidTitleWithAdGroupIdForContentPromotionBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        ContentPromotionContent content = steps.contentPromotionSteps()
                .createContentPromotionContent(adGroupInfo.getClientId(), ContentPromotionContentType.VIDEO);
        ContentPromotionBanner banner = clientContentPromoBanner(content.getId())
                .withAdGroupId(-1L)
                .withTitle(TITLE_STR);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);


        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithAdGroupId.AD_GROUP_ID)),
                adGroupNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidTitleForVideoFrontpageBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        var clientInfo = adGroupInfo.getClientInfo();
        var creativeInfo = steps.creativeSteps().addCpmVideoFrontpageCreative(clientInfo);
        var pricePackage = frontpageVideoPackage(clientInfo);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withBigKingImageHash(steps.bannerSteps().createBigKingImageFormat(clientInfo).getImageHash())
                .withLogoImageHash(steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash())
                .withBody("body")
                .withTitle(TITLE_STR);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TITLE)),
                maxTextLengthWithoutTemplateMarker(MAX_LENGTH_VIDEO_FRONTPAGE_TITLE))));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void mandatoryTitleForVideoFrontpageBanner() {
        adGroupInfo = steps.contentPromotionAdGroupSteps().createDefaultAdGroup(ContentPromotionAdgroupType.VIDEO);
        var clientInfo = adGroupInfo.getClientInfo();
        var creativeInfo = steps.creativeSteps().addCpmVideoFrontpageCreative(clientInfo);
        var pricePackage = frontpageVideoPackage(clientInfo);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withBigKingImageHash(steps.bannerSteps().createBigKingImageFormat(clientInfo).getImageHash())
                .withLogoImageHash(steps.bannerSteps().createLogoImageFormat(clientInfo).getImageHash())
                .withBody("body")
                .withTitle(null);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TITLE)),
                notNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
