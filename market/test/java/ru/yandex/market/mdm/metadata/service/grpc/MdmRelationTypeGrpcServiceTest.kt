package ru.yandex.market.mdm.metadata.service.grpc

import io.grpc.testing.GrpcCleanupRule
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmRelationType
import ru.yandex.market.mdm.http.relations.MdmRelationTypeServiceGrpc
import ru.yandex.market.mdm.http.relations.MdmRelationTypesByFilterRequest
import ru.yandex.market.mdm.http.relations.MdmRelationTypesByFilterResponse
import ru.yandex.market.mdm.http.search.MdmMetadataIds
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel

class MdmRelationTypeGrpcServiceTest : BaseAppTestClass() {
    @Autowired
    lateinit var mdmRelationTypeGrpcService: MdmRelationTypeGrpcService

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Rule
    @JvmField
    val grpcCleanupRule = GrpcCleanupRule()

    lateinit var mdmRelationClient: MdmRelationTypeServiceGrpc.MdmRelationTypeServiceBlockingStub

    @Before
    fun initClient() {
        mdmRelationClient =
            MdmRelationTypeServiceGrpc.newBlockingStub(
                createTestManagedChannel(
                    grpcCleanupRule,
                    mdmRelationTypeGrpcService
                )
            )
    }

    @Test
    fun `should return all relation types without filter`() {
        // given
        val mdmRelationType = mdmRelationType()
        mdmEntityTypeRepository.insertOrUpdate(mdmRelationType)
        mdmAttributeRepository.insertOrUpdateBatch(
            mdmRelationType.attributes
        )

        val filter = ru.yandex.market.mdm.http.relations.MdmRelationTypeSearchFilter.newBuilder()
            .build()
        val request = MdmRelationTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmRelationClient.getRelationTypeBySearchFilter(request)

        // then
        response shouldBe MdmRelationTypesByFilterResponse.newBuilder()
            .addAllMdmRelationTypes(listOf(mdmRelationType.toProto()))
            .build()
    }

    @Test
    fun `should return relation types by ids`() {
        // given
        val mdmRelationType = mdmRelationType()
        val extraMdmRelationType = mdmRelationType()

        mdmEntityTypeRepository.insertOrUpdateBatch(listOf(mdmRelationType, extraMdmRelationType))
        mdmAttributeRepository.insertOrUpdateBatch(
            mdmRelationType.attributes + extraMdmRelationType.attributes
        )

        val filter = ru.yandex.market.mdm.http.relations.MdmRelationTypeSearchFilter.newBuilder()
            .setByIds(MdmMetadataIds.newBuilder().addMdmIds(mdmRelationType.mdmId))
            .build()
        val request = MdmRelationTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmRelationClient.getRelationTypeBySearchFilter(request)

        // then
        response shouldBe MdmRelationTypesByFilterResponse.newBuilder()
            .addAllMdmRelationTypes(listOf(mdmRelationType.toProto()))
            .build()
    }

    @Test
    fun `should return relation types by related_entity_type_ids`() {
        // given
        val mdmRelationType = mdmRelationType()
        mdmEntityTypeRepository.insertOrUpdate(mdmRelationType)
        mdmAttributeRepository.insertOrUpdateBatch(
            mdmRelationType.attributes
        )

        val filter = ru.yandex.market.mdm.http.relations.MdmRelationTypeSearchFilter.newBuilder()
            .setByRelatedIds(MdmMetadataIds.newBuilder().addMdmIds(mdmRelationType.fromEntityTypeId()))
            .build()
        val request = MdmRelationTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmRelationClient.getRelationTypeBySearchFilter(request)

        // then
        response shouldBe MdmRelationTypesByFilterResponse.newBuilder()
            .addAllMdmRelationTypes(listOf(mdmRelationType.toProto()))
            .build()
    }
}
