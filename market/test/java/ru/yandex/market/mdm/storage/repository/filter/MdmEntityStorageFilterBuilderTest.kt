package ru.yandex.market.mdm.storage.repository.filter

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mbo.yt.index.read.Direction
import ru.yandex.market.mdm.http.MdmAttributeValue
import ru.yandex.market.mdm.http.MdmAttributeValues
import ru.yandex.market.mdm.http.MdmExternalKey
import ru.yandex.market.mdm.http.MdmOrderByClause
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.storage.repository.NamedMdmField
import ru.yandex.market.mdm.storage.service.physical.MdmYtIndexColumn
import ru.yandex.market.mdm.storage.service.physical.MdmYtPrimaryIndexModel
import ru.yandex.market.mdm.storage.service.physical.MdmYtSecondaryIndexModel
import ru.yandex.market.mdm.storage.service.physical.MdmYtTableModel
import ru.yandex.market.mdm.storage.service.physical.SecondaryIndexType
import ru.yandex.market.yt.util.table.model.YtColumnSchema
import ru.yandex.market.yt.util.table.model.YtTableModel

class MdmEntityStorageFilterBuilderTest {
    companion object {
        val SERVICE_ID_PATH = listOf(1L, 2L, 3L)
        val SERVICE_ID = NamedMdmField("service_id", MdmAttributeDataType.INT64)
        val ACTIVE_PATH = listOf(8L, 11L)
        val ACTIVE_ID = NamedMdmField("active", MdmAttributeDataType.BOOLEAN)
    }

    val primaryIndexModel = MdmYtPrimaryIndexModel("", listOf(
        MdmYtIndexColumn(SERVICE_ID.name, YtColumnSchema.Type.UINT64, SERVICE_ID.type, SERVICE_ID_PATH),
    ), YtTableModel())

    val secondaryIndexModel = MdmYtSecondaryIndexModel("", listOf(
        MdmYtIndexColumn(ACTIVE_ID.name, YtColumnSchema.Type.BOOLEAN, ACTIVE_ID.type, ACTIVE_PATH),
    ), YtTableModel(), SecondaryIndexType.ONE_2_ONE)

    val tableModel = MdmYtTableModel(
        tableName = "test",
        path = "\\test",
        mdmEntityTypeId = 1L,
        ytTableModel = YtTableModel(),
        primaryIndexModel = primaryIndexModel,
        secondaryIndexModels = listOf(secondaryIndexModel),
        auditRequired = true,
    )

    @Test
    fun `should build simple filter for primary index`() {
        // given
        val externalKeys = listOf(
            MdmExternalKey.newBuilder()
                .addMdmAttributeValues(
                    MdmAttributeValues.newBuilder()
                        .addAllMdmAttributePath(SERVICE_ID_PATH)
                        .addValues(
                            MdmAttributeValue.newBuilder()
                                .setInt64(100)
                                .build()
                        )
                )
                .build()
        )

        // when
        val filter = MdmEntityStorageFilterBuilder(tableModel)
            .addIndexValues(externalKeys)
            .build()

        // then
        filter.printableInfo() shouldBe MdmEntityStorageFilter().idxIn(IndexFieldValues(SERVICE_ID, listOf(100))).printableInfo()
    }

    @Test
    fun `should build simple filter for secondary index`() {
        // given
        val externalKeys = listOf(
            MdmExternalKey.newBuilder()
                .addMdmAttributeValues(
                    MdmAttributeValues.newBuilder()
                        .addAllMdmAttributePath(ACTIVE_PATH)
                        .addValues(
                            MdmAttributeValue.newBuilder()
                                .setBool(true)
                                .build()
                        )
                )
                .build()
        )

        // when
        val filter = MdmEntityStorageFilterBuilder(tableModel)
            .addIndexValues(externalKeys)
            .build()

        // then
        filter.printableInfo() shouldBe MdmEntityStorageFilter().idxIn(IndexFieldValues(ACTIVE_ID, listOf(true))).printableInfo()
    }

    @Test
    fun `should build simple filter for pagination only`() {
        // given
        val orderBy = MdmOrderByClause.newBuilder()
            .addAllMdmAttributePath(SERVICE_ID_PATH)
            .build()

        // when
        val filter = MdmEntityStorageFilterBuilder(tableModel)
            .addOrderBy(listOf(orderBy))
            .addPagination(100, 200)
            .build()

        // then
        filter.printableInfo() shouldBe MdmEntityStorageFilter()
            .addOrderBy(SERVICE_ID, Direction.ASC)
            .setLimit(200)
            .setOffset(100)
            .printableInfo()
    }

    @Test
    fun `should build simple filter for pagination with default order by`() {
        // given
        val orderBy = MdmOrderByClause.newBuilder()
            .addAllMdmAttributePath(SERVICE_ID_PATH)
            .build()

        // when
        val filter = MdmEntityStorageFilterBuilder(tableModel)
            .addPagination(100, 200)
            .build()

        // then primary index used for order by
        filter.printableInfo() shouldBe MdmEntityStorageFilter()
            .addOrderBy(SERVICE_ID, Direction.ASC)
            .setLimit(200)
            .setOffset(100)
            .printableInfo()
    }

}
