package ru.yandex.market.mdm.storage.service.logical

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.lib.model.mdm.I18nStrings
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValue
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity

class EntityDiffsTest {

    @Test
    fun `test null cases`() {
        val entity = ProtoEntity.getDefaultInstance()

        diff(null, entity) shouldBe entity
        diff(entity, entity) shouldBe null
        diff(null, null) shouldBe null
        diff(entity, null) shouldBe entity.toBuilder()
            .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().setDeleted(true))
            .build()
    }

    @Test
    fun `test fully equal`() {
        val entity = entity(meta(),
            vInt64(1, 500076L),
            vBool(2, true),
            vString(3, "mooo!"),
            vOption(4, 653456256L),
            vNumeric(5, "16.0054"),
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!"), vTimestamp(80, 1643974644000)))
        )

        diff(entity, entity) shouldBe null
    }

    @Test
    fun `test difference in versions ignored`() {
        val entity1 = entity(meta(100500),
            vInt64(1, 500076L),
            vBool(2, true),
            vString(3, "mooo!"),
            vOption(4, 653456256L),
            vNumeric(5, "16.0054"),
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!"), vTimestamp(80, 1643974644000)))
        )

        val entity2 = entity(meta(200600),
            vInt64(1, 500076L),
            vBool(2, true),
            vString(3, "mooo!"),
            vOption(4, 653456256L),
            vNumeric(5, "16.0054"),
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!"), vTimestamp(80, 1643974644000)))
        )

        diff(entity1, entity2) shouldBe null
    }

    @Test
    fun `test difference in source only is significant`() {
        val entity1 = entity(meta(),
            vInt64(1, 500076L, "172", "MEASUREMENT"),
            vBool(2, true, "172", "MEASUREMENT"),
            vString(3, "mooo!", "172", "MEASUREMENT"),
            vOption(4, 653456256L, "172", "MEASUREMENT"),
            vNumeric(5, "16.0054", "172", "MEASUREMENT"),
            vReferenceMdmId(6, 1000007563L, "172", "MEASUREMENT")
        )

        val entity2 = entity(meta(),
            vInt64(1, 500076L),
            vBool(2, true, "172", "SUPPLIER"),
            vString(3, "mooo!", "90", "MEASUREMENT"),
            vOption(4, 653456256L, "90", "SUPPLIER"),
            vNumeric(5, "16.0054", "", "MEASUREMENT"),
            vReferenceMdmId(6, 1000007563L, "172", "")
        )

        diff(entity1, entity2) shouldBe entity2
    }

    @Test
    fun `test killing or reviving equal entities is significant`() {
        val alive = entity(meta(100500),
            vInt64(1, 500076L)
        )

        val dead = entity(meta(100500, 200600),
            vInt64(1, 500076L)
        )

        val dead2 = entity(meta(100500, 0, true),
            vInt64(1, 500076L)
        )

        diff(alive, dead) shouldBe entity(meta(100500, 200600))
        diff(dead, alive) shouldBe entity(meta(100500))

        diff(alive, dead2) shouldBe entity(meta(100500, 0, true))
        diff(dead2, alive) shouldBe entity(meta(100500))
    }

    @Test
    fun `test attribute change provides diff with it`() {
        val entity1 = entity(meta(100500),
            vInt64(1, 500076L),
            vBool(2, true),
            vString(3, "mooo!"),
            vOption(4, 653456256L),
            vNumeric(5, "16.0054"),
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!")))
        )

        val entity2 = entity(meta(200600),
            vInt64(1, 500076L + 1L), // <-----
            vBool(2, true),
            vString(3, "zzzzz"), // <-----
            vOption(4, 653456256L),
            vNumeric(5, "3.14"), // <-----
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, true), vString(30, "www"))) // <-----
        )

        val expectedDiff = entity(meta(200600),
            vInt64(1, 500076L + 1L),
            vString(3, "zzzzz"),
            vNumeric(5, "3.14"),
            vStruct(9, entity(meta(), vBool(20, true), vString(30, "www")))
        )

        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test removal of attribute provides diff with specific marker`() {
        val entity1 = entity(meta(100500),
            vInt64(1, 500076L),
            vBool(2, true),
            vString(3, "mooo!"),
            vOption(4, 653456256L),
            vNumeric(5, "16.0054"),
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!")))
        )

        val entity2 = entity(meta(200600),
            // no int64
            vBool(2, true),
            // no string
            vOption(4, 653456256L),
            // no numeric
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            // no struct
        )

        val expectedDiff = entity(meta(200600),
            removed(1),
            removed(3),
            removed(5),
            removed(9)
        )

        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test addition of attribute provides diff with it`() {
        val entity1 = entity(meta(100500),
            // no int64
            vBool(2, true),
            // no string
            vOption(4, 653456256L),
            // no numeric
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            // no struct
        )

        val entity2 = entity(meta(200600),
            vInt64(1, 500076L),
            vBool(2, true),
            vString(3, "mooo!"),
            vOption(4, 653456256L),
            vNumeric(5, "16.0054"),
            vReferenceMdmId(6, 1000007563L),
            vTimestamp(8, 1643974484000),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!")))
        )

        val expectedDiff = entity(meta(200600),
            vInt64(1, 500076L),
            vString(3, "mooo!"),
            vNumeric(5, "16.0054"),
            vStruct(9, entity(meta(), vBool(20, false), vString(30, "meoow!")))
        )

        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test combination of added updated and removed attribute provides correct diff`() {
        val entity1 = entity(meta(100500),
            // int64 to be created
            vBool(2, true), // to be updated
            vString(3, "meow") // to be removed
        )
        val entity2 = entity(meta(200600),
            vInt64(1, 111111),
            vBool(2, false)
        )
        val expectedDiff = entity(meta(200600),
            vInt64(1, 111111),
            vBool(2, false),
            removed(3)
        )
        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test inners structs diffed properly layer 3`() {
        val entity111 = entity(meta(), vString(3, "mooo!"))
        val entity11 = entity(meta(), vBool(2, true), vStruct(9, entity111))
        val entity1 = entity(meta(), vInt64(1, 500076L), vStruct(9, entity11))

        val entity211 = entity(meta(), vString(3, "bark!")) // <--
        val entity21 = entity(meta(), vBool(2, true), vStruct(9, entity211))
        val entity2 = entity(meta(), vInt64(1, 500076L), vStruct(9, entity21))

        // Важно: не изменившиеся атрибуты вложенных структур в дифф не попадают
        val expectedDiff = entity(meta(), vStruct(9, entity(meta(), vStruct(9, entity211))))
        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test inners structs diffed properly layer 2 without layer 3 change`() {
        val entity111 = entity(meta(), vString(3, "mooo!"))
        val entity11 = entity(meta(), vBool(2, true), vStruct(9, entity111))
        val entity1 = entity(meta(), vInt64(1, 500076L), vStruct(9, entity11))

        val entity211 = entity(meta(), vString(3, "mooo!"))
        val entity21 = entity(meta(), vBool(2, false), vStruct(9, entity211)) // <--
        val entity2 = entity(meta(), vInt64(1, 500076L), vStruct(9, entity21))

        // Важно: не изменившиеся подструктуры вложенных структур в дифф не попадают
        val expectedDiff = entity(meta(), vStruct(9, entity(meta(), vBool(2, false))))
        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test inners structs diffed properly layer 2 with layer 3 change`() {
        val entity111 = entity(meta(), vString(3, "mooo!"))
        val entity11 = entity(meta(), vBool(2, true), vStruct(9, entity111))
        val entity1 = entity(meta(), vInt64(1, 500076L), vStruct(9, entity11))

        val entity211 = entity(meta(), vString(3, "bark!")) // <--
        val entity21 = entity(meta(), vBool(2, false), vStruct(9, entity211)) // <--
        val entity2 = entity(meta(), vInt64(1, 500076L), vStruct(9, entity21))

        // Важно: изменившиеся подструктуры вложенных структур в дифф попадают
        val expectedDiff = entity(meta(), vStruct(9, entity21))
        diff(entity1, entity2) shouldBe expectedDiff
    }

    @Test
    fun `test simple multivalues diffed by whole lists`() {
        val entity1 = entity(meta(), vInt64s(1, 100L, 200L, 300L, 400L))
        val entity2 = entity(meta(), vInt64s(1, 100L, 300L, 200L))
        diff(entity1, entity2) shouldBe entity2
    }

    @Test
    fun `test simple multivalues in nested structs diffed by whole lists`() {
        val entity1 = entity(meta(), vStruct(2, entity(meta(), vInt64s(1, 100L, 200L, 300L, 400L))))
        val entity2 = entity(meta(), vStruct(2, entity(meta(), vInt64s(1, 100L, 300L, 200L))))
        diff(entity1, entity2) shouldBe entity2
    }

    @Test
    fun `test multiple nested structs diffed element wise`() {
        val entity1 = entity(meta(), vStructs(2,
            entity(meta(), vInt64(1, 100L)),
            entity(meta(), vInt64(1, 200L)),
            entity(meta(), vInt64(1, 300L))
        ))

        val entity2 = entity(meta(), vStructs(2,
            entity(meta(), vInt64(1, 100L)),
            entity(meta(), vInt64(1, 300L))
        ))

        val expectedDiff = entity(meta(), vStructs(2,
            entity(meta(), vInt64(1, 300L)),
            removed()
        ))
        diff(entity1, entity2) shouldBe expectedDiff
    }

    private fun entity(meta: MdmBase.MdmUpdateMeta, vararg values: ProtoAttributeValues): ProtoEntity {
        return ProtoEntity.newBuilder()
            .setMdmUpdateMeta(meta)
            .putAllMdmAttributeValues(values.associateBy{ it.mdmAttributeId })
            .build()
    }

    private fun meta(from: Long = 0L, to: Long = 0L, deleted: Boolean = false): MdmBase.MdmUpdateMeta {
        return MdmBase.MdmUpdateMeta.newBuilder()
            .setFrom(from)
            .setTo(to)
            .setDeleted(deleted)
            .build()
    }

    private fun removed(mdmAttributeId: Long): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmUpdateMeta(meta(deleted = true))
            .setMdmAttributeId(mdmAttributeId)
            .build()
    }

    private fun removed(): ProtoEntity {
        return ProtoEntity.newBuilder()
            .setMdmUpdateMeta(meta(deleted = true))
            .build()
    }

    private fun vInt64(
        mdmAttributeId: Long,
        value: Long,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setInt64(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vInt64s(
        mdmAttributeId: Long,
        vararg values: Long,
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addAllValues(values.map{ ProtoAttributeValue.newBuilder().setInt64(it).build() })
            .build()
    }

    private fun vString(
        mdmAttributeId: Long,
        value: String,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setString(I18nStrings.fromRu(value).toProto()))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vTimestamp(
        mdmAttributeId: Long,
        value: Long,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setTimestamp(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vBool(
        mdmAttributeId: Long,
        value: Boolean,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setBool(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vReferenceMdmId(
        mdmAttributeId: Long,
        value: Long,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setReferenceMdmId(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vOption(
        mdmAttributeId: Long,
        value: Long,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setOption(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vNumeric(
        mdmAttributeId: Long,
        value: String,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setNumeric(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vStruct(
        mdmAttributeId: Long,
        value: ProtoEntity,
        sourceId: String = "",
        sourceType: String = ""
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addValues(ProtoAttributeValue.newBuilder().setStruct(value))
            .setMdmSourceMeta(MdmBase.MdmSourceMeta.newBuilder()
                .setSourceId(sourceId).setSourceType(sourceType))
            .build()
    }

    private fun vStructs(
        mdmAttributeId: Long,
        vararg values: ProtoEntity,
    ): ProtoAttributeValues {
        return ProtoAttributeValues.newBuilder()
            .setMdmAttributeId(mdmAttributeId)
            .addAllValues(values.map{ ProtoAttributeValue.newBuilder().setStruct(it).build() })
            .build()
    }
}
