package ru.yandex.market.mdm.service.common_entity.service.arm.storage.relation

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.given
import io.kotest.matchers.collections.shouldContain
import org.junit.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.http.MdmAttributeValue
import ru.yandex.market.mdm.http.entity.GetMdmEntityResponse
import ru.yandex.market.mdm.http.entity.MdmEntityStorageServiceGrpc
import ru.yandex.market.mdm.lib.model.mdm.MdmEntity
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityId
import ru.yandex.market.mdm.lib.model.mdm.MdmRelation
import ru.yandex.market.mdm.lib.model.mdm.ProtoAttributeValues
import ru.yandex.market.mdm.lib.model.mdm.ProtoEntity
import ru.yandex.market.mdm.service.common_entity.service.arm.filters.MdmRelationSearchFilter
import ru.yandex.market.mdm.service.common_entity.service.arm.metadata.MetadataFactory
import ru.yandex.market.mdm.service.common_entity.service.arm.structuredMdmMetadataWithOneStructAttribute

class MdmStorageRelationStorageServiceTest {

    private val entityStorageServiceBlockingStub: MdmEntityStorageServiceGrpc.MdmEntityStorageServiceBlockingStub =
        mock(MdmEntityStorageServiceGrpc.MdmEntityStorageServiceBlockingStub::class.java)
    private val metadataFactory: MetadataFactory = mock(MetadataFactory::class.java)
    private val service: MdmStorageRelationStorageService =
        MdmStorageRelationStorageService(entityStorageServiceBlockingStub, metadataFactory)

    @Test
    fun `should search Relation by id`() {
        // given
        val filter = MdmRelationSearchFilter(ids = listOf(555), mdmRelationTypeId = 112)
        val returnedEntity = ProtoEntity.newBuilder()
            .setMdmEntityTypeId(112)
            .setMdmId(555)
            .putMdmAttributeValues(
                11111, ProtoAttributeValues.newBuilder()
                    .setMdmAttributeId(11111)
                    .addValues(MdmAttributeValue.newBuilder().setReferenceMdmId(777))
                    .build()
            )
            .build()
        given(entityStorageServiceBlockingStub.getByMdmIds(any())).willReturn(
            GetMdmEntityResponse.newBuilder()
                .addMdmEntities(returnedEntity)
                .build()
        )

        // when
        val result = service.findByFilter(filter)

        // then
        result shouldContain MdmRelation(MdmEntity.fromProto(returnedEntity))
    }

    @Test
    fun `should search Relation by related id`() {
        // given
        val relationType = mdmRelationType()
        val filter = MdmRelationSearchFilter(mdmRelationTypeId = relationType.mdmId, mdmFromRelatedEntityId = MdmEntityId(1, 11))
        val returnedEntity = ProtoEntity.newBuilder()
            .setMdmEntityTypeId(112)
            .setMdmId(555)
            .putMdmAttributeValues(
                11111, ProtoAttributeValues.newBuilder()
                    .setMdmAttributeId(11111)
                    .addValues(MdmAttributeValue.newBuilder().setReferenceMdmId(777))
                    .build()
            )
            .build()
        given(entityStorageServiceBlockingStub.getByExternalKeys(any())).willReturn(
            GetMdmEntityResponse.newBuilder()
                .addMdmEntities(returnedEntity)
                .build()
        )
        given(metadataFactory.create(anyOrNull(), anyOrNull(), anyOrNull())).willReturn(
            structuredMdmMetadataWithOneStructAttribute(baseEntityType = relationType)
        )

        // when
        val result = service.findByFilter(filter)

        // then
        result shouldContain MdmRelation(MdmEntity.fromProto(returnedEntity))
    }
}
