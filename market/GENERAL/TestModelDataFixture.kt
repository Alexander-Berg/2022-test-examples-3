package ru.yandex.market.mdm.fixtures

import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.common.CommonParamViewSetting
import ru.yandex.market.mdm.lib.model.common.CommonViewType
import ru.yandex.market.mdm.lib.model.common.CommonViewTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType.ENUM
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeExternalReferenceDetails
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.MdmBooleanExternalReferenceDetails
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityKind
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityKind.SERVICE
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem.MBO
import ru.yandex.market.mdm.lib.model.mdm.MdmMetaType
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmRelationType
import ru.yandex.market.mdm.lib.model.mdm.MdmTreeEdge
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import java.time.Instant
import java.time.temporal.ChronoUnit.DAYS
import kotlin.random.Random.Default.nextBytes
import kotlin.random.Random.Default.nextLong

fun mdmEntityType(
    mdmId: Long = randomId(),
    entityKind: MdmEntityKind = SERVICE,
    internalName: String = "entity_type_name",
    description: String = "entity type",
    ruTitle: String = "заголовок типа сущности",
    version: MdmVersion = MdmVersion(),
    attributes: List<MdmAttribute> = listOf()
): MdmEntityType {
    val entityType = MdmEntityType(
        mdmId = mdmId, entityKind = entityKind, internalName = internalName,
        description = description, ruTitle = ruTitle, version = version
    )
    entityType.attributes = attributes.map { it.copy(mdmEntityTypeId = mdmId) }
    return entityType
}

fun mdmRelationType(
    mdmId: Long = randomId(),
    internalName: String = "relation_type_name",
    description: String = "relation type",
    ruTitle: String = "заголовок типа сущности",
    additionalAttributes: List<MdmAttribute> = listOf(),
    fromEntityTypeId: Long = randomId(),
    toEntityTypeId: Long = randomId(),
): MdmRelationType {
    val defaultAttributesWithRandomIds = MdmRelationType.defaultRelationAttributes(
        relationId = mdmId,
        fromEntityTypeId = fromEntityTypeId,
        toEntityTypeId = toEntityTypeId).map { it.copy(mdmId = randomId()) }
    return MdmRelationType(
        mdmId = mdmId,
        internalName = internalName,
        description = description,
        ruTitle = ruTitle,
        attributes = additionalAttributes + defaultAttributesWithRandomIds,
    )
}

fun mdmAttribute(
    mdmId: Long = nextLong(from = 1, until = 10000),
    mdmEntityTypeId: Long = randomId(),
    internalName: String = "attribute_name",
    dataType: MdmAttributeDataType = ENUM,
    ruTitle: String = "заголовок аттрибута",
    options: List<MdmEnumOption> = emptyList(),
    version: MdmVersion = MdmVersion(),
    structMdmEntityTypeId: Long? = null,
    isFilterSupported: Boolean = false,
): MdmAttribute {
    val attribute = MdmAttribute(
        mdmId = mdmId, mdmEntityTypeId = mdmEntityTypeId, internalName = internalName,
        dataType = dataType, ruTitle = ruTitle, version = version, structMdmEntityTypeId = structMdmEntityTypeId,
        options = options.map { it.copy(mdmAttributeId = mdmId) }
    )
    return attribute
}

fun commonViewType(
    mdmId: Long = randomId(),
    commonEntityTypeEnum: CommonEntityTypeEnum = CommonEntityTypeEnum.MDM_ATTR,
    viewType: CommonViewTypeEnum = CommonViewTypeEnum.EXCEL_IMPORT,
    internalName: String = "common view type name",
    ruTitle: String = "заголовок common view type",
    version: MdmVersion = MdmVersion(),
) = CommonViewType(
    mdmId = mdmId, commonEntityTypeEnum = commonEntityTypeEnum,
    viewType = viewType, internalName = internalName, ruTitle = ruTitle, version = version,
)

fun commonParamViewSetting(
    mdmId: Long = randomId(),
    isEnabled: Boolean = true,
    commonViewTypeId: Long = randomId(),
    commonParamId: Long = randomId(),
    mdmPath: MdmPath = MdmPath.fromLongs(listOf(randomId(), randomId()), MdmMetaType.MDM_ATTR),
    version: MdmVersion = MdmVersion(),
) = CommonParamViewSetting(
    mdmId = mdmId, commonViewTypeId = commonViewTypeId, isEnabled = isEnabled,
    commonParamId = commonParamId, version = version, mdmPath = mdmPath,
)

fun mdmEnumOption(
    mdmId: Long = randomId(),
    mdmAttributeId: Long = randomId(),
    value: String = "enum option",
): MdmEnumOption {
    return MdmEnumOption(
        mdmId = mdmId, mdmAttributeId = mdmAttributeId, value = value
    )
}

fun mdmExternalReferenceForAttribute(
    mdmId: Long = randomId(),
    mdmPath: MdmPath = MdmPath.fromLongs(listOf(randomId(), randomId()), MdmMetaType.MDM_ATTR),
    externalSystem: MdmExternalSystem = MBO,
    externalId: Long = randomId(),
    externalName: String = "external name",
    externalRuTitle: String = "Внешнее название",
) = MdmExternalReference(
    mdmId = mdmId, mdmPath = mdmPath, externalSystem = externalSystem, externalId = externalId,
    mdmAttributeExternalReferenceDetails = MdmAttributeExternalReferenceDetails(
        externalName = externalName,
        externalRuTitle = externalRuTitle,
    )
)

fun mdmExternalReferenceForBoolean(
    mdmId: Long = randomId(),
    mdmPath: MdmPath = MdmPath.fromLongs(listOf(randomId(), randomId()), MdmMetaType.MDM_BOOL),
    externalSystem: MdmExternalSystem = MBO,
    externalId: Long = randomId(),
    boolValue: Boolean = true,
) = MdmExternalReference(
    mdmId = mdmId, mdmPath = mdmPath, externalSystem = externalSystem, externalId = externalId,
    mdmBooleanExternalReferenceDetails = MdmBooleanExternalReferenceDetails(
        boolValue = boolValue,
    )
)

fun mdmExternalReferenceForEnumOption(
    mdmId: Long = randomId(),
    mdmPath: MdmPath = MdmPath.fromLongs(listOf(randomId(), randomId(), randomId()), MdmMetaType.MDM_ENUM_OPTION),
    externalSystem: MdmExternalSystem = MBO,
    externalId: Long = randomId()
) = MdmExternalReference(
    mdmId = mdmId, mdmPath = mdmPath, externalSystem = externalSystem, externalId = externalId,
)

fun mdmTreeEdge(
    childId: Long = randomId(),
    parentId: Long = randomId(),
    treeId: Long = randomId(),
) = MdmTreeEdge(childId = childId, parentId = parentId, treeId = treeId)

fun randomId() = nextLong(from = 1, until = 100000000)
fun randomBytes() = nextBytes(16)

fun mdmVersionRetiredNDaysAgo(numberOfDays: Long) = MdmVersion.fromVersions(
    nDaysAgoMoment(500),
    nDaysAgoMoment(numberOfDays)
)

fun mdmVersionThatIsActualForever() = MdmVersion.fromVersions(nDaysAgoMoment(500), null)
fun nDaysAgoMoment(numberOfDays: Long) = Instant.now().minus(numberOfDays, DAYS)
fun nDaysAfterMoment(numberOfDays: Long) = Instant.now().plus(numberOfDays, DAYS)

fun mdmEntity(
    mdmId: Long = randomId(),
    mdmEntityTypeId: Long = randomId(),
    values: List<MdmAttributeValues> = listOf(),
    version: MdmVersion = MdmVersion()
) = MdmEntity(mdmId, mdmEntityTypeId, values.associateBy { it.mdmAttributeId }, version)
