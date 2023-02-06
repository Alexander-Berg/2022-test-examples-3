package ru.yandex.direct.grid.processing.service.group

import org.apache.commons.lang3.RandomStringUtils
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType
import ru.yandex.direct.core.entity.adgroup.model.McBannerAdGroup
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestClients.defaultClient
import ru.yandex.direct.core.testing.info.MinusKeywordsPackInfo
import ru.yandex.direct.core.testing.info.campaign.McBannerCampaignInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.api.GdDefect
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddMcBannerAdGroup
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddMcBannerAdGroupItem
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.regions.Region.MOSCOW_REGION_ID
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy
import ru.yandex.direct.test.utils.randomNegativeLong
import ru.yandex.direct.test.utils.randomPositiveLong

private const val MUTATION_NAME = "addMcBannerAdGroups"

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
                addedAdGroupItems {
                    adGroupId
                }
            }
        }
    """.trimIndent()

private val MUTATION = GraphQlTestExecutor.TemplateMutation(MUTATION_NAME, QUERY_TEMPLATE,
        GdAddMcBannerAdGroup::class.java, GdAddAdGroupPayload::class.java)

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupGraphQlServiceAddMcBannerTest {

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    lateinit var processor: GridGraphQLProcessor

    @Autowired
    lateinit var steps: Steps

    @Autowired
    lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var graphQlTestExecutor: GraphQlTestExecutor

    private val adGroupName = RandomStringUtils.randomAlphanumeric(16)

    private var shard: Int = -1
    private lateinit var operator: User
    private lateinit var campaignInfo: McBannerCampaignInfo
    private lateinit var minusKeywordsPackInfo: MinusKeywordsPackInfo

    private val testMinusKeywords = listOf(
            RandomStringUtils.randomAlphanumeric(16),
            RandomStringUtils.randomAlphanumeric(16))
            .sorted()

    @Before
    fun before() {
        val clientInfo = steps.clientSteps().createClient(defaultClient())
        shard = clientInfo.shard
        operator = clientInfo.chiefUserInfo!!.user!!
        TestAuthHelper.setDirectAuthentication(operator)

        campaignInfo = steps.mcBannerCampaignSteps().createDefaultCampaign(clientInfo)
        minusKeywordsPackInfo = steps.minusKeywordsPackSteps().createMinusKeywordsPack(clientInfo)
    }

    @Test
    fun addMcBannerAdGroups_failure_incorrectAdGroupData() {
        val invalidCampaignId = randomNegativeLong()
        val gdMcBannerAdGroup = gdAddMcBannerAdGroup(GdAddMcBannerAdGroupItem()
                .withName(adGroupName)
                .withCampaignId(invalidCampaignId))

        val errors = graphQlTestExecutor.doMutation(MUTATION, gdMcBannerAdGroup, operator).errors

        assertThat(errors).isNotEmpty
    }

    @Test
    fun addMcBannerAdGroups_failure_nonExistentCampaign() {
        val nonExistentCampaignId = randomPositiveLong()
        val gdMcBannerAdGroup = gdAddMcBannerAdGroup(GdAddMcBannerAdGroupItem()
                .withName(adGroupName)
                .withCampaignId(nonExistentCampaignId))

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, gdMcBannerAdGroup, operator)

        SoftAssertions.assertSoftly {
            it.assertThat(payload.addedAdGroupItems).`is`(matchedBy(contains(nullValue())))
            it.assertThat(payload.validationResult.errors).contains(GdDefect()
                    .withCode(CampaignDefects.campaignNotFound().defectId().code)
                    .withPath("addItems[0]"))
        }
    }

    @Test
    fun addMcBannerAdGroups_success_partial() {
        val gdMcBannerAdGroup = gdAddMcBannerAdGroup(GdAddMcBannerAdGroupItem()
                .withName(adGroupName)
                .withCampaignId(campaignInfo.campaignId))

        val expectedAdGroup = McBannerAdGroup()
                .withType(AdGroupType.MCBANNER)
                .withName(adGroupName)
                .withCampaignId(campaignInfo.campaignId)

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, gdMcBannerAdGroup, operator)

        validateResponseSuccessful(payload)
        assertThat(payload.addedAdGroupItems).hasSize(1)

        val actualAdGroup = adGroupRepository.getAdGroups(shard, listOf(payload.addedAdGroupItems[0].adGroupId))[0]

        assertThat(actualAdGroup).`is`(matchedBy(
                beanDiffer(expectedAdGroup).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
    }

    @Test
    fun addMcBannerAdGroups_oneValid() {
        val nonExistingCampaign = randomPositiveLong()
        val gdMcBannerAdGroups = gdAddMcBannerAdGroup(
                GdAddMcBannerAdGroupItem()
                        .withName(adGroupName)
                        .withCampaignId(campaignInfo.campaignId),
                GdAddMcBannerAdGroupItem()
                        .withName(adGroupName)
                        .withCampaignId(nonExistingCampaign))

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, gdMcBannerAdGroups, operator)

        SoftAssertions.assertSoftly {
            it.assertThat(payload.addedAdGroupItems).`is`(matchedBy(contains(notNullValue(), nullValue())))
            it.assertThat(payload.validationResult.errors).contains(GdDefect()
                    .withCode(CampaignDefects.campaignNotFound().defectId().code)
                    .withPath("addItems[1]"))
        }
    }

    @Test
    fun addMcBannerAdGroups_differentClient_error() {
        val clientInfoAnother = steps.clientSteps().createDefaultClient()
        val campaignInfoAnother = steps.campaignSteps().createActiveTextCampaign(clientInfoAnother)

        val gdMcBannerAdGroup = gdAddMcBannerAdGroup(GdAddMcBannerAdGroupItem()
                .withName(adGroupName)
                .withCampaignId(campaignInfoAnother.campaignId))

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, gdMcBannerAdGroup, operator)

        SoftAssertions.assertSoftly {
            it.assertThat(payload.addedAdGroupItems).`is`(matchedBy(contains(nullValue())))
            it.assertThat(payload.validationResult.errors).contains(GdDefect()
                    .withCode(CampaignDefects.campaignNotFound().defectId().code)
                    .withPath("addItems[0]"))
        }
    }

    @Test
    fun addMcBannerAdGroup_success_complex() {
        val gdMcBannerAdGroup = gdAddMcBannerAdGroup(GdAddMcBannerAdGroupItem().apply {
            name = adGroupName
            campaignId = campaignInfo.campaignId
            regionIds = listOf(MOSCOW_REGION_ID.toInt())
            adGroupMinusKeywords = testMinusKeywords
            libraryMinusKeywordsIds = listOf(minusKeywordsPackInfo.minusKeywordPackId)
            keywords = listOf(GdUpdateAdGroupKeywordItem()
                    .withPhrase("phrase"))
            generalPrice = 123L.toBigDecimal()
            pageGroupTags = listOf()
            targetTags = listOf()
        })

        val expectedAdGroup = McBannerAdGroup().apply {
            type = AdGroupType.MCBANNER
            name = adGroupName
            campaignId = campaignInfo.campaignId
            minusKeywords = testMinusKeywords
            libraryMinusKeywordsIds = listOf(minusKeywordsPackInfo.minusKeywordPackId)
            geo = listOf(MOSCOW_REGION_ID)
        }

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, gdMcBannerAdGroup, operator)

        validateResponseSuccessful(payload)
        assertThat(payload.addedAdGroupItems).hasSize(1)

        val actualAdGroup = adGroupRepository.getAdGroups(shard, listOf(payload.addedAdGroupItems[0].adGroupId))[0]

        assertThat(actualAdGroup).`is`(matchedBy(
                beanDiffer(expectedAdGroup).useCompareStrategy(DefaultCompareStrategies.onlyExpectedFields())))
    }

    @Test
    fun addMcBannerAdGroup_nonExistentCampaign_error() {
        val nonExistingCampaign = randomPositiveLong()
        val gdMcBannerAdGroup = gdAddMcBannerAdGroup(GdAddMcBannerAdGroupItem().apply {
            name = adGroupName
            campaignId = nonExistingCampaign
            regionIds = listOf(MOSCOW_REGION_ID.toInt())
            adGroupMinusKeywords = testMinusKeywords
            libraryMinusKeywordsIds = listOf(minusKeywordsPackInfo.minusKeywordPackId)
            keywords = listOf(GdUpdateAdGroupKeywordItem()
                    .withPhrase("phrase"))
            generalPrice = 123L.toBigDecimal()
            pageGroupTags = listOf()
            targetTags = listOf()
        })

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, gdMcBannerAdGroup, operator)

        SoftAssertions.assertSoftly {
            it.assertThat(payload.addedAdGroupItems).isEmpty()
            it.assertThat(payload.validationResult.errors).contains(GdDefect()
                    .withCode(CampaignDefects.campaignNotFound().defectId().code)
                    .withPath("addItems[0]"))
        }
    }

    private fun gdAddMcBannerAdGroup(vararg items: GdAddMcBannerAdGroupItem): GdAddMcBannerAdGroup {
        return GdAddMcBannerAdGroup()
                .withAddItems(items.asList())
    }
}
