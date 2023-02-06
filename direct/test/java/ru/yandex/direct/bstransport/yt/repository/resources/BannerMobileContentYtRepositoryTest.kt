package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.OptionalPlatformName

internal class BannerMobileContentYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerMobileContentYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setMobileContentId(98765L)
            .setMobileContentBundleId("com.groundhog.mcpemaster")
            .setMobileContentSource(1)
            .setMobileContentHref("https://redirect.appmetrica.yandex" +
                ".com/serve/1178929768372987282?click_id={logid}&google_aid={google_aid}")
            .setMobileContentSite("play.google.com")
            .setMobileContentSiteFilter("com.groundhog.mcpemaster")
            .setMobileContentDomainFilter("www.taximaster.ru")
            .setPlatformName(OptionalPlatformName.newBuilder()
                .setValue(TestPlatformNames.YANDEX_MAPS_RU).build())
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "MobileContentID" to 98765L,
            "MobileContentBundleID" to "com.groundhog.mcpemaster",
            "MobileContentSource" to 1L,
            "MobileContentHref" to "https://redirect.appmetrica.yandex" +
                ".com/serve/1178929768372987282?click_id={logid}&google_aid={google_aid}",
            "MobileContentSite" to "play.google.com",
            "MobileContentSiteFilter" to "com.groundhog.mcpemaster",
            "MobileContentDomainFilter" to "www.taximaster.ru",
            "PlatformName" to TestPlatformNames.YANDEX_MAPS_RU
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
