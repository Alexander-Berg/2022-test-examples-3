package ru.yandex.market.mdm.lib.converters

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.http.MdmBase

class MdmRelationTypeProtoConverterTest {
    @Test
    fun `should convert relation type to proto`() {
        // given
        val additionalAttribute = mdmAttribute()
        val relationType = mdmRelationType(additionalAttributes = listOf(additionalAttribute))

        // when
        val proto = relationType.toProto()

        // then
        assertSoftly {
            proto.baseEntityType.mdmId shouldBe relationType.mdmId
            proto.baseEntityType.mdmEntityKind shouldBe MdmBase.MdmEntityType.EntityKind.RELATION
            proto.baseEntityType.internalName shouldBe relationType.internalName
            proto.baseEntityType.description shouldBe relationType.description
            proto.baseEntityType.ruTitle shouldBe relationType.ruTitle
            proto.baseEntityType.mdmUpdateMeta shouldBe relationType.version.toProto()
            proto.baseEntityType.attributesList shouldContainExactlyInAnyOrder
                relationType.attributes.map { it.toProto() }
        }
    }

    @Test
    fun `should map pojo to proto and back`() {
        // given
        val additionalAttribute = mdmAttribute()
        val relationType = mdmRelationType(additionalAttributes = listOf(additionalAttribute))

        // when
        val proto = relationType.toProto()
        val pojo = proto.toPojo()

        // then
        pojo shouldBe relationType
    }
}
