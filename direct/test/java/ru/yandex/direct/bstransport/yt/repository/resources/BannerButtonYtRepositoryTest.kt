package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.api.Test
import ru.yandex.adv.direct.banner.resources.BannerResources
import ru.yandex.adv.direct.banner.resources.Button
import ru.yandex.adv.direct.banner.resources.OptionalButton
import java.util.*

internal class BannerButtonYtRepositoryTest
    : AbstractBannerResourcesYtRepositoryTest(BannerButtonYtRepository::class.java) {
    @Test
    fun getSchemaWithMappingTest() {
        val button = Button.newBuilder()
            .setButtonCaption("Купить!")
            .setButtonKey("buy")
            .setButtonHref("https://site.com")
            .build()
        val resource = BannerResources.newBuilder()
            .setOrderId(12L)
            .setAdgroupId(456L)
            .setBannerId(1643L)
            .setIterId(1987L)
            .setUpdateTime(123434242L)
            .setExportId(123L)
            .setButton(OptionalButton.newBuilder().setValue(button).build())
            .build()

        val expectedColumnNameToValue = mapOf<String, Any?>(
            "OrderID" to 12L,
            "BannerID" to 1643L,
            "ExportID" to 123L,
            "AdGroupID" to 456L,
            "UpdateTime" to 123434242L,
            "IterID" to 1987L,
            "Button" to button
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
            .setButton(OptionalButton.newBuilder().build())
            .build()

        // через  collect(toMap()) нельзя, так как ожидаются значения null
        val expectedColumnNameToValue = HashMap<String, Any?>()
        expectedColumnNameToValue["OrderID"] = 12L
        expectedColumnNameToValue["BannerID"] = 1643L
        expectedColumnNameToValue["ExportID"] = 123L
        expectedColumnNameToValue["AdGroupID"] = 456L
        expectedColumnNameToValue["IterID"] = 1987L
        expectedColumnNameToValue["UpdateTime"] = 123434242L
        expectedColumnNameToValue["Button"] = null
        compareProtoWithColumns(resource, expectedColumnNameToValue)
    }
}
