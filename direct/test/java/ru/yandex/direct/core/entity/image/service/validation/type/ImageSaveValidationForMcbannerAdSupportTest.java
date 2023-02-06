package ru.yandex.direct.core.entity.image.service.validation.type;

import java.util.Optional;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.image.container.ImageFileFormat;
import ru.yandex.direct.core.entity.image.service.ImageConstants;

import static ru.yandex.direct.core.entity.image.service.ImageConstants.ALLOWED_SIZES_FOR_MCBANNER;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.MAX_IMAGES_PER_REQUEST_FOR_MCBANNER;

public class ImageSaveValidationForMcbannerAdSupportTest extends ImageSaveValidationSupportBaseTest {
    private ImageSaveValidationForMcbannerAdSupport imageSaveValidationForMcbannerAdSupport =
            new ImageSaveValidationForMcbannerAdSupport();


    @Override
    public ImageSaveValidationSupport getImageSaveValidationSupport() {
        return imageSaveValidationForMcbannerAdSupport;
    }

    @Override
    protected ImageSize getValidImageSize() {
        return ALLOWED_SIZES_FOR_MCBANNER.stream().findAny().orElse(null);
    }

    @Override
    protected ImageSize getInvalidImageSize() {
        return new ImageSize().withWidth(1).withHeight(1);
    }

    @Override
    protected int getInvalidImageFileSize() {
        return ImageConstants.MAX_IMAGE_FILE_SIZE_FOR_MC_BANNER + 1;
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
        return MAX_IMAGES_PER_REQUEST_FOR_MCBANNER;
    }
}
