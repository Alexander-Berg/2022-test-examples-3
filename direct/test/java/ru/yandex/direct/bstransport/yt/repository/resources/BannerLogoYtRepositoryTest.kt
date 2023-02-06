package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.Format
import ru.yandex.adv.direct.banner.resources.Logo
import ru.yandex.adv.direct.banner.resources.OptionalLogo

internal class BannerLogoYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerLogoYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val logo = Logo.newBuilder()
            .addFormats(Format.newBuilder()
                .setFormat("x80")
                .setHeight(53)
                .setWidth(80)
                .setUrl("http://avatars.mds.yandex.net/get-direct/2398261/RqvjpvZ5M4gxsLOCGVNYZQ/x80")
                .build())
            .build()
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setLogo(OptionalLogo.newBuilder().setValue(logo).build())
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Logo" to logo
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }


    @Test
    fun getSchemaWithMappingNullButtonTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setLogo(OptionalLogo.newBuilder().build())
            .build()

        val expectedColumnNameToValue = mapOf<String, Any?>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Logo" to null
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
