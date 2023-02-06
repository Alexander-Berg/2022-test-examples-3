package ru.yandex.direct.core.entity.banner.type.bannerimage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndImageType;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBannerImageAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final String WRONG_IMAGE_HASH = "WRONG_IMAGE_HASH";

    @Autowired
    private BannerSteps bannerSteps;


    @Test
    public void invalidBannerImageHashForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        createBannerImageWithImageAdType();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withImageHash(WRONG_IMAGE_HASH);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithBannerImage.IMAGE_HASH)),
                imageNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidBannerImageTypeForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        String imageHash = createBannerImageWithImageAdType();
        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withImageHash(imageHash);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithBannerImage.IMAGE_HASH)),
                inconsistentStateBannerTypeAndImageType())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private String createBannerImageWithImageAdType() {
        return bannerSteps.createImageAdImageFormat(adGroupInfo.getClientInfo())
                .getImageHash();
    }

    private String createBannerImageWithRegularType() {
        return bannerSteps.createRegularImageFormat(adGroupInfo.getClientInfo())
                .getImageHash();
    }

}
