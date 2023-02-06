package ru.yandex.market.mbo.core.kdepot.services;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.core.kdepot.services.validation.ValidationResult;
import ru.yandex.market.mbo.core.kdepot.services.validation.Validator;
import ru.yandex.market.mbo.core.kdepot.services.validation.image.ImageValidatorImpl;
import ru.yandex.market.mbo.core.kdepot.services.validation.image.ValidationResultWithMessage;

import static org.junit.Assert.assertEquals;
import static ru.yandex.market.mbo.core.utils.UtilFunctions.getFileBytes;

/**
 * @author Dmitry Tsyganov dtsyganov@yandex-team.ru
 */
public class ImageValidatorImplTest {

    private Validator<ValidationResultWithMessage, byte[]> imageValidator;

    @Before
    public void setUp() {
        imageValidator = new ImageValidatorImpl();
    }

    private ValidationResult getValidationResult(byte[] fileBytes) {
        return imageValidator.validate(fileBytes).getValidationResult();
    }

    @Test
    public void testFormat() throws IOException {
        //file content isn't images
        assertEquals(ValidationResult.INVALID, getValidationResult(getFileBytes("text.txt")));
        assertEquals(ValidationResult.INVALID, getValidationResult(getFileBytes("text.jpg")));
        //file content is image in supported format
        assertEquals(ValidationResult.VALID, getValidationResult(getFileBytes("image.txt")));
        assertEquals(ValidationResult.VALID, getValidationResult(getFileBytes("image.jpg")));
        assertEquals(ValidationResult.VALID, getValidationResult(getFileBytes("image.jpeg")));
        assertEquals(ValidationResult.VALID, getValidationResult(getFileBytes("image.png")));
        //file content is image in unsupported format
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, getValidationResult(getFileBytes("image.bmp")));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, getValidationResult(getFileBytes("image.gif")));
    }

    @Test
    public void testSize() throws IOException {
        assertEquals(ValidationResult.VALID, getValidationResult(getFileBytes("image.jpg")));
        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, getValidationResult(getFileBytes("too_small_height.jpg")));
        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, getValidationResult(getFileBytes("too_small_width.jpg")));
        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, getValidationResult(getFileBytes("too_small.jpg")));
        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, getValidationResult(getFileBytes("too_large_height.jpg")));
        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, getValidationResult(getFileBytes("too_large_width.jpg")));
        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, getValidationResult(getFileBytes("too_large.jpg")));
    }
}
