package ru.yandex.market.mdm.metadata.model

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath

class StructuredMdmEntityTest {

    @Test
    fun `should return all attributes with paths for structured entity type`() {
        // given
        val innerSimpleMdmAttribute = mdmAttribute()
        val outerSimpleMdmAttribute = mdmAttribute()
        val innerMdmEntityType =
            mdmEntityType(attributes = listOf(innerSimpleMdmAttribute))

        val outerStructMdmAttribute =
            mdmAttribute(
                dataType = MdmAttributeDataType.STRUCT,
                structMdmEntityTypeId = innerMdmEntityType.mdmId,
            )
        val outerMdmEntityType = mdmEntityType(attributes = listOf(outerSimpleMdmAttribute, outerStructMdmAttribute))
        val structuredMdmEntityType = StructuredMdmEntityType(
            baseEntityType = outerMdmEntityType,
            relatedEntityTypes = listOf(outerMdmEntityType, innerMdmEntityType).associateBy { it.mdmId },
            startingPath = listOf()
        )

        // when
        val attributes = structuredMdmEntityType.getAllPathedAttribute()

        // then
        attributes.size shouldBe 3
        attributes.map { it.getPath() } shouldContainExactlyInAnyOrder listOf(
            MdmPath.fromLongs(listOf(outerMdmEntityType.mdmId, outerStructMdmAttribute.mdmId, innerMdmEntityType.mdmId, innerSimpleMdmAttribute.mdmId), MdmMetaType.MDM_ATTR),
            MdmPath.fromLongs(listOf(outerMdmEntityType.mdmId, outerStructMdmAttribute.mdmId), MdmMetaType.MDM_ATTR),
            MdmPath.fromLongs(listOf(outerMdmEntityType.mdmId, outerSimpleMdmAttribute.mdmId), MdmMetaType.MDM_ATTR),
        )
    }
}
