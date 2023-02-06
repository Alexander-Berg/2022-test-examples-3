package ru.yandex.market.mdm.storage.service.physical

import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.storage.repository.NamedMdmField
import ru.yandex.market.yt.util.table.model.YtColumnSchema
import ru.yandex.market.yt.util.table.model.YtTableModel

class MdmYtPrimaryIndexModelTest {
    companion object {
        val SERVICE_ID_PATH = listOf(1L, 2L, 3L)
        val SHOP_SKU_PATH = listOf(1L, 5L, 6L)
        val SOURCE_TYPE_PATH = listOf(8L, 11L)
        val SOURCE_ID_PATH = listOf(8L, 9L, 10L)
    }

    private lateinit var model: MdmYtPrimaryIndexModel

    @Before
    fun setUp() {
        model = MdmYtPrimaryIndexModel("", listOf(
            MdmYtIndexColumn("service_id", YtColumnSchema.Type.UINT64, MdmAttributeDataType.INT64, SERVICE_ID_PATH),
            MdmYtIndexColumn("shop_sku", YtColumnSchema.Type.STRING, MdmAttributeDataType.STRING, SHOP_SKU_PATH),
            MdmYtIndexColumn("source_type", YtColumnSchema.Type.STRING, MdmAttributeDataType.STRING, SOURCE_TYPE_PATH),
            MdmYtIndexColumn("source_id", YtColumnSchema.Type.STRING, MdmAttributeDataType.STRING, SOURCE_ID_PATH),
        ), YtTableModel())
    }

    @Test
    fun testRawValuesExtraction() {
        // given
        val serviceId1 = 4245325L
        val serviceId2 = 4245326L
        val serviceId3 = 4245327L
        val shopSku = "My name is Yoshikage Kira"
        val sourceType = "I'm 33 years old"
        val sourceId = "dio"
        val entity = entity(mapOf(
            1L to listOf(entity(mapOf(
                2L to listOf(
                    entityInt64(3L, serviceId1), // path = 1,2,3
                    entityInt64(3L, serviceId2), // path = 1,2,3
                    entityInt64(3L, serviceId3) // path = 1,2,3
                ),
                5L to listOf(entityStr(6L, shopSku)) // path = 1,5,6
            ))),
            8L to listOf(entity(mapOf(
                9L to listOf(entityStr(10L, sourceId)) // path = 8,9,10
            )).toBuilder().putMdmAttributeValues(11, strVal(sourceType)).build() // path = 8,11
        )))

        // when
        val extracted = model.extractIndexValues(entity) - NamedMdmField.MDM_ID.name

        // then
        extracted shouldBe mapOf(
            "service_id" to serviceId1,
            "shop_sku" to shopSku,
            "source_type" to sourceType,
            "source_id" to sourceId,
        )
    }

    @Test
    fun testSupportCheck() {
        model.areAttributePathsSupported(listOf(
            SHOP_SKU_PATH,
            SOURCE_TYPE_PATH,
            SERVICE_ID_PATH,
            SOURCE_ID_PATH
        )) shouldBe true

        model.areAttributePathsSupported(listOf(
            SERVICE_ID_PATH,
            SHOP_SKU_PATH,
            listOf(3L, 2L, 1L),
            SOURCE_TYPE_PATH,
            SOURCE_ID_PATH,
        )) shouldBe false
    }
}
