package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils

import org.apache.commons.lang.RandomStringUtils.randomAlphanumeric
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.yandex.direct.core.entity.image.model.AvatarHost
import ru.yandex.direct.core.entity.image.model.BannerImageFormatNamespace
import ru.yandex.direct.core.entity.image.model.Image
import ru.yandex.direct.core.entity.image.model.ImageFormat
import ru.yandex.direct.core.entity.image.model.ImageMdsMeta
import ru.yandex.direct.core.entity.image.model.ImageSizeMeta
import ru.yandex.direct.core.entity.image.model.ImageSmartCenter
import ru.yandex.direct.test.utils.randomPositiveInt
import ru.yandex.direct.core.entity.image.converter.BannerImageConverter as CoreBannerImageConverter


class BannerImageConverterTest {

    @Test
    fun test() {
        val mdsGroupId = randomPositiveInt()
        val imageHash = randomAlphanumeric(20)

        val mdsMeta = ImageMdsMeta()
            .withSizes(
                mapOf(
                    "y160" to ImageSizeMeta()
                        .withWidth(160)
                        .withHeight(100)
                        .withPath("/get-direct/$mdsGroupId/$imageHash/y160")
                        .withSmartCenters(
                            mapOf(
                                "1:1" to ImageSmartCenter()
                                    .withHeight(90).withWidth(90).withX(0).withY(0),
                                "4:3" to ImageSmartCenter()
                                    .withHeight(68).withWidth(90).withX(0).withY(0),
                                "3:4" to ImageSmartCenter()
                                    .withHeight(90).withWidth(68).withX(13).withY(0),
                            )
                        ),
                    "x140" to ImageSizeMeta()
                        .withWidth(140)
                        .withHeight(100)
                        .withPath("/get-direct/$mdsGroupId/$imageHash/x140")
                        .withSmartCenters(
                            mapOf(
                                "4:3" to ImageSmartCenter()
                                    .withHeight(69).withWidth(87).withX(0).withY(1),
                                "1:1" to ImageSmartCenter()
                                    .withHeight(91).withWidth(97).withX(0).withY(0),
                                "3:4" to ImageSmartCenter()
                                    .withHeight(91).withWidth(67).withX(13).withY(0),
                            )
                        ),
                    "x120" to ImageSizeMeta()
                        .withWidth(100)
                        .withHeight(80)
                        .withPath("/get-direct/$mdsGroupId/$imageHash/x100"),
                )
            )

        val mdsMetaUserOverride = ImageMdsMeta()
            .withSizes(
                mapOf("y160" to ImageSizeMeta()
                    .withSmartCenters(
                        mapOf("1:1" to ImageSmartCenter()
                            .withHeight(89).withWidth(89).withX(1).withY(1))
                    ))
            )

        val mdsMetaWithUserOverride = CoreBannerImageConverter.mergeImageMdsMeta(mdsMeta, mdsMetaUserOverride)

        val image = Image()
            .withAvatarsHost(AvatarHost.AVATARS_MDS_YANDEX_NET)
            .withNamespace(BannerImageFormatNamespace.DIRECT)
            .withMdsGroupId(mdsGroupId)
            .withImageHash(imageHash)
            .withFormats(
                mapOf(
                    "y160" to ImageFormat()
                        .withWidth(200)
                        .withHeight(160)
                        .withSmartCenter(
                            ImageSmartCenter()
                                .withHeight(180).withWidth(140)
                                .withX(2).withY(1)
                        ),
                    "x140" to ImageFormat()
                        .withWidth(140)
                        .withHeight(100)
                        .withSmartCenter(
                            ImageSmartCenter()
                                .withHeight(140).withWidth(100)
                                .withX(0).withY(1)
                        ),
                )
            )
            .withMdsMeta(mdsMeta)
            .withMdsMetaUserOverride(mdsMetaUserOverride)
            .withMdsMetaWithUserSettings(mdsMetaWithUserOverride)

        val expected: List<TImage> = listOf(
            TImage.newBuilder()
                .setFormat("y160")
                .setUrl("https://avatars.mds.yandex.net/get-direct/$mdsGroupId/$imageHash/y160")
                .setWidth(200)
                .setHeight(160)
                .addAllSmartCenters(
                    listOf(
                        TSmartCenter.newBuilder()
                            .setH(89).setW(89).setX(1).setY(1)
                            .build(),
                        TSmartCenter.newBuilder()
                            .setH(90).setW(68).setX(13).setY(0)
                            .build(),
                        TSmartCenter.newBuilder()
                            .setH(68).setW(90).setX(0).setY(0)
                            .build(),
                    )
                )
                .build(),
            TImage.newBuilder()
                .setFormat("x140")
                .setUrl("https://avatars.mds.yandex.net/get-direct/$mdsGroupId/$imageHash/x140")
                .setWidth(140)
                .setHeight(100)
                .addAllSmartCenters(
                    listOf(
                        TSmartCenter.newBuilder()
                            .setH(91).setW(97).setX(0).setY(0)
                            .build(),
                        TSmartCenter.newBuilder()
                            .setH(91).setW(67).setX(13).setY(0)
                            .build(),
                        TSmartCenter.newBuilder()
                            .setH(69).setW(87).setX(0).setY(1)
                            .build(),
                    )
                )
                .build(),
        )

        val actual = BannerImageConverter.toImages(image)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun testEmptyMeta() {
        val mdsGroupId = randomPositiveInt()
        val imageHash = randomAlphanumeric(20)

        val image = Image()
            .withAvatarsHost(AvatarHost.AVATARS_MDS_YANDEX_NET)
            .withNamespace(BannerImageFormatNamespace.DIRECT)
            .withMdsGroupId(mdsGroupId)
            .withImageHash(imageHash)
            .withFormats(
                mapOf(
                    "y160" to ImageFormat()
                        .withWidth(200)
                        .withHeight(160),
                    "x140" to ImageFormat()
                        .withWidth(140)
                        .withHeight(100),
                )
            )
            .withMdsMeta(null)

        val expected: List<TImage> = listOf(
            TImage.newBuilder()
                .setFormat("y160")
                .setUrl("https://avatars.mds.yandex.net/get-direct/$mdsGroupId/$imageHash/y160")
                .setWidth(200)
                .setHeight(160)
                .build(),
            TImage.newBuilder()
                .setFormat("x140")
                .setUrl("https://avatars.mds.yandex.net/get-direct/$mdsGroupId/$imageHash/x140")
                .setWidth(140)
                .setHeight(100)
                .build(),
        )

        val actual = BannerImageConverter.toImages(image)

        assertThat(actual).isEqualTo(expected)
    }

}
