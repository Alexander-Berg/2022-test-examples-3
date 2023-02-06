package ru.yandex.direct.grid.processing.service.smartfilter

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.jooq.Select
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
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.entity.smartfilter.model.GdiSmartFilter
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.smartfilter.GdSmartFilterFilter
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.SmartFilterTestDataUtils
import ru.yandex.direct.grid.processing.util.StatCalculationHelper.ratio
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.math.BigDecimal

private val QUERY_TEMPLATE = """
            {
                client(searchBy: {login: "%s"}) {
                    smartFilters(input: %s) {
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
class SmartFilterGraphQlStatsTest {

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

    private lateinit var clientInfo: ClientInfo
    private lateinit var context: GridGraphQLContext

    @Before
    fun before() {
        clientInfo = steps.clientSteps().createDefaultClient()
        val operator = clientInfo.chiefUserInfo!!.user!!

        TestAuthHelper.setDirectAuthentication(operator)

        context = ContextHelper.buildContext(operator)
            .withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = context
    }

    fun parametrizedTestData() = listOf(
        listOf("With feature", true),
        listOf("Without feature", false),
    )

    /**
     * Проверка получения корректных AvgClickCost/cpmPrice для смарт-фильтров в зависимости от фичи
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun testGetStats(
        @Suppress("UNUSED_PARAMETER") description: String,
        enableFeature: Boolean,
    ) {
        val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo)
        val smartFilter = createGdiSmartFilter(adGroupInfo)

        mockDataFromYtAndStats(mapOf(smartFilter to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST)))

        steps.featureSteps().addClientFeature(clientInfo.clientId!!, CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(smartFilter))
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
     * Проверка получения AvgClickCost/cpmPrice для нескольких смарт-фильтров в зависимости от фичи
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun testGetStats_WithTwoFilters(
        @Suppress("UNUSED_PARAMETER") description: String,
        enableFeature: Boolean,
    ) {
        val avgClickCostForFilter2 = 2_255L
        val cpmPriceForFilter2 = 3_159L
        val clicksForFilter2 = 229L
        val costForFilter2 = 2_351L

        val adGroupInfo = steps.adGroupSteps().createDefaultPerformanceAdGroup(clientInfo)
        val smartFilter1 = createGdiSmartFilter(adGroupInfo)
        val smartFilter2 = createGdiSmartFilter(adGroupInfo)

        mockDataFromYtAndStats(mapOf(
            smartFilter1 to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST),
            smartFilter2 to arrayOf(avgClickCostForFilter2, cpmPriceForFilter2, clicksForFilter2, costForFilter2),
        ))

        steps.featureSteps().addClientFeature(clientInfo.clientId!!, CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(smartFilter1, smartFilter2))
        val avgClickCost1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val cpmPrice1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val avgClickCost2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val cpmPrice2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")

        val soft = SoftAssertions()
        soft.assertThat(avgClickCost1.toDouble())
            .`as`("Avg click cost for first filter")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(avgClickCost2.toDouble())
            .`as`("Avg click cost for second filter")
            .isEqualTo(avgClickCostForFilter2.toDouble())

        if (enableFeature) {
            soft.assertThat(cpmPrice1)
                .`as`("Cpm price for first filter")
                .isNull()
            soft.assertThat(cpmPrice2)
                .`as`("Cpm price for second filter")
                .isNull()
        } else {
            soft.assertThat(cpmPrice1.toDouble())
                .`as`("Cpm price for first filter")
                .isEqualTo(CPM_PRICE.toDouble())
            soft.assertThat(cpmPrice2.toDouble())
                .`as`("Cpm price for second filter")
                .isEqualTo(cpmPriceForFilter2.toDouble())
        }

        val totalCost = COST + costForFilter2
        val totalClicks = CLICKS + clicksForFilter2

        // total avgClickCost = Sum( COSTi / CLICKSi ) / n
        soft.assertThat(totalAvgClickCost.toDouble())
            .`as`("Total avg click cost")
            .isEqualTo(ratio(BigDecimal.valueOf(totalCost), BigDecimal.valueOf(totalClicks))?.toDouble())
        soft.assertAll()
    }

    private fun createGdiSmartFilter(adGroupInfo: AdGroupInfo): GdiSmartFilter {
        val bidsPerformanceRecord = steps.performanceFilterSteps().addDefaultBidsPerformance(adGroupInfo)
        return GdiSmartFilter()
            .withCampaignId(adGroupInfo.campaignId)
            .withAdGroupId(adGroupInfo.adGroupId)
            .withSmartFilterId(bidsPerformanceRecord.perfFilterId)
    }

    private fun sendRequestAndGetData(smartFilters: Set<GdiSmartFilter>): Map<String, Any> {
        val container = getContainer(smartFilters)

        val query = String.format(QUERY_TEMPLATE, clientInfo.login, GraphQlJsonUtils.graphQlSerialize(container))
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)

        val clientData = (result.getData<Any>() as Map<*, *>)["client"] as Map<*, *>
        return clientData["smartFilters"] as Map<String, Any>
    }

    private fun getContainer(
        smartFilters: Set<GdiSmartFilter>
    ) = SmartFilterTestDataUtils.getDefaultGdSmartFiltersContainer()
        .withFilter(GdSmartFilterFilter()
            .withCampaignIdIn(smartFilters.map { it.campaignId }.toSet())
            .withAdGroupIdIn(smartFilters.map { it.adGroupId }.toSet())
        )

    private fun mockDataFromYtAndStats(filterToStats: Map<GdiSmartFilter, Array<Long>>) {
        val rowset = wrapInRowset(
            filterToStats
                .map { (smartFilter, stats) ->
                    val yTreeBuilder = YTree.mapBuilder()
                        .key(GdiEntityStats.AVG_CLICK_COST.name()).value(stats[0] * 1_000_000L)
                        .key(GdiEntityStats.CPM_PRICE.name()).value(stats[1] * 1_000_000L)
                        .key(GdiEntityStats.CLICKS.name()).value(stats[2])
                        .key(GdiEntityStats.COST.name()).value(stats[3] * 1_000_000L)
                        .key(GdiEntityStats.SHOWS.name()).value(SHOWS)
                        .key("ExportID").value(smartFilter.campaignId)
                        .key("GroupExportID").value(smartFilter.adGroupId)
                        .key("PhraseExportID").value(smartFilter.smartFilterId)
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
