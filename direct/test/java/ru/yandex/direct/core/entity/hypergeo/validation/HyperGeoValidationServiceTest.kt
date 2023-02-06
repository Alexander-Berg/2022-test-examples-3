package ru.yandex.direct.core.entity.hypergeo.validation

import org.jooq.DSLContext
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.configuration.CoreTest
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegments
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.testing.steps.convertHyperGeoToSimple
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.wrapper.DslContextProvider
import ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith
import ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions
import ru.yandex.direct.testing.matchers.validation.Matchers.validationError
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Gen.MUST_BE_IN_COLLECTION
import ru.yandex.direct.validation.defect.ids.CollectionDefectIds.Size.SIZE_CANNOT_BE_LESS_THAN_MIN
import ru.yandex.direct.validation.defect.ids.StringDefectIds.CANNOT_BE_EMPTY
import ru.yandex.direct.validation.result.Defect
import ru.yandex.direct.validation.result.DefectId
import ru.yandex.direct.validation.result.DefectIds.CANNOT_BE_NULL
import ru.yandex.direct.validation.result.DefectIds.MUST_BE_VALID_ID
import ru.yandex.direct.validation.result.DefectIds.OBJECT_NOT_FOUND
import ru.yandex.direct.validation.result.Path
import ru.yandex.direct.validation.result.PathHelper.field
import ru.yandex.direct.validation.result.PathHelper.index
import ru.yandex.direct.validation.result.PathHelper.path
import ru.yandex.direct.validation.result.ValidationResult

@CoreTest
@RunWith(SpringJUnit4ClassRunner::class)
class HyperGeoValidationServiceTest {

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var dslContextProvider: DslContextProvider

    @Autowired
    private lateinit var hyperGeoValidationService: HyperGeoValidationService

    private var shard = 0

    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId
    private lateinit var dslContext: DSLContext
    private lateinit var anotherClientInfo: ClientInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        shard = clientInfo.shard
        dslContext = dslContextProvider.ppc(shard)
        anotherClientInfo = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun preValidateHyperGeos_PositiveCase() {
        val hyperGeos = defaultHyperGeo()
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoValidationService.preValidateHyperGeos(hyperGeos)
            .checkHasNoDefects()
    }

    @Test
    fun preValidateHyperGeos_BlankHyperGeoName_ValidationError() {
        val hyperGeos = defaultHyperGeo(name = "    ")
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoValidationService.preValidateHyperGeos(hyperGeos)
            .checkHasDefect(
                path = path(index(0), field("name")),
                defectId = CANNOT_BE_EMPTY
            )
    }

    @Test
    fun preValidateHyperGeos_EmptyHyperGeoSegments_ValidationError() {
        val hyperGeos = defaultHyperGeo(hyperGeoSegments = emptyList())
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoValidationService.preValidateHyperGeos(hyperGeos)
            .checkHasDefect(
                path = path(index(0), field("hyperGeoSegmentIds")),
                defectId = SIZE_CANNOT_BE_LESS_THAN_MIN
            )
    }

    @Test
    fun preValidateHyperGeos_HyperGeoSegmentWithBadId_ValidationError() {
        val hyperGeos = defaultHyperGeo(hyperGeoSegments = listOf(defaultHyperGeoSegment(id = -1L)))
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoValidationService.preValidateHyperGeos(hyperGeos)
            .checkHasDefect(
                path = path(index(0), field("hyperGeoSegmentIds"), index(0)),
                defectId = MUST_BE_VALID_ID
            )
    }

    @Test
    fun validateHyperGeos_PositiveCase() {
        val hyperGeoSegments = steps.hyperGeoSteps().createHyperGeoSegments(clientInfo, defaultHyperGeoSegments())
        val hyperGeos = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoValidationService.validateHyperGeos(shard, clientId, hyperGeos)
            .checkHasNoDefects()
    }

    @Test
    fun validateHyperGeos_HyperGeoSegmentBelongsToAnotherClient_ValidationError() {
        val hyperGeoSegments =
            steps.hyperGeoSteps().createHyperGeoSegments(anotherClientInfo, defaultHyperGeoSegments())
        val hyperGeos = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
            .putInList()
            .map { convertHyperGeoToSimple(it) }

        hyperGeoValidationService.validateHyperGeos(shard, clientId, hyperGeos)
            .checkHasDefect(
                path = path(index(0), field("hyperGeoSegmentIds"), index(0)),
                defectId = OBJECT_NOT_FOUND
            )
    }

    @Test
    fun validateDelete_Success() {
        val hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)

        hyperGeoValidationService
            .validateDelete(dslContext, clientId, listOf(hyperGeo.id))
            .checkHasNoDefects()

        hyperGeoValidationService
            .validateDelete(dslContext, null, listOf(hyperGeo.id))
            .checkHasNoDefects()
    }

    @Test
    fun validateDelete_NullIds() {
        hyperGeoValidationService
            .validateDelete(dslContext, clientId, null)
            .checkHasDefect(path = path(), defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateDelete_NullId() {
        hyperGeoValidationService
            .validateDelete(dslContext, clientId, listOf(null))
            .checkHasDefect(path = path(index(0)), defectId = CANNOT_BE_NULL)
    }

    @Test
    fun validateDelete_InvalidId() {
        val hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)

        hyperGeoValidationService
            .validateDelete(dslContext, clientId, listOf(hyperGeo.id, -10))
            .checkHasDefect(path = path(index(1)), defectId = MUST_BE_VALID_ID)
    }

    @Test
    fun validateDelete_UsedHyperGeoInAdGroup() {
        val hyperGeo = steps.hyperGeoSteps().createHyperGeo(clientInfo)
        val hyperGeo2 = steps.hyperGeoSteps().createHyperGeo(clientInfo)
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(clientInfo)
        val dslConfig = dslContext.configuration()
        steps.hyperGeoSteps().createHyperGeoLink(dslConfig, mapOf(adGroupInfo.adGroupId to hyperGeo.id))

        hyperGeoValidationService
            .validateDelete(dslContext, clientId, listOf(hyperGeo2.id, hyperGeo.id))
            .checkHasDefect(path = path(index(1)), defectId = MUST_BE_IN_COLLECTION)
    }
}

private fun <T> ValidationResult<T, Defect<*>>.checkHasNoDefects() = assertThat(this, hasNoDefectsDefinitions())
private fun <T> ValidationResult<T, Defect<*>>.checkHasDefect(path: Path, defectId: DefectId<*>) =
    assertThat(this, hasDefectDefinitionWith(validationError(path, defectId)))

private fun <T> T.putInList(): List<T> = listOf(this)
