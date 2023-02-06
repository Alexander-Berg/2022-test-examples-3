package ru.yandex.direct.grid.processing.service.dynamiccondition

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.jooq.Select
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.dynamictextadtarget.model.DynamicFeedAdTarget
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestDynamicTextAdTargets
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetFilter
import ru.yandex.direct.grid.processing.model.dynamiccondition.GdDynamicAdTargetsContainer
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.StatCalculationHelper.ratio
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.math.BigDecimal

private val QUERY_TEMPLATE = """
            {
                client(searchBy: {login: "%s"}) {
                    dynamicAdTargets(input: %s) {
                        rowset {
                            stats {
                                avgClickCost
                                cpmPrice
                            }
                        }
                        totalStats {
                            avgClickCost
                        }
                    }
                }
            }
        """.trimIndent()

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class DynamicConditionsGraphQlStatsTest {
    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        const val AVG_CLICK_COST = 1_200L
        const val CPM_PRICE = 2_100L
        const val COST = 1_555L
        const val CLICKS = 115L
        const val SHOWS = 11_555_000L
    }

    @Rule
    @JvmField
    val stringMethodRule = SpringMethodRule()

    @Autowired
    @Qualifier(GridProcessingConfiguration.GRAPH_QL_PROCESSOR)
    private lateinit var processor: GridGraphQLProcessor

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var gridContextProvider: GridContextProvider

    @Autowired
    private lateinit var gridYtSupport: YtDynamicSupport

    private var shard = 0
    private lateinit var operator: User
    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var context: GridGraphQLContext

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!
        shard = clientInfo.shard

        TestAuthHelper.setDirectAuthentication(operator)

        context = ContextHelper.buildContext(operator)
            .withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = context
    }

    @After
    fun after() {
        doReturn(Mockito.mock(UnversionedRowset::class.java))
            .`when`(gridYtSupport).selectRows(any(Select::class.java))
    }

    fun parametrizedTestData() = listOf(
        listOf("With feature", true),
        listOf("Without feature", false),
    )

    /**
     * Проверка получения корректных AvgClickCost/cpmPrice для динамических условий показа в зависимости от фичи
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun testGetStats(
        @Suppress("UNUSED_PARAMETER") description: String,
        enableFeature: Boolean,
    ) {
        val adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo)
        val target = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        val dynamicFeedAdTarget = steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, target)

        mockDataFromYtAndStats(mapOf(dynamicFeedAdTarget to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST)))

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(dynamicFeedAdTarget))
        val avgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val cpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")

        val soft = SoftAssertions()
        soft.assertThat(avgClickCost.toDouble())
            .`as`("Avg click cost")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        // total avgClickCost = Sum( COSTi / CLICKSi ) / n
        soft.assertThat(totalAvgClickCost.toDouble())
            .`as`("Total avg click cost")
            .isEqualTo(ratio(BigDecimal.valueOf(COST), BigDecimal.valueOf(CLICKS))?.toDouble())

        if (enableFeature) {
            soft.assertThat(cpmPrice)
                .`as`("Cpm price")
                .isNull()
        } else {
            soft.assertThat(cpmPrice.toDouble())
                .`as`("Cpm price")
                .isEqualTo(CPM_PRICE.toDouble())
        }
        soft.assertAll()
    }

    /**
     * Проверка получения AvgClickCost/cpmPrice для нескольких динамических условий показа в зависимости от фичи
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun testGetStats_WithTwoTargets(
        @Suppress("UNUSED_PARAMETER") description: String,
        enableFeature: Boolean,
    ) {
        val avgClickCostForTarget2 = 2_255L
        val cpmPriceForTarget2 = 3_159L
        val clicksForTarget2 = 229L
        val costForTarget2 = 2_351L

        val adGroupInfo = steps.adGroupSteps().createActiveDynamicFeedAdGroup(clientInfo)
        val target1 = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        val dynamicFeedAdTarget1 = steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, target1)
        val target2 = TestDynamicTextAdTargets.dynamicFeedAdTargetWithRandomRules(adGroupInfo)
        val dynamicFeedAdTarget2 = steps.dynamicTextAdTargetsSteps().createDynamicFeedAdTarget(adGroupInfo, target2)

        mockDataFromYtAndStats(mapOf(
            dynamicFeedAdTarget1 to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST),
            dynamicFeedAdTarget2 to arrayOf(avgClickCostForTarget2, cpmPriceForTarget2, clicksForTarget2, costForTarget2),
        ))

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(dynamicFeedAdTarget1, dynamicFeedAdTarget2))
        val avgClickCost1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val cpmPrice1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val avgClickCost2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val cpmPrice2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")

        val soft = SoftAssertions()
        soft.assertThat(avgClickCost1.toDouble())
            .`as`("Avg click cost for first target")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(avgClickCost2.toDouble())
            .`as`("Avg click cost for second target")
            .isEqualTo(avgClickCostForTarget2.toDouble())

        if (enableFeature) {
            soft.assertThat(cpmPrice1)
                .`as`("Cpm price for first target")
                .isNull()
            soft.assertThat(cpmPrice2)
                .`as`("Cpm price for second target")
                .isNull()
        } else {
            soft.assertThat(cpmPrice1.toDouble())
                .`as`("Cpm price for first target")
                .isEqualTo(CPM_PRICE.toDouble())
            soft.assertThat(cpmPrice2.toDouble())
                .`as`("Cpm price for second target")
                .isEqualTo(cpmPriceForTarget2.toDouble())
        }

        val totalCost = COST + costForTarget2
        val totalClicks = CLICKS + clicksForTarget2

        // total avgClickCost = Sum( COSTi / CLICKSi ) / n
        soft.assertThat(totalAvgClickCost.toDouble())
            .`as`("Total avg click cost")
            .isEqualTo(ratio(BigDecimal.valueOf(totalCost), BigDecimal.valueOf(totalClicks))?.toDouble())
        soft.assertAll()
    }

    private fun sendRequestAndGetData(dynamicFeedAdTarget: Set<DynamicFeedAdTarget>): Map<String, Any> {
        val container = getContainer(dynamicFeedAdTarget)

        val query = String.format(QUERY_TEMPLATE, clientInfo.login, GraphQlJsonUtils.graphQlSerialize(container))
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)

        val clientData = (result.getData<Any>() as Map<*, *>)["client"] as Map<*, *>
        return clientData["dynamicAdTargets"] as Map<String, Any>
    }

    private fun getContainer(
        dynamicFeedAdTarget: Set<DynamicFeedAdTarget>
    ) = GdDynamicAdTargetsContainer()
        .withFilter(GdDynamicAdTargetFilter()
            .withAdGroupIdIn(dynamicFeedAdTarget.map { it.adGroupId }.toSet())
            .withCampaignIdIn(dynamicFeedAdTarget.map { it.campaignId }.toSet())
            .withIdIn(dynamicFeedAdTarget.map { it.dynamicConditionId }.toSet())
        )

    private fun mockDataFromYtAndStats(targetToStats: Map<DynamicFeedAdTarget, Array<Long>>) {
        val rowset = wrapInRowset(
            targetToStats
                .map { (target, stats) ->
                    val yTreeBuilder = YTree.mapBuilder()
                        .key(GdiEntityStats.AVG_CLICK_COST.name()).value(stats[0] * 1_000_000L)
                        .key(GdiEntityStats.CPM_PRICE.name()).value(stats[1] * 1_000_000L)
                        .key(GdiEntityStats.CLICKS.name()).value(stats[2])
                        .key(GdiEntityStats.COST.name()).value(stats[3] * 1_000_000L)
                        .key(GdiEntityStats.SHOWS.name()).value(SHOWS)
                        .key("ExportID").value(target.campaignId)
                        .key("GroupExportID").value(target.adGroupId)
                        .key("PhraseExportID").value(target.dynamicConditionId)
                    yTreeBuilder.endMap().build()
                }.toList())
        doReturn(rowset)
            .`when`(gridYtSupport).selectRows(any(Select::class.java))
    }

    private fun wrapInRowset(nodes: List<YTreeNode>): UnversionedRowset {
        val rowset = Mockito.mock(UnversionedRowset::class.java)
        doReturn(nodes).`when`(rowset).yTreeRows
        return rowset
    }
}
