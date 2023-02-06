package ru.yandex.market.mbo.core.kdepot.services.validation.image;

import java.io.IOException;

import junit.framework.TestCase;
import org.junit.Before;

import ru.yandex.market.mbo.core.kdepot.services.validation.ValidationResult;
import ru.yandex.market.mbo.gwt.models.ImageType;

import static ru.yandex.market.mbo.core.kdepot.services.validation.image.ImageSizeValidator.MAX_PICTURE_AREA;
import static ru.yandex.market.mbo.core.kdepot.services.validation.image.ImageSizeValidator.MAX_SIDE_SIDE;
import static ru.yandex.market.mbo.core.kdepot.services.validation.image.ImageSizeValidator.hasValidSize;
import static ru.yandex.market.mbo.core.utils.UtilFunctions.getFileBytes;
import static ru.yandex.market.mbo.gwt.models.ImageType.UNKNOWN;
import static ru.yandex.market.mbo.gwt.models.ImageType.XL_PICTURE;

public class ImageSizeValidatorTest extends TestCase {

    private ImageSizeValidator imageValidator;

    @Before
    public void setUp() {
        imageValidator = new ImageSizeValidator();
    }

    @SuppressWarnings("checkstyle:MagicNumber")
    public void testHasValidSize() {
        int minLongSize = ImageSizeValidator.getMinImageSize(XL_PICTURE.getMinLongWidth());
        int minShortSize = ImageSizeValidator.getMinImageSize(XL_PICTURE.getMinShortWidth());
        assertResult(hasValidSize(UNKNOWN, 1, MAX_SIDE_SIDE), true);
        assertResult(hasValidSize(UNKNOWN, MAX_SIDE_SIDE, 1), true);

        assertResult(hasValidSize(XL_PICTURE, minLongSize, minLongSize), true);
        assertResult(hasValidSize(XL_PICTURE, minShortSize, minLongSize), true);
        assertResult(hasValidSize(XL_PICTURE, minLongSize, minShortSize), true);

        assertResult(hasValidSize(UNKNOWN, MAX_SIDE_SIDE + 1, 1), false);
        assertResult(hasValidSize(UNKNOWN, 1, MAX_SIDE_SIDE + 1), false);

        assertResult(hasValidSize(XL_PICTURE, minLongSize, minShortSize - 1), false);
        assertResult(hasValidSize(XL_PICTURE, minShortSize - 1, minLongSize), false);

        int sideSize = (int) Math.sqrt(MAX_PICTURE_AREA);

        assertResult(hasValidSize(XL_PICTURE, sideSize - 1, sideSize), true);

        assertResult(hasValidSize(XL_PICTURE, sideSize, sideSize), false);
        assertResult(hasValidSize(XL_PICTURE, sideSize + 1, sideSize), false);
    }

    public void testEmptyImage() throws IOException {
        assertEquals(ValidationResult.EMPTY_FILE, getValidationResult(
            getFileBytes("empty_image.jpg"), "jpg", "broken_image", UNKNOWN)
        );
    }

    private void assertResult(ValidationResult result, boolean valid) {
        assertEquals(result.getCode().equals(ValidationResult.VALID.getCode()), valid);
    }

    private ValidationResult getValidationResult(byte[] fileBytes, String fileType, String fileName,
                                                 ImageType imageType) {
        return imageValidator.validate(new ImageValidationParams(fileBytes, fileType, fileName, imageType));
    }
}
