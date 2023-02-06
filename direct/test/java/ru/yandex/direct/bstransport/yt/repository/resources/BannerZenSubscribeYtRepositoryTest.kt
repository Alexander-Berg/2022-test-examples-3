package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.MetrikaSnippet
import ru.yandex.adv.direct.banner.resources.OptionalMetrikaSnippet

const val COUNTER = 123L
const val GOAL = "yazen-subscribe"
const val PUBLISHER_ITEM_ID = "123abc"

class BannerZenSubscribeYtRepositoryTest
    : AbstractBannerResourcesYtRepositoryTest(BannerZenSubscribeYtRepository::class.java) {

    @Test
    fun getSchemaWithMappingTest() {
        val snippet = MetrikaSnippet.newBuilder()
            .setCounter(COUNTER)
            .setGoal(GOAL)
            .build()

        val resource = BannerResources.newBuilder().apply {
            orderId = 12L
            adgroupId = 13L
            bannerId = 14L
            iterId = 15L
            updateTime = 16L
            exportId = 17L
            publisherItemId = PUBLISHER_ITEM_ID
            metrikaSnippet = OptionalMetrikaSnippet.newBuilder()
                .setValue(snippet)
                .build()
        }.build()

        val expected = mapOf(
            "OrderID" to 12L,
            "AdGroupID" to 13L,
            "BannerID" to 14L,
            "IterID" to 15L,
            "UpdateTime" to 16L,
            "ExportID" to 17L,
            "PublisherItemId" to PUBLISHER_ITEM_ID,
            "MetrikaSnippet" to snippet,
        )

        compareProtoWithColumns(resource, expected)
    }

    @Test
    fun getSchemaWithoutZenSubscribeInfoTest() {

        val resource = BannerResources.newBuilder().apply {
            orderId = 12L
            adgroupId = 13L
            bannerId = 14L
            iterId = 15L
            updateTime = 16L
            exportId = 17L
            publisherItemId = ""
            metrikaSnippet = OptionalMetrikaSnippet.newBuilder().build()
        }.build()

        val expected = mapOf(
            "OrderID" to 12L,
            "AdGroupID" to 13L,
            "BannerID" to 14L,
            "IterID" to 15L,
            "UpdateTime" to 16L,
            "ExportID" to 17L,
            "PublisherItemId" to "",
            "MetrikaSnippet" to null,
        )

        compareProtoWithColumns(resource, expected)
    }
}
