package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import javax.annotation.ParametersAreNonnullByDefault

@ParametersAreNonnullByDefault
internal class BannerNameYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerNameYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val name = "Укрывной материал ПВХ"
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setName(name)
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Name" to name
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
