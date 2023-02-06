package ru.yandex.market.mdm.metadata.service.grpc

import io.grpc.testing.GrpcCleanupRule
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypeSearchFilter
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypeServiceGrpc
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypesByFilterRequest
import ru.yandex.market.mdm.http.entity_types.MdmEntityTypesList
import ru.yandex.market.mdm.http.entity_types.UpdateMdmEntityTypesRequest
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion.Companion.fromVersions
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEventRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel
import java.time.Instant

class MdmEntityTypeGrpcServiceTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmEntityTypeGrpcService: MdmEntityTypeGrpcService

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var mdmEventRepository: MdmEventRepository

    @Rule
    @JvmField
    val grpcCleanupRule = GrpcCleanupRule()

    lateinit var mdmEntityClient: MdmEntityTypeServiceGrpc.MdmEntityTypeServiceBlockingStub

    @Before
    fun initClient() {
        mdmEntityClient =
            MdmEntityTypeServiceGrpc.newBlockingStub(createTestManagedChannel(grpcCleanupRule, mdmEntityTypeGrpcService))
    }

    @Test
    fun `should return all entity types by filter`() {
        // given
        val mdmEntity = mdmEntityTypeRepository.insert(mdmEntityType())
        val filter = MdmEntityTypeSearchFilter.newBuilder()
            .addMdmId(mdmEntity.mdmId)
            .build()
        val request = MdmEntityTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmEntityClient.getEntityTypesByFilter(request)

        // then
        response.mdmEntityTypes shouldBe MdmEntityTypesList.newBuilder()
            .addAllMdmEntityTypes(listOf(mdmEntity.toProto()))
            .build()
    }

    @Test
    fun `should return all entity types`() {
        // given
        mdmEntityTypeRepository.insert(mdmEntityType())

        // when
        val response = mdmEntityClient.getAllActiveEntityTypesOnly(MdmBase.MdmEmptyRequest.getDefaultInstance())

        // then
        response.mdmEntityTypes.mdmEntityTypesCount shouldBe mdmEntityTypeRepository.findAllActive().count()
    }

    @Test
    fun `should return all full entity types with attributes`() {
        // given
        val mdmEntity = mdmEntityTypeRepository.insert(mdmEntityType())
        val mdmAttribute = mdmAttributeRepository.insertOrUpdate(
            mdmAttribute(
                mdmEntityTypeId = mdmEntity.mdmId,
            )
        )
        mdmEntity.attributes = listOf(mdmAttribute)

        val filter = MdmEntityTypeSearchFilter.newBuilder()
            .addMdmId(mdmEntity.mdmId)
            .setIncludeRelatedData(true)
            .build()
        val request = MdmEntityTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmEntityClient.getEntityTypesByFilter(request)

        // then
        assertSoftly {
            response.mdmEntityTypes.mdmEntityTypesCount shouldBe 1
            response.mdmEntityTypes.mdmEntityTypesList shouldContain mdmEntity.toProto()
            response.mdmEntityTypes.mdmEntityTypesList[0].attributesList shouldContain mdmAttribute.toProto()
        }
    }

    @Test
    fun `should return all full shallow entity types with attributes`() {
        // given
        val mdmEntity = mdmEntityTypeRepository.insert(mdmEntityType())
        val innerMdmEntity = mdmEntityTypeRepository.insert(mdmEntityType())
        val mdmAttribute = mdmAttributeRepository.insertOrUpdate(
            mdmAttribute(
                mdmEntityTypeId = mdmEntity.mdmId,
                structMdmEntityTypeId = innerMdmEntity.mdmId
            )
        )
        mdmEntity.attributes = listOf(mdmAttribute)


        val filter = MdmEntityTypeSearchFilter.newBuilder()
            .addMdmId(mdmEntity.mdmId)
            .setIncludeRelatedData(true)
            .setShallow(true)
            .build()
        val request = MdmEntityTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmEntityClient.getEntityTypesByFilter(request)

        // then
        assertSoftly {
            response.mdmEntityTypes.mdmEntityTypesCount shouldBe 1
            response.mdmEntityTypes.mdmEntityTypesList shouldContain mdmEntity.toProto()
            response.mdmEntityTypes.mdmEntityTypesList[0].attributesList shouldContain mdmAttribute.toProto()
        }
    }

    @Test
    fun `should return all full entity types with attributes and all inner structs`() {
        // given
        val mdmEntity = mdmEntityTypeRepository.insert(mdmEntityType())
        val innerMdmEntity = mdmEntityTypeRepository.insert(mdmEntityType())
        val mdmAttribute = mdmAttributeRepository.insertOrUpdate(
            mdmAttribute(
                mdmEntityTypeId = mdmEntity.mdmId,
                structMdmEntityTypeId = innerMdmEntity.mdmId
            )
        )
        val innerMdmAttribute = mdmAttributeRepository.insertOrUpdate(
            mdmAttribute(mdmEntityTypeId = innerMdmEntity.mdmId,)
        )
        mdmEntity.attributes = listOf(mdmAttribute)
        innerMdmEntity.attributes = listOf(innerMdmAttribute)

        val filter = MdmEntityTypeSearchFilter.newBuilder()
            .addMdmId(mdmEntity.mdmId)
            .setIncludeRelatedData(true)
            .setShallow(false)
            .build()
        val request = MdmEntityTypesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmEntityClient.getEntityTypesByFilter(request)

        // then
        assertSoftly {
            response.mdmEntityTypes.mdmEntityTypesCount shouldBe 2
            response.mdmEntityTypes.mdmEntityTypesList shouldContain mdmEntity.toProto()
            response.mdmEntityTypes.mdmEntityTypesList shouldContain innerMdmEntity.toProto()
        }
    }

    @Test
    fun `should update entity type`() {
        // given
        val initialEntityType = mdmEntityTypeRepository.insert(mdmEntityType())
        val updatedEntityType = initialEntityType.toBuilder()
            .ruTitle("new Title")
            .version(fromVersions(Instant.now(), null))
            .build()

        val request = UpdateMdmEntityTypesRequest.newBuilder()
            .setUpdates(MdmEntityTypesList.newBuilder().addMdmEntityTypes(updatedEntityType.toProto()).build())
            .setContext(MdmBase.MdmUpdateContext.newBuilder().setCommitMessage("updated by me").build())
            .build()

        // when
        val response = mdmEntityClient.updateEntityTypesOnly(request)

        // then
        assertSoftly {
            response.results.mdmEntityTypesCount shouldBe 1
            response.results.mdmEntityTypesList[0].ruTitle shouldBe "new Title"
            response.errorsCount shouldBe 0
        }

        val saved = mdmEntityTypeRepository.findLatestById(response.results.mdmEntityTypesList[0].mdmId)

        // and
        val auditEvent = mdmEventRepository.findById(saved!!.version.eventId)
        auditEvent.commitMessage shouldBe "updated by me"
    }

    @Test
    fun `should save audit event on create`() {
        // given
        val mdmEntityType = mdmEntityType()

        val toCreate = MdmEntityTypesList.newBuilder()
            .addMdmEntityTypes(mdmEntityType.toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateMdmEntityTypesRequest.newBuilder()
            .setUpdates(toCreate)
            .setContext(context).build()

        // when
        val response = mdmEntityClient.updateEntityTypesOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.mdmEntityTypesCount shouldBe 1

            val created = response.results.mdmEntityTypesList[0]

            val auditEventId = created.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmEntities = mdmEntityTypeRepository.findLatestById(created.mdmId)
            mdmEntities shouldNotBe null
            mdmEntities!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on update`() {
        // given
        val mdmEntityType = mdmEntityTypeRepository.insert(mdmEntityType())

        val toUpdate = MdmEntityTypesList.newBuilder()
            .addMdmEntityTypes(mdmEntityType.toBuilder()
                .internalName("updated")
                .build().toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateMdmEntityTypesRequest.newBuilder()
            .setUpdates(toUpdate)
            .setContext(context).build()

        // when
        val response = mdmEntityClient.updateEntityTypesOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.mdmEntityTypesCount shouldBe 1

            val updated = response.results.mdmEntityTypesList[0]

            val auditEventId = updated.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmEntities = mdmEntityTypeRepository.findLatestById(updated.mdmId)
            mdmEntities shouldNotBe null
            mdmEntities!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on delete`() {
        // given
        val mdmEntityType = mdmEntityTypeRepository.insert(mdmEntityType())

        val toDelete = MdmEntityTypesList.newBuilder()
            .addMdmEntityTypes(
                mdmEntityType.toBuilder()
                    .internalName("deleted")
                    .version(MdmVersion(to = Instant.now()))
                    .build().toProto())
            .build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateMdmEntityTypesRequest.newBuilder()
            .setUpdates(toDelete)
            .setContext(context).build()

        // when
        val response = mdmEntityClient.updateEntityTypesOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.mdmEntityTypesCount shouldBe 1

            val deleted = response.results.mdmEntityTypesList[0]
            val auditEventId = deleted.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmEntities = mdmEntityTypeRepository.findAll().sortedBy { it.version.from }
            mdmEntities.last().version.eventId shouldBe auditEventId
        }
    }
}
