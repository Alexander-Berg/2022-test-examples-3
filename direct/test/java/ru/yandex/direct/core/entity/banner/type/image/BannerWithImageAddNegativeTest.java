package ru.yandex.direct.core.entity.banner.type.image;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.ImageBanner.IMAGE_HASH;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageSizeInvalid;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndImageType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyImageHashOrCreativeId;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredOnlyImageOrCreativeId;
import static ru.yandex.direct.core.testing.data.TestBanners.imageAdImageFormat;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.clientImageBannerWithImage;
import static ru.yandex.direct.core.testing.data.TestNewMcBanners.clientMcBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithImageAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String WRONG_IMAGE_HASH = "WRONG_IMAGE_HASH";

    @Autowired
    private BannerSteps bannerSteps;

    @Test
    public void invalidImageHashForImageBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        var banner = clientImageBannerWithImage(WRONG_IMAGE_HASH)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(IMAGE_HASH)), imageNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidImageTypeForImageBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        var imageHash = bannerSteps.createRegularImageFormat(adGroupInfo.getClientInfo())
                .getImageHash();
        var banner = clientImageBannerWithImage(imageHash)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(IMAGE_HASH)),
                inconsistentStateBannerTypeAndImageType())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void emptyImageHashAndCreativeIdForImageBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        var banner = clientImageBannerWithImage(null)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(), requiredButEmptyImageHashOrCreativeId())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidImageHashAndCreativeIdForImageBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();


        var imageHash = bannerSteps.createImageAdImageFormat(adGroupInfo.getClientInfo())
                .getImageHash();
        var creativeId = steps.creativeSteps().addDefaultCanvasCreative(adGroupInfo.getClientInfo()).getCreativeId();
        var banner = clientImageBannerWithImage(imageHash)
                .withCreativeId(creativeId)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(), requiredOnlyImageOrCreativeId())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidImageSizeForMcBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveMcBannerAdGroup();

        var imageHash = steps.bannerSteps().createBannerImageFormat(adGroupInfo.getClientInfo(),
                imageAdImageFormat(null).withWidth(300L).withHeight(300L))
                .getImageHash();
        var banner = clientMcBanner(imageHash)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(IMAGE_HASH)), imageSizeInvalid())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
