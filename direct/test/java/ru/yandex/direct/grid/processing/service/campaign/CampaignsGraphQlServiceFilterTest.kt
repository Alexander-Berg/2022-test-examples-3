package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.Campaign
import ru.yandex.direct.core.entity.campaign.model.CampaignSource
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.model.campaign.GdCampaignSource
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.GdCampaignFilter
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQLUtils.set
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils

/**
 * Тесты на фильтрацию по полям при запросе кампаний.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignsGraphQlServiceFilterTest {

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
    private lateinit var campaignInfo3: CampaignInfo
    private lateinit var campaignInfo4: CampaignInfo
    private lateinit var clientInfo: ClientInfo

    @Before
    fun setUp() {
        clientInfo = steps.clientSteps().createDefaultClient()
        campaignInfo1 = steps.campaignSteps().createActiveCampaign(clientInfo)
        steps.campaignSteps().setCampaignProperty(campaignInfo1, Campaign.NAME, "First test campaign")
        steps.campaignSteps().setCampaignProperty(campaignInfo1, Campaign.SOURCE, CampaignSource.ZEN)
        campaignInfo2 = steps.campaignSteps().createActiveCampaign(clientInfo)
        steps.campaignSteps().setCampaignProperty(campaignInfo2, Campaign.NAME, "Second test campaign")
        steps.campaignSteps().setCampaignProperty(campaignInfo2, Campaign.SOURCE, CampaignSource.ZEN)
        campaignInfo3 = steps.campaignSteps().createActiveCampaign(clientInfo)
        steps.campaignSteps().setCampaignProperty(campaignInfo3, Campaign.SOURCE, CampaignSource.UAC)
        campaignInfo4 = steps.campaignSteps().createActiveCampaign(clientInfo)
        steps.campaignSteps().setCampaignProperty(campaignInfo4, Campaign.SOURCE, CampaignSource.API)
    }

    @Test
    fun campaigns_getFirstByName_success() {
        val filter = GdCampaignFilter()
                .withNameContains("First")
        val data = sendRequest(filter)

        val totalCount = GraphQLUtils.getDataValue<Int>(data, "client/campaigns/totalCount")
        val id = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).describedAs("totalCount").isEqualTo(1)
            it.assertThat(id).describedAs("id").isEqualTo(campaignInfo1.campaignId)
        }
    }

    @Test
    fun campaigns_getSecondByName_success() {
        val filter = GdCampaignFilter()
                .withNameContains("Second")
        val data = sendRequest(filter)

        val totalCount = GraphQLUtils.getDataValue<Int>(data, "client/campaigns/totalCount")
        val id = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).describedAs("totalCount").isEqualTo(1)
            it.assertThat(id).describedAs("id").isEqualTo(campaignInfo2.campaignId)
        }
    }

    @Test
    fun campaigns_getFirstByNameIns_success() {
        val filter = GdCampaignFilter()
                .withNameInMulti(set(set("First"), set("test"), set("campaign")))
        val data = sendRequest(filter)

        val totalCount = GraphQLUtils.getDataValue<Int>(data, "client/campaigns/totalCount")
        val id = GraphQLUtils.getDataValue<Long>(data, "client/campaigns/rowset/0/id")
        SoftAssertions.assertSoftly {
            it.assertThat(totalCount).describedAs("totalCount").isEqualTo(1)
            it.assertThat(id).describedAs("id").isEqualTo(campaignInfo1.campaignId)
        }
    }

    @Test
    fun campaigns_getFirstAndSecondByNameIns_success() {
        val filter = GdCampaignFilter()
                .withNameInMulti(set(set("First", "Second"), set("test"), set("campaign")))
        val data = sendRequest(filter)

        val totalCount = GraphQLUtils.getDataValue<Int>(data, "client/campaigns/totalCount")
        assertThat(totalCount).describedAs("totalCount").isEqualTo(2)
    }

    @Test
    fun campaigns_SoursInOneOption_success() {
        val filter = GdCampaignFilter()
            .withSourceIn(set(GdCampaignSource.ZEN))
        val data = sendRequest(filter)

        val sources = GraphQLUtils.getDataValue<ArrayList<LinkedHashMap<String, String>>>(data, "client/campaigns/rowset")
            .mapNotNull { it["source"] }
            .map { GdCampaignSource.valueOf(it) }

        softly {
            assertThat(sources).isNotEmpty()
            assertThat(sources).containsOnly(GdCampaignSource.ZEN)
        }
    }

    @Test
    fun campaigns_SoursInFewOption_success() {
        val filter = GdCampaignFilter()
            .withSourceIn(set(GdCampaignSource.ZEN, GdCampaignSource.UAC, GdCampaignSource.DC))
        val data = sendRequest(filter)

        val sources = GraphQLUtils.getDataValue<ArrayList<LinkedHashMap<String, String>>>(data, "client/campaigns/rowset")
            .mapNotNull { it["source"] }
            .map { GdCampaignSource.valueOf(it) }

        softly {
            assertThat(sources).isNotEmpty()
            assertThat(sources).isSubsetOf(GdCampaignSource.ZEN, GdCampaignSource.UAC, GdCampaignSource.DC)
        }
    }

    @Test
    fun campaigns_SoursNotInOneOption_success() {
        val filter = GdCampaignFilter()
            .withSourceNotIn(set(GdCampaignSource.ZEN))
        val data = sendRequest(filter)

        val sources = GraphQLUtils.getDataValue<ArrayList<LinkedHashMap<String, String>>>(data, "client/campaigns/rowset")
            .mapNotNull { it["source"] }
            .map { GdCampaignSource.valueOf(it) }

        softly {
            assertThat(sources).isNotEmpty()
            assertThat(sources).doesNotContain(GdCampaignSource.ZEN)
        }
    }

    @Test
    fun campaigns_SoursNotInFewOption_success() {
        val filter = GdCampaignFilter()
            .withSourceNotIn(set(GdCampaignSource.ZEN, GdCampaignSource.UAC))
        val data = sendRequest(filter)

        val sources = GraphQLUtils.getDataValue<ArrayList<LinkedHashMap<String, String>>>(data, "client/campaigns/rowset")
            .mapNotNull { it["source"] }
            .map { GdCampaignSource.valueOf(it) }

        softly {
            assertThat(sources).isNotEmpty()
            assertThat(sources).doesNotContain(GdCampaignSource.ZEN, GdCampaignSource.UAC)
        }
    }

    private fun sendRequest(filter: GdCampaignFilter): Map<String, Any> {
        val user = userService.getUser(clientInfo.uid)
        val context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null)
        val campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
        campaignsContainer.filter = filter
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
                totalCount
                rowset {
                  id
                  source
                }
              }
            }
          }
        """.trimIndent()
    }

}
