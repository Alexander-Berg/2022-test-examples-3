package ru.yandex.direct.grid.processing.service.hypergeo

import com.nhaarman.mockitokotlin2.any
import graphql.ErrorType
import graphql.ExecutionResult
import junitparams.JUnitParamsRunner
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
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoSegmentValidationService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.PERIOD_LENGTH
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.data.defaultGdCreateHyperGeoSegment
import ru.yandex.direct.grid.processing.model.hypergeo.GdGeoSegmentType
import ru.yandex.direct.grid.processing.model.hypergeo.mutation.createhypergeosegment.GdCreateHyperGeoSegments
import ru.yandex.direct.grid.processing.model.hypergeo.mutation.createhypergeosegment.GdCreateHyperGeoSegmentsPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.randomPositiveLong

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
class HyperGeoMutationGraphQlServiceCreateHyperGeoSegmentsUsingClientLoginTest {
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
    private lateinit var yaAudienceClient: YaAudienceClient

    private var shard: Int = 1
    private lateinit var agencyOperator: User
    private lateinit var agencyClient: ClientInfo
    private lateinit var randomClient: ClientInfo

    @Before
    fun before() {
        val agencyClientInfo = steps.clientSteps().createDefaultAgency()
        agencyOperator = agencyClientInfo.chiefUserInfo!!.user!!
        agencyClient = steps.clientSteps().createClientUnderAgency(agencyClientInfo)
        shard = agencyClient.shard

        randomClient = steps.clientSteps().createDefaultClient()
    }

    @Test
    fun createHyperGeoSegment_UsingAgencyClientLogin_Positive() {
        TestAuthHelper.setDirectAuthentication(agencyOperator, agencyClient.chiefUserInfo!!.user!!)
        val (_, payload) = getHyperGeoSegmentIdAndMutationPayload(agencyClient.login, agencyOperator, agencyClient.chiefUserInfo!!.user!!)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }

    @Test
    fun createHyperGeoSegment_UsingRandomClientLogin_Negative() {
        TestAuthHelper.setDirectAuthentication(agencyOperator, randomClient.chiefUserInfo!!.user!!)
        val (_, result) = getHyperGeoSegmentIdAndMutation(randomClient.login, agencyOperator, randomClient.chiefUserInfo!!.user!!)
        val soft = SoftAssertions()
        soft.assertThat(!result.isDataPresent)
        soft.assertThat(result.errors.size).isEqualTo(1)
        soft.assertThat(result.errors[0].errorType).isEqualTo(ErrorType.DataFetchingException)
        soft.assertAll()
    }

    @Test
    fun createHyperGeoSegment_UsingNoClientLogin_Positive() {
        TestAuthHelper.setDirectAuthentication(agencyOperator)
        val (_, payload) = getHyperGeoSegmentIdAndMutationPayload(agencyOperator.login, agencyOperator, agencyOperator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)
    }


    private fun getHyperGeoSegmentIdAndMutationPayload(
        login: String,
        operator: User,
        subjectUser: User
    ): Pair<Long, GdCreateHyperGeoSegmentsPayload> {
        val hyperGeoSegmentId = mockYaAudienceClient(login)
        val gdCreateHyperGeoSegments = GdCreateHyperGeoSegments()
            .withCreateHyperGeoSegmentItems(listOf(
                defaultGdCreateHyperGeoSegment(
                    gdGeoSegmentType = GdGeoSegmentType.CONDITION,
                    periodLength = PERIOD_LENGTH,
                    timesQuantity = HyperGeoSegmentValidationService.MIN_GEO_SEGMENT_TIMES_QUANTITY
                )))

        return Pair(
            hyperGeoSegmentId,
            graphQlTestExecutor.doMutationAndGetPayload(CREATE_MUTATION, gdCreateHyperGeoSegments, operator, subjectUser)
        )
    }

    private fun getHyperGeoSegmentIdAndMutation(login: String, operator: User, subjectUser: User): Pair<Long, ExecutionResult> {
        val hyperGeoSegmentId = mockYaAudienceClient(login)
        val gdCreateHyperGeoSegments = GdCreateHyperGeoSegments()
            .withCreateHyperGeoSegmentItems(listOf(
                defaultGdCreateHyperGeoSegment(
                    gdGeoSegmentType = GdGeoSegmentType.CONDITION,
                    periodLength = PERIOD_LENGTH,
                    timesQuantity = HyperGeoSegmentValidationService.MIN_GEO_SEGMENT_TIMES_QUANTITY

                )))

        return Pair(
            hyperGeoSegmentId,
            graphQlTestExecutor.doMutation(CREATE_MUTATION, gdCreateHyperGeoSegments, operator, subjectUser)
        )
    }

    private fun mockYaAudienceClient(
        login: String
    ): Long {
        val hyperGeoSegmentId = randomPositiveLong(Int.MAX_VALUE.toLong())
        Mockito.`when`(yaAudienceClient.createGeoSegment(
            eq(login),
            any(),
            any(),
            any(),
            any(),
            any(),
            any()))
            .thenReturn(
                SegmentResponse()
                    .withSegment(
                        AudienceSegment()
                            .withId(hyperGeoSegmentId)
                            .withStatus(SegmentStatus.UPLOADED)
                    )
            )
        return hyperGeoSegmentId
    }
}
