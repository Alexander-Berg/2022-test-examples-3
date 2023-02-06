package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources

internal class BannerTitleAndBodyYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerTitleAndBodyYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setBody("Укрывной материал ПВХ в Саратове. От 500 рублей за шт. Звоните сейчас!")
            .setTitle("Купить полога в Саратове")
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Title" to "Купить полога в Саратове",
            "Body" to "Укрывной материал ПВХ в Саратове. От 500 рублей за шт. Звоните сейчас!"
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
