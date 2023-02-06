package ru.yandex.direct.grid.processing.service.dynamiccondition

import graphql.ExecutionResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets.defaultDynamicTextAdTargetWithRandomRules
import ru.yandex.direct.core.testing.data.TestUsers
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.UserInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.grid.model.GdOrderByParams
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
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
import ru.yandex.direct.test.utils.randomPositiveInt

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class DynamicConditionsGraphQlServiceOrderByTest {

    private val queryTemplate = """
        {
          client(searchBy: {login: "%s"}) {
            dynamicAdTargets(input: %s) {
              rowset {
                id
                name
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
    }

    @Test
    fun getDynamicWebpageAdTargets_orderById() {
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))

        val result = processQuery(orderBy(GdDynamicAdTargetOrderByField.ID, Order.ASC))

        val firstId = getDataValue<Long>(result.getData(), "client/dynamicAdTargets/rowset/0/id")
        val secondId = getDataValue<Long>(result.getData(), "client/dynamicAdTargets/rowset/1/id")

        assertThat(firstId).isLessThan(secondId)
    }

    @Test
    fun getDynamicWebpageAdTargets_orderById_desc() {
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup))

        val result = processQuery(orderBy(GdDynamicAdTargetOrderByField.ID, Order.DESC))

        val firstId = getDataValue<Long>(result.getData(), "client/dynamicAdTargets/rowset/0/id")
        val secondId = getDataValue<Long>(result.getData(), "client/dynamicAdTargets/rowset/1/id")

        assertThat(firstId).isGreaterThan(secondId)
    }

    @Test
    fun getDynamicWebpageAdTargets_orderByName() {
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                        .withConditionName("A"))
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                        .withConditionName("B"))

        val result = processQuery(orderBy(GdDynamicAdTargetOrderByField.NAME, Order.ASC))

        val firstName = getDataValue<String>(result.getData(), "client/dynamicAdTargets/rowset/0/name")
        val secondName = getDataValue<String>(result.getData(), "client/dynamicAdTargets/rowset/1/name")

        assertSoftly {
            it.assertThat(firstName).isEqualTo("A")
            it.assertThat(secondName).isEqualTo("B")
        }
    }

    @Test
    fun getDynamicWebpageAdTargets_orderByName_desc() {
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                        .withConditionName("A"))
        steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                        .withConditionName("B"))

        val result = processQuery(orderBy(GdDynamicAdTargetOrderByField.NAME, Order.DESC))

        val firstName = getDataValue<String>(result.getData(), "client/dynamicAdTargets/rowset/0/name")
        val secondName = getDataValue<String>(result.getData(), "client/dynamicAdTargets/rowset/1/name")

        assertSoftly {
            it.assertThat(firstName).isEqualTo("B")
            it.assertThat(secondName).isEqualTo("A")
        }
    }

    @Test
    fun getDynamicWebpageAdTargets_orderBy_multiple() {
        val startId = randomPositiveInt().toLong()
        val (firstId, secondId, thirdId) = listOf(startId, startId + 1, startId + 2)

        mapOf(firstId to "B", secondId to "A", thirdId to "B").forEach { (id, name) ->
            steps.dynamicTextAdTargetsSteps().createDynamicTextAdTarget(dynamicTextAdGroup,
                    defaultDynamicTextAdTargetWithRandomRules(dynamicTextAdGroup)
                            .withId(id)
                            .withConditionName(name))
        }

        val result = processQuery(
                orderBy(GdDynamicAdTargetOrderByField.NAME, Order.DESC),
                orderBy(GdDynamicAdTargetOrderByField.ID, Order.ASC))

        val dynamicAdTargets = getDataValue<List<Map<String, *>>>(result.getData(), "client/dynamicAdTargets/rowset")

        assertThat(dynamicAdTargets).isEqualTo(listOf(
                mapOf("name" to "B", "id" to firstId),
                mapOf("name" to "B", "id" to thirdId),
                mapOf("name" to "A", "id" to secondId)
        ))
    }

    private fun processQuery(vararg orderByItems: GdDynamicAdTargetOrderBy): ExecutionResult {
        val container = GdDynamicAdTargetsContainer()
                .withFilter(GdDynamicAdTargetFilter()
                        .withCampaignIdIn(setOf(dynamicTextAdGroup.campaignId)))
                .withOrderBy(orderByItems.toMutableList())

        val query = String.format(queryTemplate, context.operator.login,
                GraphQlJsonUtils.graphQlSerialize(container))

        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)
        return result
    }

    private fun orderBy(field: GdDynamicAdTargetOrderByField, order: Order, params: GdOrderByParams? = null)
            : GdDynamicAdTargetOrderBy {
        return GdDynamicAdTargetOrderBy().also {
            it.field = field
            it.order = order
            it.params = params
        }
    }
}

