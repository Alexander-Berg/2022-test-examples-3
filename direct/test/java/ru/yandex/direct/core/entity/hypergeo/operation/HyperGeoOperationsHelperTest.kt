package ru.yandex.direct.core.entity.hypergeo.operation

import com.nhaarman.mockitokotlin2.eq
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.exception.YaAudienceClientException
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.util.HyperGeoUtils.convertGoalIdToGeoSegment

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoOperationsHelperTest {
    @Autowired
    private lateinit var hyperGeoOperationsHelper: HyperGeoOperationsHelper

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    private var hyperGeoSegmentGoalId1 = 2022792925L
    private var hyperGeoSegmentId1 = convertGoalIdToGeoSegment(hyperGeoSegmentGoalId1)
    private var hyperGeoSegmentGoalId2 = 2022792926L
    private var hyperGeoSegmentId2 = convertGoalIdToGeoSegment(hyperGeoSegmentGoalId2)
    private var hyperGeoSegmentGoalIds: List<Long> = listOf(hyperGeoSegmentGoalId1, hyperGeoSegmentGoalId2)

    @Before
    fun before() {
        mockYaAudienceClient()
    }

    private fun mockYaAudienceClient() {
        Mockito.`when`(yaAudienceClient.deleteSegment(eq(hyperGeoSegmentId1)))
            .thenReturn(true)
        Mockito.`when`(yaAudienceClient.deleteSegment(eq(hyperGeoSegmentId2)))
            .thenReturn(false)
    }

    @Test
    fun deleteGeoSegments_AllSegmentsDeletedSuccessfully() {
        Mockito.`when`(yaAudienceClient.deleteSegment(eq(hyperGeoSegmentId2)))
            .thenReturn(true)
        val deletedIds = hyperGeoOperationsHelper.deleteGeoSegments(hyperGeoSegmentGoalIds)
        Assertions.assertThat(deletedIds).containsExactlyInAnyOrder(hyperGeoSegmentGoalId1, hyperGeoSegmentGoalId2)
    }

    @Test
    fun deleteGeoSegments_OneSegmentDeletedSuccessfully() {
        val deletedIds = hyperGeoOperationsHelper.deleteGeoSegments(hyperGeoSegmentGoalIds)
        Assertions.assertThat(deletedIds).isEqualTo(listOf(hyperGeoSegmentGoalId1))
    }

    @Test
    fun deleteGeoSegments_NoSegmentsDeletedSuccessfully() {
        Mockito.`when`(yaAudienceClient.deleteSegment(eq(hyperGeoSegmentId1)))
            .thenReturn(false)
        val deletedIds = hyperGeoOperationsHelper.deleteGeoSegments(hyperGeoSegmentGoalIds)
        Assertions.assertThat(deletedIds).isNull()
    }

    @Test
    fun deleteGeoSegments_WithException() {
        Mockito.`when`(yaAudienceClient.deleteSegment(eq(hyperGeoSegmentId1)))
            .thenThrow(YaAudienceClientException("Error"))

        val deletedIds = hyperGeoOperationsHelper.deleteGeoSegments(hyperGeoSegmentGoalIds)
        Assertions.assertThat(deletedIds).isNull()
    }
}
