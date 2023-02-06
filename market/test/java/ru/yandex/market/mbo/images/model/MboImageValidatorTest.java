package ru.yandex.market.mbo.images.model;

import org.junit.Test;
import ru.yandex.market.mbo.core.kdepot.services.validation.ValidationResult;
import ru.yandex.market.mbo.core.kdepot.services.validation.image.ImageValidationParams;
import ru.yandex.market.mbo.core.kdepot.services.validation.image.MboImageValidator;
import ru.yandex.market.mbo.gwt.models.ImageType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;

public class MboImageValidatorTest {

    private MboImageValidator mboImageValidator = new MboImageValidator();

    @Test
    public void testXLPicture() throws Exception {
        assertEquals(ValidationResult.VALID, validate("450x550.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("400x300.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("400x400.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("300x100.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("100x300.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("100x400.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("100x500.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("100x600.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("100x700.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("400x100.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("600x100.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("700x100.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("500x300.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("700x300.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("700x400.jpg", "test.jpg"));
        assertEquals(ValidationResult.VALID, validate("4000x300.jpg", "test.jpg"));

        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("450x550.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("400x300.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("400x400.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("300x100.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("100x300.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("100x400.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("100x500.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("100x600.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("100x700.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("400x100.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("600x100.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("700x100.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("500x300.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("700x300.jpg", "test.gif"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("700x400.jpg", "test.gif"));

        assertEquals(ValidationResult.INVALID_IMAGE_SIZE, validate("200x4002.jpg", "test.jpg"));
        assertEquals(ValidationResult.INVALID_FILE, validate("empty.jpg", "test.jpg"));
        assertEquals(ValidationResult.INVALID_IMAGE_FORMAT, validate("broken.jpg", "test.jpg"));
    }

    private ValidationResult validate(String file, String name) throws IOException {
        byte[] imageBytes = getFileBytes("/images/" + file);
        ImageValidationParams imageParams = new ImageValidationParams(imageBytes, "", name, ImageType.XL_PICTURE);
        return mboImageValidator.validate(imageParams);
    }

    @SuppressWarnings("checkstyle:magicNumber")
    private byte[] getFileBytes(String s) throws IOException {
        try (InputStream fileStream = MboImageValidatorTest.class.getResourceAsStream(s)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buff = new byte[2048];
            int read;
            while ((read = fileStream.read(buff)) > 0) {
                baos.write(buff, 0, read);
            }
            return baos.toByteArray();
        }
    }
}
