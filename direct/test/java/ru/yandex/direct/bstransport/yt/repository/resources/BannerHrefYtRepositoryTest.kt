package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.OptionalPlatformName

internal class BannerHrefYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerHrefYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setHref("http://www.celebris.ru/w_0.html")
            .setSite("www.celebris.ru")
            .setSiteFilter("celebris.ru")
            .setDomainFilter("celebris.com")
            .setSiteFilterId(12345L)
            .setDomainFilterId(12345L)
            .setPlatformName(OptionalPlatformName.newBuilder().setValue(TestPlatformNames.YANDEX_MAPS_RU).build())
            .build()

        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Href" to "http://www.celebris.ru/w_0.html",
            "Site" to "www.celebris.ru",
            "DomainFilter" to "celebris.com",
            "SiteFilter" to "celebris.ru",
            "DomainFilterID" to 12345L,
            "SiteFilterID" to 12345L,
            "PlatformName" to TestPlatformNames.YANDEX_MAPS_RU
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }

    @Test
    fun getSchemaWithMappingEmptyPlatformNameTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setHref("http://www.celebris.ru/w_0.html")
            .setSite("www.celebris.ru")
            .setSiteFilter("celebris.ru")
            .setDomainFilter("celebris.com")
            .setSiteFilterId(12345L)
            .setDomainFilterId(12345L)
            .setPlatformName(OptionalPlatformName.newBuilder().build())
            .build()

        val expectedColumnNameToValue = mapOf<String, Any?>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Href" to "http://www.celebris.ru/w_0.html",
            "Site" to "www.celebris.ru",
            "DomainFilter" to "celebris.com",
            "SiteFilter" to "celebris.ru",
            "DomainFilterID" to 12345L,
            "SiteFilterID" to 12345L,
            "PlatformName" to null
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
