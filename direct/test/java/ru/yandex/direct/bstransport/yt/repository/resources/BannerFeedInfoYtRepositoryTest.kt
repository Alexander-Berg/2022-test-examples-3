package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.FeedInfo
import ru.yandex.adv.direct.banner.resources.OptionalFeedInfo

class BannerFeedInfoYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerFeedInfoYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val feedInfoProto = FeedInfo.newBuilder().apply {
            marketBusinessId = 1
            marketShopId = 2
            marketFeedId = 3
            directFeedId = 4
            filterId = 5
        }.build()

        val resource = BannerResources.newBuilder().apply {
            orderId = 12L
            adgroupId = 13L
            bannerId = 14L
            iterId = 15L
            updateTime = 16L
            exportId = 17L
            feedInfo = OptionalFeedInfo.newBuilder().setValue(feedInfoProto).build()
        }.build()

        val expected = mapOf(
            "OrderID" to 12L,
            "AdGroupID" to 13L,
            "BannerID" to 14L,
            "IterID" to 15L,
            "UpdateTime" to 16L,
            "ExportID" to 17L,
            "FeedInfo" to feedInfoProto,
        )
        compareProtoWithColumns(resource, expected)
    }

    @Test
    fun getSchemaWithoutFeedInfo() {
        val resource = BannerResources.newBuilder().apply {
            orderId = 12L
            adgroupId = 13L
            bannerId = 14L
            iterId = 15L
            updateTime = 16L
            exportId = 17L
            feedInfo = OptionalFeedInfo.newBuilder().build()
        }.build()

        val expected = mapOf(
            "OrderID" to 12L,
            "AdGroupID" to 13L,
            "BannerID" to 14L,
            "IterID" to 15L,
            "UpdateTime" to 16L,
            "ExportID" to 17L,
            "FeedInfo" to null,
        )
        compareProtoWithColumns(resource, expected)
    }
}
