package ru.yandex.direct.core.entity.hypergeo.repository

import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.CLIENT_ID
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.randomPositiveLong

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoSegmentRepositoryTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var hyperGeoSegment: HyperGeoSegment
    private lateinit var hyperGeo: HyperGeo

    private var shard = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        hyperGeoSegment = defaultHyperGeoSegment()
        steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, listOf(hyperGeoSegment))
        hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)
    }

    @Test
    fun addHyperGeoSegments_AddingOneHyperGeoSegment() {
        val hyperGeoSegment = defaultHyperGeoSegment()
        hyperGeoSegmentRepository.addHyperGeoSegments(shard, listOf(hyperGeoSegment))

        hyperGeoSegmentRepository
            .getHyperGeoSegmentById(shard, ClientId.fromLong(CLIENT_ID), listOf(hyperGeoSegment.id))
            .checkEquals(mapOf(hyperGeoSegment.id to hyperGeoSegment))
    }

    @Test
    fun getHyperGeoSegmentById_GettingOneHyperGeoSegment() {
        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id))
            .checkEquals(mapOf(hyperGeoSegment.id to hyperGeoSegment))
    }

    @Test
    fun getHyperGeoSegmentById_GettingOneHyperGeoSegmentByWrongId_NothingReturned() {
        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(randomPositiveLong()))
            .checkEquals(mapOf())
    }

    @Test
    fun getUnusedHyperGeoSegmentIds_NoHyperGeo_GettingOneHyperGeoSegment() {
        hyperGeoSegmentRepository.getUnusedHyperGeoSegmentIds(shard, listOf(hyperGeoSegment.id))
            .checkEquals(setOf(hyperGeoSegment.id))
    }

    @Test
    fun getUnusedHyperGeoSegmentIds_HasHyperGeo_NothingReturned() {
        hyperGeoSegmentRepository.getUnusedHyperGeoSegmentIds(shard, hyperGeo.hyperGeoSegments.map { it.id })
            .checkEquals(setOf())
    }

    @Test
    fun getHyperGeoSegmentIdByHyperGeoId() {
        val hyperGeo2 = steps.hyperGeoSteps().createHyperGeo(clientInfo)
        val clientInfo2 = steps.clientSteps().createDefaultClient()
        val hyperGeo3 = steps.hyperGeoSteps().createHyperGeo(clientInfo2)
        val hyperGeoIds = listOf(hyperGeo.id, hyperGeo2.id, hyperGeo3.id)

        hyperGeoSegmentRepository.getHyperGeoSegmentIdsByHyperGeoId(shard, clientId, hyperGeoIds)
            .checkEquals(setOf(hyperGeo.hyperGeoSegments[0].id, hyperGeo2.hyperGeoSegments[0].id))
        hyperGeoSegmentRepository.getHyperGeoSegmentIdsByHyperGeoId(shard, null, hyperGeoIds)
            .checkEquals(
                setOf(
                    hyperGeo.hyperGeoSegments[0].id,
                    hyperGeo2.hyperGeoSegments[0].id,
                    hyperGeo3.hyperGeoSegments[0].id
                )
            )
    }

    @Test
    fun deleteHyperGeoSegmentById() {
        hyperGeoSegmentRepository.deleteHyperGeoSegmentById(shard, listOf(hyperGeoSegment.id))
        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id))
            .checkEquals(mapOf())
    }
}

private fun <T> T.checkEquals(expected: T?) = assertThat(this, equalTo(expected))
