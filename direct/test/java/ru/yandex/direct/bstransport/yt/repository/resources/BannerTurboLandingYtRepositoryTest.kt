package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources

internal class BannerTurboLandingYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerTurbolandingYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setTurbolandingId(1234625L)
            .setTurbolandingHref(
                "https://yandex" +
                    ".ru/turbo?text=lpc%2Fe1979b3c5154e0e71b52a814838a7775b01a43d23e41fa207d49e16b11174157")
            .setTurbolandingSite("yandex.ru")
            .setTurbolandingDomainFilter("1234625.y-turbo")
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "TurbolandingID" to 1234625L,
            "TurbolandingHref" to "https://yandex" +
                ".ru/turbo?text=lpc%2Fe1979b3c5154e0e71b52a814838a7775b01a43d23e41fa207d49e16b11174157",
            "TurbolandingSite" to "yandex.ru",
            "TurbolandingDomainFilter" to "1234625.y-turbo"
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
