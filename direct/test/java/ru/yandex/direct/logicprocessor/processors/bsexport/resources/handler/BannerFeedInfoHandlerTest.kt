package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.FeedInfo
import ru.yandex.adv.direct.banner.resources.OptionalFeedInfo

class BannerFeedInfoHandlerTest {

    lateinit var handler: BannerFeedInfoHandler

    @BeforeEach
    fun before() {
        handler = mock()
        whenever(handler.mapResourceToProto()).thenCallRealMethod()
    }

    @Test
    fun mapResourceToProto_NullResourceTest() {
        val actual = createBuilder()
        handler.mapResourceToProto()(null, actual)

        val expected = createBuilder().apply {
            feedInfo = OptionalFeedInfo.newBuilder().build()
        }

        Assertions.assertThat(actual.build()).isEqualTo(expected.build())
    }

    @Test
    fun mapResourceToProto() {
        val actual = createBuilder()

        val feedInfoMessage = FeedInfo.newBuilder().apply {
            marketFeedId = 1
            marketShopId = 2
            marketBusinessId = 3
            directFeedId = 4
            filterId = 5
        }.build()

        handler.mapResourceToProto()(feedInfoMessage, actual)

        val expected = createBuilder().apply {
            feedInfo = OptionalFeedInfo.newBuilder()
                .setValue(feedInfoMessage)
                .build()
        }

        Assertions.assertThat(actual.build()).isEqualTo(expected.build())
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
