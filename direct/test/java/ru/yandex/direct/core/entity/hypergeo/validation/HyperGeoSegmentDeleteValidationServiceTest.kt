package ru.yandex.direct.core.entity.hypergeo.validation

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.testing.matchers.validation.Matchers
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.CollectionDefects.inCollection
import ru.yandex.direct.validation.defect.CommonDefects.notNull
import ru.yandex.direct.validation.defect.CommonDefects.validId
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoSegmentDeleteValidationServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var hyperGeoSegmentValidationService: HyperGeoSegmentValidationService

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var hyperGeoSegment: HyperGeoSegment
    private lateinit var hyperGeoSegment2: HyperGeoSegment

    private var shard = 0

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        hyperGeoSegment = defaultHyperGeoSegment()
        hyperGeoSegment2 = defaultHyperGeoSegment()
        steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, listOf(hyperGeoSegment, hyperGeoSegment2))
    }

    @Test
    fun validateDelete_Success() {
        val validationResult = hyperGeoSegmentValidationService
            .validateDelete(shard, listOf(hyperGeoSegment.id, hyperGeoSegment2.id))
        Assertions.assertThat(validationResult)
            .`is`(matchedBy(Matchers.hasNoDefectsDefinitions<Any>()))
    }

    @Test
    fun validateDelete_NullSegmentIds() {
        val validationResult = hyperGeoSegmentValidationService
            .validateDelete(shard, null)
        Assertions.assertThat(validationResult)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(validationError(path(), notNull()))))
    }

    @Test
    fun validateDelete_NullSegmentId() {
        val validationResult = hyperGeoSegmentValidationService
            .validateDelete(shard, listOf(hyperGeoSegment2.id, null, hyperGeoSegment.id))
        Assertions.assertThat(validationResult)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(validationError(path(index(1)), notNull()))))
    }

    @Test
    fun validateDelete_InvalidSegmentId() {
        val validationResult = hyperGeoSegmentValidationService
            .validateDelete(shard, listOf(-10, hyperGeoSegment2.id))
        Assertions.assertThat(validationResult)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(validationError(path(index(0)), validId()))))
    }

    @Test
    fun validateDelete_UsedSegmentIdInHyperGeo() {
        val hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)
        val validationResult = hyperGeoSegmentValidationService
            .validateDelete(shard, listOf(hyperGeoSegment.id, hyperGeo.hyperGeoSegments[0].id))
        Assertions.assertThat(validationResult)
            .`is`(matchedBy(hasDefectWithDefinition<Any>(validationError(path(index(1)), inCollection()))))
    }
}
