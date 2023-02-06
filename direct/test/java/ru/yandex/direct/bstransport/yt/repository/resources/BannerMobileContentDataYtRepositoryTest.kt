package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources

internal class BannerMobileContentDataYtRepositoryTest : AbstractBannerResourcesYtRepositoryTest(BannerMobileContentDataYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setMobileContentImpressionUrl("https://view.adjust.com/impression/qwerty" +
                "?ya_click_id={logid}&google_aid={google_aid}")
            .build()
        val expectedColumnNameToValue = mapOf<String, Any>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "MobileContentImpressionUrl" to "https://view.adjust.com/impression/qwerty" +
                "?ya_click_id={logid}&google_aid={google_aid}"
        )
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
