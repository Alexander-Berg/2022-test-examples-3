package ru.yandex.direct.core.entity.image.service.validation;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankImageData;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class ImageSaveValidationServiceTest {
    private SaveImageValidationService saveImageValidationService = new SaveImageValidationService();

    @Test
    public void validateMimeTypeForSingleImage() {
        byte[] imageData = generateBlankImageData(128, 128, "bmp", BufferedImage.TYPE_INT_RGB);

        ValidationResult<String, Defect> vr =
                new ValidationResult<>(RandomStringUtils.randomAlphabetic(10));
        saveImageValidationService.validateMimeType(vr, imageData);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(), ImageDefects
                                .imageMimeTypeIsNotSupported()))));
    }

    @Test
    public void validateMimeTypeForMultiImage() {
        byte[] imageDataFirst = generateBlankImageData(128, 128, "bmp", BufferedImage.TYPE_INT_RGB);
        byte[] imageDataSecond = generateBlankImageData(128, 128, "gif", BufferedImage.TYPE_INT_RGB);

        int firstImageId = 0;
        int secondImageId = 1;
        Map<Integer, byte[]> imageDataById = ImmutableMap.<Integer, byte[]>builder()
                .put(firstImageId, imageDataFirst)
                .put(secondImageId, imageDataSecond)
                .build();

        ValidationResult<List<Integer>, Defect> vr =
                new ValidationResult<>(Arrays.asList(firstImageId, secondImageId));
        saveImageValidationService.validateMimeType(vr, imageDataById);

        assertThat(vr.flattenErrors()).hasSize(1);
        assertThat(vr)
                .is(matchedBy(hasDefectDefinitionWith(
                        validationError(path(index(firstImageId)), ImageDefects
                                .imageMimeTypeIsNotSupported()))));
    }

}
