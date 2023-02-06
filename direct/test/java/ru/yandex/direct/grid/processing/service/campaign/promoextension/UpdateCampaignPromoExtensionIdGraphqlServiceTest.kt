package ru.yandex.direct.grid.processing.service.campaign.promoextension

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.campaign.model.BaseCampaign
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign
import ru.yandex.direct.core.entity.campaign.model.TextCampaign
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository
import ru.yandex.direct.core.entity.campaign.service.validation.CampaignConstantsService
import ru.yandex.direct.core.entity.promoextension.PromoExtensionRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.PromoExtensionInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaignUnion
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdAddCampaigns
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignPayload
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaignUnion
import ru.yandex.direct.grid.processing.model.campaign.mutation.GdUpdateCampaigns
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
class UpdateCampaignPromoExtensionIdGraphqlServiceTest {
    private val ADD_MUTATION_NAME = "addCampaigns"
    private val UPDATE_MUTATION_NAME = "updateCampaigns"
    private val ADD_MUTATION_TEMPLATE = """
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
    private val UPDATE_MUTATION_TEMPLATE = """
        mutation {
          %s (input: %s) {
            validationResult {
              errors {
                code
                path
                params
              }
            }
            updatedCampaigns {
              id
            }
          }
        }
    """

    private val ADD_CAMPAIGN_MUTATION = TemplateMutation(ADD_MUTATION_NAME, ADD_MUTATION_TEMPLATE,
        GdAddCampaigns::class.java, GdAddCampaignPayload::class.java)
    private val UPDATE_CAMPAIGN_MUTATION = TemplateMutation(UPDATE_MUTATION_NAME, UPDATE_MUTATION_TEMPLATE,
        GdUpdateCampaigns::class.java, GdUpdateCampaignPayload::class.java)

    @Autowired
    private lateinit var campaignTypedRepository: CampaignTypedRepository

    @Autowired
    private lateinit var promoExtensionRepository: PromoExtensionRepository

    @Autowired
    private lateinit var processor: GraphQlTestExecutor

    @Autowired
    private lateinit var ktGraphQLTestExecutor: KtGraphQLTestExecutor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var campaignConstantsService: CampaignConstantsService

    private lateinit var defaultAttributionModel: CampaignAttributionModel

    private lateinit var clientInfo: ClientInfo
    private lateinit var operator: User
    private lateinit var promoExtensionInfoFirst: PromoExtensionInfo
    private lateinit var promoExtensionInfoSecond: PromoExtensionInfo

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        operator = UserHelper.getUser(clientInfo.client)
        TestAuthHelper.setDirectAuthentication(operator)
        ktGraphQLTestExecutor.withDefaultGraphQLContext(operator)
        promoExtensionInfoFirst = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        promoExtensionInfoSecond = steps.promoExtensionSteps().createDefaultPromoExtension(clientInfo)
        defaultAttributionModel = campaignConstantsService.defaultAttributionModel
    }

    @Test
    fun updateTextNullToSecondPromoIdTest() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            null, promoExtensionInfoSecond.promoExtensionId
        )
    }

    @Test
    fun updateTextNullToFirstPromoIdTest() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            null, promoExtensionInfoFirst.promoExtensionId
        )
    }

    @Test
    fun updateTextNullToNullTest() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            null, null
        )
    }

    @Test
    fun updateTextSecondPromoIdToNullTest() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoSecond.promoExtensionId, null
        )
    }

    @Test
    fun updateTextSecondPromoIdToFirstPromoIdTest() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoSecond.promoExtensionId, promoExtensionInfoFirst.promoExtensionId
        )
    }

    @Test
    fun updateTextFirstPromoIdTestToSecondPromoId() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoFirst.promoExtensionId, promoExtensionInfoSecond.promoExtensionId
        )
    }

    @Test
    fun updateTextFirstPromoIdToFirstPromoIdTest() {
        updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoFirst.promoExtensionId, promoExtensionInfoFirst.promoExtensionId
        )
    }

    @Test
    fun updateDynamicNullToSecondPromoIdTest() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            null, promoExtensionInfoSecond.promoExtensionId
        )
    }

    @Test
    fun updateDynamicNullToFirstPromoIdTest() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            null, promoExtensionInfoFirst.promoExtensionId
        )
    }

    @Test
    fun updateDynamicNullToNullTest() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            null, null
        )
    }

    @Test
    fun updateDynamicSecondPromoIdToNullTest() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoSecond.promoExtensionId, null
        )
    }

    @Test
    fun updateDynamicSecondPromoIdToFirstPromoIdTest() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoSecond.promoExtensionId, promoExtensionInfoFirst.promoExtensionId
        )
    }

    @Test
    fun updateDynamicFirstPromoIdTestToSecondPromoId() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoFirst.promoExtensionId, promoExtensionInfoSecond.promoExtensionId
        )
    }

    @Test
    fun updateDynamicFirstPromoIdToFirstPromoIdTest() {
        updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
            promoExtensionInfoFirst.promoExtensionId, promoExtensionInfoFirst.promoExtensionId
        )
    }

    private fun updateTextCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
        idForAdd: Long?,
        idForUpdate: Long?,
    ) {
        val gdAddTextCampaignIgnored = CampaignTestDataUtils.defaultGdAddTextCampaign(defaultAttributionModel).withPromoExtensionId(null)
        val gdAddTextCampaign = CampaignTestDataUtils.defaultGdAddTextCampaign(defaultAttributionModel).withPromoExtensionId(idForAdd)
        val inputForAdd = GdAddCampaigns().withCampaignAddItems(listOf(
            GdAddCampaignUnion().withTextCampaign(gdAddTextCampaignIgnored),
            GdAddCampaignUnion().withTextCampaign(gdAddTextCampaign),
            GdAddCampaignUnion().withTextCampaign(gdAddTextCampaignIgnored),
        ))

        val campaignIds = addCampaignsAndCheckPayload(inputForAdd)

        val (firstInDb, secondInDb, thirdInDb) = getCampaigns(campaignIds)

        val inputForUpdate = GdUpdateCampaigns().withCampaignUpdateItems(listOf(
            GdUpdateCampaignUnion().withTextCampaign(
                CampaignTestDataUtils.defaultGdUpdateTextCampaign(firstInDb as TextCampaign, defaultAttributionModel)
                    .withPromoExtensionId(null)
            ),
            GdUpdateCampaignUnion().withTextCampaign(
                CampaignTestDataUtils.defaultGdUpdateTextCampaign(secondInDb as TextCampaign, defaultAttributionModel)
                    .withPromoExtensionId(idForUpdate)
            ),
            GdUpdateCampaignUnion().withTextCampaign(
                CampaignTestDataUtils.defaultGdUpdateTextCampaign(thirdInDb as TextCampaign, defaultAttributionModel)
                    .withPromoExtensionId(promoExtensionInfoSecond.promoExtensionId)
            ),
        ))

        updateCampaignsAndCheckPayload(inputForUpdate, campaignIds, idForUpdate)
    }

    private fun updateDynamicCampaignPromoExtensionIdSavedAndReturnedCorrectlyInternal(
        idForAdd: Long?,
        idForUpdate: Long?,
    ) {
        val gdAddDynamicCampaignIgnored = CampaignTestDataUtils.defaultGdAddDynamicCampaign(defaultAttributionModel).withPromoExtensionId(null)
        val gdAddDynamicCampaign = CampaignTestDataUtils.defaultGdAddDynamicCampaign(defaultAttributionModel).withPromoExtensionId(idForAdd)
        val inputForAdd = GdAddCampaigns().withCampaignAddItems(listOf(
            GdAddCampaignUnion().withDynamicCampaign(gdAddDynamicCampaignIgnored),
            GdAddCampaignUnion().withDynamicCampaign(gdAddDynamicCampaign),
            GdAddCampaignUnion().withDynamicCampaign(gdAddDynamicCampaignIgnored),
        ))

        val campaignIds = addCampaignsAndCheckPayload(inputForAdd)

        val (firstInDb, secondInDb, thirdInDb) = getCampaigns(campaignIds)

        val inputForUpdate = GdUpdateCampaigns().withCampaignUpdateItems(listOf(
            GdUpdateCampaignUnion().withDynamicCampaign(
                CampaignTestDataUtils.defaultGdUpdateDynamicCampaign(firstInDb as DynamicCampaign, defaultAttributionModel)
                    .withPromoExtensionId(null)
            ),
            GdUpdateCampaignUnion().withDynamicCampaign(
                CampaignTestDataUtils.defaultGdUpdateDynamicCampaign(secondInDb as DynamicCampaign, defaultAttributionModel)
                    .withPromoExtensionId(idForUpdate)
            ),
            GdUpdateCampaignUnion().withDynamicCampaign(
                CampaignTestDataUtils.defaultGdUpdateDynamicCampaign(thirdInDb as DynamicCampaign, defaultAttributionModel)
                    .withPromoExtensionId(promoExtensionInfoSecond.promoExtensionId)
            ),
        ))

        updateCampaignsAndCheckPayload(inputForUpdate, campaignIds, idForUpdate)
    }

    private fun addCampaignsAndCheckPayload(input: GdAddCampaigns) : List<Long> {
        val gdAddCampaignPayload = processor.doMutationAndGetPayload(ADD_CAMPAIGN_MUTATION, input, operator)

        gdAddCampaignPayload.validationResult.checkNull()
        gdAddCampaignPayload.addedCampaigns.checkSize(3)
        val campaignIdFirstIgnored = gdAddCampaignPayload.addedCampaigns[0].id
        val campaignIdToUpdateWithValue = gdAddCampaignPayload.addedCampaigns[1].id
        val campaignIdSecondIgnored = gdAddCampaignPayload.addedCampaigns[2].id

        return listOf(campaignIdFirstIgnored, campaignIdToUpdateWithValue, campaignIdSecondIgnored)
    }

    private fun getCampaigns(ids: List<Long>) : Array<BaseCampaign> {
        return campaignTypedRepository.getTypedCampaigns(clientInfo.shard, ids).toTypedArray()
    }

    private fun updateCampaignsAndCheckPayload(input: GdUpdateCampaigns, ids: List<Long>, idForUpdate: Long?) {
        val (campaignIdFirstIgnored, campaignIdToUpdateWithValue, campaignIdSecondIgnored) = ids

        val gdUpdateCampaignPayload = processor.doMutationAndGetPayload(UPDATE_CAMPAIGN_MUTATION, input, operator)
        gdUpdateCampaignPayload.validationResult.checkNull()

        val campaignPromoExtensionMap = ktGraphQLTestExecutor.getCampaignIdToPromoExtension(
            clientInfo.login, setOf(campaignIdFirstIgnored, campaignIdSecondIgnored, campaignIdToUpdateWithValue)
        )
        val promoExtensionForIdForUpdate = idForUpdate?.let { promoExtensionRepository.getByIds(clientInfo.shard, listOf(it))[0] }
        softly {
            campaignPromoExtensionMap[campaignIdFirstIgnored].checkNull()
            campaignPromoExtensionMap[campaignIdSecondIgnored].checkGdPromoextensionEqualsExpected(
                promoExtensionInfoSecond.promoExtension,
                ON_MODERATION,
                if (idForUpdate == promoExtensionInfoSecond.promoExtensionId)
                    listOf(campaignIdToUpdateWithValue, campaignIdSecondIgnored) else listOf(campaignIdSecondIgnored)
            )
            if (idForUpdate == null) {
                campaignPromoExtensionMap[campaignIdToUpdateWithValue].checkNull()
            } else {
                campaignPromoExtensionMap[campaignIdToUpdateWithValue].checkGdPromoextensionEqualsExpected(
                    promoExtensionForIdForUpdate!!,
                    ON_MODERATION,
                    if (idForUpdate == promoExtensionInfoSecond.promoExtensionId)
                        listOf(campaignIdToUpdateWithValue, campaignIdSecondIgnored) else listOf(campaignIdToUpdateWithValue)
                )
            }
        }
    }
}
