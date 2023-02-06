package ru.yandex.direct.core.entity.hypergeo.operation

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoSegmentsDeleteOperationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoOperationsFactory: HyperGeoOperationsFactory

    private lateinit var clientId: ClientId
    private lateinit var hyperGeoSegment: HyperGeoSegment
    private lateinit var hyperGeo: HyperGeo
    private var shard = 0

    @Before
    fun before() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        mockYaAudienceClient()
        hyperGeoSegment = defaultHyperGeoSegment()
        steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, listOf(hyperGeoSegment))
        hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)
    }

    private fun mockYaAudienceClient() {
        Mockito.`when`(yaAudienceClient.deleteSegment(ArgumentMatchers.anyLong()))
            .thenReturn(true)
    }

    @Test
    fun prepareAndApply_Partial_OneValidItem_ResultIsFullySuccessful() {
        val deleteOperation = hyperGeoOperationsFactory.createHyperGeoSegmentsDeleteOperation(
            Applicability.PARTIAL, listOf(hyperGeoSegment.id), shard
        )
        val massResult = deleteOperation.prepareAndApply()
        assertThat(massResult)
            .`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(Long::class.java))))

        val firstResult = massResult[0].result
        val hyperGeoSegment =
            hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(firstResult))[firstResult]
        assertThat(hyperGeoSegment).isNull()
    }

    @Test
    fun prepareAndApply_Partial_OneInvalidItem_ResultHasElementError_HyperGeoSegmentRemains() {
        val hyperGeoSegmentId = hyperGeo.hyperGeoSegments[0].id
        val deleteOperation = hyperGeoOperationsFactory.createHyperGeoSegmentsDeleteOperation(
            Applicability.PARTIAL, listOf(hyperGeoSegmentId), shard
        )
        val massResult = deleteOperation.prepareAndApply()
        assertThat(massResult)
            .`is`(matchedBy(isSuccessful<Long>(false)))

        val dbHyperGeoSegment = hyperGeoSegmentRepository
            .getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegmentId))[hyperGeoSegmentId]
        assertThat(dbHyperGeoSegment).isEqualTo(hyperGeo.hyperGeoSegments[0])
    }

    @Test
    fun prepareAndApply_Partial_OneValidAndOneInvalidItem_ResultIsPartiallySuccessful() {
        val deleteOperation = hyperGeoOperationsFactory.createHyperGeoSegmentsDeleteOperation(
            Applicability.PARTIAL, listOf(hyperGeoSegment.id, hyperGeo.hyperGeoSegments[0].id), shard
        )
        deleteOperation.prepareAndApply()

        val hyperGeoSegments = hyperGeoSegmentRepository
            .getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id, hyperGeo.hyperGeoSegments[0].id))
        assertThat(hyperGeoSegments).hasSize(1)
    }

    @Test
    fun prepareAndApply_Partial_OneValidItem_CannotDeleteYaAudienceSegment_HyperGeoSegmentRemains() {
        Mockito.`when`(yaAudienceClient.deleteSegment(ArgumentMatchers.anyLong()))
            .thenReturn(false)

        val deleteOperation = hyperGeoOperationsFactory.createHyperGeoSegmentsDeleteOperation(
            Applicability.PARTIAL, listOf(hyperGeoSegment.id), shard
        )
        val massResult = deleteOperation.prepareAndApply()
        assertThat(massResult)
            .`is`(matchedBy(isSuccessfulWithMatchers(notNullValue(Long::class.java))))

        val firstResult = massResult[0].result
        val dbHyperGeoSegment =
            hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(firstResult))[firstResult]
        assertThat(dbHyperGeoSegment).isEqualTo(hyperGeoSegment)
    }
}
