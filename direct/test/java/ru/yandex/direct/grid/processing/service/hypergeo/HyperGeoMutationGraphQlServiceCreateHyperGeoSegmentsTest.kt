package ru.yandex.direct.grid.processing.service.hypergeo


import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.audience.client.YaAudienceClient
import ru.yandex.direct.audience.client.model.AudienceSegment
import ru.yandex.direct.audience.client.model.SegmentResponse
import ru.yandex.direct.audience.client.model.SegmentStatus
import ru.yandex.direct.audience.client.model.geosegment.YaAudienceGeoSegmentType
import ru.yandex.direct.core.entity.hypergeo.model.GeoSegmentType
import ru.yandex.direct.core.entity.hypergeo.operation.HyperGeoOperationsHelper
import ru.yandex.direct.core.entity.hypergeo.repository.HyperGeoSegmentRepository
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService.MAX_GEO_SEGMENT_PERIOD_LENGTH
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService.MAX_GEO_SEGMENT_TIMES_QUANTITY
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService.MIN_GEO_SEGMENT_PERIOD_LENGTH
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService.MIN_GEO_SEGMENT_TIMES_QUANTITY
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.PERIOD_LENGTH
import ru.yandex.direct.core.testing.data.SEGMENT_NAME
import ru.yandex.direct.core.testing.data.SEGMENT_RADIUS
import ru.yandex.direct.core.testing.data.TIMES_QUANTITY
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegmentDetails
import ru.yandex.direct.core.testing.data.defaultHyperPoints
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.core.util.HyperGeoUtils
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.data.defaultGdCreateHyperGeoSegment
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.hypergeo.GdGeoSegmentType
import ru.yandex.direct.grid.processing.model.hypergeo.GdGeoSegmentType.CONDITION
import ru.yandex.direct.grid.processing.model.hypergeo.mutation.createhypergeosegment.GdCreateHyperGeoSegments
import ru.yandex.direct.grid.processing.model.hypergeo.mutation.createhypergeosegment.GdCreateHyperGeoSegmentsPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.regions.Region.GLOBAL_REGION_ID
import ru.yandex.direct.test.utils.randomPositiveLong
import ru.yandex.direct.validation.defect.ids.NumberDefectIds.MUST_BE_IN_THE_INTERVAL_INCLUSIVE
import ru.yandex.direct.validation.result.DefectIds

private const val MUTATION_NAME = "createHyperGeoSegments"

private val QUERY_TEMPLATE = """
        mutation {
            %s (input: %s) {
                validationResult {
                    errors {
                        code
                        path
                        params
                    }
                }
                createdHyperGeoSegments {
                    id
                }
            }
        }
    """.trimIndent()

private val CREATE_MUTATION = GraphQlTestExecutor.TemplateMutation(
    MUTATION_NAME, QUERY_TEMPLATE, GdCreateHyperGeoSegments::class.java, GdCreateHyperGeoSegmentsPayload::class.java)

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class HyperGeoMutationGraphQlServiceCreateHyperGeoSegmentsTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    lateinit var processor: GridGraphQLProcessor

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    lateinit var hyperGeoSegmentRepository: HyperGeoSegmentRepository

    @Autowired
    private lateinit var yaAudienceClient: YaAudienceClient

    private var shard: Int = 1
    private lateinit var operator: User
    private lateinit var clientInfo: ClientInfo
    private lateinit var clientId: ClientId

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!
        shard = clientInfo.shard

        TestAuthHelper.setDirectAuthentication(operator)
    }

    fun parametrizedTestData(): MutableList<List<Any?>> {
        val parametrize: MutableList<List<Any?>> = mutableListOf()

        val periodLengthCannotBeNullDefect = GdDefect()
            .withCode(DefectIds.CANNOT_BE_NULL.code)
            .withPath("createHyperGeoSegmentItems[0].segmentDetails.periodLength")
        val timesQuantityMustBeNullDefect = GdDefect()
            .withCode(DefectIds.MUST_BE_NULL.code)
            .withPath("createHyperGeoSegmentItems[0].segmentDetails.timesQuantity")
        val periodLengthMustBeNullDefect = GdDefect()
            .withCode(DefectIds.MUST_BE_NULL.code)
            .withPath("createHyperGeoSegmentItems[0].segmentDetails.periodLength")

        // Без отправки типа GdGeoSegmentType
        parametrize.add(listOf("Создание сегмента гипергео без отправки типа", null, null, null, null))
        parametrize.add(listOf("Создание сегмента без отправки типа, с частотой посещений", null, PERIOD_LENGTH, null,
            periodLengthMustBeNullDefect
        ))
        parametrize.add(listOf("Создание сегмента без отправки типа, с периодом посещений", null, null, TIMES_QUANTITY,
            GdDefect()
                .withCode(DefectIds.MUST_BE_NULL.code)
                .withPath("createHyperGeoSegmentItems[0].segmentDetails.timesQuantity")
        ))
        parametrize.add(
            listOf("Создание сегмента без отправки типа, с периодом и частотой посещений",
                CONDITION, PERIOD_LENGTH, TIMES_QUANTITY, null),
        )

        // Без отправки period_length и times_quantity
        parametrize.addAll(GdGeoSegmentType.values()
            .map { type ->
                if (type == CONDITION) {
                    listOf("Создание сегмента $type гипергео", type, null, null, periodLengthCannotBeNullDefect)
                } else {
                    listOf("Создание сегмента $type гипергео", type, null, null, null)
                }
            }
        )

        parametrize.addAll(GdGeoSegmentType.values()
            .map { type ->
                if (type == CONDITION) {
                    listOf("Создание сегмента $type гипергео", type, null, null, periodLengthCannotBeNullDefect)
                } else {
                    listOf("Создание сегмента $type гипергео", type, null, null, null)
                }
            }
        )

        // С отправкой period_length и без times_quantity
        parametrize.addAll(GdGeoSegmentType.values()
            .map { type ->
                if (type == CONDITION) {
                    listOf("Создание сегмента CONDITION гипергео с периодом посещений", type, PERIOD_LENGTH, null,
                        GdDefect()
                            .withCode(DefectIds.CANNOT_BE_NULL.code)
                            .withPath("createHyperGeoSegmentItems[0].segmentDetails.timesQuantity")
                    )
                } else {
                    listOf("Создание сегмента $type гипергео с периодом посещений", type, PERIOD_LENGTH, null,
                        periodLengthMustBeNullDefect
                    )
                }
            }
        )

        // С отправкой times_quantity и без period_length
        parametrize.addAll(GdGeoSegmentType.values()
            .map { type ->
                if (type == CONDITION) {
                    listOf("Создание сегмента $type гипергео с частотой посещений", type, null, TIMES_QUANTITY,
                        periodLengthCannotBeNullDefect
                    )
                } else {
                    listOf("Создание сегмента $type гипергео с частотой посещений", type, null, TIMES_QUANTITY,
                        timesQuantityMustBeNullDefect
                    )
                }
            }
        )

        // С отправкой times_quantity и period_length
        parametrize.addAll(GdGeoSegmentType.values()
            .map { type ->
                if (type == CONDITION) {
                    listOf("Создание сегмента $type с частотой и периодом", type, PERIOD_LENGTH, TIMES_QUANTITY, null)
                } else {
                    listOf("Создание сегмента $type с частотой и периодом", type, PERIOD_LENGTH, TIMES_QUANTITY,
                        timesQuantityMustBeNullDefect
                    )
                }
            }
        )

        return parametrize
    }

    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun createHyperGeoSegment(
        @Suppress("UNUSED_PARAMETER") description: String,
        gdGeoSegmentType: GdGeoSegmentType?,
        periodLength: Int?,
        timesQuantity: Int?,
        gdDefect: GdDefect?,
    ) {
        val (hyperGeoSegmentId, payload) = getHyperGeoSegmentIdAndMutationPayload(gdGeoSegmentType, periodLength, timesQuantity)

        val soft = SoftAssertions()
        if (gdDefect != null) {

            soft.assertThat(payload.createdHyperGeoSegments).isEmpty()
            soft.assertThat(payload.validationResult.errors).contains(gdDefect)

        } else {

            GraphQlTestExecutor.validateResponseSuccessful(payload)

            val hyperGeoSegmentById = hyperGeoSegmentRepository.getHyperGeoSegmentById(
                shard, clientId, payload.createdHyperGeoSegments.map { it.id }.toSet())

            val expectGeoSegmentType =
                if (gdGeoSegmentType == null) GeoSegmentType.REGULAR
                else GdGeoSegmentType.toSource(gdGeoSegmentType)
            val expectHyperGeoSegment = getExpectedHyperGeoSegment(
                hyperGeoSegmentId = hyperGeoSegmentId,
                geoSegmentType = expectGeoSegmentType!!,
                periodLength = periodLength,
                timesQuantity = timesQuantity,
            )

            soft.assertThat(hyperGeoSegmentById)
                .`as`("Количество сегментов")
                .hasSize(1)
            soft.assertThat(hyperGeoSegmentById.values.firstOrNull())
                .`as`("Сегмент гипергео")
                .isEqualTo(expectHyperGeoSegment)
        }
        soft.assertAll()
    }

    /**
     * При попытке создать сегмент гипергео с периодом посещений меньшим минимума - ошибка валидации
     */
    @Test
    fun createHyperGeoSegment_withPeriodLengthUnderMinLimit_Error() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, MIN_GEO_SEGMENT_PERIOD_LENGTH - 1, TIMES_QUANTITY)

        val soft = SoftAssertions()
        soft.assertThat(payload.createdHyperGeoSegments).isEmpty()
        soft.assertThat(payload.validationResult.errors).contains(
            GdDefect()
                .withCode(MUST_BE_IN_THE_INTERVAL_INCLUSIVE.code)
                .withPath("createHyperGeoSegmentItems[0].segmentDetails.periodLength")
                .withParams(mapOf(
                    "max" to MAX_GEO_SEGMENT_PERIOD_LENGTH,
                    "min" to MIN_GEO_SEGMENT_PERIOD_LENGTH)
                )
        )
    }

    /**
     * При попытке создать сегмент гипергео с периодом посещений равным минимуму - сегмент создается
     */
    @Test
    fun createHyperGeoSegment_withMinPeriodLength() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, MIN_GEO_SEGMENT_PERIOD_LENGTH, TIMES_QUANTITY)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }

    /**
     * При попытке создать сегмент гипергео с периодом посещений равным максимуму - сегмент создается
     */
    @Test
    fun createHyperGeoSegment_withMaxPeriodLength() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, MAX_GEO_SEGMENT_PERIOD_LENGTH, TIMES_QUANTITY)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }

    /**
     * При попытке создать сегмент гипергео с периодом посещений большим минимума - ошибка валидации
     */
    @Test
    fun createHyperGeoSegment_withPeriodLengthOverMaxLimit_Error() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, MAX_GEO_SEGMENT_PERIOD_LENGTH + 1, TIMES_QUANTITY)

        val soft = SoftAssertions()
        soft.assertThat(payload.createdHyperGeoSegments).isEmpty()
        soft.assertThat(payload.validationResult.errors).contains(
            GdDefect()
                .withCode(MUST_BE_IN_THE_INTERVAL_INCLUSIVE.code)
                .withPath("createHyperGeoSegmentItems[0].segmentDetails.periodLength")
                .withParams(mapOf(
                    "max" to MAX_GEO_SEGMENT_PERIOD_LENGTH,
                    "min" to MIN_GEO_SEGMENT_PERIOD_LENGTH)
                )
        )
    }

    /**
     * При попытке создать сегмент гипергео с частотой посещений меньшим минимума - ошибка валидации
     */
    @Test
    fun createHyperGeoSegment_withTimeQuantityUnderMinLimit_Error() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, PERIOD_LENGTH, MIN_GEO_SEGMENT_TIMES_QUANTITY - 1)

        val soft = SoftAssertions()
        soft.assertThat(payload.createdHyperGeoSegments).isEmpty()
        soft.assertThat(payload.validationResult.errors).contains(
            GdDefect()
                .withCode(MUST_BE_IN_THE_INTERVAL_INCLUSIVE.code)
                .withPath("createHyperGeoSegmentItems[0].segmentDetails.timesQuantity")
                .withParams(mapOf(
                    "max" to MAX_GEO_SEGMENT_TIMES_QUANTITY,
                    "min" to MIN_GEO_SEGMENT_TIMES_QUANTITY)
                )
        )
        soft.assertAll()
    }

    /**
     * При попытке создать сегмент гипергео с частотой посещений равным минимуму - сегмент создается
     */
    @Test
    fun createHyperGeoSegment_withMinTimeQuantity() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, PERIOD_LENGTH, MIN_GEO_SEGMENT_TIMES_QUANTITY)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }

    /**
     * При попытке создать сегмент гипергео с частотой посещений равным максимуму - сегмент создается
     */
    @Test
    fun createHyperGeoSegment_withMaxTimeQuantity() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, PERIOD_LENGTH, MAX_GEO_SEGMENT_TIMES_QUANTITY)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }

    /**
     * При попытке создать сегмент гипергео с частотой посещений больши максимума - ошибка валидации
     */
    @Test
    fun createHyperGeoSegment_withTimeQuantityOverMaxLimit_Error() {
        val (_, payload) =
            getHyperGeoSegmentIdAndMutationPayload(CONDITION, PERIOD_LENGTH, MAX_GEO_SEGMENT_TIMES_QUANTITY + 1)

        val soft = SoftAssertions()
        soft.assertThat(payload.createdHyperGeoSegments).isEmpty()
        soft.assertThat(payload.validationResult.errors).contains(
            GdDefect()
                .withCode(MUST_BE_IN_THE_INTERVAL_INCLUSIVE.code)
                .withPath("createHyperGeoSegmentItems[0].segmentDetails.timesQuantity")
                .withParams(mapOf(
                    "max" to MAX_GEO_SEGMENT_TIMES_QUANTITY,
                    "min" to MIN_GEO_SEGMENT_TIMES_QUANTITY)
                )
        )
        soft.assertAll()
    }

    private fun getHyperGeoSegmentIdAndMutationPayload(
        gdGeoSegmentType: GdGeoSegmentType?,
        periodLength: Int?,
        timesQuantity: Int?,
    ): Pair<Long, GdCreateHyperGeoSegmentsPayload> {
        val yaAudienceGeoSegmentType =
            if (gdGeoSegmentType == null) YaAudienceGeoSegmentType.REGULAR
            else YaAudienceGeoSegmentType.fromTypedValue(gdGeoSegmentType.name.lowercase())
        val hyperGeoSegmentId = mockYaAudienceClient(yaAudienceGeoSegmentType, periodLength, timesQuantity)

        val gdCreateHyperGeoSegments = GdCreateHyperGeoSegments()
            .withCreateHyperGeoSegmentItems(listOf(
                defaultGdCreateHyperGeoSegment(
                    gdGeoSegmentType = gdGeoSegmentType,
                    periodLength = periodLength,
                    timesQuantity = timesQuantity,
                )))

        return Pair(
            hyperGeoSegmentId,
            graphQlTestExecutor.doMutationAndGetPayload(CREATE_MUTATION, gdCreateHyperGeoSegments, operator)
        )
    }

    private fun mockYaAudienceClient(
        yaAudienceGeoSegmentType: YaAudienceGeoSegmentType?,
        periodLength: Int?,
        timesQuantity: Int?,
    ): Long {
        val hyperGeoSegmentId = randomPositiveLong(Int.MAX_VALUE.toLong())

        Mockito.`when`(yaAudienceClient.createGeoSegment(
            eq(operator.login),
            eq(SEGMENT_NAME),
            eq(SEGMENT_RADIUS),
            eq(HyperGeoOperationsHelper.convertPoints(defaultHyperPoints())),
            eq(yaAudienceGeoSegmentType),
            eq(periodLength),
            eq(timesQuantity)))
            .thenReturn(
                SegmentResponse()
                    .withSegment(
                        AudienceSegment()
                            .withId(hyperGeoSegmentId)
                            .withStatus(SegmentStatus.UPLOADED)))

        return hyperGeoSegmentId
    }

    private fun getExpectedHyperGeoSegment(
        hyperGeoSegmentId: Long,
        geoSegmentType: GeoSegmentType,
        periodLength: Int?,
        timesQuantity: Int?,
    ) = defaultHyperGeoSegment(
        id = HyperGeoUtils.convertGeoSegmentIdToGoalId(hyperGeoSegmentId),
        clientId = clientId.asLong(),
        coveringGeo = listOf(GLOBAL_REGION_ID),
        segmentDetails = defaultHyperGeoSegmentDetails(
            geoSegmentType = geoSegmentType,
            periodLength = periodLength,
            timesQuantity = timesQuantity,
        )
    )
}
