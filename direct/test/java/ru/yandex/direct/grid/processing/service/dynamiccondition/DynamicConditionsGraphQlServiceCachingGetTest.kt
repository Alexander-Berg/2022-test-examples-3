package ru.yandex.direct.grid.processing.service.dynamiccondition

import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer
import ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetFilter
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetOrderBy
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetOrderByField
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetsContainer
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQLUtils.getDataValue
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.test.utils.TestUtils.assumeThat
import ru.yandex.direct.test.utils.assertj.Conditions.matchedBy

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class DynamicConditionsGraphQlServiceCachingGetTest {

    private val queryTemplate = """
        {
          client(searchBy: {login: "%s"}) {
            dynamicAdTargets(input: %s) {
              cacheKey
              rowset {
                id
                dynamicConditionId
                adGroupId
                campaignId
                name
                price
                priceContext
                autobudgetPriority
                isSuspended
                tab
                ... on GdDynamicFeedAdTarget {
                  feedConditions {
                    field
                    operator
                    stringValue
                  }
                }
                ... on GdDynamicWebpageAdTarget {
                  webpageConditions {
                    operand
                    operator
                    arguments
                  }
                }
               }
            }
          }
        }
        """.trimIndent()

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var steps: Steps

    private lateinit var dynamicTextAdGroup: AdGroupInfo
    private lateinit var context: GridGraphQLContext

    @Before
    fun before() {
        val userInfo: UserInfo = steps.userSteps().createUser(TestUsers.generateNewUser())
        val clientInfo = userInfo.clientInfo

        context = ContextHelper.buildContext(userInfo.user)
                .withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = context

        dynamicTextAdGroup = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo)

        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))
    }

    @Test
    fun getDynamicAdTargetsCaching() {
        val dataBefore = processQuery()
        val cacheKey = extractCacheKey(dataBefore)

        assumeThat("returned cache key", cacheKey, notNullValue())

        val dataAfter = processQuery(cacheKey = cacheKey)

        assertThat(dataAfter).`is`(matchedBy(beanDiffer(dataBefore)))
    }

    @Test
    fun getDynamicAdTargetsCaching_limitOffset() {
        val dataBefore = processQuery()
        val cacheKeyBefore = extractCacheKey(dataBefore)

        val firstBefore = getDataValue<Map<String, *>>(dataBefore.getData(), "client/dynamicAdTargets/rowset/0")
        val secondBefore = getDataValue<Map<String, *>>(dataBefore.getData(), "client/dynamicAdTargets/rowset/1")

        assumeThat("returned cache key", cacheKeyBefore, notNullValue())

        val dataAfter1 = processQuery(cacheKey = cacheKeyBefore, limitOffset = GdLimitOffset()
                .withLimit(1)
                .withOffset(0))
        val cacheKeyAfter1 = extractCacheKey(dataAfter1)

        val dataAfter2 = processQuery(cacheKey = cacheKeyBefore, limitOffset = GdLimitOffset()
                .withLimit(1)
                .withOffset(1))
        val cacheKeyAfter2 = extractCacheKey(dataAfter1)

        val firstAfter = getDataValue<Map<String, *>>(dataAfter1.getData(), "client/dynamicAdTargets/rowset/0")
        val secondAfter = getDataValue<Map<String, *>>(dataAfter2.getData(), "client/dynamicAdTargets/rowset/0")

        assertSoftly {
            it.assertThat(cacheKeyAfter1).isEqualTo(cacheKeyBefore)
            it.assertThat(firstAfter).isEqualTo(firstBefore)

            it.assertThat(cacheKeyAfter2).isEqualTo(cacheKeyBefore)
            it.assertThat(secondAfter).isEqualTo(secondBefore)
        }
    }

    private fun extractCacheKey(data: ExecutionResult) =
            getDataValue<String>(data.getData(), "client/dynamicAdTargets/cacheKey")

    private fun processQuery(
            limitOffset: GdLimitOffset? = null,
            cacheKey: String? = null
    ): ExecutionResult {
        val container = GdDynamicAdTargetsContainer()
                .withOrderBy(listOf(GdDynamicAdTargetOrderBy()
                        .withField(GdDynamicAdTargetOrderByField.ID)
                        .withOrder(Order.ASC)))
                .withFilter(GdDynamicAdTargetFilter()
                        .withCampaignIdIn(setOf(dynamicTextAdGroup.campaignId)))
                .withCacheKey(cacheKey)
                .withLimitOffset(limitOffset)

        val query = String.format(queryTemplate, context.operator.login,
                GraphQlJsonUtils.graphQlSerialize(container))

        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        return result
    }

}
