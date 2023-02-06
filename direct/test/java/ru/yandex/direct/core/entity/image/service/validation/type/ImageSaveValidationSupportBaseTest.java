package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import one.util.streamex.IntStreamEx;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.BannerImageType;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.container.ImageMetaInformation;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefectIds;
import ru.yandex.direct.core.entity.image.service.validation.ImageDefects;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.emptyPath;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public abstract class ImageSaveValidationSupportBaseTest {
    @Test
    public void validate_ImageInvalid_ValidationResultNotChanged() {
        ImageMetaInformation imageMetaInformation = getInvalidImageMetaInformation();

        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));
        vr.addError(invalidValue());

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);

        assertThat(actualVr.flattenErrors()).is(matchedBy(beanDiffer(vr.flattenErrors())));
    }

    @Test
    public void validate_ImageValid_ValidationResultHasNoErrors() {
        ImageMetaInformation imageMetaInformation = getValidImageMetaInformation();

        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);

        assertThat(actualVr).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_ImageSizeInvalid_HasError() {
        ImageMetaInformation imageMetaInformation = getInvalidImageMetaInformation();

        validateHasError(imageMetaInformation);
    }

    public void validateHasError(ImageMetaInformation imageMetaInformation) {
        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);

        assertThat(actualVr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0)),
                                getImageSaveValidationSupport().getBannerImageType().equals(BannerImageType.BANNER_MCBANNER) ?
                                        ImageDefects.imageSizeIsNotAllowedMcBanner() :
                                        ImageDefects.imageSizeIsNotAllowed()))));
    }

    @Test
    public void validate_ImageFileFormatInvalid_HasError() {
        ImageMetaInformation imageMetaInformation = getValidImageMetaInformation();
        getInvalidImageFileFormat().ifPresent(imageMetaInformation::setFormat);

        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);

        assertThat(actualVr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0)), ImageDefectIds.Format.IMAGE_FILE_FORMAT_NOT_ALLOWED))));
    }

    @Test
    public void validate_ImageFileSizeInvalid_HasError() {
        ImageMetaInformation imageMetaInformation = getValidImageMetaInformation()
                .withImageFileSize(getInvalidImageFileSize());

        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Collections.singletonList(0));

        Map<Integer, Integer> indexMap = ImmutableMap.of(0, 0);

        ValidationResult<List<Integer>, Defect> actualVr = getImageSaveValidationSupport()
                .validate(Collections.singletonList(imageMetaInformation), vr, indexMap);

        assertThat(actualVr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(0)), ImageDefects.imageFileSizeGreaterThanMax()))));
    }

    @Test
    public void validate_ImageCountValid_NoErrors() {
        List<ImageMetaInformation> imagesMeta = IntStreamEx.range(getMaximumImagesPerRequest())
                .mapToObj(index -> getValidImageMetaInformation())
                .toList();

        List<Integer> indexes = IntStreamEx.range(getMaximumImagesPerRequest()).boxed().toList();
        ValidationResult<List<Integer>, Defect> vr = new ValidationResult<>(indexes);

        Map<Integer, Integer> indexMap = IntStreamEx.range(getMaximumImagesPerRequest())
                .mapToEntry(Integer::valueOf, Integer::valueOf)
                .toMap();

        ValidationResult<List<Integer>, Defect> result = getImageSaveValidationSupport()
                .validate(imagesMeta, vr, indexMap);

        Assertions.assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
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

        Assertions.assertThat(result).is(matchedBy(hasDefectDefinitionWith(validationError(emptyPath(),
                CollectionDefects.maxCollectionSize(getMaximumImagesPerRequest())))));
    }

    @Test
    public void validateBeforeSave_shouldReturnOk() {
        var result = getImageSaveValidationSupport().validateBeforeSave(0, null);
        assertThat(result)
                .is(matchedBy(hasNoDefectsDefinitions()));
    }


    protected ImageMetaInformation getValidImageMetaInformation() {
        return new ImageMetaInformation()
                .withFormat(getValidImageFileFormat())
                .withImageFileSize(1)
                .withSize(getValidImageSize())
                .withFramesNumber(1);
    }

    protected ImageMetaInformation getInvalidImageMetaInformation() {
        return new ImageMetaInformation()
                .withFormat(ImageFileFormat.JPEG)
                .withImageFileSize(1)
                .withSize(getInvalidImageSize())
                .withFramesNumber(1);
    }

    protected abstract ImageSize getValidImageSize();

    protected abstract ImageSize getInvalidImageSize();

    protected abstract int getInvalidImageFileSize();

    protected abstract ImageFileFormat getValidImageFileFormat();

    protected abstract Optional<ImageFileFormat> getInvalidImageFileFormat();

    public abstract ImageSaveValidationSupport getImageSaveValidationSupport();

    protected abstract int getMaximumImagesPerRequest();
}
