package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.GreenUrlTexts

class BannerGreenUrlTextsHandlerTest {

    lateinit var bannerGreenUrlTextsHandler: BannerGreenUrlTextsHandler

    @BeforeEach
    fun before() {
        bannerGreenUrlTextsHandler = mock()
        `when`(bannerGreenUrlTextsHandler.mapResourceToProto()).thenCallRealMethod()
    }

    @Test
    fun mapResourceToProto_NullResourceTest() {
        val actual = createBuilder()
        bannerGreenUrlTextsHandler.mapResourceToProto()(null, actual)

        val expected = createBuilder().apply {
            greenUrlTextPrefix = ""
            greenUrlTextSuffix = ""
        }

        assertThat(actual.build()).isEqualTo(expected.build())
    }

    @Test
    fun mapResourceToProto_PartiallyFilled() {
        val greenUrlTexts = GreenUrlTexts(null, "Suffix")

        val actual = createBuilder()
        bannerGreenUrlTextsHandler.mapResourceToProto()(greenUrlTexts, actual)

        val expected = createBuilder().apply {
            greenUrlTextPrefix = ""
            greenUrlTextSuffix = "Suffix"
        }

        assertThat(actual.build()).isEqualTo(expected.build())
    }

    @Test
    fun mapResourceToProto() {
        val greenUrlTexts = GreenUrlTexts("Prefix", "Suffix")

        val actual = createBuilder()
        bannerGreenUrlTextsHandler.mapResourceToProto()(greenUrlTexts, actual)

        val expected = createBuilder().apply {
            greenUrlTextPrefix = "Prefix"
            greenUrlTextSuffix = "Suffix"
        }

        assertThat(actual.build()).isEqualTo(expected.build())
    }

    private fun createBuilder() = BannerResources.newBuilder().apply {
        exportId = 1
        adgroupId = 2
        bannerId = 3
        orderId = 4
        iterId = 5
        updateTime = 6
    }
}
