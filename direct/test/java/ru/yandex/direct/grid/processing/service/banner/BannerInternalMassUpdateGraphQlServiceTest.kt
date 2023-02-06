package ru.yandex.direct.grid.processing.service.banner

import graphql.ExecutionResult
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.banner.model.InternalBanner
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo
import ru.yandex.direct.core.entity.banner.model.TemplateVariable
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestNewInternalBanners
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.NewInternalBannerInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_3
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChange
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeCustomComment
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeStatusShow
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeTemplateVariable
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannerFieldChangeValueUnion
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersAggregatedState
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersMassUpdate
import ru.yandex.direct.grid.processing.model.banner.GdInternalBannersMassUpdatePayload
import ru.yandex.direct.grid.processing.model.banner.GdTemplateVariable
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.TestAuthHelper

private val MODERATED_TEMPLATE_ID: Long = PLACE_3_TEMPLATE_3

private val TEMPLATE_VAR_3675: TemplateVariable = TemplateVariable()
    .withTemplateResourceId(3675L)
    .withInternalValue("Common title-1")
private val TEMPLATE_VAR_3676_1: TemplateVariable = TemplateVariable()
    .withTemplateResourceId(3676L)
    .withInternalValue("First title-2")
private val TEMPLATE_VAR_3676_2: TemplateVariable = TemplateVariable()
    .withTemplateResourceId(3676L)
    .withInternalValue("Second title-2")

private val MODERATED_TEMPLATE_VARS_1: List<TemplateVariable> = listOf(TEMPLATE_VAR_3675, TEMPLATE_VAR_3676_1)
private val MODERATED_TEMPLATE_VARS_2: List<TemplateVariable> = listOf(TEMPLATE_VAR_3675, TEMPLATE_VAR_3676_2)

private const val CUSTOM_COMMENT: String = "Оставьте комментарий для модератора"
private const val TICKET_URL: String = "https://st.yandex-team.ru/LEGAL-113"
private const val IS_SECRET_AD: Boolean = false
private const val STATUS_SHOW_AFTER_MODERATION: Boolean = true
private const val SEND_TO_MODERATION: Boolean = false
private const val STATUS_SHOW: Boolean = true

private const val TICKET_URL_1: String = "https://st.yandex-team.ru/LEGAL-113"
private const val TICKET_URL_2: String = "https://st.yandex-team.ru/LEGAL-115"

private const val STATUS_SHOW_1: Boolean = true
private const val STATUS_SHOW_2: Boolean = false

private const val NEW_CUSTOM_COMMENT: String = "$CUSTOM_COMMENT!!!"


private const val UNMODERATED_TEMPLATE_ID: Long = PLACE_1_TEMPLATE_1
private val TEMPLATE_VAR_11_1: TemplateVariable = TemplateVariable()
    .withTemplateResourceId(11L)
    .withInternalValue("aaa")
private val TEMPLATE_VAR_11_2: TemplateVariable = TemplateVariable()
    .withTemplateResourceId(11L)
    .withInternalValue("bbb")
private val UNMODERATED_TEMPLATE_VARS_1: List<TemplateVariable> = listOf(TEMPLATE_VAR_11_1)
private val UNMODERATED_TEMPLATE_VARS_2: List<TemplateVariable> = listOf(TEMPLATE_VAR_11_2)


private const val AGGREGATED_STATE_QUERY_TEMPLATE = """{
  client(searchBy: {login: "%s"}) {
    internalBannersAggregatedState(input: %s) {
      bannerIds
      aggregatedState {
        ids
        value {
          ... on GdInternalBannerFieldStateCustomComment {
        		__typename
            customComment
          }
          ... on GdInternalBannerFieldStateTicketUrl {
        		__typename
            ticketUrl
          }
          ... on GdInternalBannerFieldStateIsSecretAd {
        		__typename
            isSecretAd
          }
          ... on GdInternalBannerFieldStateStatusShowAfterModeration {
        		__typename
            statusShowAfterModeration
          }
          ... on GdInternalBannerFieldStateSendToModeration {
        		__typename
            sendToModeration
          }
          ... on GdInternalBannerFieldStateStatusShow {
        		__typename
            statusShow
          }
          ... on GdInternalBannerFieldStateTemplateVariable {
        		__typename
            templateVariable {
                value
                templateResourceId
            }
          }
        }
      }
    }
  }
}
"""

private const val MASS_UPDATE_MUTATION_TEMPLATE = """mutation {
  %s (input: %s) {
  	validationResult {
      errors {
        code
        path
        params
      }
    }
  }
}"""

private val MUTATION = GraphQlTestExecutor.TemplateMutation(
    "internalBannersMassUpdate", MASS_UPDATE_MUTATION_TEMPLATE,
    GdInternalBannersMassUpdate::class.java, GdInternalBannersMassUpdatePayload::class.java
)


@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class BannerInternalMassUpdateGraphQlServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var bannerRepository: BannerTypedRepository

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    private lateinit var context: GridGraphQLContext


    private lateinit var clientId: ClientId
    private lateinit var moderatedBanner1: NewInternalBannerInfo
    private lateinit var moderatedBanner2: NewInternalBannerInfo
    private lateinit var unmoderatedBanner1: NewInternalBannerInfo
    private lateinit var unmoderatedBanner2: NewInternalBannerInfo

    private var shard: Int = 0
    private lateinit var operator: User

    @Before
    fun before() {
        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
        clientId = clientInfo.clientId!!

        val campaignWithDefaultPlace = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo)
        val adGroupWithDefaultPlace = steps.adGroupSteps().createActiveInternalAdGroup(campaignWithDefaultPlace)

        val campaignWithModeratedPlace =
            steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo)
        val adGroupWithModeratedPlace = steps.adGroupSteps().createActiveInternalAdGroup(campaignWithModeratedPlace)

        moderatedBanner1 = createInternalBanner(
            adGroupWithModeratedPlace,
            MODERATED_TEMPLATE_ID,
            MODERATED_TEMPLATE_VARS_1,
            createDefaultModerationInfo().withTicketUrl(TICKET_URL_1),
            STATUS_SHOW_1
        )
        moderatedBanner2 = createInternalBanner(
            adGroupWithModeratedPlace,
            MODERATED_TEMPLATE_ID,
            MODERATED_TEMPLATE_VARS_2,
            createDefaultModerationInfo().withTicketUrl(TICKET_URL_2),
            STATUS_SHOW_2
        )

        unmoderatedBanner1 = createInternalBanner(
            adGroupWithDefaultPlace,
            UNMODERATED_TEMPLATE_ID,
            UNMODERATED_TEMPLATE_VARS_1,
            null,
            STATUS_SHOW
        )
        unmoderatedBanner2 = createInternalBanner(
            adGroupWithDefaultPlace,
            UNMODERATED_TEMPLATE_ID,
            UNMODERATED_TEMPLATE_VARS_2,
            null,
            STATUS_SHOW
        )

        shard = clientInfo.shard
        operator = userRepository.fetchByUids(shard, listOf(clientInfo.uid))[0]
        TestAuthHelper.setDirectAuthentication(operator)

        context = configureTestGridContext(operator, clientInfo.chiefUserInfo!!)
    }

    @Test
    fun internalBannersAggregatedState_moderatedBanners() {
        val input = GdInternalBannersAggregatedState().withBannerIds(
            listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId)
        )

        val query = String.format(
            AGGREGATED_STATE_QUERY_TEMPLATE, moderatedBanner1.clientInfo.login,
            GraphQlJsonUtils.graphQlSerialize(input)
        )
        val result: ExecutionResult = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        Assertions.assertThat(result.errors).isEmpty()

        val data: Map<String, Any> = result.getData()
        val payloadRaw = (data["client"] as LinkedHashMap<*, *>)["internalBannersAggregatedState"]
        Assertions.assertThat(payloadRaw)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                mapOf(
                    "bannerIds" to listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId).map { it.toString() },
                    "aggregatedState" to listOf(
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateCustomComment",
                                "customComment" to CUSTOM_COMMENT
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTicketUrl",
                                "ticketUrl" to TICKET_URL_1
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTicketUrl",
                                "ticketUrl" to TICKET_URL_2
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateIsSecretAd",
                                "isSecretAd" to IS_SECRET_AD
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateStatusShowAfterModeration",
                                "statusShowAfterModeration" to STATUS_SHOW_AFTER_MODERATION
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateSendToModeration",
                                "sendToModeration" to SEND_TO_MODERATION
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateStatusShow",
                                "statusShow" to STATUS_SHOW_1
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateStatusShow",
                                "statusShow" to STATUS_SHOW_2
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTemplateVariable",
                                "templateVariable" to mapOf(
                                    "value" to TEMPLATE_VAR_3675.internalValue,
                                    "templateResourceId" to TEMPLATE_VAR_3675.templateResourceId
                                )
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner1.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTemplateVariable",
                                "templateVariable" to mapOf(
                                    "value" to TEMPLATE_VAR_3676_1.internalValue,
                                    "templateResourceId" to TEMPLATE_VAR_3676_1.templateResourceId
                                )
                            )
                        ),
                        mapOf(
                            "ids" to listOf(moderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTemplateVariable",
                                "templateVariable" to mapOf(
                                    "value" to TEMPLATE_VAR_3676_2.internalValue,
                                    "templateResourceId" to TEMPLATE_VAR_3676_2.templateResourceId
                                )
                            )
                        ),
                    )
                )
            )
    }

    @Test
    fun internalBannersAggregatedState_unmoderatedBanners() {
        val input = GdInternalBannersAggregatedState().withBannerIds(
            listOf(unmoderatedBanner1.bannerId, unmoderatedBanner2.bannerId)
        )

        val query = String.format(
            AGGREGATED_STATE_QUERY_TEMPLATE, moderatedBanner1.clientInfo.login,
            GraphQlJsonUtils.graphQlSerialize(input)
        )
        val result: ExecutionResult = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        Assertions.assertThat(result.errors).isEmpty()

        val data: Map<String, Any> = result.getData()
        val payloadRaw = (data["client"] as LinkedHashMap<*, *>)["internalBannersAggregatedState"]
        Assertions.assertThat(payloadRaw)
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                mapOf(
                    "bannerIds" to listOf(
                        unmoderatedBanner1.bannerId,
                        unmoderatedBanner2.bannerId
                    ).map { it.toString() },
                    "aggregatedState" to listOf(
                        mapOf(
                            "ids" to listOf(unmoderatedBanner1.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTemplateVariable",
                                "templateVariable" to mapOf(
                                    "value" to TEMPLATE_VAR_11_1.internalValue,
                                    "templateResourceId" to TEMPLATE_VAR_11_1.templateResourceId
                                )
                            )
                        ),
                        mapOf(
                            "ids" to listOf(unmoderatedBanner2.bannerId).map { it.toString() },
                            "value" to mapOf(
                                "__typename" to "GdInternalBannerFieldStateTemplateVariable",
                                "templateVariable" to mapOf(
                                    "value" to TEMPLATE_VAR_11_2.internalValue,
                                    "templateResourceId" to TEMPLATE_VAR_11_2.templateResourceId
                                )
                            )
                        )
                    )
                )
            )
    }

    @Test
    fun internalBannersMassUpdate_ValidRequest_Success() {
        val input = GdInternalBannersMassUpdate().withChanges(listOf(
            GdInternalBannerFieldChange().apply {
                ids = listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId)
                value = GdInternalBannerFieldChangeValueUnion().apply {
                    customComment = GdInternalBannerFieldChangeCustomComment().apply {
                        innerValue = NEW_CUSTOM_COMMENT
                    }
                }
            },
            GdInternalBannerFieldChange().apply {
                ids = listOf(moderatedBanner1.bannerId)
                value = GdInternalBannerFieldChangeValueUnion().apply {
                    templateVariable = GdInternalBannerFieldChangeTemplateVariable().apply {
                        innerValue = GdTemplateVariable().apply {
                            templateResourceId = TEMPLATE_VAR_3676_2.templateResourceId
                            value = TEMPLATE_VAR_3676_2.internalValue
                        }
                    }
                }
            },
            GdInternalBannerFieldChange().apply {
                ids = listOf(moderatedBanner2.bannerId)
                value = GdInternalBannerFieldChangeValueUnion().apply {
                    statusShow = GdInternalBannerFieldChangeStatusShow().apply {
                        innerValue = STATUS_SHOW_1
                    }
                }
            }
        ))

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val actualBanners = bannerRepository
            .getBanners(shard, listOf(moderatedBanner1.bannerId, moderatedBanner2.bannerId), null)
            .filterIsInstance<InternalBanner>()
            .associateBy { it.id }

        softly {
            assertThat(actualBanners[moderatedBanner1.bannerId]!!.moderationInfo).hasFieldOrPropertyWithValue(
                "customComment", NEW_CUSTOM_COMMENT
            )
            assertThat(actualBanners[moderatedBanner2.bannerId]!!.moderationInfo).hasFieldOrPropertyWithValue(
                "customComment", NEW_CUSTOM_COMMENT
            )
            assertThat(actualBanners[moderatedBanner1.bannerId]!!.templateVariables).contains(
                TEMPLATE_VAR_3676_2
            )
            assertThat(actualBanners[moderatedBanner2.bannerId]).hasFieldOrPropertyWithValue(
                "statusShow", STATUS_SHOW_1
            )
        }
    }

    private fun configureTestGridContext(operator: User, subjectUserInfo: UserInfo): GridGraphQLContext {
        val context = ContextHelper.buildContext(operator, subjectUserInfo.user)
        gridContextProvider.gridContext = context
        return context
    }

    private fun createDefaultModerationInfo(): InternalModerationInfo {
        return InternalModerationInfo().apply {
            this.sendToModeration = SEND_TO_MODERATION
            this.statusShowAfterModeration = STATUS_SHOW_AFTER_MODERATION
            this.customComment = CUSTOM_COMMENT
            this.ticketUrl = TICKET_URL
            this.isSecretAd = IS_SECRET_AD
        }
    }

    private fun createInternalBanner(
        adGroupInfo: AdGroupInfo,
        templateId: Long,
        templateVariables: List<TemplateVariable>,
        moderationInfo: InternalModerationInfo?,
        statusShow: Boolean,
    ): NewInternalBannerInfo {
        val internalBanner = TestNewInternalBanners.fullInternalBanner(adGroupInfo.campaignId, adGroupInfo.adGroupId)
            .withTemplateId(templateId)
            .withTemplateVariables(templateVariables)
            .withModerationInfo(moderationInfo)
            .withStatusShow(statusShow)
        val bannerInfo = NewInternalBannerInfo()
            .withAdGroupInfo(adGroupInfo)
            .withCampaignInfo(adGroupInfo.campaignInfo)
            .withBanner(internalBanner)
        return steps.internalBannerSteps().createInternalBanner(bannerInfo)
    }
}
