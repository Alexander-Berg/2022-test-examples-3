package ru.yandex.direct.core.entity.banner.type.bannerimage;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.banner.model.BannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerWithBannerImage;
import ru.yandex.direct.core.entity.banner.type.BannerOldBannerInfoUpdateOperationTestBase;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.TextBannerInfo;
import ru.yandex.direct.core.testing.steps.BannerSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.imageNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.inconsistentStateBannerTypeAndImageType;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class BannerWithBannerImageUpdateNegativeTest extends BannerOldBannerInfoUpdateOperationTestBase<OldBannerWithBannerImage> {

    private static final String WRONG_IMAGE_HASH = "WRONG_IMAGE_HASH";

    @Autowired
    private BannerSteps bannerSteps;

    @Test
    public void invalidBannerHashTypeForTextBanner() {
        bannerInfo = createTextBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, WRONG_IMAGE_HASH);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithBannerImage.IMAGE_HASH)),
                imageNotFound())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidBannerImageTypeForTextBanner() {
        bannerInfo = createTextBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        String newImageHash = createBannerImageWithImageAdType();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, newImageHash);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(path(field(BannerWithBannerImage.IMAGE_HASH)),
                inconsistentStateBannerTypeAndImageType())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    @Test
    public void invalidChangingToSmallBannerImageTypeForTextBanner() {
        bannerInfo = createTextBannerWithImage();
        Long bannerId = bannerInfo.getBannerId();

        String newImageHash = createBannerImageWithSmallType();

        ModelChanges<TextBanner> modelChanges = createTextBannerModelChanges(bannerId, newImageHash);
        ValidationResult<?, Defect> vr = prepareAndApplyInvalid(modelChanges);

        assertThat(vr, hasDefectDefinitionWith(validationError(emptyPath(),
                inconsistentStateBannerTypeAndImageType())));
        assertThat(vr.flattenErrors(), hasSize(1));
    }

    private ModelChanges<TextBanner> createTextBannerModelChanges(Long bannerId, String newImageHash) {
        return new ModelChanges<>(bannerId, TextBanner.class)
                .process(newImageHash, BannerWithBannerImage.IMAGE_HASH);
    }

    private TextBannerInfo createTextBannerWithImage() {
        TextBannerInfo bannerInfo = bannerSteps.createBanner(activeTextBanner());
        bannerSteps.createBannerImage(bannerInfo);
        return bannerInfo;
    }

    private String createBannerImageWithImageAdType() {
        return bannerSteps.createImageAdImageFormat(bannerInfo.getClientInfo())
                .getImageHash();
    }

    private String createBannerImageWithSmallType() {
        return bannerSteps.createSmallImageFormat(bannerInfo.getClientInfo())
                .getImageHash();
    }

}
