package ru.yandex.market.mdm.metadata.service.grpc

import io.grpc.testing.GrpcCleanupRule
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mdm.http.MdmBase
import ru.yandex.market.mdm.http.external_references.MdmExternalReferenceList
import ru.yandex.market.mdm.http.external_references.MdmExternalReferenceSearchFilter
import ru.yandex.market.mdm.http.external_references.MdmExternalReferenceServiceGrpc
import ru.yandex.market.mdm.http.external_references.MdmExternalReferencesByFilterRequest
import ru.yandex.market.mdm.http.external_references.UpdateMdmExternalReferencesRequest
import ru.yandex.market.mdm.http.search.VersionCriterion
import ru.yandex.market.mdm.lib.converters.toPojo
import ru.yandex.market.mdm.lib.converters.toProto
import ru.yandex.market.mdm.lib.model.mdm.MdmAttribute
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeDataType
import ru.yandex.market.mdm.lib.model.mdm.MdmAttributeExternalReferenceDetails
import ru.yandex.market.mdm.lib.model.mdm.MdmEntityType
import ru.yandex.market.mdm.lib.model.mdm.MdmEnumOption
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalReference
import ru.yandex.market.mdm.lib.model.mdm.MdmExternalSystem
import ru.yandex.market.mdm.lib.model.mdm.MdmPath
import ru.yandex.market.mdm.lib.model.mdm.MdmPathSegment
import ru.yandex.market.mdm.lib.model.mdm.MdmVersion
import ru.yandex.market.mdm.metadata.repository.MdmAttributeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEntityTypeRepository
import ru.yandex.market.mdm.metadata.repository.MdmEnumOptionRepository
import ru.yandex.market.mdm.metadata.repository.MdmEventRepository
import ru.yandex.market.mdm.metadata.repository.MdmExternalReferenceRepository
import ru.yandex.market.mdm.metadata.testutils.BaseAppTestClass
import ru.yandex.market.mdm.metadata.testutils.createTestManagedChannel
import java.time.Instant
import java.time.temporal.ChronoUnit

class MdmExternalReferenceGrpcServiceTest : BaseAppTestClass() {
    @Autowired
    private lateinit var mdmExternalReferenceGrpcService: MdmExternalReferenceGrpcService

    @Autowired
    private lateinit var mdmExternalReferenceRepository: MdmExternalReferenceRepository

    @Autowired
    private lateinit var mdmEnumOptionRepository: MdmEnumOptionRepository

    @Autowired
    private lateinit var mdmAttributeRepository: MdmAttributeRepository

    @Autowired
    private lateinit var mdmEntityTypeRepository: MdmEntityTypeRepository

    @Autowired
    private lateinit var mdmEventRepository: MdmEventRepository

    @Rule
    @JvmField
    val grpcCleanupRule = GrpcCleanupRule()

    lateinit var mdmExternalReferenceClient: MdmExternalReferenceServiceGrpc.MdmExternalReferenceServiceBlockingStub

    @Before
    fun initClient() {
        mdmExternalReferenceClient = MdmExternalReferenceServiceGrpc.newBlockingStub(
            createTestManagedChannel(grpcCleanupRule, mdmExternalReferenceGrpcService)
        )
    }

    @Test
    fun `test search by exact path`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setPath(testData.reference4.mdmPath.toProto())
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.EXACT)
                )
        ).build()
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then
        result.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(testData.reference4)
    }

    @Test
    fun `test search with children`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setPath(
                            MdmBase.MdmPath.newBuilder()
                                .addSegments(MdmPathSegment.entity(testData.entity1.mdmId).toProto())
                                .addSegments(MdmPathSegment.attribute(testData.attribute2.mdmId).toProto())
                                .addSegments(MdmPathSegment.entity(testData.entity4.mdmId).toProto())
                        )
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_CHILDREN)
                )
        ).build()
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then
        result.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(testData.reference3, testData.reference4, testData.reference5)
    }

    @Test
    fun `test search with parents`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setPath(
                            MdmBase.MdmPath.newBuilder()
                                .addSegments(MdmPathSegment.option(testData.option6.mdmId).toProto())
                        )
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_PARENTS)
                )
        ).build()
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then
        result.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(testData.reference3, testData.reference5)
    }

    @Test
    fun `test search with all`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setPath(
                            MdmBase.MdmPath.newBuilder()
                                .addSegments(MdmPathSegment.attribute(testData.attribute2.mdmId).toProto())
                        )
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_ALL)
                )
        ).build()
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then
        result.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(testData.reference3, testData.reference4, testData.reference5)
    }

    @Test
    fun `when search by several path filters should return union`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setPath(
                            MdmBase.MdmPath.newBuilder()
                                .addSegments(MdmPathSegment.option(testData.option6.mdmId).toProto())
                        )
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_PARENTS)
                )
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setPath(
                            MdmBase.MdmPath.newBuilder()
                                .addSegments(MdmPathSegment.entity(testData.entity7.mdmId).toProto())
                        )
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_CHILDREN)
                )
        ).build()
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then
        result.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(testData.reference1, testData.reference3, testData.reference5)
    }

    @Test
    fun `test search by mdm id`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val response = mdmExternalReferenceClient.getExternalReferencesByFilter(
            MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
                MdmExternalReferenceSearchFilter.newBuilder()
                    .addMdmId(testData.reference1.mdmId)
                    .addMdmId(testData.reference3.mdmId)
            ).build()
        )

        // then
        response.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(testData.reference1, testData.reference3)
    }

    @Test
    fun `when search by empty path with children should return all external references`() {
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_CHILDREN)
                )
        ).build()
        val initData = mdmExternalReferenceClient.getExternalReferencesByFilter(request).mdmExternalReferences
            .mdmExternalReferencesList

        // given
        val testData = generateAndLoadTestData()

        // when
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then

        //listOf(testData.reference1, testData.reference2, testData.reference3, testData.reference4,
        //    testData.reference5) shouldBeIn result.mdmExternalReferences.mdmExternalReferencesList.map { it.toPojo() }
        result.mdmExternalReferences.mdmExternalReferencesList.filter { !initData.contains(it) }
            .map { it.toPojo() } shouldContainExactlyInAnyOrder
            listOf(
                testData.reference1, testData.reference2, testData.reference3, testData.reference4, testData.reference5
            )
    }

    @Test
    fun `test search with time`() {
        // given
        val testData = generateAndLoadTestData()
        super.jdbcTemplate.update(
            "UPDATE metadata.mdm_external_reference SET version_from  = '2020-01-01'" +
                "WHERE mdm_id IN (${testData.reference1.mdmId}, ${testData.reference2.mdmId})"
        )

        // when
        val request = MdmExternalReferencesByFilterRequest.newBuilder().setFilter(
            MdmExternalReferenceSearchFilter.newBuilder()
                .addPaths(
                    MdmBase.MdmPathFilter.newBuilder()
                        .setCondition(MdmBase.MdmPathFilter.MdmPathCondition.WITH_CHILDREN)
                )
                .setCriterion(VersionCriterion.newBuilder()
                    .setAliveAt(Instant.parse("2020-01-02T10:15:30.00Z").toEpochMilli()))
        ).build()
        val result = mdmExternalReferenceClient.getExternalReferencesByFilter(request)

        // then
        val resultWithClearedVersion = result.mdmExternalReferences.mdmExternalReferencesList.asSequence()
            .map { it.toPojo() }
            .map { it.copy(version = MdmVersion()) }
            .toList()
        resultWithClearedVersion shouldContainExactlyInAnyOrder
            listOf(testData.reference1, testData.reference2).map { it.copy(version = MdmVersion()) }
    }

    @Test
    fun `should save audit event on create`() {
        // given
        val testData = generateAndLoadTestData()
        val externalReferenceToCreate = testData.reference1.copy(mdmId = 0, version = MdmVersion(), externalId = 128)

        // when
        val referencesListToCreate = MdmExternalReferenceList.newBuilder()
            .addMdmExternalReferences(externalReferenceToCreate.toProto())
            .build()
        val context = MdmBase.MdmUpdateContext.newBuilder()
            .setContext("Java is better")
            .build()
        val request = UpdateMdmExternalReferencesRequest.newBuilder()
            .setUpdates(referencesListToCreate)
            .setContext(context).build()
        val response = mdmExternalReferenceClient.updateExternalReferences(request)

        // then
        response.errorsCount shouldBe 0
        response.results.mdmExternalReferencesList shouldHaveSize 1
        val auditEventId = response.results.mdmExternalReferencesList[0].mdmUpdateMeta.auditEventId
        auditEventId shouldBeGreaterThan 0
        val auditEvents = mdmEventRepository.findByIds(listOf(auditEventId))
        auditEvents shouldHaveSize 1
        auditEvents[0].context shouldBe context.context

        val existingRefIds = testData.referenceIds()
        val newExternalReferences = mdmExternalReferenceRepository.findAll()
            .filter { !existingRefIds.contains(it.mdmId) }
        newExternalReferences shouldHaveSize 6
        val externalReference = newExternalReferences[5]
        externalReference.version.to shouldBe null
        externalReference shouldBe
            externalReferenceToCreate.copy(mdmId = externalReference.mdmId, version = externalReference.version)
        externalReference.version.eventId shouldBe auditEventId
    }

    @Test
    fun `should save audit event on update`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val updatedReference = testData.reference1.copy(
            mdmAttributeExternalReferenceDetails = MdmAttributeExternalReferenceDetails(
                externalName = "Louise Aimée Julie Davout"
            )
        )
        val updatedReferencesList = MdmExternalReferenceList.newBuilder()
            .addMdmExternalReferences(updatedReference.toProto())
            .build()
        val context = MdmBase.MdmUpdateContext.newBuilder()
            .setContext("Java is better")
            .build()
        val request = UpdateMdmExternalReferencesRequest.newBuilder()
            .setUpdates(updatedReferencesList)
            .setContext(context).build()
        val response = mdmExternalReferenceClient.updateExternalReferences(request)

        // then
        response.errorsCount shouldBe 0
        response.results.mdmExternalReferencesList shouldHaveSize 1
        val auditEventId = response.results.mdmExternalReferencesList[0].mdmUpdateMeta.auditEventId
        auditEventId shouldBeGreaterThan 0
        val auditEvents = mdmEventRepository.findByIds(listOf(auditEventId))
        auditEvents shouldHaveSize 1
        auditEvents[0].context shouldBe context.context

        val allExternalReferenceVersions = mdmExternalReferenceRepository.findAll()
            .filter { it.mdmId == updatedReference.mdmId }
            .sortedBy { it.version.from }
        allExternalReferenceVersions shouldHaveSize 2
        allExternalReferenceVersions.first().version.to shouldNotBe null
        val lastExternalReference = allExternalReferenceVersions.last()
        lastExternalReference.version.to shouldBe null
        lastExternalReference shouldBe updatedReference.copy(version = lastExternalReference.version)
        lastExternalReference.version.eventId shouldBe auditEventId
    }

    @Test
    fun `should save audit event on delete`() {
        // given
        val testData = generateAndLoadTestData()

        // when
        val to = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        val toDelete = MdmExternalReferenceList.newBuilder()
            .addMdmExternalReferences(testData.reference1.copy(version = MdmVersion(to = to)).toProto())
            .build()
        val context = MdmBase.MdmUpdateContext.newBuilder()
            .setContext("Java is better")
            .build()
        val request = UpdateMdmExternalReferencesRequest.newBuilder()
            .setUpdates(toDelete)
            .setContext(context).build()
        val response = mdmExternalReferenceClient.updateExternalReferences(request)

        // then
        response.errorsCount shouldBe 0
        response.results.mdmExternalReferencesList shouldHaveSize 1
        val auditEventId = response.results.mdmExternalReferencesList[0].mdmUpdateMeta.auditEventId
        auditEventId shouldBeGreaterThan 0
        val auditEvents = mdmEventRepository.findByIds(listOf(auditEventId))
        auditEvents shouldHaveSize 1
        auditEvents[0].context shouldBe context.context

        val allExternalReferenceVersions = mdmExternalReferenceRepository.findAll()
            .filter { it.mdmId == testData.reference1.mdmId }
            .sortedBy { it.version.from }
        allExternalReferenceVersions.map { it.version.to }.all { it != null } shouldBe true
        allExternalReferenceVersions shouldHaveSize 2
        val lastExternalReference = allExternalReferenceVersions.last()
        lastExternalReference.version.eventId shouldBe auditEventId
    }

    /**
     * Создает и загружает в репозитории тестовые данные в соответствии с контрактом
     */
    private fun generateAndLoadTestData(): TestData {
        val entity1 = MdmEntityType(
            internalName = "friedland",
            ruTitle = "Битва под Фридландом",
            description = "The Battle of Friedland (June 14, 1807)"
        )
        val entity4 = MdmEntityType(
            internalName = "heilsberg",
            ruTitle = "Битва при Гейльсберге",
            description = "The Battle of Heilsberg(10 June 1807)."
        )
        val entity7 = MdmEntityType(
            internalName = "tilsit",
            ruTitle = "Тильзитский мир",
            description = "The Treaties of Tilsit."
        )
        mdmEntityTypeRepository.insertBatch(listOf(entity1, entity4, entity7))

        val attribute2 = MdmAttribute(
            mdmEntityTypeId = entity1.mdmId,
            dataType = MdmAttributeDataType.STRUCT,
            internalName = "davout",
            ruTitle = "Даву, Луи Никола"
        )
        val attribute3 = MdmAttribute(
            mdmEntityTypeId = entity1.mdmId,
            dataType = MdmAttributeDataType.STRING,
            internalName = "augereau",
            ruTitle = "Ожеро, Пьер-Франсуа-Шарль"
        )
        val attribute5 = MdmAttribute(
            mdmEntityTypeId = entity4.mdmId,
            dataType = MdmAttributeDataType.ENUM,
            internalName = "murat",
            ruTitle = "Мюрат, Иоахим"
        )
        val attribute8 = MdmAttribute(
            mdmEntityTypeId = entity7.mdmId,
            dataType = MdmAttributeDataType.INT64,
            internalName = "lestocq",
            ruTitle = "Лесток, Антон Вильгельм фон"
        )
        mdmAttributeRepository.insertBatch(listOf(attribute2, attribute3, attribute5, attribute8))

        val option6 = MdmEnumOption(
            mdmAttributeId = attribute5.mdmId,
            value = "Levin August von Bennigsen"
        )
        mdmEnumOptionRepository.insert(option6)

        val entity1Segment = MdmPathSegment.entity(entity1.mdmId)
        val attribute2Segment = MdmPathSegment.attribute(attribute2.mdmId)
        val attribute3Segment = MdmPathSegment.attribute(attribute3.mdmId)
        val entity4Segment = MdmPathSegment.entity(entity4.mdmId)
        val attribute5Segment = MdmPathSegment.attribute(attribute5.mdmId)
        val option6Segment = MdmPathSegment.option(option6.mdmId)
        val entity7Segment = MdmPathSegment.entity(entity7.mdmId)
        val attribute8Segment = MdmPathSegment.attribute(attribute8.mdmId)

        val reference1 = MdmExternalReference(
            mdmPath = MdmPath(listOf(entity7Segment, attribute8Segment)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 10
        )
        val reference2 = MdmExternalReference(
            mdmPath = MdmPath(listOf(entity1Segment, attribute3Segment)),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 11
        )
        val reference3 = MdmExternalReference(
            mdmPath = MdmPath(
                listOf(entity1Segment, attribute2Segment, entity4Segment, attribute5Segment, option6Segment)
            ),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 12
        )
        val reference4 = MdmExternalReference(
            mdmPath = MdmPath(
                listOf(entity1Segment, attribute2Segment, entity4Segment, attribute5Segment)
            ),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 13
        )
        val reference5 = MdmExternalReference(
            mdmPath = MdmPath(
                listOf(entity1Segment, attribute2Segment, entity4Segment, attribute5Segment, option6Segment)
            ),
            externalSystem = MdmExternalSystem.OLD_MDM,
            externalId = 14
        )
        mdmExternalReferenceRepository.insertBatch(listOf(reference1, reference2, reference3, reference4, reference5))

        return TestData(
            entity1 = entity1,
            attribute2 = attribute2,
            attribute3 = attribute3,
            entity4 = entity4,
            attribute5 = attribute5,
            option6 = option6,
            entity7 = entity7,
            attribute8 = attribute8,
            reference1 = reference1,
            reference2 = reference2,
            reference3 = reference3,
            reference4 = reference4,
            reference5 = reference5
        )
    }

    /**
     * Специальный набор тестовых данных, ожидается, что он соответствует:
     * e1 -> a2 -> e4 -> a5 -> o6
     *    -> a3
     * e7 -> a8
     * С внешними свзями:
     * r1 на e7 -> a8
     * r2 на e1 -> a3
     * r3 на e1 -> a2 -> e4 -> a5 -> o6
     * r4 на e1 -> a2 -> e4 -> a5
     * r5 на e1 -> a2 -> e4 -> a5 -> o6
     */
    private data class TestData(
        val entity1: MdmEntityType,
        val attribute2: MdmAttribute,
        val attribute3: MdmAttribute,
        val entity4: MdmEntityType,
        val attribute5: MdmAttribute,
        val option6: MdmEnumOption,
        val entity7: MdmEntityType,
        val attribute8: MdmAttribute,
        val reference1: MdmExternalReference,
        val reference2: MdmExternalReference,
        val reference3: MdmExternalReference,
        val reference4: MdmExternalReference,
        val reference5: MdmExternalReference
    ) {
        fun referenceIds(): Set<Long> =
            sequenceOf(reference1, reference2, reference3, reference4, reference5).map { it.mdmId }.toSet()
    }
}
