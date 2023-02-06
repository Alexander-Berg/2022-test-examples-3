package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Optional;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.service.ImageConstants;

import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_LOGO;

public class ImageSaveValidationForLogoExtendedSupportTest extends ImageSaveValidationSupportBaseTest {
    private final ImageSaveValidationForLogoExtendedSupport imageSaveValidationForLogoExtendedSupport =
            new ImageSaveValidationForLogoExtendedSupport();


    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return imageSaveValidationForLogoExtendedSupport;
    }

    @Override
    protected ImageSize getValidImageSize() {
        return new ImageSize().withWidth(80).withHeight(120);
    }

    @Override
    protected ImageSize getInvalidImageSize() {
        return new ImageSize().withWidth(1).withHeight(1);
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

    @Override
    protected int getMaximumImagesPerRequest() {
        return MAX_IMAGES_PER_REQUEST_FOR_LOGO;
    }
}
