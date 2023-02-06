package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import one.util.streamex.IntStreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.container.ImageMetaInformation;
import ru.yandex.direct.core.entity.image.service.ImageConstants;
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_MULTICARD;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_LINEAR_SIZE_IN_PIXEL;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;

public class ImageSaveValidationForMulticardSupportTest extends ImageSaveValidationSupportBaseTest {
    private static final ImageSaveValidationForMulticardSupport imageSaveValidationForMulticardSupport =
            new ImageSaveValidationForMulticardSupport();

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
        return ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_MULTICARD + 1;
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
        return imageSaveValidationForMulticardSupport;
    }

    @Override
    protected int getMaximumImagesPerRequest() {
        return MAX_IMAGES_PER_REQUEST_FOR_MULTICARD;
    }

    @Test
    @Override
    public void validate_ImageCountInvalid_NoErrors() {
        int invalidCount = getMaximumImagesPerRequest() + 1;
        List<ImageMetaInformation> imagesMeta = IntStreamEx.range(invalidCount)
                .mapToObj(index -> getValidImageMetaInformation())
                .toList();

        List<Integer> indexes = IntStreamEx.range(invalidCount).boxed().toList();
        ValidationResult<List<Integer>, Defect> vr = new ValidationResult<>(indexes);

        Map<Integer, Integer> indexMap = IntStreamEx.range(invalidCount)
                .mapToEntry(Integer::valueOf, Integer::valueOf)
                .toMap();

        ValidationResult<List<Integer>, Defect> result = getImageSaveValidationSupport()
                .validate(imagesMeta, vr, indexMap);

        Assertions.assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(CollectionDefectIds.Size.SIZE_CANNOT_BE_MORE_THAN_MAX))));
    }
}
