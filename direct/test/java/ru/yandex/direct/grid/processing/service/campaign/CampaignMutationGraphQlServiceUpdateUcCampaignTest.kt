package ru.yandex.direct.grid.processing.service.campaign


import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
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
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.campaign.service.CampaignOperationService
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeo
import ru.yandex.direct.core.entity.hypergeo.model.HyperGeoSegment
import ru.yandex.direct.core.entity.hypergeo.validation.HyperGeoDefectIds
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.data.defaultHyperGeo
import ru.yandex.direct.core.testing.data.defaultHyperGeoSegment
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.repository.TestCampaignRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.dbutil.model.UidAndClientId
import ru.yandex.direct.feature.FeatureName.HYPERLOCAL_GEO_FOR_UC_CAMPAIGNS_ENABLED_FOR_DNA
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.data.defaultGdUpdateUcCampaignInput
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUcCampaignMutationPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateUcCampaignInput
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper

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

private val UPDATE_MUTATION = GraphQlTestExecutor.TemplateMutation(CampaignMutationGraphQlService.UPDATE_UC_CAMPAIGN,
    QUERY_TEMPLATE, GdUpdateUcCampaignInput::class.java, GdUcCampaignMutationPayload::class.java)

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class CampaignMutationGraphQlServiceUpdateUcCampaignTest {
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

    @Autowired
    lateinit var testCampaignRepository: TestCampaignRepository

    @Autowired
    lateinit var campaignOperationService: CampaignOperationService

    private var shard: Int = 0
    private var campaignId: Long = 0
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

        createUcCampaign()

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
     * Проверка обновления uc кампании с гипергео
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun updateUcCampaign(
        @Suppress("UNUSED_PARAMETER") description: String,
        multipleGeoSegmentsInHyperGeoForUcEnabled: Boolean,
        multipleGeoSegmentsInHyperGeoForTextCampaignEnabled: Boolean,
        hyperGeoMultiSegments: Boolean,
        expectUpdateCampaign: Boolean,
    ) {
        val hyperGeoId = if (hyperGeoMultiSegments) hyperGeoWithMultipleSegments.id else hyperGeo.id

        steps.featureSteps().addClientFeature(clientId,
            HYPERLOCAL_GEO_FOR_UC_CAMPAIGNS_ENABLED_FOR_DNA, multipleGeoSegmentsInHyperGeoForUcEnabled)

        val gdUpdateUcCampaignInput = defaultGdUpdateUcCampaignInput(campaignId, hyperGeoId)

        val payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, gdUpdateUcCampaignInput, operator)

        val soft = SoftAssertions()
        if (expectUpdateCampaign) {
            GraphQlTestExecutor.validateResponseSuccessful(payload)

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

    private fun createUcCampaign() {
        val ucCampaign = TestCampaigns.defaultTextCampaignWithSystemFields(clientInfo)
            .withAgencyId(null)
            .withClientId(null)
            .withWalletId(null)
            .withIsServiceRequested(null)
            .withAttributionModel(CampaignAttributionModel.LAST_CLICK)
            .withIsUniversal(true)

        val result = campaignOperationService.createRestrictedCampaignAddOperation(listOf(ucCampaign),
            clientInfo.uid, UidAndClientId.of(clientInfo.uid, clientId), CampaignOptions()).prepareAndApply()
        assertThat(result.successfulCount)
            .`as`("campaign added successfully")
            .isEqualTo(1)
        campaignId = result[0].result

        testCampaignRepository.setSource(shard, campaignId, CampaignSource.UAC)
    }

    private fun createHyperGeo(
        hyperGeoSegments: List<HyperGeoSegment> = listOf(defaultHyperGeoSegment())
    ): HyperGeo {
        val hyperGeo = defaultHyperGeo(hyperGeoSegments = hyperGeoSegments)
        return steps.hyperGeoSteps().createHyperGeo(clientInfo, hyperGeo)
    }
}
