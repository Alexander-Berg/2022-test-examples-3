package ru.yandex.direct.core.entity.banner.type.logos;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithLogo;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageSizeInvalid;
import static ru.yandex.direct.core.testing.data.TestBanners.logoImageFormat;
import static ru.yandex.direct.core.testing.data.TestNewCpmBanners.clientCpmBanner;
import static ru.yandex.direct.core.testing.data.TestPricePackages.frontpageVideoPackage;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.notNull;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithLogoAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {
    private CreativeInfo creativeInfo;

    @Before
    public void before() {
        adGroupInfo = steps.adGroupSteps().createActiveCpmBannerAdGroup();
        creativeInfo = steps.creativeSteps().addDefaultHtml5Creative(adGroupInfo.getClientInfo(),
                steps.creativeSteps().getNextCreativeId());

    }

    @Test
    public void logoImageNotFound() {
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash("123");

        var validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithLogo.LOGO_IMAGE_HASH.name())),
                imageNotFound())));
    }

    @Test
    public void logoImageSizeInvalid() {
        BannerImageFormat bannerImageFormat = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(),
                logoImageFormat(null)
                        .withWidth(30L)
                        .withHeight(45L));

        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withLogoImageHash(bannerImageFormat.getImageHash());

        var validationResult = prepareAndApplyInvalid(banner);

        Assert.assertThat(validationResult, hasDefectWithDefinition(validationError(
                path(field(BannerWithLogo.LOGO_IMAGE_HASH.name())),
                imageSizeInvalid())));
    }

    @Test
    public void mandatoryLogoForVideoFrontpageBanner() {
        var clientInfo = adGroupInfo.getClientInfo();
        creativeInfo = steps.creativeSteps().addCpmVideoFrontpageCreative(clientInfo);
        var pricePackage = frontpageVideoPackage(clientInfo);
        steps.pricePackageSteps().createPricePackage(pricePackage);
        var campaign = steps.campaignSteps().createActiveCpmPriceCampaign(clientInfo, pricePackage);
        var adGroup = steps.adGroupSteps().createDefaultVideoAdGroupForPriceSales(campaign, clientInfo);
        CpmBanner banner = clientCpmBanner(creativeInfo.getCreativeId())
                .withAdGroupId(adGroup.getId())
                .withBigKingImageHash(steps.bannerSteps().createBigKingImageFormat(clientInfo).getImageHash())
                .withBody("body")
                .withTitle("title");

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithLogo.LOGO_IMAGE_HASH)),
                notNull())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
