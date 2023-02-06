package ru.yandex.direct.grid.processing.service.client.converter;

import java.util.List;

import javax.annotation.Nullable;

import ru.yandex.direct.core.entity.banner.model.ImageType;
import ru.yandex.direct.core.entity.image.converter.BannerImageConverter;
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdAdImageAvatarsHost;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdAdImageNamespace;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdAdImageType;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImage;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageFormat;
import ru.yandex.direct.grid.processing.model.cliententity.image.GdImageSize;

import static com.google.common.base.Preconditions.checkNotNull;
import static ru.yandex.direct.core.testing.info.BannerImageFormat.imageTypeToDb;
import static ru.yandex.direct.core.testing.steps.BannerSteps.DEFAULT_IMAGE_NAME_TEMPLATE;

public class TestClientEntityConverter {

    public static GdAdImageAvatarsHost toGdAdImageAvatarsHost(BannerImageFormat.AvatarHost avatarHost) {
        switch (avatarHost) {
            case TEST:
                return GdAdImageAvatarsHost.AVATARS_MDST_YANDEX_NET;
            case PROD:
                return GdAdImageAvatarsHost.AVATARS_MDS_YANDEX_NET;
            default:
                throw new IllegalArgumentException("Unsupported avatar host: " + avatarHost);
        }
    }

    public static GdAdImageNamespace toGdAdImageNamespace(BannerImageFormat.AvatarNamespace avatarNamespace) {
        switch (avatarNamespace) {
            case DIRECT:
                return GdAdImageNamespace.DIRECT;
            case DIRECT_PICTURE:
                return GdAdImageNamespace.DIRECT_PICTURE;
            default:
                throw new IllegalArgumentException("Unsupported avatar namespace: " + avatarNamespace);
        }
    }

    public static GdAdImageType toGdAdImageType(BannerImageFormat.ImageType imageType) {
        switch (imageType) {
            case REGULAR:
                return GdAdImageType.REGULAR;
            case WIDE:
                return GdAdImageType.WIDE;
            case SMALL:
                return GdAdImageType.SMALL;
            case IMAGE_AD:
                return GdAdImageType.IMAGE_AD;
            default:
                throw new IllegalArgumentException("Unsupported image type: " + imageType);
        }
    }

    @Nullable
    public static List<GdImageFormat> toGdImageFormats(BannerImageFormat imageFormat) {
        ImageType coreImageType = ImageType.fromSource(imageTypeToDb(imageFormat.getImageType()));
        checkNotNull(coreImageType, "image type can't be null");
        ImageMdsMeta imageMdsMeta = BannerImageConverter.toImageMdsMeta(imageFormat.getMdsMetaJson());

        return ClientEntityConverter.toSupportedGdImageFormats(coreImageType, imageMdsMeta);
    }

    public static GdImage toGdImage(BannerImageFormat imageFormat) {
        return new GdImage()
                .withImageHash(imageFormat.getImageHash())
                .withName(String.format(DEFAULT_IMAGE_NAME_TEMPLATE, imageFormat.getImageHash()))
                .withImageSize(new GdImageSize()
                        .withHeight(imageFormat.getHeight().intValue())
                        .withWidth(imageFormat.getWidth().intValue())
                )
                .withFormats(toGdImageFormats(imageFormat))
                .withMdsGroupId(imageFormat.getMdsGroupId())
                .withAvatarsHost(toGdAdImageAvatarsHost(imageFormat.getAvatarHost()))
                .withType(toGdAdImageType(imageFormat.getImageType()))
                .withNamespace(toGdAdImageNamespace(imageFormat.getAvatarNamespace()));
    }
}
