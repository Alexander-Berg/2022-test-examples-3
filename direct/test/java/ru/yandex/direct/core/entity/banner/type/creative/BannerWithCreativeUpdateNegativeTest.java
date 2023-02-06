package ru.yandex.direct.core.entity.banner.type.creative;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.CreativeInfo;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotChangeCreativeToImageId;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.cannotChangeImageToCreativeId;
import static ru.yandex.direct.core.testing.data.TestCreatives.defaultVideoAddition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithCreativeUpdateNegativeTest
        extends BannerNewBannerInfoUpdateOperationTestBase {

    @Test
    public void invalidChangeImageToCreativeIdForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        Long creativeId = steps.creativeSteps().addDefaultCanvasCreative(bannerInfo.getClientInfo())
                .getCreativeId();

        ModelChanges<ImageBanner> modelChanges = new ModelChanges<>(bannerId, ImageBanner.class)
                .process(creativeId, ImageBanner.CREATIVE_ID);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(),
                cannotChangeImageToCreativeId())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidChangeCreativeIdToImageForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithCreative();
        Long bannerId = bannerInfo.getBannerId();

        var imageHash = steps.bannerSteps().createImageAdImageFormat(bannerInfo.getClientInfo())
                .getImageHash();

        ModelChanges<ImageBanner> modelChanges = new ModelChanges<>(bannerId, ImageBanner.class)
                .process(imageHash, ImageBanner.IMAGE_HASH);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(),
                cannotChangeCreativeToImageId())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidShowTitleBodyValue_WithoutFeature_NewCreativeAdded() {
        bannerInfo = steps.textBannerSteps().createDefaultTextBanner();
        steps.featureSteps()
                .addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, false);

        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, bannerInfo.getClientInfo());
        Long creativeId = creativeInfo.getCreativeId();
        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(creativeId, TextBanner.CREATIVE_ID)
                .process(true, TextBanner.SHOW_TITLE_AND_BODY);

        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextBanner.SHOW_TITLE_AND_BODY)),
                invalidValue())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidShowTitleBodyValue_WithoutFeature_CreativeisBinded() {
        bannerInfo = steps.textBannerSteps().createDefaultTextBanner();
        Creative creative = defaultVideoAddition(null, null);
        CreativeInfo creativeInfo = steps.creativeSteps().createCreative(creative, bannerInfo.getClientInfo());
        Long creativeId = creativeInfo.getCreativeId();

        prepareAndApplyValid(
                new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                        .process(creativeId, TextBanner.CREATIVE_ID)
        );
        steps.featureSteps()
                .addClientFeature(bannerInfo.getClientId(), FeatureName.DISABLE_VIDEO_CREATIVE, false);

        ModelChanges<TextBanner> modelChanges = new ModelChanges<>(bannerInfo.getBannerId(), TextBanner.class)
                .process(true, TextBanner.SHOW_TITLE_AND_BODY);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(TextBanner.SHOW_TITLE_AND_BODY)),
                invalidValue())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
}
