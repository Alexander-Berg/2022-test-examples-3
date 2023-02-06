package ru.yandex.direct.grid.processing.service.campaign

import org.assertj.core.api.SoftAssertions
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.entity.user.service.UserService
import ru.yandex.direct.core.testing.data.TestCampaigns.activeTextCampaign
import ru.yandex.direct.core.testing.data.TestPlacements
import ru.yandex.direct.core.testing.data.campaign.TestTextCampaigns.fullTextCampaign
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.campaign.TextCampaignInfo
import ru.yandex.direct.core.testing.repository.TestPlacementRepository
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils

/**
 * Тесты на подзапрос за белым списком пейджей.
 */
@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignsGraphQlServiceAllowedPagesTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var placementRepository: TestPlacementRepository

    @Autowired
    private lateinit var userService: UserService

    private lateinit var campaignInfo: CampaignInfo
    private lateinit var clientInfo: ClientInfo

    @Test
    fun textcampaign_allowedPages_success() {
        val pageId = placementRepository.nextPageId + 42L;
        steps.placementSteps().addPlacement(TestPlacements.emptyPlacement("mail.ru",
            pageId))
        clientInfo = steps.clientSteps().createDefaultClient()
        var campInfo = TextCampaignInfo()
            .withTypedCampaign(fullTextCampaign())
            .withCampaign(activeTextCampaign(null, null).withAllowedPageIds(listOf(pageId)))
            .withClientInfo(clientInfo)
        campaignInfo = steps.campaignSteps().createCampaign(campInfo)

        val data = sendRequest()

        val domain = GraphQLUtils.getDataValue<String>(data, "client/campaigns/rowset/0/allowedPages/0/domain")
        SoftAssertions.assertSoftly {
            it.assertThat(domain).describedAs("domain").isEqualTo("mail.ru")
        }
    }

    private fun sendRequest(): Map<String, Any> {
        val user = userService.getUser(clientInfo.uid)
        val context = ContextHelper.buildContext(user).withFetchedFieldsReslover(null)
        val campaignsContainer = CampaignTestDataUtils.getDefaultCampaignsContainerInput()
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
                  ... on GdTextCampaign {
                    allowedPages {
                      id
                      domain
                    }
                  }
                }
              }
            }
          }
        """.trimIndent()
    }
}
