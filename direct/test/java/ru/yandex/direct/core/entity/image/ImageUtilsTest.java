package ru.yandex.direct.core.entity.image;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.ImageSize;
import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.image.container.ImageMetaInformation;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta;
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter;
import ru.yandex.direct.core.entity.image.service.ImageUtils;
import ru.yandex.direct.utils.io.ResourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.BANNER_IMAGE_RATIO;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.BANNER_IMAGE_WIDE_MIN_WIDTH_SIZE;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.BANNER_IMAGE_WIDE_RATIO;
import static ru.yandex.direct.core.entity.image.service.ImageConstants.BANNER_REGULAR_IMAGE_MIN_SIZE;
import static ru.yandex.direct.core.entity.image.service.ImageUtils.calculateImageTypeOfTextBannerImage;
import static ru.yandex.direct.core.testing.data.TestImages.generateBlankGifImageData;

public class ImageUtilsTest {

    @Test
    public void copyImageMdsMeta() {
        ImageMdsMeta mdsMeta = new ImageMdsMeta().withSizes(defaultSizes());
        ImageMdsMeta copy = ImageUtils.copyImageMdsMeta(mdsMeta);
        assertThat(copy).isEqualTo(mdsMeta);
    }

    @Test
    public void copyImageMdsMeta_deepCopyCheck() {
        ImageMdsMeta mdsMeta = new ImageMdsMeta().withSizes(defaultSizes());
        ImageMdsMeta copy = ImageUtils.copyImageMdsMeta(mdsMeta);
        mdsMeta.getSizes().put("x80", new ImageSizeMeta());
        // При изменении исходного объекта копия не должна поменяться
        assertThat(copy).isNotEqualTo(mdsMeta);
    }

    private static Map<String, ImageSizeMeta> defaultSizes() {
        ImageSmartCenter smartCenter = new ImageSmartCenter().withX(1).withY(2);
        Map<String, ImageSmartCenter> smarts = new HashMap<>();
        smarts.put("1:1", smartCenter);
        ImageSizeMeta sizeMeta = new ImageSizeMeta().withPath("path")
                .withSmartCenters(smarts);
        Map<String, ImageSizeMeta> imageSizeMeta = new HashMap<>();
        imageSizeMeta.put("x80", sizeMeta);
        imageSizeMeta.put("x90", sizeMeta);
        return imageSizeMeta;
    }

    @Test
    public void getFramesNumber() throws IOException {
        int width = 101;
        int height = 203;
        byte[] bytes = generateBlankGifImageData(width, height);
        ImageMetaInformation imageMetaInformation = ImageUtils.collectImageMetaInformation(bytes);
        assertThat(imageMetaInformation.getFramesNumber()).isEqualTo(1);
    }

    @Test
    public void getImageSize() throws IOException {
        int width = 101;
        int height = 203;
        byte[] bytes = generateBlankGifImageData(width, height);
        ImageSize imageSize = ImageUtils.collectImageMetaInformation(bytes).getSize();
        assertThat(imageSize.getHeight()).isEqualTo(height);
        assertThat(imageSize.getWidth()).isEqualTo(width);
    }

    @Test
    public void calculateImageType_WideImage() {
        ImageSize imageSize = new ImageSize()
                .withWidth(BANNER_IMAGE_WIDE_MIN_WIDTH_SIZE)
                .withHeight((int) (BANNER_IMAGE_WIDE_MIN_WIDTH_SIZE / BANNER_IMAGE_WIDE_RATIO));
        ImageType imageType = calculateImageTypeOfTextBannerImage(imageSize);
        assertThat(imageType).isEqualTo(ImageType.WIDE);
    }

    @Test
    public void calculateImageType_RegularImage() {
        ImageSize imageSize = new ImageSize()
                .withWidth(BANNER_REGULAR_IMAGE_MIN_SIZE)
                .withHeight((int) (BANNER_REGULAR_IMAGE_MIN_SIZE * BANNER_IMAGE_RATIO) - 1);
        ImageType imageType = calculateImageTypeOfTextBannerImage(imageSize);
        assertThat(imageType).isEqualTo(ImageType.REGULAR);
    }

    @Test
    public void calculateImageType_SmallImage() {
        ImageSize imageSize = new ImageSize()
                .withWidth(BANNER_REGULAR_IMAGE_MIN_SIZE - 1)
                .withHeight((int) ((BANNER_REGULAR_IMAGE_MIN_SIZE - 1) * BANNER_IMAGE_RATIO) - 1);
        ImageType imageType = calculateImageTypeOfTextBannerImage(imageSize);
        assertThat(imageType).isEqualTo(ImageType.SMALL);
    }

    @Test
    public void getImageSize_JDK_7132728_WorkaroundWorks() throws IOException {
        byte[] data = ResourceUtils.readResourceBytes("./ru/yandex/direct/core/entity/image/JDK-7132728.gif");

        ImageMetaInformation meta = ImageUtils.collectImageMetaInformation(data);
        assertThat(meta.getSize())
                .isEqualTo(new ImageSize().withWidth(4096).withHeight(4096));
    }
}
