package ru.yandex.direct.core.entity.banner.type.image;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.type.BannerNewBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndImageType;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.requiredButEmptyImageHashOrCreativeId;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithImageUpdateNegativeTest
        extends BannerNewBannerInfoUpdateOperationTestBase {

    private static final String WRONG_IMAGE_HASH = "WRONG_IMAGE_HASH";

    @Test
    public void invalidImageHashTypeForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        var modelChanges = createImageBannerModelChanges(bannerId, WRONG_IMAGE_HASH);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithBannerImage.IMAGE_HASH)),
                imageNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidImageTypeForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        String newImageHash = createBannerImageWithSmallType();

        var modelChanges = createImageBannerModelChanges(bannerId, newImageHash);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithBannerImage.IMAGE_HASH)),
                inconsistentStateBannerTypeAndImageType())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void emptyImageHashAndCreativeIdForImageBanner() {
        bannerInfo = steps.imageBannerSteps().createImageBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        var modelChanges = createImageBannerModelChanges(bannerId, null);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(), requiredButEmptyImageHashOrCreativeId())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }
    private ModelChanges<ImageBanner> createImageBannerModelChanges(Long bannerId, String newImageHash) {
        return new ModelChanges<>(bannerId, ImageBanner.class)
                .process(newImageHash, ImageBanner.IMAGE_HASH);
    }

    private String createBannerImageWithSmallType() {
        return steps.bannerSteps().createSmallImageFormat(bannerInfo.getClientInfo())
                .getImageHash();
    }
}
