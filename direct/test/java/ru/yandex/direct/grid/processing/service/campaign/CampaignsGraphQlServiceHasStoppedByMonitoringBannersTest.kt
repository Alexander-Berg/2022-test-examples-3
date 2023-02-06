package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.DomainInfo
import ru.yandex.direct.core.testing.info.NewTextBannerInfo
import ru.yandex.direct.core.testing.repository.TestBsDeadDomainsRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignsGraphQlServiceHasStoppedByMonitoringBannersTest {

    private val queryTemplate = """
        {
          client(searchBy: {login: "%s"}) {
            campaigns(input: %s) {
              rowset {
                hasBannersStoppedByMonitoring
              }
            }
          }
        }
    """.trimIndent()

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var testBsDeadDomainsRepository: TestBsDeadDomainsRepository

    private lateinit var clientInfo: ClientInfo
    private lateinit var domainInfo: DomainInfo
    private lateinit var campaignInfo: CampaignInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        campaignInfo = steps.campaignSteps().createActiveCampaign(clientInfo)
        testBsDeadDomainsRepository.setStatusMetricaControl(campaignInfo, true)

        domainInfo = steps.domainSteps().createDomain(clientInfo.shard)
        testBsDeadDomainsRepository.addDeadDomain(domainInfo)
    }

    @Test
    fun getCampaignHasBannersStoppedByMonitoring_emptyCampaign() {
        assertThat(hasBannersStoppedByMonitoring(campaignInfo)).isFalse
    }

    @Test
    fun getCampaignHasBannersStoppedByMonitoring_activeBanner() {
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withCampaignInfo(campaignInfo))
        assertThat(hasBannersStoppedByMonitoring(campaignInfo)).isFalse
    }

    @Test
    fun getCampaignHasBannersStoppedByMonitoring_activeBannerDeadUrl() {
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withCampaignInfo(campaignInfo)
                .withBanner(fullTextBanner()
                        .withDomain(domainInfo.domain.domain)
                        .withDomainId(domainInfo.domainId)))
        assertThat(hasBannersStoppedByMonitoring(campaignInfo)).isTrue
    }

    @Test
    fun getCampaignHasBannersStoppedByMonitoring_campaignDisabledMetricaControl() {
        testBsDeadDomainsRepository.setStatusMetricaControl(campaignInfo, false);
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withCampaignInfo(campaignInfo)
                .withBanner(fullTextBanner()
                        .withDomain(domainInfo.domain.domain)
                        .withDomainId(domainInfo.domainId)))
        assertThat(hasBannersStoppedByMonitoring(campaignInfo)).isFalse
    }

    @Test
    fun getCampaignHasBannersStoppedByMonitoring_nonModeratedBannerDeadUrl() {
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withCampaignInfo(campaignInfo)
                .withBanner(fullTextBanner()
                        .withStatusModerate(BannerStatusModerate.NEW)
                        .withDomain(domainInfo.domain.domain)
                        .withDomainId(domainInfo.domainId)))
        assertThat(hasBannersStoppedByMonitoring(campaignInfo)).isFalse
    }

    @Test
    fun getCampaignHasBannersStoppedByMonitoring_twoBanners() {
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withCampaignInfo(campaignInfo)
                .withBanner(fullTextBanner()))
        steps.textBannerSteps().createBanner(NewTextBannerInfo()
                .withCampaignInfo(campaignInfo)
                .withBanner(fullTextBanner()
                        .withDomain(domainInfo.domain.domain)
                        .withDomainId(domainInfo.domainId)))
        assertThat(hasBannersStoppedByMonitoring(campaignInfo)).isTrue
    }

    private fun hasBannersStoppedByMonitoring(campaignInfo: CampaignInfo): Boolean {
        val rowset = GraphQLUtils.getDataValue<List<Any>>(query(campaignInfo), "client/campaigns/rowset")
        Assume.assumeThat("Got exaclty one campaign", rowset.size, `is`(1));
        return GraphQLUtils.getDataValue(rowset[0], "hasBannersStoppedByMonitoring")
    }

    private fun query(campaignInfo: CampaignInfo): Map<String, Any> {
        val user = userService.getUser(clientInfo.uid)
        val context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null)
        val campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
                .withFilter(GdCampaignFilter().withCampaignIdIn(setOf(campaignInfo.campaignId)))
        val serializedContainer = GraphQlJsonUtils.graphQlSerialize(campaignsContainer)
        val query = String.format(queryTemplate, clientInfo.login, serializedContainer)
        gridContextProvider.gridContext = context
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        return result.getData()
    }
}
