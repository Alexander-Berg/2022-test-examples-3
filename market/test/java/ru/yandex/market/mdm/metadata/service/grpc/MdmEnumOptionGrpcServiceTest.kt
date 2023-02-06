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
import ru.yandex.market.mdm.fixtures.mdmEnumOption
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionList
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionSearchFilter
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionServiceGrpc
import ru.yandex.market.mdm.http.enum_options.MdmEnumOptionsByFilterRequest
import ru.yandex.market.mdm.http.enum_options.UpdateMdmEnumOptionsRequest
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEnumOptionRepository
import ru.yandex.market.mdm.metadata.repository.MdmEventRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel
import java.time.Instant

class MdmEnumOptionGrpcServiceTest : BaseAppTestClass() {

    @Autowired
    lateinit var mdmEnumOptionGrpcService: MdmEnumOptionGrpcService

    @Autowired
    lateinit var mdmEnumOptionRepository: MdmEnumOptionRepository

    @Autowired
    lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    lateinit var mdmEventRepository: MdmEventRepository

    @Rule
    @JvmField
    final val grpcCleanupRule = GrpcCleanupRule()

    lateinit var mdmEnumOptionClient: MdmEnumOptionServiceGrpc.MdmEnumOptionServiceBlockingStub

    @Before
    fun initClient() {
        mdmEnumOptionClient =
            MdmEnumOptionServiceGrpc.newBlockingStub(createTestManagedChannel(grpcCleanupRule, mdmEnumOptionGrpcService))
    }

    @Test
    fun `should return all enum options by filter`() {
        // given
        val mdmEnumOption = mdmEnumOptionRepository.insert(mdmEnumOption())
        val filter = MdmEnumOptionSearchFilter.newBuilder()
            .addMdmId(mdmEnumOption.mdmId)
            .build()
        val request = MdmEnumOptionsByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmEnumOptionClient.getEnumOptionsOnlyByFilter(request)

        // then
        response.mdmEnumOptions.mdmEnumOptionsList shouldContain mdmEnumOption.toProto()
    }

    @Test
    fun `should return all enum options by attribute id`() {
        // given
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val mdmEnumOption = mdmEnumOptionRepository.insert(mdmEnumOption(mdmAttributeId = mdmAttribute.mdmId))
        val filter = MdmEnumOptionSearchFilter.newBuilder()
            .addAllMdmAttributeId(listOf(mdmAttribute.mdmId))
            .build()
        val request = MdmEnumOptionsByFilterRequest.newBuilder()
            .setFilter(filter)
            .build()

        // when
        val response = mdmEnumOptionClient.getEnumOptionsOnlyByFilter(request)

        // then
        response.mdmEnumOptions.mdmEnumOptionsList shouldContain mdmEnumOption.toProto()
    }

    fun `should update enum option`() {
        // given
        val initialEnumOption = mdmEnumOptionRepository.insert(mdmEnumOption())
        val updatedEnumOption = initialEnumOption.copy(
            value = "new value",
            version = MdmVersion.fromVersions(Instant.now(), null)
        )

        val request = UpdateMdmEnumOptionsRequest.newBuilder()
            .setUpdates(MdmEnumOptionList.newBuilder().addMdmEnumOptions(updatedEnumOption.toProto()).build())
            .setContext(MdmBase.MdmUpdateContext.newBuilder().setCommitMessage("updated by me").build())
            .build()

        // when
        val response = mdmEnumOptionClient.updateEnumOptionsOnly(request)

        // then
        assertSoftly {
            response.results.mdmEnumOptionsCount shouldBe 1
            response.results.mdmEnumOptionsList[0].value shouldBe "new Title"
            response.errorsCount shouldBe 0
        }

        val saved = mdmEnumOptionRepository.findLatestById(response.results.mdmEnumOptionsList[0].mdmId)

        // and
        val auditEvent = mdmEventRepository.findById(saved!!.version.eventId)
        auditEvent.commitMessage shouldBe "updated by me"
    }

    @Test
    fun `should save audit event on create`() {
        // given
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val mdmEnumOption = mdmEnumOption(mdmAttributeId = mdmAttribute.mdmId)

        val toCreate = MdmEnumOptionList.newBuilder()
            .addMdmEnumOptions(mdmEnumOption.toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateMdmEnumOptionsRequest.newBuilder()
            .setUpdates(toCreate)
            .setContext(context).build()

        // when
        val response = mdmEnumOptionClient.updateEnumOptionsOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.mdmEnumOptionsCount shouldBe 1

            val created = response.results.mdmEnumOptionsList[0]

            val auditEventId = created.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmEnumOptions = mdmEnumOptionRepository.findLatestById(created.mdmId)
            mdmEnumOptions shouldNotBe null
            mdmEnumOptions!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on update`() {
        // given
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val mdmEnumOption = mdmEnumOptionRepository.insert(mdmEnumOption(mdmAttributeId = mdmAttribute.mdmId))

        val toUpdate = MdmEnumOptionList.newBuilder()
            .addMdmEnumOptions(mdmEnumOption.toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateMdmEnumOptionsRequest.newBuilder()
            .setUpdates(toUpdate)
            .setContext(context).build()

        // when
        val response = mdmEnumOptionClient.updateEnumOptionsOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.mdmEnumOptionsCount shouldBe 1

            val updated = response.results.mdmEnumOptionsList[0]

            val auditEventId = updated.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmEnumOptions = mdmEnumOptionRepository.findLatestById(updated.mdmId)
            mdmEnumOptions shouldNotBe null
            mdmEnumOptions!!.version.eventId shouldBe auditEventId
        }
    }

    @Test
    fun `should save audit event on delete`() {
        // given
        val mdmAttribute = mdmAttributeRepository.insert(mdmAttribute())
        val mdmEnumOption = mdmEnumOptionRepository.insert(mdmEnumOption(mdmAttributeId = mdmAttribute.mdmId))

        val toDelete = MdmEnumOptionList.newBuilder()
            .addMdmEnumOptions(mdmEnumOption.copy(version = MdmVersion(to = Instant.now())).toProto()).build()

        val context = MdmBase.MdmUpdateContext.newBuilder().build()
        val request = UpdateMdmEnumOptionsRequest.newBuilder()
            .setUpdates(toDelete)
            .setContext(context).build()

        // when
        val response = mdmEnumOptionClient.updateEnumOptionsOnly(request)

        // then
        assertSoftly {
            response.errorsCount shouldBe 0
            response.results.mdmEnumOptionsCount shouldBe 1

            val deleted = response.results.mdmEnumOptionsList[0]
            val auditEventId = deleted.mdmUpdateMeta.auditEventId
            auditEventId shouldNotBe 0L

            val auditEvent = mdmEventRepository.findByIds(listOf(auditEventId))
            auditEvent.size shouldBe 1

            val mdmEnumOptions = mdmEnumOptionRepository.findAll().sortedBy { it.version.from }
            mdmEnumOptions.last().version.eventId shouldBe auditEventId
        }
    }
}
