package ru.yandex.direct.grid.processing.service.group

import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.HasPassportIdAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.QueryReferersAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexuidAgeAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.OsFamiliesAdGroupAdditionalTargeting
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.entity.user.repository.UserRepository
import ru.yandex.direct.core.testing.data.TestGroups
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChange
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeFinishTime
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeHasPassportId
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeOperation
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeOperation.ADD
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeOperation.REMOVE
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeOsFamilies
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeQueryReferers
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeStartTime
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeValueUnion
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldChangeYandexuidAge
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldStateFinishTime
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldStateHasPassportId
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldStateOsFamilies
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldStateQueryReferers
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldStateStartTime
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupFieldStateYandexuidAge
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsAggregatedState
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsMassUpdate
import ru.yandex.direct.grid.processing.model.group.GdInternalAdGroupsMassUpdatePayload
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingVersioned
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.service.group.internalad.CoreAdditionalTargetingValue
import ru.yandex.direct.grid.processing.service.group.internalad.HAS_PASSPORT_ID_TARGETING_1
import ru.yandex.direct.grid.processing.service.group.internalad.HAS_PASSPORT_ID_TARGETING_2
import ru.yandex.direct.grid.processing.service.group.internalad.OS_FAMILIES_TARGETING_1
import ru.yandex.direct.grid.processing.service.group.internalad.OS_FAMILIES_TARGETING_2
import ru.yandex.direct.grid.processing.service.group.internalad.QUERY_REFERERS_TARGETING_1
import ru.yandex.direct.grid.processing.service.group.internalad.QUERY_REFERERS_TARGETING_2
import ru.yandex.direct.grid.processing.service.group.internalad.toGdEnum
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private val START_TIME: LocalDateTime = LocalDateTime.now().minusMonths(1).truncatedTo(ChronoUnit.MINUTES)

private val FINISH_TIME1: LocalDateTime = LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.MINUTES)
private val FINISH_TIME2: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

private val YANDEXUID_AGE_POSITIVE_1: YandexuidAgeAdGroupAdditionalTargeting
    get() = YandexuidAgeAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ANY
        value = CoreAdditionalTargetingValue<Int>().withValue(5)
    }
private val YANDEXUID_AGE_POSITIVE_2: YandexuidAgeAdGroupAdditionalTargeting
    get() = YandexuidAgeAdGroupAdditionalTargeting().apply {
        targetingMode = TARGETING
        joinType = ANY
        value = CoreAdditionalTargetingValue<Int>().withValue(7)
    }
private val YANDEXUID_AGE_POSITIVE_NEW = YandexuidAgeAdGroupAdditionalTargeting().apply {
    targetingMode = TARGETING
    joinType = ANY
    value = CoreAdditionalTargetingValue<Int>().withValue(8)
}
private val YANDEXUID_AGE_NEGATIVE: YandexuidAgeAdGroupAdditionalTargeting
    get() = YandexuidAgeAdGroupAdditionalTargeting().apply {
        targetingMode = FILTERING
        joinType = ALL
        value = CoreAdditionalTargetingValue<Int>().withValue(10)
    }

private val QUERY_REFERERS_1 = QUERY_REFERERS_TARGETING_1
private val QUERY_REFERERS_NEW = QUERY_REFERERS_TARGETING_2

private val OS_FAMILIES_2 = OS_FAMILIES_TARGETING_1
private val OS_FAMILIES_NEW = OS_FAMILIES_TARGETING_2

private val HAS_PASSPORT_ID_1 = HAS_PASSPORT_ID_TARGETING_1
private val HAS_PASSPORT_ID_NEW = HAS_PASSPORT_ID_TARGETING_2

private const val AGGREGATED_STATE_QUERY_TEMPLATE = """{
  client(searchBy: {login: "%s"}) {
    internalAdGroupsAggregatedState(input: %s) {
      adGroupIds
      aggregatedState {
        ids
        value {
          ... on GdInternalAdGroupFieldStateStartTime {
            __typename
            startTime
          }
          ... on GdInternalAdGroupFieldStateFinishTime {
            __typename
            finishTime
          }
          ... on GdInternalAdGroupFieldStateRf {
            __typename
            rf
          }
          ... on GdInternalAdGroupFieldStateRfReset {
            __typename
            rfReset
          }
          ... on GdInternalAdGroupFieldStateMaxClicksCount {
            __typename
            maxClicksCount
          }
          ... on GdInternalAdGroupFieldStateMaxClicksPeriod {
            __typename
            maxClicksPeriod
          }
          ... on GdInternalAdGroupFieldStateMaxStopsCount {
            __typename
            maxStopsCount
          }
          ... on GdInternalAdGroupFieldStateMaxStopsPeriod {
            __typename
           maxStopsPeriod
          }
          ... on GdInternalAdGroupFieldStateYandexuidAge {
            __typename
            targetingMode
            joinType
            yandexuidAge
          }
          ... on GdInternalAdGroupFieldStateTestIds {
            __typename
            targetingMode
            joinType
            testId
          }
          ... on GdInternalAdGroupFieldStateClids {
            __typename
            targetingMode
            joinType
            clid
          }
          ... on GdInternalAdGroupFieldStateQueryReferers {
            __typename
            targetingMode
            joinType
            queryReferer
          }
          ... on GdInternalAdGroupFieldStateCallerReferrers {
            __typename
            targetingMode
            joinType
            callerReferrer
          }
          ... on GdInternalAdGroupFieldStateUserAgents {
            __typename
            targetingMode
            joinType
            userAgent
          }
          ... on GdInternalAdGroupFieldStateYandexUids {
            __typename
            targetingMode
            joinType
            yandexUid
          }
          ... on GdInternalAdGroupFieldStateYpCookies {
            __typename
            targetingMode
            joinType
            ypCookie
          }
          ... on GdInternalAdGroupFieldStateOsFamilies {
            __typename
            targetingMode
            joinType
            osFamily {
              targetingValueEntryId
              minVersion
              maxVersion
            }
          }
          ... on GdInternalAdGroupFieldStateBrowserNames {
            __typename
            targetingMode
            joinType
            browserName {
              targetingValueEntryId
              minVersion
              maxVersion
            }
          }
          ... on GdInternalAdGroupFieldStateHasPassportId {
            __typename
            targetingMode
            joinType
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

private val MUTATION = TemplateMutation(
    "internalAdGroupsMassUpdate",
    MASS_UPDATE_MUTATION_TEMPLATE,
    GdInternalAdGroupsMassUpdate::class.java,
    GdInternalAdGroupsMassUpdatePayload::class.java
)

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class AdGroupInternalMassUpdateGraphQlServiceTest {
    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var adGroupRepository: AdGroupRepository

    @Autowired
    private lateinit var adGroupAdditionalTargetingRepository: AdGroupAdditionalTargetingRepository

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var graphQlTestExecutor: GraphQlTestExecutor

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    private lateinit var context: GridGraphQLContext

    private lateinit var clientId: ClientId
    private lateinit var internalAdGroup1: AdGroupInfo
    private lateinit var internalAdGroup2: AdGroupInfo

    private var shard: Int = 0
    private lateinit var operator: User

    @Before
    fun before() {
        val clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct()
        clientId = clientInfo.clientId!!
        val campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo)

        internalAdGroup1 = steps.adGroupSteps().createAdGroup(
            TestGroups.activeInternalAdGroup(campaignInfo.campaignId, 0L).withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME1), campaignInfo
        )
        internalAdGroup2 = steps.adGroupSteps().createAdGroup(
            TestGroups.activeInternalAdGroup(campaignInfo.campaignId, 0L).withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME2), campaignInfo
        )

        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(
            internalAdGroup1,
            listOf(YANDEXUID_AGE_POSITIVE_1, YANDEXUID_AGE_NEGATIVE, QUERY_REFERERS_1, HAS_PASSPORT_ID_1)
        )
        steps.adGroupAdditionalTargetingSteps().addValidTargetingsToAdGroup(
            internalAdGroup2, listOf(YANDEXUID_AGE_POSITIVE_2, YANDEXUID_AGE_NEGATIVE, OS_FAMILIES_2)
        )

        shard = clientInfo.shard
        operator = userRepository.fetchByUids(shard, listOf(clientInfo.uid))[0]
        TestAuthHelper.setDirectAuthentication(operator)

        context = configureTestGridContext(operator, clientInfo.chiefUserInfo!!)
    }

    @Test
    fun internalAdGroupsAggregatedState() {
        val input = GdInternalAdGroupsAggregatedState().withAdGroupIds(
            listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId)
        )

        val query = String.format(
            AGGREGATED_STATE_QUERY_TEMPLATE, internalAdGroup1.clientInfo.login, GraphQlJsonUtils.graphQlSerialize(input)
        )
        val result: ExecutionResult = processor.processQuery(null, query, null, context)
        GraphQLUtils.logErrors(result.errors)
        assertThat(result.errors).isEmpty()

        val data: Map<String, Any> = result.getData()
        val payloadRaw = (data["client"] as LinkedHashMap<*, *>)["internalAdGroupsAggregatedState"]
        assertThat(payloadRaw).usingRecursiveComparison().ignoringCollectionOrder().isEqualTo(
            mapOf(
                "adGroupIds" to listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId).map { it.toString() },
                "aggregatedState" to convertStartTimeTargetingToStateResult(
                    listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId), START_TIME
                ) + convertFinishTimeTargetingToStateResult(
                    listOf(internalAdGroup1.adGroupId), FINISH_TIME1
                ) + convertFinishTimeTargetingToStateResult(
                    listOf(internalAdGroup2.adGroupId), FINISH_TIME2
                ) + convertYandexuidAgeTargetingToStateResult(
                    listOf(internalAdGroup1.adGroupId), YANDEXUID_AGE_POSITIVE_1
                ) + convertYandexuidAgeTargetingToStateResult(
                    listOf(internalAdGroup2.adGroupId), YANDEXUID_AGE_POSITIVE_2
                ) + convertYandexuidAgeTargetingToStateResult(
                    listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId), YANDEXUID_AGE_NEGATIVE
                ) + convertQueryReferersTargetingToStateResult(
                    listOf(internalAdGroup1.adGroupId), QUERY_REFERERS_1
                ) + convertOsFamiliesTargetingToStateResult(
                    listOf(internalAdGroup2.adGroupId), OS_FAMILIES_2
                ) + convertHasPassportIdTargetingToStateResult(
                    listOf(internalAdGroup1.adGroupId), HAS_PASSPORT_ID_1
                )
            )
        )
    }

    @Test
    fun internalAdGroupsMassUpdate_ValidRequest_Success() {
        val input = GdInternalAdGroupsMassUpdate().withChanges(
            convertStartTimeTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId), REMOVE, START_TIME
            ) + convertFinishTimeTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId), REMOVE, FINISH_TIME1
            ) + convertFinishTimeTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId), ADD, FINISH_TIME2
            ) + convertYandexuidAgeTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId), REMOVE, YANDEXUID_AGE_POSITIVE_1
            ) + convertYandexuidAgeTargetingToFieldChanges(
                listOf(internalAdGroup2.adGroupId), REMOVE, YANDEXUID_AGE_POSITIVE_2
            ) + convertYandexuidAgeTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId), ADD, YANDEXUID_AGE_POSITIVE_NEW
            ) + convertQueryReferersTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId), REMOVE, QUERY_REFERERS_1
            ) + convertQueryReferersTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId), ADD, QUERY_REFERERS_NEW
            ) + convertOsFamiliesTargetingToFieldChanges(
                listOf(internalAdGroup2.adGroupId), REMOVE, OS_FAMILIES_2
            ) + convertOsFamiliesTargetingToFieldChanges(
                listOf(internalAdGroup2.adGroupId), ADD, OS_FAMILIES_NEW
            ) + convertHasPassportIdTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId), REMOVE, HAS_PASSPORT_ID_1
            ) + convertHasPassportIdTargetingToFieldChanges(
                listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId), ADD, HAS_PASSPORT_ID_NEW
            )
        )

        val payload = graphQlTestExecutor.doMutationAndGetPayload(MUTATION, input, operator)
        GraphQlTestExecutor.validateResponseSuccessful(payload)

        val actualAdGroups = adGroupRepository.getAdGroups(
            shard, listOf(internalAdGroup1.adGroupId, internalAdGroup2.adGroupId)
        ).associateBy { it.id }


        softly {
            assertThat(actualAdGroups[internalAdGroup1.adGroupId]).hasFieldOrPropertyWithValue(
                "startTime", null
            )
            assertThat(actualAdGroups[internalAdGroup2.adGroupId]).hasFieldOrPropertyWithValue(
                "startTime", null
            )
            assertThat(actualAdGroups[internalAdGroup1.adGroupId]).hasFieldOrPropertyWithValue(
                "finishTime", FINISH_TIME2
            )
            assertThat(actualAdGroups[internalAdGroup2.adGroupId]).hasFieldOrPropertyWithValue(
                "finishTime", FINISH_TIME2
            )
            assertThat(getAdGroupTargetings(internalAdGroup1)).containsExactlyInAnyOrder(
                YANDEXUID_AGE_POSITIVE_NEW, YANDEXUID_AGE_NEGATIVE, QUERY_REFERERS_NEW, HAS_PASSPORT_ID_NEW
            )
            assertThat(getAdGroupTargetings(internalAdGroup2)).containsExactlyInAnyOrder(
                YANDEXUID_AGE_POSITIVE_NEW, YANDEXUID_AGE_NEGATIVE, OS_FAMILIES_NEW, HAS_PASSPORT_ID_NEW
            )
        }
    }

    private fun configureTestGridContext(operator: User, subjectUserInfo: UserInfo): GridGraphQLContext {
        val context = ContextHelper.buildContext(operator, subjectUserInfo.user)
        gridContextProvider.gridContext = context
        return context
    }

    private fun convertStartTimeTargetingToStateResult(
        adGroupIds: List<Long>, expectedTime: LocalDateTime
    ): List<Map<String, Any>> = listOf(
        mapOf(
            "ids" to adGroupIds.map { it.toString() },
            "value" to mapOf(
                "__typename" to GdInternalAdGroupFieldStateStartTime::class.simpleName,
                "startTime" to expectedTime.toString()
            )
        )
    )

    private fun convertStartTimeTargetingToFieldChanges(
        adGroupIds: List<Long>,
        fieldChangeOperation: GdInternalAdGroupFieldChangeOperation,
        changedTime: LocalDateTime
    ) = listOf(
        GdInternalAdGroupFieldChange().apply {
            ids = adGroupIds
            operation = fieldChangeOperation
            value = GdInternalAdGroupFieldChangeValueUnion().apply {
                startTime = GdInternalAdGroupFieldChangeStartTime().apply {
                    innerValue = changedTime
                }
            }
        }
    )

    private fun convertFinishTimeTargetingToStateResult(
        adGroupIds: List<Long>, expectedTime: LocalDateTime
    ): List<Map<String, Any>> = listOf(
        mapOf(
            "ids" to adGroupIds.map { it.toString() },
            "value" to mapOf(
                "__typename" to GdInternalAdGroupFieldStateFinishTime::class.simpleName,
                "finishTime" to expectedTime.toString()
            )
        )
    )

    private fun convertFinishTimeTargetingToFieldChanges(
        adGroupIds: List<Long>,
        fieldChangeOperation: GdInternalAdGroupFieldChangeOperation,
        changedTime: LocalDateTime
    ) = listOf(
        GdInternalAdGroupFieldChange().apply {
            ids = adGroupIds
            operation = fieldChangeOperation
            value = GdInternalAdGroupFieldChangeValueUnion().apply {
                finishTime = GdInternalAdGroupFieldChangeFinishTime().apply {
                    innerValue = changedTime
                }
            }
        }
    )

    private fun convertYandexuidAgeTargetingToStateResult(
        adGroupIds: List<Long>, targeting: YandexuidAgeAdGroupAdditionalTargeting
    ): List<Map<String, Any>> = listOf(
        mapOf(
            "ids" to adGroupIds.map { it.toString() },
            "value" to mapOf(
                "__typename" to GdInternalAdGroupFieldStateYandexuidAge::class.simpleName,
                "joinType" to targeting.joinType.toString(),
                "targetingMode" to targeting.targetingMode.toString(),
                "yandexuidAge" to targeting.value.value
            )
        )
    )

    private fun convertYandexuidAgeTargetingToFieldChanges(
        adGroupIds: List<Long>,
        fieldChangeOperation: GdInternalAdGroupFieldChangeOperation,
        targeting: YandexuidAgeAdGroupAdditionalTargeting
    ) = listOf(
        GdInternalAdGroupFieldChange().apply {
            ids = adGroupIds
            operation = fieldChangeOperation
            value = GdInternalAdGroupFieldChangeValueUnion().apply {
                yandexuidAge = GdInternalAdGroupFieldChangeYandexuidAge().apply {
                    innerValue = targeting.value.value
                    targetingMode = targeting.targetingMode.toGdEnum()
                    joinType = targeting.joinType.toGdEnum()
                }
            }
        }
    )

    private fun convertQueryReferersTargetingToStateResult(
        adGroupIds: List<Long>, targeting: QueryReferersAdGroupAdditionalTargeting
    ): List<Map<String, Any>> = targeting.value.map {
        mapOf(
            "ids" to adGroupIds.map { id -> id.toString() },
            "value" to mapOf(
                "__typename" to GdInternalAdGroupFieldStateQueryReferers::class.simpleName,
                "joinType" to targeting.joinType.toString(),
                "targetingMode" to targeting.targetingMode.toString(),
                "queryReferer" to it
            )
        )
    }

    private fun convertQueryReferersTargetingToFieldChanges(
        adGroupIds: List<Long>,
        fieldChangeOperation: GdInternalAdGroupFieldChangeOperation,
        targeting: QueryReferersAdGroupAdditionalTargeting
    ) = targeting.value.map {
        GdInternalAdGroupFieldChange().apply {
            ids = adGroupIds
            operation = fieldChangeOperation
            value = GdInternalAdGroupFieldChangeValueUnion().apply {
                queryReferers = GdInternalAdGroupFieldChangeQueryReferers().apply {
                    innerValue = it
                    targetingMode = targeting.targetingMode.toGdEnum()
                    joinType = targeting.joinType.toGdEnum()
                }
            }
        }
    }

    private fun convertOsFamiliesTargetingToStateResult(
        adGroupIds: List<Long>, targeting: OsFamiliesAdGroupAdditionalTargeting
    ): List<Map<String, Any>> = targeting.value.map {
        mapOf(
            "ids" to adGroupIds.map { id -> id.toString() },
            "value" to mapOf(
                "__typename" to GdInternalAdGroupFieldStateOsFamilies::class.simpleName,
                "joinType" to targeting.joinType.toString(),
                "targetingMode" to targeting.targetingMode.toString(),
                "osFamily" to mapOf(
                    "targetingValueEntryId" to it.targetingValueEntryId,
                    "minVersion" to it.minVersion,
                    "maxVersion" to it.maxVersion
                )
            )
        )
    }

    private fun convertOsFamiliesTargetingToFieldChanges(
        adGroupIds: List<Long>,
        fieldChangeOperation: GdInternalAdGroupFieldChangeOperation,
        targeting: OsFamiliesAdGroupAdditionalTargeting
    ) = targeting.value.map {
        GdInternalAdGroupFieldChange().apply {
            ids = adGroupIds
            operation = fieldChangeOperation
            value = GdInternalAdGroupFieldChangeValueUnion().apply {
                osFamilies = GdInternalAdGroupFieldChangeOsFamilies().apply {
                    innerValue = GdAdditionalTargetingVersioned().apply {
                        targetingValueEntryId = it.targetingValueEntryId
                        minVersion = it.minVersion
                        maxVersion = it.maxVersion
                    }
                    targetingMode = targeting.targetingMode.toGdEnum()
                    joinType = targeting.joinType.toGdEnum()
                }
            }
        }
    }

    private fun convertHasPassportIdTargetingToStateResult(
        adGroupIds: List<Long>, targeting: HasPassportIdAdGroupAdditionalTargeting
    ): List<Map<String, Any>> = listOf(
        mapOf(
            "ids" to adGroupIds.map { id -> id.toString() },
            "value" to mapOf(
                "__typename" to GdInternalAdGroupFieldStateHasPassportId::class.simpleName,
                "joinType" to targeting.joinType.toString(),
                "targetingMode" to targeting.targetingMode.toString()
            )
        )
    )

    private fun convertHasPassportIdTargetingToFieldChanges(
        adGroupIds: List<Long>,
        fieldChangeOperation: GdInternalAdGroupFieldChangeOperation,
        targeting: HasPassportIdAdGroupAdditionalTargeting
    ) = listOf(
        GdInternalAdGroupFieldChange().apply {
            ids = adGroupIds
            operation = fieldChangeOperation
            value = GdInternalAdGroupFieldChangeValueUnion().apply {
                hasPassportId = GdInternalAdGroupFieldChangeHasPassportId().apply {
                    targetingMode = targeting.targetingMode.toGdEnum()
                    joinType = targeting.joinType.toGdEnum()
                }
            }
        }
    )

    private fun getAdGroupTargetings(adGroup: AdGroupInfo): List<AdGroupAdditionalTargeting> =
        adGroupAdditionalTargetingRepository.getByAdGroupId(adGroup.shard, adGroup.adGroupId).map {
            it.apply {
                id = null
                adGroupId = null
            }
        }
}
