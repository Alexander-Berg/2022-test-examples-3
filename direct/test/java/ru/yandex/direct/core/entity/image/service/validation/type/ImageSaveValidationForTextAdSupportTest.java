package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.container.ImageMetaInformation;
import ru.yandex.direct.core.entity.image.service.ImageConstants;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_TEXT_AD;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_LINEAR_SIZE_IN_PIXEL;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ImageSaveValidationForTextAdSupportTest extends ImageSaveValidationSupportBaseTest {
    private ImageSaveValidationForTextAdSupport imageSaveValidationForTextAdSupport =
            new ImageSaveValidationForTextAdSupport();

    @Test
    public void validateImageMetaInformation_ImageGreaterThanMax_IsNotValid() {
        ImageSize imageSize = new ImageSize()
                .withHeight(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL)
                .withWidth(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL + 1);

        ImageMetaInformation imageMetaInformation = new ImageMetaInformation()
                .withFormat(ImageFileFormat.JPEG)
                .withImageFileSize(1)
                .withSize(imageSize)
                .withFramesNumber(1);

        ValidationResult<List<Integer>, Defect> vr = new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> validationResult = imageSaveValidationForTextAdSupport
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);
        AssertionsForClassTypes.assertThat(validationResult)
                .is(matchedBy(
                        hasDefectDefinitionWith(
                                validationError(path(index(0)), ImageDefects
                                        .imageSizesGreaterThanMax(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL)))));
    }

    @Test
    public void validate_ImageIsAnimated_HasError() {
        ImageMetaInformation imageMetaInformation = getValidImageMetaInformation()
                .withFramesNumber(2);

        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);

        assertThat(actualVr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0)), ImageDefects
                                .imageContainsAnimation()))));
    }

    @Override
    protected ImageSize getValidImageSize() {
        return new ImageSize()
                .withHeight(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL)
                .withWidth(MAX_IMAGE_LINEAR_SIZE_IN_PIXEL);
    }

    @Override
    protected ImageSize getInvalidImageSize() {
        return new ImageSize()
                .withHeight(1)
                .withWidth(1);
    }

    @Override
    protected int getInvalidImageFileSize() {
        return ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_TEXT_BANNER + 1;
    }

    @Override
    protected ImageFileFormat getValidImageFileFormat() {
        return ImageFileFormat.GIF;
    }

    @Override
    protected Optional<ImageFileFormat> getInvalidImageFileFormat() {
        return Optional.of(ImageFileFormat.SVG);
    }

    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return imageSaveValidationForTextAdSupport;
    }

    @Override
    protected int getMaximumImagesPerRequest() {
        return MAX_IMAGES_PER_REQUEST_FOR_TEXT_AD;
    }
}
