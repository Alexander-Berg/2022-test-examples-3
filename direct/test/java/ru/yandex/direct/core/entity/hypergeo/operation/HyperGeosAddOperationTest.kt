package ru.yandex.direct.core.entity.hypergeo.operation

import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoRepository
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.convertHyperGeoToSimple
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.operation.Applicability.FULL
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeosAddOperationTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var hyperGeoRepository: HyperGeoRepository

    @Autowired
    private lateinit var hyperGeoOperationsFactory: HyperGeoOperationsFactory

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private var shard = 0

    private lateinit var anotherClientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard

        anotherClientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun prepareAndApply_PositiveCase_NoValidationErrors() {
        val hyperGeo = defaultHyperGeo()
        steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, hyperGeo.hyperGeoSegments)
        val hyperGeos = hyperGeo
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoOperationsFactory.createHyperGeosAddOperation(FULL, hyperGeos, shard, clientId)
            .prepareAndApply()
            .validationResult
            .checkHasNoDefects()
    }

    @Test
    fun prepareAndApply_PositiveCase_HyperGeoAddedCorrectly() {
        val hyperGeo = defaultHyperGeo()
        steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, hyperGeo.hyperGeoSegments)
        val hyperGeos = hyperGeo
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        val addedHyperGeoId =
            hyperGeoOperationsFactory.createHyperGeosAddOperation(FULL, hyperGeos, shard, clientId)
                .prepareAndApply()
                .result[0].result
        hyperGeo.withId(addedHyperGeoId)

        hyperGeoRepository.getHyperGeoById(shard, clientId, listOf(addedHyperGeoId))
            .checkEquals(mapOf(addedHyperGeoId to hyperGeo))
    }

    @Test
    fun prepareAndApply_HyperGeoSegmentNotExists_ValidationError() {
        val hyperGeoRetargetingConditions = defaultHyperGeo()
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoOperationsFactory.createHyperGeosAddOperation(FULL, hyperGeoRetargetingConditions, shard, clientId)
            .prepareAndApply()
            .validationResult
            .checkHasDefect(
                path = path(index(0), field("hyperGeoSegmentIds"), index(0)),
                defectId = DefectIds.OBJECT_NOT_FOUND)
    }

    @Test
    fun prepareAndApply_HyperGeoSegmentBelongsToAnotherClient_ValidationError() {
        val hyperGeo = defaultHyperGeo()
        steps.hyperGeoSteps().createHyperGeoSegments(anotherClientInfo, hyperGeo.hyperGeoSegments)
        val hyperGeoRetargetingConditions = hyperGeo
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoOperationsFactory.createHyperGeosAddOperation(FULL, hyperGeoRetargetingConditions, shard, clientId)
            .prepareAndApply()
            .validationResult
            .checkHasDefect(
                path = path(index(0), field("hyperGeoSegmentIds"), index(0)),
                defectId = DefectIds.OBJECT_NOT_FOUND)
    }
}

private fun <T> T.putInList(): List<T> = listOf(this)

private fun <T> ValidationResult<T, Defect<*>>.checkHasNoDefects() = assertThat(this, hasNoDefectsDefinitions())
private fun <T> ValidationResult<T, Defect<*>>.checkHasDefect(path: Path, defectId: DefectId<*>) =
    assertThat(this, hasDefectDefinitionWith(validationError(path, defectId)))
