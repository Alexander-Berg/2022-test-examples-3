package ru.yandex.direct.web.entity.uac.model

import junitparams.JUnitParamsRunner
import org.junit.Test
import org.junit.runner.RunWith
import ru.yandex.direct.avatars.client.model.answer.ImageSize
import ru.yandex.direct.avatars.client.model.answer.SmartArea
import ru.yandex.direct.core.entity.uac.model.UacAvatarsMeta
import ru.yandex.direct.core.entity.uac.model.UacImageSize
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.web.configuration.DirectWebTest

@DirectWebTest
@RunWith(JUnitParamsRunner::class)
class MetaFilterTest {
    private val metaFilter = MetaFilter()

    private val avatarsMeta = UacAvatarsMeta(
        colorWizBack = "#FFFFFF",
        colorWizButton = "#EEEEEE",
        colorWizButtonText = "#DDDDDD",
        colorWizText = "#CCCCCC",
        crc64 = "A0EDB296A3629F59",
        origSize = UacImageSize(width = 1200, height = 800),
        origFormat = "JPEG",
        origAnimated = false,
        origSizeBytes = 123456,
        origOrientation = 1,
    )

    private val smartCenter = SmartArea(300, 500, 80, 3)

    private val unknownSmartCenter = SmartArea(50, 100, 1, 1)

    private val sizes: Map<String, ImageSize> = mapOf(
        "wx1080" to ImageSize()
            .withHeight(603)
            .withWidth(1080)
            .withSmartCenter(smartCenter)
            .withSmartCenters(emptyMap()),
        "unkonwn" to ImageSize()
            .withHeight(100)
            .withWidth(200)
            .withSmartCenter(unknownSmartCenter)
            .withSmartCenters(emptyMap()),
    )

    private val filteredMeta: Map<String, Any> = mapOf(
        "ColorWizBack" to "#FFFFFF",
        "ColorWizButton" to "#EEEEEE",
        "ColorWizButtonText" to "#DDDDDD",
        "ColorWizText" to "#CCCCCC",
        "crc64" to "A0EDB296A3629F59",
        "orig-size" to mapOf(
            "width" to 1200,
            "height" to 800,
        ),
        "orig-format" to "image/jpeg",
        "orig-animated" to false,
        "orig-size-bytes" to 123456,
        "sizes" to mapOf(
            "wx1080" to mapOf(
                "height" to 603,
                "width" to 1080,
                "smart-center" to mapOf(
                    "h" to 300,
                    "w" to 500,
                    "x" to 80,
                    "y" to 3,
                ),
                "smart-centers" to emptyMap<String, Any>()
            ),
        )
    )

    @Test
    fun test() {
        metaFilter.filter(avatarsMeta, sizes).checkEquals(filteredMeta)
    }
}
