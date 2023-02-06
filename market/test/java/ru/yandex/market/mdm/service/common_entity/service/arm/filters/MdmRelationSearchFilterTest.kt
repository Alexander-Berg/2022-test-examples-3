package ru.yandex.market.mdm.service.common_entity.service.arm.filters

import io.kotest.matchers.shouldBe
import org.junit.Test
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.MdmProtocol
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.model.StructuredMdmMetadata

class MdmRelationSearchFilterTest {

    @Test
    fun `should create mdmRelationFilter from CommonEntity for FROM side`() {
        // given
        val fromEntityTypeId = 15L
        val fromEntityId = 16L
        val relationType = mdmRelationType(fromEntityTypeId = fromEntityTypeId)
        val metadata = StructuredMdmMetadata(relationType, listOf(relationType), listOf())

        val findRelationRequest = CommonEntity(
            commonEntityType = CommonEntityType(CommonEntityTypeEnum.MDM_RELATION),
            commonParamValues = listOf(
                MdmProtocol.RELATION_TYPE_ID.byLong(relationType.mdmId),
                MdmProtocol.RELATED_ENTITY_ID.byLong(fromEntityId),
                MdmProtocol.RELATED_ENTITY_TYPE_ID.byLong(fromEntityTypeId)
            )
        )

        // when
        val mdmRelationSearchFilter = MdmRelationSearchFilter(findRelationRequest, metadata)

        // then
        mdmRelationSearchFilter.mdmFromRelatedEntityId!!.mdmId shouldBe fromEntityId
        mdmRelationSearchFilter.mdmFromRelatedEntityId!!.mdmEntityTypeId shouldBe fromEntityTypeId
    }

    @Test
    fun `should create mdmRelationFilter from CommonEntity for TO side`() {
        // given
        val toEntityTypeId = 15L
        val toEntityId = 16L
        val relationType = mdmRelationType(toEntityTypeId = toEntityTypeId)
        val metadata = StructuredMdmMetadata(relationType, listOf(relationType), listOf())

        val findRelationRequest = CommonEntity(
            commonEntityType = CommonEntityType(CommonEntityTypeEnum.MDM_RELATION),
            commonParamValues = listOf(
                MdmProtocol.RELATION_TYPE_ID.byLong(relationType.mdmId),
                MdmProtocol.RELATED_ENTITY_ID.byLong(toEntityId),
                MdmProtocol.RELATED_ENTITY_TYPE_ID.byLong(toEntityTypeId)
            )
        )

        // when
        val mdmRelationSearchFilter = MdmRelationSearchFilter(findRelationRequest, metadata)

        // then
        mdmRelationSearchFilter.mdmToRelatedEntityId!!.mdmId shouldBe toEntityId
        mdmRelationSearchFilter.mdmToRelatedEntityId!!.mdmEntityTypeId shouldBe toEntityTypeId
    }
}
