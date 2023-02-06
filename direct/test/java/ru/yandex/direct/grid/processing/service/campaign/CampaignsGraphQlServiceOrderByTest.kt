package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderBy
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignOrderByField
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import java.time.LocalDate

/**
 * Тесты на сортировку по полям при запросе кампаний.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignsGraphQlServiceOrderByTest {

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var steps: Steps

    private lateinit var campaignInfo1: CampaignInfo
    private lateinit var campaignInfo2: CampaignInfo
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        campaignInfo1 = steps.campaignSteps().createActiveCampaign(clientInfo)
        campaignInfo2 = steps.campaignSteps().createActiveCampaign(clientInfo)
    }

    @Test
    fun campaigns_whenAscOrderByStartDate_success() {
        steps.campaignSteps().setCampaignProperty(campaignInfo1, Campaign.START_TIME, TOMORROW)
        steps.campaignSteps().setCampaignProperty(campaignInfo2, Campaign.START_TIME, AFTER_TOMORROW)

        val campaignOrderBy = GdCampaignOrderBy()
                .withField(GdCampaignOrderByField.START_DATE)
                .withOrder(Order.ASC)
        val data = sendRequest(listOf(campaignOrderBy))

        val firstId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        val secondId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/1/id")
        SoftAssertions.assertSoftly { 
            it.assertThat(firstId).describedAs("the first id").isEqualTo(campaignInfo1.campaignId)
            it.assertThat(secondId).describedAs("the second id").isEqualTo(campaignInfo2.campaignId)
        }
    }

    @Test
    fun campaigns_whenDescOrderByStartDate_success() {
        steps.campaignSteps().setCampaignProperty(campaignInfo1, Campaign.START_TIME, TOMORROW)
        steps.campaignSteps().setCampaignProperty(campaignInfo2, Campaign.START_TIME, AFTER_TOMORROW)

        val campaignOrderBy = GdCampaignOrderBy()
                .withField(GdCampaignOrderByField.START_DATE)
                .withOrder(Order.DESC)
        val data = sendRequest(listOf(campaignOrderBy))

        val firstId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        val secondId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/1/id")
        SoftAssertions.assertSoftly { 
            it.assertThat(firstId).describedAs("the first id").isEqualTo(campaignInfo2.campaignId)
            it.assertThat(secondId).describedAs("the second id").isEqualTo(campaignInfo1.campaignId)
        }
    }

    @Test
    fun campaigns_whenAscOrderByEndDate_success() {
        steps.campaignSteps().setCampaignProperty(campaignInfo1, Campaign.FINISH_TIME, TOMORROW)
        steps.campaignSteps().setCampaignProperty(campaignInfo2, Campaign.FINISH_TIME, AFTER_TOMORROW)

        val campaignOrderBy = GdCampaignOrderBy()
                .withField(GdCampaignOrderByField.END_DATE)
                .withOrder(Order.ASC)
        val data = sendRequest(listOf(campaignOrderBy))

        val firstId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        val secondId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/1/id")
        SoftAssertions.assertSoftly { 
            it.assertThat(firstId).describedAs("first id").isEqualTo(campaignInfo1.campaignId)
            it.assertThat(secondId).describedAs("second id").isEqualTo(campaignInfo2.campaignId)
        }
    }

    @Test
    fun campaigns_whenDescOrderByEndDate_success() {
        steps.campaignSteps().setCampaignProperty(campaignInfo1, Campaign.FINISH_TIME, TOMORROW)
        steps.campaignSteps().setCampaignProperty(campaignInfo2, Campaign.FINISH_TIME, AFTER_TOMORROW)

        val campaignOrderBy = GdCampaignOrderBy()
                .withField(GdCampaignOrderByField.END_DATE)
                .withOrder(Order.DESC)
        val data = sendRequest(listOf(campaignOrderBy))

        val firstId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        val secondId = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/1/id")
        SoftAssertions.assertSoftly { 
            it.assertThat(firstId).describedAs("first id").isEqualTo(campaignInfo2.campaignId)
            it.assertThat(secondId).describedAs("second id").isEqualTo(campaignInfo1.campaignId)
        }
    }

    private fun sendRequest(orderBy: List<GdCampaignOrderBy>): Map<String, Any> {
        val user = userService.getUser(clientInfo.uid)
        val context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null)
        val campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
        campaignsContainer.orderBy = orderBy
        val serializedContainer = GraphQlJsonUtils.graphQlSerialize(campaignsContainer)
        val query = String.format(QUERY_TEMPLATE, clientInfo.clientId, serializedContainer)
        gridContextProvider.gridContext = context
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        return result.getData()
    }

    private companion object {
        private val QUERY_TEMPLATE = """
          {
            client(searchBy: {id: %s}) {
              campaigns(input: %s) {
                rowset {
                  id
                }
              }
            }
          }
        """.trimIndent()
        private val TOMORROW = LocalDate.now().plusDays(1L)
        private val AFTER_TOMORROW = LocalDate.now().plusDays(2L)
    }

}
