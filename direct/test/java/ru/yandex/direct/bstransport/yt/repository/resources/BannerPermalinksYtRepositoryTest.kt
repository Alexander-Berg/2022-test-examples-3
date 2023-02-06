package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.UInt64List
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree

internal class BannerPermalinksYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerPermalinksYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setPermalinkId(123L)
            .setPermalinkHref(
                "https://yandex.ru/profile/231414")
            .setPermalinkSite("yandex.ru")
            .setPermalinkDomainFilter("231414.ya-profile")
            .setPermalinkAssignType("auto")
            .setPermalinkChainIds(
                UInt64List.newBuilder()
                    .addAllValues(listOf(1L, 2L, 3L)).build())
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "PermalinkID" to 123L,
            "PermalinkHref" to "https://yandex.ru/profile/231414",
            "PermalinkSite" to "yandex.ru",
            "PermalinkDomainFilter" to "231414.ya-profile",
            "PermalinkAssignType" to "auto",
            "PermalinkChainIDs" to YTree.listBuilder().value(1L).value(2L).value(3L).buildList()
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
