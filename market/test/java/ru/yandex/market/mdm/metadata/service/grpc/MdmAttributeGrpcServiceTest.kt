package ru.yandex.market.mdm.metadata.service.grpc

import io.grpc.testing.GrpcCleanupRule
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.fixtures.mdmAttribute
import ru.yandex.market.mdm.fixtures.mdmEntityType
import ru.yandex.market.mdm.fixtures.mdmVersionRetiredNDaysAgo
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.MdmBase.MdmEmptyRequest
import ru.yandex.market.mdm.http.attributes.MdmAttributeList
import ru.yandex.market.mdm.http.attributes.MdmAttributeSearchFilter
import ru.yandex.market.mdm.http.attributes.MdmAttributeServiceGrpc
import ru.yandex.market.mdm.http.attributes.MdmAttributesByFilterRequest
import ru.yandex.market.mdm.http.attributes.MdmUpdateAttributesRequest
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEventRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel
import java.time.Instant
import java.time.Instant.now
import kotlin.random.Random

class MdmAttributeGrpcServiceTest : BaseAppTestClass() {

    private val random = Random(1234)

    @Autowired
    lateinit var mdmAttributeGrpcService: MdmAttributeGrpcService

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Autowired
    lateinit var mdmEventRepository: MdmEventRepository

    @Rule
    @JvmField
    final val grpcCleanupRule = GrpcCleanupRule()

    lateinit var mdmAttributeClient: MdmAttributeServiceGrpc.MdmAttributeServiceBlockingStub

    @Before
    fun initClient() {
        mdmAttributeClient =
            MdmAttributeServiceGrpc.newBlockingStub(createTestManagedChannel(grpcCleanupRule, mdmAttributeGrpcService))
    }

    @Test
    fun `should return all attributes by filter`() {
        // given
        val attributeFirstVersion = mdmAttributeRepository.insert(mdmAttribute())

        val filter = MdmAttributeSearchFilter.newBuilder()
            .addMdmId(attributeFirstVersion.mdmId)
            .build()
        val request = MdmAttributesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmAttributeClient.getAttributesByFilter(request)

        // then
        assertSoftly {
            response.attributes shouldBe MdmAttributeList.newBuilder()
                .addAllAttribute(listOf(attributeFirstVersion.toProto())).build()
        }
    }

    @Test
    fun `should follow deep struct references`() {
        // given
        val topLevelEntityTypeId = 100L
        val midLevelEntityTypeId = 200L
        val lowLevelEntityTypeId1 = 300L
        val lowLevelEntityTypeId2 = 300L

        val topLevelAttribute = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Top level",
            dataType = MdmAttributeDataType.STRUCT,
            mdmEntityTypeId = topLevelEntityTypeId,
            structMdmEntityTypeId = midLevelEntityTypeId
        ))
        val midLevelAttribute1 = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Mid level 1",
            dataType = MdmAttributeDataType.STRUCT,
            mdmEntityTypeId = midLevelEntityTypeId,
            structMdmEntityTypeId = lowLevelEntityTypeId1
        ))
        val midLevelAttribute2 = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Mid level 2",
            dataType = MdmAttributeDataType.STRUCT,
            mdmEntityTypeId = midLevelEntityTypeId,
            structMdmEntityTypeId = lowLevelEntityTypeId2
        ))
        val lowLevelAttribute11 = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Top level 1-1",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = lowLevelEntityTypeId1
        ))
        val lowLevelAttribute12 = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Top level 1-2",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = lowLevelEntityTypeId1
        ))
        val lowLevelAttribute21 = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Top level 2-1",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = lowLevelEntityTypeId2
        ))
        val lowLevelAttribute22 = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Top level 2-2",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = lowLevelEntityTypeId2
        ))
        val irrelevantTopLevelAttribute = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Irrelevant top level",
            dataType = MdmAttributeDataType.STRUCT,
            mdmEntityTypeId = random.nextLong(),
            structMdmEntityTypeId = midLevelEntityTypeId
        ))
        val irrelevantMidLevelAttribute = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Irrelevant mid level",
            dataType = MdmAttributeDataType.STRUCT,
            mdmEntityTypeId = random.nextLong(),
            structMdmEntityTypeId = lowLevelEntityTypeId1
        ))
        val irrelevantLowLevelAttribute = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Irrelevant low level",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = random.nextLong()
        ))

        val filter = MdmAttributeSearchFilter.newBuilder()
            .setShallow(false)
            .addMdmEntityTypeId(topLevelEntityTypeId)
            .build()
        val request = MdmAttributesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmAttributeClient.getAttributesByFilter(request)

        // then
        response.attributes.attributeList.toSet() shouldBe setOf(
            topLevelAttribute.toProto(),
            midLevelAttribute1.toProto(),
            midLevelAttribute2.toProto(),
            lowLevelAttribute11.toProto(),
            lowLevelAttribute12.toProto(),
            lowLevelAttribute21.toProto(),
            lowLevelAttribute22.toProto()
        )
    }

    @Test
    fun `should extract only requested versions of inner attributes`() {
        // given
        val topLevelEntityTypeId = 100L
        val lowLevelEntityTypeId = 200L

        val topLevelAttribute = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Top level",
            dataType = MdmAttributeDataType.STRUCT,
            mdmEntityTypeId = topLevelEntityTypeId,
            structMdmEntityTypeId = lowLevelEntityTypeId
        ))

        val lowLevelAttributeOutdated = mdmAttributeRepository.insert(mdmAttribute(
            ruTitle = "Low level 1 outdated",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = lowLevelEntityTypeId,
            version = mdmVersionRetiredNDaysAgo(10),
        ))

        val lowLevelAttribute = mdmAttributeRepository.insertOrUpdate(mdmAttribute(
            mdmId = lowLevelAttributeOutdated.mdmId,
            ruTitle = "Low level 1",
            dataType = MdmAttributeDataType.INT64,
            mdmEntityTypeId = lowLevelEntityTypeId,
        ))

        val filter = MdmAttributeSearchFilter.newBuilder()
            .setShallow(false)
            .addMdmEntityTypeId(topLevelEntityTypeId)
            .build()
        val request = MdmAttributesByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmAttributeClient.getAttributesByFilter(request)

        // then
        response.attributes.attributeList shouldContain topLevelAttribute.toProto()
        response.attributes.attributeList shouldContain lowLevelAttribute.toProto()
        response.attributes.attributeList shouldNotContain lowLevelAttributeOutdated.toProto()
    }

    @Test
    fun `should return all attributes`() {
        // given
        mdmAttributeRepository.insert(mdmAttribute())

        // when
        val response = mdmAttributeClient.getAllActiveAttributesOnly(MdmEmptyRequest.getDefaultInstance())

        // then
        assertSoftly {
            response.attributes.attributeList shouldHaveSize mdmAttributeRepository.findAllActive().count()
        }
    }

    @Test
    fun `should update attribute`() {
        // given
        val entityType = mdmEntityTypeRepository.insert(mdmEntityType())
        val initialAttribute = mdmAttributeRepository.insert(mdmAttribute(mdmEntityTypeId = entityType.mdmId))
        val updatedAttribute = initialAttribute.copy(
            ruTitle = "new Title",
            version = MdmVersion.Companion.fromVersions(now(), null)
        )

        val request = MdmUpdateAttributesRequest.newBuilder()
            .setUpdates(MdmAttributeList.newBuilder().addAttribute(updatedAttribute.toProto()).build())
            .setContext(MdmBase.MdmUpdateContext.newBuilder().setCommitMessage("updated by me").build())
            .build()

        // when
        val response = mdmAttributeClient.updateAttributesOnly(request)

        // then
        assertSoftly {
            response.results.attributeCount shouldBe 1
            response.results.getAttribute(0).ruTitle shouldBe "new Title"
            response.errorsCount shouldBe 0
        }

        val saved = mdmAttributeRepository.findLatestById(response.results.getAttribute(0).mdmId)

        // and
        val auditEvent = mdmEventRepository.findById(saved!!.version.eventId)
        auditEvent.commitMessage shouldBe "updated by me"
    }

    @Test
    fun `should save audit event on create`() {
        // given
        val mdmEntityType = mdmEntityTypeRepository.insert(mdmEntityType())
        val mdmAttribute = mdmAttribute(mdmEntityTypeId = mdmEntityType.mdmId)

        val toCreate = MdmAttributeList.newBuilder()
            .addAttribute(mdmAttribute.toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = MdmUpdateAttributesRequest.newBuilder()
            .setUpdates(toCreate)
            .setContext(context).build()

        // when
        val response = mdmAttributeClient.updateAttributesOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.attributeCount shouldBe 1

            val created = response.results.attributeList[0]

            val auditEventId = created.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmAttributes = mdmAttributeRepository.findLatestById(created.mdmId)
            mdmAttributes shouldNotBe null
            mdmAttributes!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on update`() {
        // given
        val mdmEntityType = mdmEntityTypeRepository.insert(mdmEntityType())
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute(mdmEntityTypeId = mdmEntityType.mdmId))

        val toUpdate = MdmAttributeList.newBuilder()
            .addAttribute(mdmAttribute.toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = MdmUpdateAttributesRequest.newBuilder()
            .setUpdates(toUpdate)
            .setContext(context).build()

        // when
        val response = mdmAttributeClient.updateAttributesOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.attributeCount shouldBe 1

            val updated = response.results.attributeList[0]

            val auditEventId = updated.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmAttributes = mdmAttributeRepository.findLatestById(updated.mdmId)
            mdmAttributes shouldNotBe null
            mdmAttributes!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on delete`() {
        // given
        val mdmEntityType = mdmEntityTypeRepository.insert(mdmEntityType())
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute(mdmEntityTypeId = mdmEntityType.mdmId))

        val toDelete = MdmAttributeList.newBuilder()
            .addAttribute(mdmAttribute.copy(version = MdmVersion(to = Instant.now())).toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = MdmUpdateAttributesRequest.newBuilder()
            .setUpdates(toDelete)
            .setContext(context).build()

        // when
        val response = mdmAttributeClient.updateAttributesOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.attributeCount shouldBe 1

            val deleted = response.results.attributeList[0]
            val auditEventId = deleted.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmAttributes = mdmAttributeRepository.findAll().sortedBy { it.version.from }
            // хотим чтобы удаление происходило новой записью, а не просто затиранием versionTo активной записи
            mdmAttributes.last().version.eventId shouldBe auditEventId
        }
    }
}
