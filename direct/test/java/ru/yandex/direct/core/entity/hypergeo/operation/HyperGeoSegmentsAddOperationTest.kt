package ru.yandex.direct.core.entity.hypergeo.operation

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentResponse
import ru.yandex.direct.audience.client.model.SegmentStatus.UPLOADED
import ru.yandex.direct.audience.client.model.geosegment.YaAudienceGeoSegmentType.REGULAR
import ru.yandex.direct.core.entity.hypergeo.operation.HyperGeoOperationsHelper.convertPoints
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.SEGMENT_NAME
import ru.yandex.direct.core.testing.data.SEGMENT_RADIUS
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.data.defaultHyperPoints
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.operation.Applicability.FULL
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoSegmentsAddOperationTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    @Autowired
    private lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var hyperGeoOperationsFactory: HyperGeoOperationsFactory

    private lateinit var clientInfo: ClientInfo
    private lateinit var userInfo: UserInfo
    private lateinit var clientId: ClientId
    private var shard = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        userInfo = clientInfo.chiefUserInfo!!
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
    }

    @Test
    fun prepareAndApply_PositiveCase_NoValidationErrors() {
        val hyperGeoSegment = defaultHyperGeoSegment(id = mockYaAudienceClient(), clientId = clientId.asLong())

        hyperGeoOperationsFactory.createHyperGeoSegmentsAddOperation(FULL, listOf(hyperGeoSegment), shard, clientInfo.login)
            .prepareAndApply()
            .validationResult
            .checkHasNoDefects()
    }

    @Test
    fun prepareAndApply_PositiveCase_HyperGeoSegmentAddedCorrectly() {
        val hyperGeoSegment = defaultHyperGeoSegment(id = mockYaAudienceClient(), clientId = clientId.asLong())

        hyperGeoOperationsFactory.createHyperGeoSegmentsAddOperation(FULL, listOf(hyperGeoSegment), shard, clientInfo.login)
            .prepareAndApply()

        hyperGeoSegmentRepository.getHyperGeoSegmentById(shard, clientId, listOf(hyperGeoSegment.id))
            .checkEquals(mapOf(hyperGeoSegment.id to hyperGeoSegment))
    }

    @Test
    fun prepareAndApply_NoSegmentDetailsProvided_ValidationError() {
        val hyperGeoSegment =
            defaultHyperGeoSegment(id = mockYaAudienceClient(), clientId = clientId.asLong(), segmentDetails = null)

        hyperGeoOperationsFactory.createHyperGeoSegmentsAddOperation(FULL, listOf(hyperGeoSegment), shard, clientInfo.login)
            .prepareAndApply()
            .validationResult
            .checkHasDefect(
                path = path(index(0), field("segmentDetails")),
                defectId = CANNOT_BE_NULL)
    }

    private fun mockYaAudienceClient(): Long {
        val hyperGeoSegmentId = randomPositiveLong(Int.MAX_VALUE.toLong())

        `when`(yaAudienceClient.createGeoSegment(
            eq(userInfo.login),
            eq(SEGMENT_NAME),
            eq(SEGMENT_RADIUS),
            eq(convertPoints(defaultHyperPoints())),
            eq(REGULAR),
            eq(null),
            eq(null)))
            .thenReturn(
                SegmentResponse()
                    .withSegment(
                        AudienceSegment()
                            .withId(hyperGeoSegmentId)
                            .withStatus(UPLOADED)))

        return hyperGeoSegmentId
    }
}

private fun <T> ValidationResult<T, Defect<*>>.checkHasNoDefects() = assertThat(this, hasNoDefectsDefinitions())
private fun <T> ValidationResult<T, Defect<*>>.checkHasDefect(path: Path, defectId: DefectId<*>) =
    assertThat(this, hasDefectDefinitionWith(validationError(path, defectId)))
