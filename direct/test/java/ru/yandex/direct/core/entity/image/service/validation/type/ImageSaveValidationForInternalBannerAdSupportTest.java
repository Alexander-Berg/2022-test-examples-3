package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.entity.image.model.ImageMdsMetaInfo;
import ru.yandex.direct.core.entity.image.model.ImageValidationContainer;
import ru.yandex.direct.core.entity.image.service.ImageConstants;
import ru.yandex.direct.core.entity.internalads.model.ResourceInfo;
import ru.yandex.direct.core.entity.internalads.restriction.InternalAdRestrictionDefects;
import ru.yandex.direct.core.entity.internalads.restriction.Restriction;
import ru.yandex.direct.core.entity.internalads.restriction.RestrictionImageFormat;
import ru.yandex.direct.core.entity.internalads.restriction.Restrictions;
import ru.yandex.direct.utils.JsonUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_LINEAR_SIZE_IN_PIXEL;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasWarningWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;


public class ImageSaveValidationForInternalBannerAdSupportTest extends ImageSaveValidationSupportBaseTest {

    private ImageSaveValidationForInternalBannerAdSupport imageSaveValidationForInternalBannerAdSupport =
            new ImageSaveValidationForInternalBannerAdSupport();

    @Ignore("не применимо")
    @Test
    @Override
    public void validate_ImageInvalid_ValidationResultNotChanged() {
    }

    @Ignore("не применимо")
    @Test
    @Override
    public void validate_ImageSizeInvalid_HasError() {
    }

    @Ignore("не применимо")
    @Test
    @Override
    public void validate_ImageFileFormatInvalid_HasError() {
    }

    @Ignore("не применимо")
    @Test
    @Override
    public void validate_ImageCountValid_NoErrors() {
    }

    @Ignore("не применимо")
    @Test
    @Override
    public void validate_ImageCountInvalid_NoErrors() {
    }

    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return imageSaveValidationForInternalBannerAdSupport;
    }

    @Override
    protected ImageSize getValidImageSize() {
        return new ImageSize().withWidth(64).withHeight(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL);
    }

    @Override
    protected ImageSize getInvalidImageSize() {
        return new ImageSize().withWidth(0).withHeight(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL + 1);
    }

    @Override
    protected int getInvalidImageFileSize() {
        return ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_INTERNAL_BANNER + 1;
    }

    @Override
    protected ImageFileFormat getValidImageFileFormat() {
        return ImageFileFormat.SVG;
    }

    @Override
    protected Optional<ImageFileFormat> getInvalidImageFileFormat() {
        return Optional.empty();
    }

    @Override
    protected int getMaximumImagesPerRequest() {
        return -1;
    }

    @Test
    public void validateBeforeSave_shouldReturnOk() {
        var value = 0;
        var imageFormat = new BannerImageFormat()
                .withMdsMeta(JsonUtils.toJson(
                        new ImageMdsMeta()
                                .withMeta(new ImageMdsMetaInfo()
                                        .withOrigFormat(RestrictionImageFormat.PNG.getMdsFormat())
                                )
                        )
                ).withSize(new ImageSize().withHeight(50).withWidth(50));
        var resourceInfo = new ResourceInfo()
                .withValueRestrictions(createImageRestrictions());
        var container = new ImageValidationContainer(imageFormat, resourceInfo);

        var result = imageSaveValidationForInternalBannerAdSupport.validateBeforeSave(value, container);

        assertThat(result)
                .is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateBeforeSave_shouldReturnOk_whenResourceIsNull() {
        var value = 0;
        var imageFormat = mock(BannerImageFormat.class);
        var container = new ImageValidationContainer(imageFormat, null);

        var result = imageSaveValidationForInternalBannerAdSupport.validateBeforeSave(value, container);

        assertThat(result)
                .is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateBeforeSave_shouldReturnOk_whenNoRestrictions() {
        var value = 0;
        var imageFormat = mock(BannerImageFormat.class);
        var resourceInfo = new ResourceInfo();
        var container = new ImageValidationContainer(imageFormat, resourceInfo);

        var result = imageSaveValidationForInternalBannerAdSupport.validateBeforeSave(value, container);

        assertThat(result)
                .is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validateBeforeSave_shouldReturnErrors() {
        var value = 0;
        var imageFormat = new BannerImageFormat()
                .withSize(new ImageSize().withHeight(200).withWidth(200));
        var resourceInfo = new ResourceInfo()
                .withValueRestrictions(createImageRestrictions());
        var container = new ImageValidationContainer(imageFormat, resourceInfo);

        var result = imageSaveValidationForInternalBannerAdSupport.validateBeforeSave(value, container);

        assertThat(result)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(InternalAdRestrictionDefects.ImageFormat.IMAGE_FORMAT_INVALID))
                        )
                ).is(matchedBy(hasWarningWithDefinition(
                        validationError(InternalAdRestrictionDefects.ImageDimension.IMAGE_DIMENSION_TOO_BIG))
                )
        );
    }

    private List<Restriction> createImageRestrictions() {
        var strictRestriction = Restrictions.imageFormatIn(true, Set.of(RestrictionImageFormat.PNG));
        var weakRestrictions = Restrictions.imageDimensionsMax(false, 100, 100);

        return List.of(strictRestriction, weakRestrictions);
    }
}
