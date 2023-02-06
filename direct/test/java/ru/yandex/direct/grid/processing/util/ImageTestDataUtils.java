package ru.yandex.direct.grid.processing.util;

import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.core.entity.image.model.ImageSizeMeta;
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter;
import ru.yandex.direct.test.utils.RandomNumberUtils;

@ParametersAreNonnullByDefault
public class ImageTestDataUtils {

    public static final String DEFAULT_IMAGE_ASPECT = "1x1";

    public static ImageSmartCenter generateImageSmartCenter() {
        return new ImageSmartCenter()
                .withHeight(RandomNumberUtils.nextPositiveInteger())
                .withWidth(RandomNumberUtils.nextPositiveInteger())
                .withX(RandomNumberUtils.nextPositiveInteger())
                .withY(RandomNumberUtils.nextPositiveInteger());
    }

    public static ImageSizeMeta generateImageSizeMeta() {
        return new ImageSizeMeta()
                .withPath(RandomStringUtils.randomAlphanumeric(14))
                .withHeight(RandomNumberUtils.nextPositiveInteger())
                .withWidth(RandomNumberUtils.nextPositiveInteger())
                .withSmartCenters(Collections.singletonMap(DEFAULT_IMAGE_ASPECT, generateImageSmartCenter()));
    }

}
