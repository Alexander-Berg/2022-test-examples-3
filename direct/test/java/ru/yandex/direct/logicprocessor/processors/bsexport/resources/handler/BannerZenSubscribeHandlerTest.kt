package ru.yandex.direct.logicprocessor.processors.bsexport.resources.handler

import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.MetrikaSnippet
import ru.yandex.adv.direct.banner.resources.OptionalMetrikaSnippet
import ru.yandex.direct.logicprocessor.processors.bsexport.resources.container.ZenSubscribeInfo

const val COUNTER = 123L
const val GOAL = "yazen-subscribe"
const val PUBLISHER_ITEM_ID = "123abc"

class BannerZenSubscribeHandlerTest {

    lateinit var bannerZenSubscribeHandler: BannerZenSubscribeHandler

    @BeforeEach
    fun before() {
        bannerZenSubscribeHandler = mock()
        `when`(bannerZenSubscribeHandler.mapResourceToProto()).thenCallRealMethod()
    }

    @Test
    fun mapResourceToProto_NullResourceTest() {
        val actual = createBuilder()
        bannerZenSubscribeHandler.mapResourceToProto()(null, actual)

        val expected = createBuilder().apply {
            metrikaSnippet = OptionalMetrikaSnippet.newBuilder().build()
        }

        assertThat(actual.build()).isEqualTo(expected.build())
    }

    @Test
    fun mapResourceToProto() {
        val snippet = MetrikaSnippet.newBuilder()
            .setCounter(COUNTER)
            .setGoal(GOAL)
            .build()

        val zenSubscribeInfo = ZenSubscribeInfo(PUBLISHER_ITEM_ID, snippet)

        val actual = createBuilder()

        bannerZenSubscribeHandler.mapResourceToProto()(zenSubscribeInfo, actual)

        val expected = createBuilder().apply {
            publisherItemId = PUBLISHER_ITEM_ID
            metrikaSnippet = OptionalMetrikaSnippet.newBuilder().setValue(snippet).build()
        }

        assertThat(actual.build()).isEqualTo(expected.build())
    }

    @Test
    fun mapResourceToProto_WithoutPublisherItemId() {
        val snippet = MetrikaSnippet.newBuilder()
            .setCounter(COUNTER)
            .setGoal(GOAL)
            .build()

        val zenSubscribeInfo = ZenSubscribeInfo(null, snippet)

        val actual = createBuilder()

        bannerZenSubscribeHandler.mapResourceToProto()(zenSubscribeInfo, actual)

        val expected = createBuilder().apply {
            metrikaSnippet = OptionalMetrikaSnippet.newBuilder().setValue(snippet).build()
        }

        assertThat(actual.build()).isEqualTo(expected.build())
    }

    @Test
    fun mapResourceToProto_WithoutMetrikaSnippet() {

        val zenSubscribeInfo = ZenSubscribeInfo(PUBLISHER_ITEM_ID, null)

        val actual = createBuilder()

        bannerZenSubscribeHandler.mapResourceToProto()(zenSubscribeInfo, actual)

        val expected = createBuilder().apply {
            publisherItemId = PUBLISHER_ITEM_ID
            metrikaSnippet = OptionalMetrikaSnippet.newBuilder().build()
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
