package ru.yandex.direct.core.testing.data

import com.google.common.collect.ImmutableMap
import org.apache.commons.lang3.RandomStringUtils
import ru.yandex.direct.core.entity.banner.model.ImageSize
import ru.yandex.direct.core.entity.banner.model.ImageType
import ru.yandex.direct.core.entity.image.model.AvatarHost
import ru.yandex.direct.core.entity.image.model.BannerImageFormat
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta
import ru.yandex.direct.core.entity.image.model.ImageMdsMetaInfo
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter
import ru.yandex.direct.utils.JsonUtils

object TestBannerImageFormat {

    @JvmStatic
    fun createBannerImageFormat(width: Number, height: Number, format: String = "PNG", fileSizeKb: Int = 100): BannerImageFormat {
        return BannerImageFormat()
                .withImageHash(RandomStringUtils.randomAlphabetic(22))
                .withFormats(HashMap())
                .withMdsMeta(JsonUtils.toJson(createDefaultMdsMeta(format, fileSizeKb)))
                .withImageType(ImageType.IMAGE_AD)
                .withAvatarsHost(AvatarHost.AVATARS_MDST_YANDEX_NET)
                .withMdsGroupId(1)
                .withNamespace(BannerImageFormatNamespace.DIRECT_PICTURE)
                .withSize(ImageSize().withWidth(width.toInt()).withHeight(height.toInt()))
    }

    private fun createDefaultMdsMeta(format: String, fileSizeKb: Int): ImageMdsMeta {
        val smartCenter = ImageSmartCenter()
                .withHeight(1)
                .withWidth(1)
                .withX(0)
                .withY(0)
        val sizeMeta = ImageSizeMeta()
                .withHeight(10)
                .withWidth(10)
                .withPath("path")
                .withSmartCenters(ImmutableMap.of("1:1", smartCenter))

        return ImageMdsMeta()
                .withMeta(
                        ImageMdsMetaInfo()
                                .withOrigFormat(format)
                                .withOrigSizeBytes(fileSizeKb * 1024L)
                )
                .withSizes(ImmutableMap.of(
                        "x150", sizeMeta,
                        "x300", sizeMeta))
    }

}
