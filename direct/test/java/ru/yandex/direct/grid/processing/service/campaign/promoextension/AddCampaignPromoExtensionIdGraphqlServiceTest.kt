package ru.yandex.direct.grid.processing.service.campaign.promoextension

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.PromoExtensionInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns
import ru.yandex.direct.grid.processing.model.checkGdPromoextensionEqualsExpected
import ru.yandex.direct.grid.processing.model.promoextension.GdPromoExtensionStatus.ON_MODERATION
import ru.yandex.direct.grid.processing.util.CampaignTestDataUtils
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation
import ru.yandex.direct.grid.processing.util.KtGraphQLTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.processing.util.UserHelper
import ru.yandex.direct.test.utils.checkNull
import ru.yandex.direct.test.utils.checkSize

@GridProcessingTest
@RunWith(SpringRunner::class)
class AddCampaignPromoExtensionIdGraphqlServiceTest {
    private val MUTATION_NAME = "addCampaigns"
    private val MUTATION_TEMPLATE = """
        mutation {
          %s (input: %s) {
            validationResult {
              errors {
                code
                path
                params
              }
            }
            addedCampaigns {
              id
            }
          }
        }
    """
    private val ADD_CAMPAIGN_MUTATION = TemplateMutation(MUTATION_NAME, MUTATION_TEMPLATE,
        GdAddCampaigns::class.java, GdAddCampaignPayload::class.java
    )

    @Autowired
    private lateinit var processor: GraphQlTestExecutor

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignConstantsService: CampaignConstantsService

    private lateinit var operator: User
    private lateinit var promoExtensionInfo: PromoExtensionInfo

    @Before
    fun before() {
        val clientInfo = steps.clientSteps().createDefaultClient()
        operator = UserHelper.getUser(clientInfo.client)
        TestAuthHelper.setDirectAuthentication(operator)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(operator)
        promoExtensionInfo = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
    }

    @Test
    fun addTextCampaignsWithAndWithoutPromoExtensionIdSavedAndReturnedCorrectly() {
        val expectedPromoId = promoExtensionInfo.promoExtensionId
        val gdAddTextCampaignFirst =
            CampaignTestDataUtils.defaultGdAddTextCampaign(campaignConstantsService.defaultAttributionModel)
                .withPromoExtensionId(expectedPromoId)
        val gdAddTextCampaignSecond =
            CampaignTestDataUtils.defaultGdAddTextCampaign(campaignConstantsService.defaultAttributionModel)
                .withPromoExtensionId(null)
        val gdAddCampaignUnionFirst = GdAddCampaignUnion().withTextCampaign(gdAddTextCampaignFirst)
        val gdAddCampaignUnionSecond = GdAddCampaignUnion().withTextCampaign(gdAddTextCampaignSecond)
        val input = GdAddCampaigns().withCampaignAddItems(listOf(gdAddCampaignUnionFirst, gdAddCampaignUnionSecond))
        addCampaignsAndCheckPayload(input)
    }

    @Test
    fun addDynamicCampaignsWithAndWithoutPromoExtensionIdSavedAndReturnedCorrectly() {
        val expectedPromoId = promoExtensionInfo.promoExtensionId
        val gdAddDynamicCampaignFirst =
            CampaignTestDataUtils.defaultGdAddDynamicCampaign(campaignConstantsService.defaultAttributionModel)
                .withPromoExtensionId(expectedPromoId)
        val gdAddDynamicCampaignSecond =
            CampaignTestDataUtils.defaultGdAddDynamicCampaign(campaignConstantsService.defaultAttributionModel)
                .withPromoExtensionId(null)
        val gdAddCampaignUnionFirst = GdAddCampaignUnion().withDynamicCampaign(gdAddDynamicCampaignFirst)
        val gdAddCampaignUnionSecond = GdAddCampaignUnion().withDynamicCampaign(gdAddDynamicCampaignSecond)
        val input = GdAddCampaigns().withCampaignAddItems(listOf(gdAddCampaignUnionFirst, gdAddCampaignUnionSecond))
        addCampaignsAndCheckPayload(input)
    }

    private fun addCampaignsAndCheckPayload(input: GdAddCampaigns) {
        val gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input, operator)

        gdAddCampaignPayload.validationResult.checkNull()
        gdAddCampaignPayload.addedCampaigns.checkSize(2)
        val campaignIdFirst = gdAddCampaignPayload.addedCampaigns[0].id
        val campaignIdSecond = gdAddCampaignPayload.addedCampaigns[1].id

        val campaignPromoExtensionMap = ktGraphQLTestExecutor.getCampaignIdToPromoExtension(
            promoExtensionInfo.clientInfo.login, setOf(campaignIdFirst, campaignIdSecond)
        )
        softly {
            campaignPromoExtensionMap[campaignIdSecond].checkNull()
            campaignPromoExtensionMap[campaignIdFirst].checkGdPromoextensionEqualsExpected(
                promoExtensionInfo.promoExtension, ON_MODERATION, listOf(campaignIdFirst)
            )
        }
    }
}
