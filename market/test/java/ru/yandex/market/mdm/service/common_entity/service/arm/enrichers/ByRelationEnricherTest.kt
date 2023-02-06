package ru.yandex.market.mdm.service.common_entity.service.arm.enrichers

import com.nhaarman.mockitokotlin2.any
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.fixtures.mdmRelationWithOnlyDefaultAttributes
import ru.yandex.market.mdm.lib.model.common.CommonEntityTypeEnum
import ru.yandex.market.mdm.lib.model.mdm.MdmRelationType
import ru.yandex.market.mdm.service.common_entity.model.CommonEntity
import ru.yandex.market.mdm.service.common_entity.model.CommonEntityType
import ru.yandex.market.mdm.service.common_entity.model.CommonParam
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValue
import ru.yandex.market.mdm.service.common_entity.model.CommonParamValueType
import ru.yandex.market.mdm.service.common_entity.model.MdmProtocol
import ru.yandex.market.mdm.service.common_entity.service.arm.storage.relation.RelationStorageService
import ru.yandex.market.mdm.service.common_entity.service.common.metadata.MetadataService
import ru.yandex.market.mdm.service.common_entity.service.constructor.filters.MdmRelationTypeSearchFilter

class ByRelationEnricherTest {

    private val relationTypeMetadataService =
        mock(MetadataService::class.java) as MetadataService<MdmRelationType, MdmRelationTypeSearchFilter>

    private val relationStorageService = mock(RelationStorageService::class.java)

    private val service: ByRelationEnricher = ByRelationEnricher(relationTypeMetadataService, relationStorageService)

    @Test
    fun `when relation types presents on entity should add info about them`() {
        // given
        val existedEntity = commonEntity(123L, 1L)
        val existedRelationType = mdmRelationType(toEntityTypeId = 1L)
        given(relationTypeMetadataService.findByFilter(any())).willReturn(listOf(existedRelationType))
        val existedRelation = mdmRelationWithOnlyDefaultAttributes(toEntityTypeId = 1L).mdmRelation
        given(relationStorageService.findByFilter(any())).willReturn(listOf(existedRelation))

        // when
        val enrichedEntity = service.enrich(existedEntity)

        // then
        enrichedEntity.commonParamValues!!.size shouldBe 3
        enrichedEntity.commonEntityType.commonParams!!.map { it.commonParamName } shouldContain existedRelationType.internalName

        // and
        enrichedEntity.getLongValue(existedRelationType.internalName)!! shouldBe 1
    }

    private fun commonEntity(id: Long, entityTypeId: Long) = CommonEntity(
        id,
        CommonEntityType(
            CommonEntityTypeEnum.MDM_ENTITY,
            commonParams = listOf(
                MdmProtocol.REQUEST_ENTITY_TYPE_ID.param,
                CommonParam(
                    commonParamName = "some_attr",
                    commonParamValueType = CommonParamValueType.NUMERIC
                )
            )
        ),
        listOf(
            CommonParamValue.byLong(
                commonParamName = MdmProtocol.REQUEST_ENTITY_TYPE_ID.paramName(),
                value = entityTypeId,
            ),
            CommonParamValue.byLong(
                commonParamName = "some_attr",
                value = 12
            )
        )
    )
}
