package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Optional;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.container.ImageMetaInformation;
import ru.yandex.direct.core.entity.image.service.ImageConstants;

import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_LOGO;

public class ImageSaveValidationForLogoSupportTest extends ImageSaveValidationSupportBaseTest {
    private ImageSaveValidationForLogoSupport imageSaveValidationForLogoSupport =
            new ImageSaveValidationForLogoSupport();


    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return imageSaveValidationForLogoSupport;
    }

    @Override
    protected ImageSize getValidImageSize() {
        return new ImageSize().withWidth(80).withHeight(80);
    }

    @Override
    protected ImageSize getInvalidImageSize() {
        return new ImageSize().withWidth(79).withHeight(79);
    }

    @Override
    protected int getInvalidImageFileSize() {
        return ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_LOGO + 1;
    }

    @Override
    protected ImageFileFormat getValidImageFileFormat() {
        return ImageFileFormat.GIF;
    }

    @Override
    protected Optional<ImageFileFormat> getInvalidImageFileFormat() {
        return Optional.of(ImageFileFormat.SVG);
    }

    @Test
    public void validate_ImageSizeInvalid_IsNotSquare() {
        ImageMetaInformation imageMetaInformation = getValidImageMetaInformation()
                .withSize(new ImageSize().withWidth(80).withHeight(90));

        validateHasError(imageMetaInformation);
    }

    @Override
    protected int getMaximumImagesPerRequest() {
        return MAX_IMAGES_PER_REQUEST_FOR_LOGO;
    }
}
