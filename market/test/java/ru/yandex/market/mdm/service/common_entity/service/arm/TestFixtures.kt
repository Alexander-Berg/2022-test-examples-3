package ru.yandex.market.mdm.service.common_entity.service.arm

import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.fixtures.randomId
import ru.yandex.market.mdm.lib.model.common.CommonParamViewSetting
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.FlatMdmMetadata
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.StructuredMdmMetadata

/**
 * Метадата для entityType c произвольным набором простых атрибутов и одной структурой
 */
fun flatMdmMetadataWithOneStructAttribute(
    baseEntityType: MdmEntityType = mdmEntityType(),
    structEntityType: MdmEntityType = mdmEntityType(),
    structAttributeName: String = "struct",
    structAttributeId: Long = randomId(),
    settings: List<CommonParamViewSetting> = listOf(),
): FlatMdmMetadata {
    val mdmAttributeStruct = mdmAttribute(
        mdmId = structAttributeId,
        internalName = structAttributeName,
        mdmEntityTypeId = baseEntityType.mdmId,
        dataType = MdmAttributeDataType.STRUCT,
        structMdmEntityTypeId = structEntityType.mdmId,
    )
    baseEntityType.attributes += mdmAttributeStruct
    return FlatMdmMetadata(
        baseEntityType,
        listOf(baseEntityType, structEntityType),
        settings,
    )
}

fun structuredMdmMetadataWithOneStructAttribute(
    baseEntityType: MdmEntityType = mdmEntityType(),
    structEntityType: MdmEntityType = mdmEntityType(),
    structAttributeName: String = "struct",
    structAttributeId: Long = randomId(),
    settings: List<CommonParamViewSetting> = listOf(),
): StructuredMdmMetadata {
    val mdmAttributeStruct = mdmAttribute(
        mdmId = structAttributeId,
        internalName = structAttributeName,
        mdmEntityTypeId = baseEntityType.mdmId,
        dataType = MdmAttributeDataType.STRUCT,
        structMdmEntityTypeId = structEntityType.mdmId,
    )
    baseEntityType.attributes += mdmAttributeStruct
    return StructuredMdmMetadata(
        baseEntityType,
        listOf(baseEntityType, structEntityType),
        settings,
    )
}
