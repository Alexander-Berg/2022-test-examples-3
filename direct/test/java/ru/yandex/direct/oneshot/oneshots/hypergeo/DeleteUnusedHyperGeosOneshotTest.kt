package ru.yandex.direct.oneshot.oneshots.hypergeo

import org.assertj.core.api.Assertions
import org.jooq.DSLContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.oneshot.configuration.OneshotTest
import ru.yandex.direct.oneshot.oneshots.OneshotTestsUtils.Companion.hasDefect
import ru.yandex.direct.validation.defect.CommonDefects
import ru.yandex.direct.validation.defect.NumberDefects

@OneshotTest
@RunWith(SpringRunner::class)
class DeleteUnusedHyperGeosOneshotTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var deleteUnusedHyperGeosOneshot: DeleteUnusedHyperGeosOneshot

    private var shard: Int = 0
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var dslContext: DSLContext
    private lateinit var hyperGeoSegment1: HyperGeoSegment
    private lateinit var hyperGeoSegment2: HyperGeoSegment
    private lateinit var hyperGeoSegment3: HyperGeoSegment
    private lateinit var hyperGeo1: HyperGeo
    private lateinit var hyperGeo2: HyperGeo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
        dslContext = dslContextProvider.ppc(shard)
    }

    @Test
    fun validate_ValidInputData_success() {
        val inputData = InputData()
        val vr = deleteUnusedHyperGeosOneshot.validate(inputData)
        Assertions.assertThat(vr.hasAnyErrors()).`as`("hasAnyErrors").isFalse
    }

    @Test
    fun validate_InvalidInputData_failure() {
        val inputData =
            InputData(deleteHyperGeos = false, deleteHyperGeoSegments = false, idsToDeleteByShardLimit = -5)
        val vr = deleteUnusedHyperGeosOneshot.validate(inputData)
        Assertions.assertThat(vr).`as`("bothDeletePropertiesFalse")
            .hasDefect("deleteHyperGeoSegments", CommonDefects.invalidValue())
            .hasDefect("idsToDeleteByShardLimit", NumberDefects.greaterThanOrEqualTo(1))
    }

    private fun createHyperGeosAndSegments() {
        hyperGeoSegment1 = defaultHyperGeoSegment()
        hyperGeoSegment2 = defaultHyperGeoSegment()
        hyperGeoSegment3 = steps.hyperGeoSteps()
            .createHyperGeoSegments(clientInfo, listOf(defaultHyperGeoSegment()))[0]
        hyperGeo1 = steps.hyperGeoSteps()
            .createHyperGeo(clientInfo, defaultHyperGeo(hyperGeoSegments = listOf(hyperGeoSegment1)))
        hyperGeo2 = steps.hyperGeoSteps()
            .createHyperGeo(clientInfo, defaultHyperGeo(hyperGeoSegments = listOf(hyperGeoSegment2)))

        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo)
        val dslConfig = dslContext.configuration()
        steps.hyperGeoSteps().createHyperGeoLink(dslConfig, mapOf(adGroupInfo.adGroupId to hyperGeo1.id))

        val dbHyperGeoSegments = hyperGeoSegmentRepository.getHyperGeoSegmentById(
            shard, clientId, listOf(hyperGeoSegment1.id, hyperGeoSegment2.id, hyperGeoSegment3.id)
        )
        Assertions.assertThat(dbHyperGeoSegments).hasSize(3)
        val dbHyperGeo = hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo1.id, hyperGeo2.id))
        Assertions.assertThat(dbHyperGeo).hasSize(2)
    }

    private fun mockYaAudienceClient(deleteSuccess: Boolean) {
        Mockito.`when`(yaAudienceClient.deleteSegment(ArgumentMatchers.anyLong()))
            .thenReturn(deleteSuccess)
    }

    @Test
    fun execute_deleteOnlySegments_success() {
        createHyperGeosAndSegments()
        mockYaAudienceClient(true)

        val inputData = InputData(deleteHyperGeos = false)
        deleteUnusedHyperGeosOneshot.execute(inputData = inputData, prevState = null, shard = shard)
        // гипер гео не удаляются; удалится только 3-ий сегмент, ибо он не связан ни с одним из гипер гео

        val dbHyperGeoSegments = hyperGeoSegmentRepository.getHyperGeoSegmentById(
            shard, clientId, listOf(hyperGeoSegment1.id, hyperGeoSegment2.id, hyperGeoSegment3.id)
        )
        Assertions.assertThat(dbHyperGeoSegments.values).containsOnly(hyperGeoSegment1, hyperGeoSegment2)
        val dbHyperGeo = hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo1.id, hyperGeo2.id))
        Assertions.assertThat(dbHyperGeo).hasSize(2)
    }

    @Test
    fun execute_deleteOnlyHyperGeosAndTheirSegments_success() {
        createHyperGeosAndSegments()
        mockYaAudienceClient(true)

        val inputData = InputData(deleteHyperGeoSegments = false)
        deleteUnusedHyperGeosOneshot.execute(inputData = inputData, prevState = null, shard = shard)
        // удалится только 2-ой гипер гео, ибо 1-ый связан с группой; и связанный с ним 2-ой сегмент

        val dbHyperGeoSegments = hyperGeoSegmentRepository.getHyperGeoSegmentById(
            shard, clientId, listOf(hyperGeoSegment1.id, hyperGeoSegment2.id, hyperGeoSegment3.id)
        )
        Assertions.assertThat(dbHyperGeoSegments.values).containsOnly(hyperGeoSegment1, hyperGeoSegment3)
        val dbHyperGeo = hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo1.id, hyperGeo2.id))
        Assertions.assertThat(dbHyperGeo.values).containsOnly(hyperGeo1)
    }

    @Test
    fun execute_deleteHyperGeosAndSegments_success() {
        createHyperGeosAndSegments()
        mockYaAudienceClient(true)

        val inputData = InputData()
        deleteUnusedHyperGeosOneshot.execute(inputData = inputData, prevState = null, shard = shard)
        // удалится 2-ой гипер гео и 2-ой сегмент (1-ый гипер гео связан с группой, 1-ый сегмент - с первым гипер гео),
        // а также 3-ий сегмент

        val dbHyperGeoSegments = hyperGeoSegmentRepository.getHyperGeoSegmentById(
            shard, clientId, listOf(hyperGeoSegment1.id, hyperGeoSegment2.id, hyperGeoSegment3.id)
        )
        Assertions.assertThat(dbHyperGeoSegments.values).containsOnly(hyperGeoSegment1)
        val dbHyperGeo = hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo1.id, hyperGeo2.id))
        Assertions.assertThat(dbHyperGeo.values).containsOnly(hyperGeo1)
    }

    @Test
    fun execute_deleteHyperGeosAndSegments_cannotDeleteAudienceSegments_success() {
        createHyperGeosAndSegments()
        mockYaAudienceClient(false)

        val inputData = InputData()
        deleteUnusedHyperGeosOneshot.execute(inputData = inputData, prevState = null, shard = shard)
        // удалится только 2-ой гипер гео

        val dbHyperGeoSegments = hyperGeoSegmentRepository.getHyperGeoSegmentById(
            shard, clientId, listOf(hyperGeoSegment1.id, hyperGeoSegment2.id, hyperGeoSegment3.id)
        )
        Assertions.assertThat(dbHyperGeoSegments.values)
            .containsOnly(hyperGeoSegment1, hyperGeoSegment2, hyperGeoSegment3)
        val dbHyperGeo = hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo1.id, hyperGeo2.id))
        Assertions.assertThat(dbHyperGeo.values).containsOnly(hyperGeo1)
    }
}
