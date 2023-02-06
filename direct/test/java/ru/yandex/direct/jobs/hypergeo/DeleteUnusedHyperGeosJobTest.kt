package ru.yandex.direct.jobs.hypergeo

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.entity.hypergeo.service.HyperGeoService
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.jobs.configuration.JobsTest

@JobsTest
@ExtendWith(SpringExtension::class)
class DeleteUnusedHyperGeosJobTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    @Autowired
    private lateinit var hyperGeoService: HyperGeoService

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    private lateinit var deleteUnusedHyperGeosJob: DeleteUnusedHyperGeosJob

    private var shard: Int = 0
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var hyperGeoSegment1: HyperGeoSegment
    private lateinit var hyperGeoSegment2: HyperGeoSegment
    private lateinit var hyperGeoSegment3: HyperGeoSegment
    private lateinit var hyperGeo1: HyperGeo
    private lateinit var hyperGeo2: HyperGeo

    @BeforeEach
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
        val dslContext = dslContextProvider.ppc(shard)

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

        deleteUnusedHyperGeosJob = DeleteUnusedHyperGeosJob(
            hyperGeoService,
            hyperGeoRepository,
            hyperGeoSegmentRepository,
            ppcPropertiesSupport
        ).withShard(clientInfo.shard) as DeleteUnusedHyperGeosJob
    }

    private fun mockYaAudienceClient(deleteSuccess: Boolean) {
        Mockito.`when`(yaAudienceClient.deleteSegment(ArgumentMatchers.anyLong()))
            .thenReturn(deleteSuccess)
    }

    @Test
    fun execute_deleteHyperGeosAndSegments_success() {
        mockYaAudienceClient(true)

        deleteUnusedHyperGeosJob.execute()
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
        mockYaAudienceClient(false)

        deleteUnusedHyperGeosJob.execute()
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
