package ru.yandex.direct.core.entity.internalads.restriction;

import ru.yandex.direct.core.entity.image.model.BannerImageFormat;
import ru.yandex.direct.core.testing.data.TestBannerImageFormat;

class RestrictionImageTestUtils {
    private RestrictionImageTestUtils() {
    }

    static BannerImageFormat createBannerImageFormatDim(int width, int height) {
        return TestBannerImageFormat.createBannerImageFormat(width, height, "PNG", 50);
    }

    static BannerImageFormat createBannerImageFormat(String format) {
        return createBannerImageFormatEx(20, 20, format);
    }

    static BannerImageFormat createBannerImageFormatFileSize(int fileSizeKb) {
        return TestBannerImageFormat.createBannerImageFormat(20, 20, "PNG", fileSizeKb);
    }

    static BannerImageFormat createBannerImageFormatEx(int width, int height, String format) {
        return TestBannerImageFormat.createBannerImageFormat(width, height, format, 50);
    }

}
