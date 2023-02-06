package ru.yandex.market.mdm.lib.converters

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType

class MdmEntityTypeProtoConverterTest {
    @Test
    fun `should convert entity type to proto`() {
        // given
        val mdmAttribute = mdmAttribute()
        val entityType = mdmEntityType()
        entityType.attributes = listOf(mdmAttribute)

        // when
        val proto = entityType.toProto()

        // then
        assertSoftly {
            proto.mdmId shouldBe entityType.mdmId
            proto.mdmEntityKind.name shouldBe entityType.entityKind.name
            proto.internalName shouldBe entityType.internalName
            proto.description shouldBe entityType.description
            proto.ruTitle shouldBe entityType.ruTitle
            proto.mdmUpdateMeta shouldBe entityType.version.toProto()
            proto.attributesList shouldContain mdmAttribute.toProto()
        }
    }

    @Test
    fun `should map pojo to light proto and back`() {
        // given
        val mdmAttribute = mdmAttribute()
        val mdmEntityType = mdmEntityType()
        mdmEntityType.attributes = listOf(mdmAttribute)

        // when
        val proto = mdmEntityType.toProto()
        val pojo = proto.toPojo()

        // then
        pojo shouldBe mdmEntityType
    }
}
