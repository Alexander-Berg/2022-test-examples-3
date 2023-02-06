package ru.yandex.direct.core.entity.hypergeo.operation

import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingConditionRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.operation.Applicability
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful
import ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessful

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeosDeleteOperationTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var retargetingConditionRepository: RetargetingConditionRepository

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoOperationsFactory: HyperGeoOperationsFactory

    private lateinit var clientId: ClientId
    private lateinit var clientId2: ClientId
    private lateinit var hyperGeo1: HyperGeo
    private lateinit var hyperGeoIds: List<Long>
    private lateinit var dslContext: DSLContext
    private var shard = 0

    @Before
    fun before() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        val clientInfo2 = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        clientId2 = clientInfo2.clientId!!
        shard = clientInfo.shard
        dslContext = dslContextProvider.ppc(shard)

        mockYaAudienceClient()
        hyperGeo1 = steps.hyperGeoSteps().createHyperGeo(clientInfo)
        val hyperGeo2 = steps.hyperGeoSteps().createHyperGeo(clientInfo2)
        hyperGeoIds = listOf(hyperGeo1.id, hyperGeo2.id)
    }

    private fun mockYaAudienceClient() {
        Mockito.`when`(yaAudienceClient.deleteSegment(ArgumentMatchers.anyLong()))
            .thenReturn(true)
    }

    @Test
    fun prepareAndApply_Partial_OnlyValidItems_ResultIsFullySuccessful() {
        val deleteOperation = hyperGeoOperationsFactory.createHyperGeosDeleteOperation(
            Applicability.PARTIAL, hyperGeoIds, dslContext, null
        )
        val massResult = deleteOperation.prepareAndApply()
        assertThat(massResult)
            .`is`(matchedBy(isFullySuccessful<Long>()))

        val existingHyperGeoIds = retargetingConditionRepository.getConditions(shard, hyperGeoIds)
            .filter { !it.deleted }
            .map { it.id }
        assertThat(existingHyperGeoIds).isEmpty()
    }

    @Test
    fun prepareAndApply_Partial_OneInvalidItem_ResultHasElementError_HyperGeoRemains() {
        val deleteOperation = hyperGeoOperationsFactory.createHyperGeosDeleteOperation(
            Applicability.PARTIAL, listOf(hyperGeo1.id), dslContext, clientId2
        )
        val massResult = deleteOperation.prepareAndApply()
        assertThat(massResult)
            .`is`(matchedBy(isSuccessful<Long>(false)))

        val dbHyperGeo = hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(hyperGeo1.id))[hyperGeo1.id]
        assertThat(dbHyperGeo).isEqualTo(hyperGeo1)
    }

    @Test
    fun prepareAndApply_Partial_OneValidAndOneInvalidItem_ResultIsPartiallySuccessful() {
        val deleteOperation = hyperGeoOperationsFactory.createHyperGeosDeleteOperation(
            Applicability.PARTIAL, hyperGeoIds, dslContext, clientId
        )
        deleteOperation.prepareAndApply()

        val existingHyperGeoIds = retargetingConditionRepository.getConditions(shard, hyperGeoIds)
            .filter { !it.deleted }
            .map { it.id }
        assertThat(existingHyperGeoIds).isEqualTo(listOf(hyperGeoIds[1]))
    }

    @Test
    fun prepareAndApply_Partial_OneValidItem_CannotDeleteYaAudienceSegment_HyperGeoSegmentRemains() {
        Mockito.`when`(yaAudienceClient.deleteSegment(ArgumentMatchers.anyLong()))
            .thenReturn(false)

        val deleteOperation = hyperGeoOperationsFactory.createHyperGeosDeleteOperation(
            Applicability.PARTIAL, listOf(hyperGeo1.id), dslContext, clientId
        )
        deleteOperation.prepareAndApply()

        // гипер гео (условия ретаргетинга) удалились, а сегмент - нет
        val existingHyperGeoIds = retargetingConditionRepository.getExistingIds(shard, clientId, listOf(hyperGeo1.id))
        assertThat(existingHyperGeoIds).isEmpty()

        val hyperGeoSegment = hyperGeo1.hyperGeoSegments[0]
        val dbHyperGeoSegment = hyperGeoSegmentRepository
            .getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id))[hyperGeoSegment.id]
        assertThat(dbHyperGeoSegment).isEqualTo(hyperGeoSegment)
    }
}
