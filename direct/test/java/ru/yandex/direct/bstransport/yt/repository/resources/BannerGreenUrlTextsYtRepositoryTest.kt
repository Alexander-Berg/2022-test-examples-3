package ru.yandex.direct.bstransport.yt.repository.resources

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import ru.yandex.adv.direct.banner.resources.BannerResources

class BannerGreenUrlTextsYtRepositoryTest
    : AbstractBannerResourcesYtRepositoryTest(BannerGreenUrlTextsYtRepository::class.java) {

    companion object {
        @JvmStatic
        fun parameters(): List<Arguments> = listOf(
            arguments(
                "Filled fields",
                BannerResources.newBuilder().apply {
                    orderId = 12L
                    adgroupId = 13L
                    bannerId = 14L
                    iterId = 15L
                    updateTime = 16L
                    exportId = 17L
                    greenUrlTextPrefix = "prefix"
                    greenUrlTextSuffix = "suffix"
                }.build(),
                mapOf(
                    "OrderID" to 12L,
                    "AdGroupID" to 13L,
                    "BannerID" to 14L,
                    "IterID" to 15L,
                    "UpdateTime" to 16L,
                    "ExportID" to 17L,
                    "GreenUrlTextPrefix" to "prefix",
                    "GreenUrlTextSuffix" to "suffix",
                )
            ),
            arguments(
                "Filled partial empty fields",
                BannerResources.newBuilder().apply {
                    orderId = 12L
                    adgroupId = 13L
                    bannerId = 14L
                    iterId = 15L
                    updateTime = 16L
                    exportId = 17L
                    greenUrlTextPrefix = "prefix"
                    greenUrlTextSuffix = ""
                }.build(),
                mapOf(
                    "OrderID" to 12L,
                    "AdGroupID" to 13L,
                    "BannerID" to 14L,
                    "IterID" to 15L,
                    "UpdateTime" to 16L,
                    "ExportID" to 17L,
                    "GreenUrlTextPrefix" to "prefix",
                    "GreenUrlTextSuffix" to "",
                )
            ),
            arguments(
                "Filled empty fields",
                BannerResources.newBuilder().apply {
                    orderId = 12L
                    adgroupId = 13L
                    bannerId = 14L
                    iterId = 15L
                    updateTime = 16L
                    exportId = 17L
                    greenUrlTextPrefix = ""
                    greenUrlTextSuffix = ""
                }.build(),
                mapOf(
                    "OrderID" to 12L,
                    "AdGroupID" to 13L,
                    "BannerID" to 14L,
                    "IterID" to 15L,
                    "UpdateTime" to 16L,
                    "ExportID" to 17L,
                    "GreenUrlTextPrefix" to "",
                    "GreenUrlTextSuffix" to "",
                )
            ),
        )
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("parameters")
    fun getSchemaWithMappingTest(caseName: String, resource: BannerResources, ytFields: Map<String, Any?>) {
        compareProtoWithColumns(resource, ytFields)
    }
}
