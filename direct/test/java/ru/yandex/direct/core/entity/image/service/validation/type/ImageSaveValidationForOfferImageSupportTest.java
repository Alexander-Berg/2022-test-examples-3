package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Optional;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;

import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_OFFER_IMAGE;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_OFFER_IMAGE;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGE_LINEAR_SIZE_IN_PIXEL;

public class ImageSaveValidationForOfferImageSupportTest extends ImageSaveValidationSupportBaseTest {
    private static final ImageSaveValidationForOfferImageSupport imageSaveValidationForOfferImageSupport =
            new ImageSaveValidationForOfferImageSupport();

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
        return MAX_IMAGE_FILE_SIZE_FOR_OFFER_IMAGE + 1;
    }

    @Override
    protected ImageFileFormat getValidImageFileFormat() {
        return ImageFileFormat.PNG;
    }

    @Override
    protected Optional<ImageFileFormat> getInvalidImageFileFormat() {
        return Optional.of(ImageFileFormat.GIF);
    }

    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return imageSaveValidationForOfferImageSupport;
    }

    @Override
    protected int getMaximumImagesPerRequest() {
        return MAX_IMAGES_PER_REQUEST_FOR_OFFER_IMAGE;
    }
}
