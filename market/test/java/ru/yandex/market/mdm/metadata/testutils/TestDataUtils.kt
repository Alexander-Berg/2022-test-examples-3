package ru.yandex.market.mdm.metadata.testutils

import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEnumOptionRepository

object TestDataUtils {

    fun generatedData(
        mdmEntityTypeRepository: MdmEntityTypeRepository,
        mdmAttributeRepository: MdmAttributeRepository,
        mdmEnumOptionRepository: MdmEnumOptionRepository
    ): List<MdmEntityType> {
        val entityType1 = mdmEntityTypeRepository.insert(
            MdmEntityType(internalName = "entity_type_1", description = "description1", ruTitle = "entity_title1")
        )
        val entityType2 = mdmEntityTypeRepository.insert(
            MdmEntityType(internalName = "entity_type_2", description = "description2", ruTitle = "entity_title2")
        )
        val attr1 = mdmAttributeRepository.insert(
            MdmAttribute(
                mdmEntityTypeId = entityType1.mdmId, internalName = "attr_1",
                dataType = MdmAttributeDataType.STRING, ruTitle = "attr_title1"
            )
        )
        val attr2 = mdmAttributeRepository.insert(
            MdmAttribute(
                mdmEntityTypeId = entityType1.mdmId, internalName = "attr_2",
                dataType = MdmAttributeDataType.BOOLEAN, ruTitle = "attr_title2"
            )
        )
        val attr3 = mdmAttributeRepository.insert(
            MdmAttribute(
                mdmEntityTypeId = entityType2.mdmId, internalName = "attr_3",
                dataType = MdmAttributeDataType.ENUM, ruTitle = "attr_title3"
            )
        )
        val enumOption1 = mdmEnumOptionRepository.insert(MdmEnumOption(mdmAttributeId = attr1.mdmId, value = "option1"))
        val enumOption2 = mdmEnumOptionRepository.insert(MdmEnumOption(mdmAttributeId = attr2.mdmId, value = "option2"))

        entityType1.attributes =
            listOf(attr1.copy(options = listOf(enumOption1)), attr2.copy(options = listOf(enumOption2)))
        entityType2.attributes = listOf(attr3)

        return listOf(entityType1, entityType2)
    }

    fun entityTypeWithOneStruct(
        innerSimpleAttributes: List<MdmAttribute> = listOf(),
        outerSimpleAttributes: List<MdmAttribute> = listOf(),
    ) : EntityTypeWithOneStruct {
        val innerMdmEntityType =
            mdmEntityType(attributes = innerSimpleAttributes)

        val outerStructMdmAttribute =
            mdmAttribute(
                dataType = MdmAttributeDataType.STRUCT,
                structMdmEntityTypeId = innerMdmEntityType.mdmId,
            )
        val outerMdmEntityType = mdmEntityType(attributes = outerSimpleAttributes + outerStructMdmAttribute)
        return EntityTypeWithOneStruct(outerMdmEntityType, innerMdmEntityType, outerStructMdmAttribute)
    }

    data class EntityTypeWithOneStruct(
        val outerMdmEntityType : MdmEntityType,
        val innerMdmEntityType: MdmEntityType,
        val structAttribute: MdmAttribute
    )

}
