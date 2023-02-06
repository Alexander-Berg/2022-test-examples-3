package ru.yandex.direct.grid.processing.service.campaign

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoDefectIds
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName.HYPERLOCAL_GEO_FOR_UC_CAMPAIGNS_ENABLED_FOR_DNA
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.data.defaultGdAddUcCampaignInput
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddUcCampaignInput
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUcCampaignMutationPayload
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.campaign.CampaignMutationGraphQlService.ADD_UC_CAMPAIGN
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.test.utils.TestUtils.assumeThat

private val QUERY_TEMPLATE = """
        mutation {
            %s (input: %s) {
                result {
                    campaignId
                }
                validationResult {
                    errors {
                        code
                        path
                        params
                    }
                }
            }
        }
    """.trimIndent()

private val ADD_MUTATION = GraphQlTestExecutor.TemplateMutation(
    ADD_UC_CAMPAIGN, QUERY_TEMPLATE, GdAddUcCampaignInput::class.java, GdUcCampaignMutationPayload::class.java)

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class CampaignMutationGraphQlServiceAddUcCampaignTest {

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
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    lateinit var adGroupRepository: AdGroupRepository

    private var shard: Int = 1
    private lateinit var operator: User
    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var hyperGeo: HyperGeo
    private lateinit var hyperGeoWithMultipleSegments: HyperGeo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!
        shard = clientInfo.shard

        TestAuthHelper.setDirectAuthentication(operator)

        hyperGeo = createHyperGeo()
        hyperGeoWithMultipleSegments = createHyperGeo(listOf(defaultHyperGeoSegment(), defaultHyperGeoSegment()))
    }

    fun parametrizedTestData() = listOf(
        listOf("uc с гипергео", false, false, false, true),
        listOf("uc с мультисегментным гипергео", false, false, true, false),
        listOf("uc с мультисегментным гипергео и с фичей", true, false, true, true),
        listOf("uc с мультисегментным гипергео и с фичей для ТГО", false, true, true, false),
    )

    /**
     * Проверка добавления новой кампании с гипергео
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun addUcCampaign(
        @Suppress("UNUSED_PARAMETER") description: String,
        multipleGeoSegmentsInHyperGeoForUcEnabled: Boolean,
        multipleGeoSegmentsInHyperGeoForTextCampaignEnabled: Boolean,
        hyperGeoMultiSegments: Boolean,
        expectCreateCampaign: Boolean,
    ) {
        val hyperGeoId = if (hyperGeoMultiSegments) hyperGeoWithMultipleSegments.id else hyperGeo.id

        steps.featureSteps().addClientFeature(clientId,
            HYPERLOCAL_GEO_FOR_UC_CAMPAIGNS_ENABLED_FOR_DNA, multipleGeoSegmentsInHyperGeoForUcEnabled)

        val gdAddUcCampaignInput = defaultGdAddUcCampaignInput(hyperGeoId)

        val payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, gdAddUcCampaignInput, operator)

        val soft = SoftAssertions()
        if (expectCreateCampaign) {
            GraphQlTestExecutor.validateResponseSuccessful(payload)

            assumeThat(payload.result?.campaignId, CoreMatchers.notNullValue())

            val campaignId = payload.result.campaignId

            val adGroupIds = adGroupRepository.getAdGroupIdsByCampaignIds(shard, listOf(campaignId))
            val actualAdGroups = adGroupRepository.getAdGroups(shard, adGroupIds.getOrDefault(campaignId, emptyList()))

            soft.assertThat(actualAdGroups)
                .`as`("Количество групп у uc кампании")
                .hasSize(1)
            soft.assertThat(actualAdGroups.firstOrNull()?.hyperGeoId)
                .`as`("Гипергео группы")
                .isEqualTo(hyperGeoId)
        } else {
            val expectDefect = GdDefect()
                .withCode(HyperGeoDefectIds.Gen.COUNT_OF_SEGMENTS_MUST_BE_IN_INTERVAL.code)
                .withPath("hyperGeoId")
                .withParams(mapOf("minSize" to 1, "maxSize" to 1))

            soft.assertThat(payload.validationResult.errors)
                .`as`("Ошибка валидации")
                .containsExactlyInAnyOrder(expectDefect)
        }
        soft.assertAll()
    }

    private fun createHyperGeo(
        hyperGeoSegments: List<HyperGeoSegment> = listOf(defaultHyperGeoSegment())
    ): HyperGeo {
        val hyperGeo = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
        return steps.hyperGeoSteps().createHyperGeo(clientInfo, hyperGeo)
    }
}
