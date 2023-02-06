package ru.yandex.direct.core.entity.image.service.validation;

import java.awt.image.BufferedImage;

import org.bouncycastle.util.Arrays;
import org.junit.Test;

import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_TEXT_BANNER;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankImageData;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class SaveImageFromUrlToMdsValidationServiceTest {
    private SaveImageFromUrlToMdsValidationService saveImageFromUrlToMdsValidationService =
            new SaveImageFromUrlToMdsValidationService();

    @Test
    public void preValidate_UrlInvalid_HasError() {
        ValidationResult<String, Defect> vr = saveImageFromUrlToMdsValidationService.preValidate("url");
        assertThat(vr)
                .is(matchedBy(
                        hasDefectDefinitionWith(validationError(path(), CommonDefects.invalidValue()))));
    }

    @Test
    public void validate_ImageFileGreaterThanMax_HasError() {
        byte[] imageData = generateBlankGifImageData(128, 128);
        byte[] trashData = new byte[MAX_IMAGE_FILE_SIZE_FOR_TEXT_BANNER];
        ValidationResult<String, Defect> vr = new ValidationResult<>("url");
        saveImageFromUrlToMdsValidationService.validate(vr, Arrays.concatenate(imageData, trashData));

        assertThat(vr)
                .is(matchedBy(
                        hasDefectDefinitionWith(validationError(path(), ImageDefects
                                .imageFileSizeGreaterThanMax()))));
    }

    @Test
    public void validate_ImageFileHasUnsupportedMimeType_HasError() {
        byte[] imageData = generateBlankImageData(128, 128, "bmp", BufferedImage.TYPE_INT_RGB);
        ValidationResult<String, Defect> vr = new ValidationResult<>("url");
        saveImageFromUrlToMdsValidationService.validate(vr, Arrays.concatenate(imageData, imageData));

        assertThat(vr)
                .is(matchedBy(
                        hasDefectDefinitionWith(validationError(path(), ImageDefects
                                .imageMimeTypeIsNotSupported()))));
    }
}
