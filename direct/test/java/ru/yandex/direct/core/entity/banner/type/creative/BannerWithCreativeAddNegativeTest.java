package ru.yandex.direct.core.entity.banner.type.creative;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerAdGroupInfoAddOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.creativeNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredCreativesWithVideoAdditionTypeOnly;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.videoExtensionNotFound;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultCpmAudioAddition;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.core.testing.data.TestNewImageBanners.clientImageBannerWithCreative;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.clientTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCreativeAddNegativeTest extends BannerAdGroupInfoAddOperationTestBase {

    private static final Long WRONG_CREATIVE_ID = 1L;

    @Test
    public void invalidCreativeTypeForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        Creative creativeWithWrongType = defaultCpmAudioAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creativeWithWrongType,
                adGroupInfo.getClientInfo());
        Long expectedCreativeId = creativeInfo.getCreativeId();

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCreativeId(expectedCreativeId);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextBanner.CREATIVE_ID)),
                requiredCreativesWithVideoAdditionTypeOnly())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidCreativeIdForTextBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        Long expectedCreativeId = RandomNumberUtils.nextPositiveLong();

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCreativeId(expectedCreativeId);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextBanner.CREATIVE_ID)),
                videoExtensionNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidCreativeForImageBanner() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();

        var banner = clientImageBannerWithCreative(WRONG_CREATIVE_ID)
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId());

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(ImageBanner.CREATIVE_ID)),
                creativeNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }


    @Test
    public void invalidShowTitleBodyValue_WithoutFeature() {
        adGroupInfo = steps.adGroupSteps().createActiveTextAdGroup();
        steps.featureSteps().addClientFeature(adGroupInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, false);


        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, adGroupInfo.getClientInfo());
        Long expectedCreativeId = creativeInfo.getCreativeId();

        TextBanner banner = clientTextBanner()
                .withAdGroupId(adGroupInfo.getAdGroupId())
                .withCampaignId(adGroupInfo.getCampaignId())
                .withCreativeId(expectedCreativeId)
                .withShowTitleAndBody(true);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(banner);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextBanner.SHOW_TITLE_AND_BODY)),
                invalidValue())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
