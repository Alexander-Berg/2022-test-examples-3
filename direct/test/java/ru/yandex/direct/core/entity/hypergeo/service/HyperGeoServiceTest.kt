package ru.yandex.direct.core.entity.hypergeo.service

import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anySet
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentResponse
import ru.yandex.direct.audience.client.model.SegmentStatus.UPLOADED
import ru.yandex.direct.audience.client.model.geosegment.YaAudienceGeoSegmentType.REGULAR
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegmentDetails
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.util.HyperGeoUtils.convertGeoSegmentIdToGoalId
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.regions.Region.GLOBAL_REGION_ID
import ru.yandex.direct.test.utils.randomPositiveLong

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoService: HyperGeoService

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var userInfo: UserInfo
    private lateinit var user: User
    private var hyperGeoSegmentId: Long = 0
    private var shard = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        userInfo = clientInfo.chiefUserInfo!!
        user = userInfo.user!!
        shard = clientInfo.shard

        val hyperGeoSegmentGoalId = mockYaAudienceClient()
        hyperGeoSegmentId = convertGeoSegmentIdToGoalId(hyperGeoSegmentGoalId)
    }

    private fun mockYaAudienceClient(): Long {
        val hyperGeoSegmentId = randomPositiveLong(Int.MAX_VALUE.toLong())

        `when`(
            yaAudienceClient.createGeoSegment(
                anyString(), anyString(), anyInt(), anySet(), eq(REGULAR), eq(null), eq(null)
            )
        )
            .thenReturn(
                SegmentResponse()
                    .withSegment(
                        AudienceSegment()
                            .withId(hyperGeoSegmentId)
                            .withStatus(UPLOADED)
                    )
            )

        `when`(yaAudienceClient.deleteSegment(anyLong()))
            .thenReturn(true)

        return hyperGeoSegmentId
    }

    @Test
    fun createHyperGeoSegments_PositiveCase_HyperGeoSegmentAddedCorrectly() {
        val hyperGeoSegmentDetails = defaultHyperGeoSegmentDetails()
        hyperGeoService.createHyperGeoSegments(clientId, clientInfo.login, listOf(hyperGeoSegmentDetails))

        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, hyperGeoSegmentId.putInList())
            .checkEquals(
                mapOf(
                    hyperGeoSegmentId to defaultHyperGeoSegment(
                        id = hyperGeoSegmentId,
                        clientId = clientId.asLong(),
                        coveringGeo = listOf(GLOBAL_REGION_ID),
                        segmentDetails = hyperGeoSegmentDetails
                    )
                )
            )
    }

    @Test
    fun deleteHyperGeoSegments_PositiveCase_HyperGeoSegmentDeletedCorrectly() {
        val hyperGeoSegment = defaultHyperGeoSegment()
        steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, listOf(hyperGeoSegment))
        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id))
            .checkEquals(mapOf(hyperGeoSegment.id to hyperGeoSegment))

        hyperGeoService.deleteHyperGeoSegments(shard, listOf(hyperGeoSegment.id))

        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id))
            .checkEquals(mapOf())
    }

    @Test
    fun deleteHyperGeos_PositiveCase_HyperGeoDeletedCorrectly() {
        val hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)
        hyperGeoSegmentRepository.getHyperGeoSegmentIdsByHyperGeoId(shard, clientId, listOf(hyperGeo.id))
            .checkEquals(hyperGeo.hyperGeoSegments.map { it.id }.toSet())

        hyperGeoService.deleteHyperGeos(shard, clientId, listOf(hyperGeo.id))

        hyperGeoSegmentRepository.getHyperGeoSegmentIdsByHyperGeoId(shard, clientId, listOf(hyperGeo.id))
            .checkEquals(setOf())
    }
}

private fun <T> T.checkEquals(expected: T) = assertThat(this, equalTo(expected))

private fun <T> T.putInList(): List<T> = listOf(this)
