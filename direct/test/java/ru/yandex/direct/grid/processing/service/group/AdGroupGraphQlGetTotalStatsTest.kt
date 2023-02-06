package ru.yandex.direct.grid.processing.service.group

import junitparams.JUnitParamsRunner
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.assertj.core.api.SoftAssertions
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_DEALS
import ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_AUTOBUDGET
import ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.AdGroupInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.model.GdEntityStatsFilter
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.group.GdAdGroupFilter
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderBy
import ru.yandex.direct.grid.processing.model.group.GdAdGroupOrderByField
import ru.yandex.direct.grid.processing.model.group.GdAdGroupsContainer
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.campaign.CampaignGraphQlGetTotalStatsTest
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize
import ru.yandex.direct.grid.processing.util.StatCalculationHelper.ratio
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.schema.yt.Tables
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.math.BigDecimal
import java.util.Locale

private val QUERY_TEMPLATE = """
    {
      client(searchBy: {userId: "%s"}) {
        adGroups(input: %s) {
          rowset {
             id
             stats {
                 avgClickCost
                 cpmPrice
             }
          }
          totalStats {
             avgClickCost
             cpmPrice
          }
        }
      }
    }
""".trimIndent()

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class AdGroupGraphQlGetTotalStatsTest {

    companion object {
        @ClassRule
        @JvmField
        val springClassRule = SpringClassRule()

        const val AVG_CLICK_COST = 1_200L
        const val CPM_PRICE = 2_100L
        const val COST = 1_555L
        const val CLICKS = 115L
        const val SHOWS = 11_555_000L

        val ORDER_BY_ID = GdAdGroupOrderBy()
            .withField(GdAdGroupOrderByField.ID)
            .withOrder(Order.ASC)
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
        Mockito.doReturn(Mockito.mock(UnversionedRowset::class.java))
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any(), ArgumentMatchers.anyBoolean())
    }

    fun parametrizedTestData() = listOf(
        listOf("Group in TEXT campaign with feature", true, TEXT, true, false),
        listOf("Group in CPM campaign with feature", true, CPM_DEALS, false, true),
        listOf("Group in INTERNAL campaign with feature", true, INTERNAL_AUTOBUDGET, true, true),
        listOf("Group in TEXT campaign without feature", false, TEXT, true, true),
        listOf("Group in CPM campaign without feature", false, CPM_DEALS, true, true),
        listOf("Group in INTERNAL campaign without feature", false, INTERNAL_AUTOBUDGET, true, true),
    )

    /**
     * Проверка получения корректных AvgClickCost/cpmPrice для разных групп в разных типах кампаний
     */
    @Test
    @Parameters(method = "parametrizedTestData")
    @TestCaseName("{0}")
    fun testGetStats(
        @Suppress("UNUSED_PARAMETER") description: String,
        enableFeature: Boolean,
        campaignType: CampaignType,
        expectAvgClickCost: Boolean,
        expectCpmPrice: Boolean,
    ) {
        val campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo)
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo)

        doReturnFromYt(mapOf(adGroupInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST)))

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(adGroupInfo))
        val avgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val cpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")
        val totalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/cpmPrice")

        val soft = SoftAssertions()
        if (expectAvgClickCost) {
            soft.assertThat(avgClickCost.toDouble())
                .`as`("Avg click cost")
                .isEqualTo(AVG_CLICK_COST.toDouble())
            // total avgClickCost = Sum( COSTi / CLICKSi ) / n
            soft.assertThat(totalAvgClickCost.toDouble())
                .`as`("Total avg click cost")
                .isEqualTo(ratio(BigDecimal.valueOf(COST), BigDecimal.valueOf(CLICKS))?.toDouble())
        } else {
            soft.assertThat(avgClickCost)
                .`as`("Avg click cost")
                .isNull()
            soft.assertThat(totalAvgClickCost)
                .`as`("Total avg click cost")
                .isNull()
        }

        if (expectCpmPrice) {
            soft.assertThat(cpmPrice.toDouble())
                .`as`("Cpm price")
                .isEqualTo(CPM_PRICE.toDouble())
        } else {
            soft.assertThat(cpmPrice)
                .`as`("Cpm price")
                .isNull()
        }
        // totalStats/cpmPrice не отправляем на фронт
        soft.assertThat(totalCpmPrice)
            .`as`("Total cpm price")
            .isEqualTo(BigDecimal.ZERO)
        soft.assertAll()
    }

    /**
     * При отборе трех групп от разных типов кампаний (CPC, CPM, не CPC/CPM) и включенной фиче
     * -> получаем корректные AvgClickCost/cpmPrice по каждой группе и в общей статистике
     */
    @Test
    fun getStats_WithFeatureAndMultipleCampaigns() {
        val avgClickCostForCpm = 2_255L
        val cpmPriceForCpm = 3_159L
        val clicksForCpm = 229L
        val costForCpm = 2_351L

        val avgClickCostForInternal = 3_151L
        val cpmPriceForInternal = 4_051L
        val clicksForInternal = 330L
        val costForInternal = 3_911L

        val textAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        )
        val cpmAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        )
        val internalAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(INTERNAL_AUTOBUDGET, clientInfo)
        )

        val campTypeToPrices = mapOf(
            textAdGroupInfo to arrayOf(CampaignGraphQlGetTotalStatsTest.AVG_CLICK_COST, CampaignGraphQlGetTotalStatsTest.CPM_PRICE, CampaignGraphQlGetTotalStatsTest.CLICKS, CampaignGraphQlGetTotalStatsTest.COST),
            cpmAdGroupInfo to arrayOf(avgClickCostForCpm, cpmPriceForCpm, clicksForCpm, costForCpm),
            internalAdGroupInfo to arrayOf(avgClickCostForInternal, cpmPriceForInternal, clicksForInternal, costForInternal),
        )

        doReturnFromYt(campTypeToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, true)

        val data = sendRequestAndGetData(setOf(textAdGroupInfo, cpmAdGroupInfo, internalAdGroupInfo))
        val textAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val textCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val cpmAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val cpmCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val internalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/avgClickCost")
        val internalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")
        val totalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/cpmPrice")

        val soft = SoftAssertions()
        soft.assertThat(textAvgClickCost.toDouble())
            .`as`("Avg click cost for group from text campaign")
            .isEqualTo(CampaignGraphQlGetTotalStatsTest.AVG_CLICK_COST.toDouble())
        soft.assertThat(cpmAvgClickCost)
            .`as`("Avg click cost for group from cpm campaign")
            .isNull()
        soft.assertThat(internalAvgClickCost.toDouble())
            .`as`("Avg click cost for group from internal campaign")
            .isEqualTo(avgClickCostForInternal.toDouble())

        soft.assertThat(textCpmPrice)
            .`as`("Cpm price for group from text campaign")
            .isNull()
        soft.assertThat(cpmCpmPrice.toDouble())
            .`as`("Cpm price for group from cpm campaign")
            .isEqualTo(cpmPriceForCpm.toDouble())
        soft.assertThat(internalCpmPrice.toDouble())
            .`as`("Cpm price for group from internal campaign")
            .isEqualTo(cpmPriceForInternal.toDouble())

        val totalCost = CampaignGraphQlGetTotalStatsTest.COST + costForInternal
        val totalClicks = CampaignGraphQlGetTotalStatsTest.CLICKS + clicksForInternal

        // total avgClickCost = Sum( COSTi / CLICKSi ) / n
        soft.assertThat(totalAvgClickCost.toDouble())
            .`as`("Total avg click cost")
            .isEqualTo(ratio(BigDecimal.valueOf(totalCost), BigDecimal.valueOf(totalClicks))?.toDouble())
        // totalStats/cpmPrice не отправляем на фронт
        soft.assertThat(totalCpmPrice)
            .`as`("Total cpm price")
            .isEqualTo(BigDecimal.ZERO)
        soft.assertAll()
    }

    /**
     * При отборе трех групп от разных типов кампаний (CPC, CPM, не CPC/CPM) и выключенной фиче
     * -> получаем корректные AvgClickCost/cpmPrice по каждой группе и в общей статистике
     */
    @Test
    fun getStats_WithoutFeatureAndMultipleCampaigns() {
        val avgClickCostForCpm = 2_255L
        val cpmPriceForCpm = 3_159L
        val clicksForCpm = 229L
        val costForCpm = 2_351L

        val avgClickCostForInternal = 3_151L
        val cpmPriceForInternal = 4_051L
        val clicksForInternal = 330L
        val costForInternal = 3_911L

        val textAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        )
        val cpmAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        )
        val internalAdGroupInfo = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(INTERNAL_AUTOBUDGET, clientInfo)
        )

        val campTypeToPrices = mapOf(
            textAdGroupInfo to arrayOf(CampaignGraphQlGetTotalStatsTest.AVG_CLICK_COST, CampaignGraphQlGetTotalStatsTest.CPM_PRICE, CampaignGraphQlGetTotalStatsTest.CLICKS, CampaignGraphQlGetTotalStatsTest.COST, CampaignGraphQlGetTotalStatsTest.SHOWS),
            cpmAdGroupInfo to arrayOf(avgClickCostForCpm, cpmPriceForCpm, clicksForCpm, costForCpm),
            internalAdGroupInfo to arrayOf(avgClickCostForInternal, cpmPriceForInternal, clicksForInternal, costForInternal),
        )
        doReturnFromYt(campTypeToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, false)

        val data = sendRequestAndGetData(setOf(textAdGroupInfo, cpmAdGroupInfo, internalAdGroupInfo))
        val textAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val textCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val cpmAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val cpmCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val internalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/avgClickCost")
        val internalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")
        val totalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/cpmPrice")

        val soft = SoftAssertions()
        soft.assertThat(textAvgClickCost.toDouble())
            .`as`("Avg click cost for group from text campaign")
            .isEqualTo(CampaignGraphQlGetTotalStatsTest.AVG_CLICK_COST.toDouble())
        soft.assertThat(cpmAvgClickCost.toDouble())
            .`as`("Avg click cost for group from cpm campaign")
            .isEqualTo(avgClickCostForCpm.toDouble())
        soft.assertThat(internalAvgClickCost.toDouble())
            .`as`("Avg click cost for group from internal campaign")
            .isEqualTo(avgClickCostForInternal.toDouble())

        soft.assertThat(textCpmPrice.toDouble())
            .`as`("Cpm price for group from text campaign")
            .isEqualTo(CampaignGraphQlGetTotalStatsTest.CPM_PRICE.toDouble())
        soft.assertThat(cpmCpmPrice.toDouble())
            .`as`("Cpm price for group from cpm campaign")
            .isEqualTo(cpmPriceForCpm.toDouble())
        soft.assertThat(internalCpmPrice.toDouble())
            .`as`("Cpm price for group from internal campaign")
            .isEqualTo(cpmPriceForInternal.toDouble())

        val totalCost = CampaignGraphQlGetTotalStatsTest.COST + costForCpm + costForInternal
        val totalClicks = CampaignGraphQlGetTotalStatsTest.CLICKS + clicksForCpm + clicksForInternal

        // total avgClickCost = Sum( COSTi / CLICKSi ) / n
        soft.assertThat(totalAvgClickCost.toDouble())
            .`as`("Total avg click cost")
            .isEqualTo(ratio(BigDecimal.valueOf(totalCost), BigDecimal.valueOf(totalClicks))?.toDouble())
        // totalStats/cpmPrice не отправляем на фронт
        soft.assertThat(totalCpmPrice)
            .`as`("Total cpm price")
            .isEqualTo(BigDecimal.ZERO)
        soft.assertAll()
    }

    /**
     * При отборе групп от двух CPC и двух CPM кампаний получаем корректные AvgClickCost/cpmPrice по каждой группе
     * и в общей статистике
     */
    @Test
    fun getStats_WithFeatureAndSameCampaignTypes() {
        val avgClickCostForText2 = 2_255L
        val cpmPriceForText2 = 3_159L
        val clicksForText2 = 229L
        val costForText2 = 2_351L

        val avgClickCostForCpm1 = 3_205L
        val cpmPriceForCpm1 = 4_109L
        val clicksForCpm1 = 109L
        val costForCpm1 = 3_301L

        val avgClickCostForCpm2 = 4_115L
        val cpmPriceForCpm2 = 5_119L
        val clicksForCpm2 = 319L
        val costForCpm2 = 4_311L

        val textAdGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        )
        val textAdGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        )
        val cpmAdGroupInfo1 = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        )
        val cpmAdGroupInfo2 = steps.adGroupSteps().createDefaultAdGroup(
            steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        )

        val campTypeToPrices = mapOf(
            textAdGroupInfo1 to arrayOf(CampaignGraphQlGetTotalStatsTest.AVG_CLICK_COST, CampaignGraphQlGetTotalStatsTest.CPM_PRICE, CampaignGraphQlGetTotalStatsTest.CLICKS, CampaignGraphQlGetTotalStatsTest.COST),
            textAdGroupInfo2 to arrayOf(avgClickCostForText2, cpmPriceForText2, clicksForText2, costForText2),
            cpmAdGroupInfo1 to arrayOf(avgClickCostForCpm1, cpmPriceForCpm1, clicksForCpm1, costForCpm1),
            cpmAdGroupInfo2 to arrayOf(avgClickCostForCpm2, cpmPriceForCpm2, clicksForCpm2, costForCpm2),
        )

        doReturnFromYt(campTypeToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, true)

        val data = sendRequestAndGetData(setOf(textAdGroupInfo1, textAdGroupInfo2, cpmAdGroupInfo1, cpmAdGroupInfo2))
        val textAvgClickCost1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val textCpmPrice1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val textAvgClickCost2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val textCpmPrice2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val cpmAvgClickCost1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/avgClickCost")
        val cpmCpmPrice1 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/cpmPrice")
        val cpmAvgClickCost2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/3/stats/avgClickCost")
        val cpmCpmPrice2 = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/3/stats/cpmPrice")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")
        val totalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/cpmPrice")

        val soft = SoftAssertions()
        soft.assertThat(textAvgClickCost1.toDouble())
            .`as`("Avg click cost for first group from text campaign")
            .isEqualTo(CampaignGraphQlGetTotalStatsTest.AVG_CLICK_COST.toDouble())
        soft.assertThat(textAvgClickCost2.toDouble())
            .`as`("Avg click cost for second group from text campaign")
            .isEqualTo(avgClickCostForText2.toDouble())
        soft.assertThat(cpmAvgClickCost1)
            .`as`("Avg click cost for first group from cpm campaign")
            .isNull()
        soft.assertThat(cpmAvgClickCost2)
            .`as`("Avg click cost for second group from cpm campaign")
            .isNull()

        soft.assertThat(textCpmPrice1)
            .`as`("Cpm price for first group from text campaign")
            .isNull()
        soft.assertThat(textCpmPrice2)
            .`as`("Cpm price for second group from text campaign")
            .isNull()
        soft.assertThat(cpmCpmPrice1.toDouble())
            .`as`("Cpm price for first group from cpm campaign")
            .isEqualTo(cpmPriceForCpm1.toDouble())
        soft.assertThat(cpmCpmPrice2.toDouble())
            .`as`("Cpm price for second group from cpm campaign")
            .isEqualTo(cpmPriceForCpm2.toDouble())

        val totalCost = CampaignGraphQlGetTotalStatsTest.COST + costForText2
        val totalClicks = CampaignGraphQlGetTotalStatsTest.CLICKS + clicksForText2

        // total avgClickCost = Sum( COSTi / CLICKSi ) / n
        soft.assertThat(totalAvgClickCost.toDouble())
            .`as`("Total avg click cost")
            .isEqualTo(ratio(BigDecimal.valueOf(totalCost), BigDecimal.valueOf(totalClicks))?.toDouble())
        // totalStats/cpmPrice не отправляем на фронт
        soft.assertThat(totalCpmPrice)
            .`as`("Total cpm price")
            .isEqualTo(BigDecimal.ZERO)
        soft.assertAll()
    }

    private fun sendRequestAndGetData(adGroupsInfo: Set<AdGroupInfo>): Map<String, Any> {
        val adGroupsContainer = getAdGroupsContainer(
            adGroupsInfo.map { it.campaignId }.toSet(),
            adGroupsInfo.map { it.adGroupId }.toSet())
        val query = String.format(QUERY_TEMPLATE, clientInfo.uid, graphQlSerialize(adGroupsContainer))

        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)

        val clientData = (result.getData<Any>() as Map<*, *>)["client"] as Map<*, *>
        return clientData["adGroups"] as Map<String, Any>
    }

    private fun getAdGroupsContainer(campaignIds: Set<Long>, groupIds: Set<Long>): GdAdGroupsContainer? {
        return AdGroupTestDataUtils.getDefaultGdAdGroupsContainer()
            .withFilter(GdAdGroupFilter()
                .withAdGroupIdIn(groupIds)
                .withCampaignIdIn(campaignIds)
                .withStats(GdEntityStatsFilter()
                    .withMinClicks(1L)))
            .withOrderBy(listOf(ORDER_BY_ID))
    }

    private fun doReturnFromYt(groupToStats: Map<AdGroupInfo, Array<Long>>) {
        Mockito.doReturn(wrapInRowset(
            groupToStats
                .map { (groupInfo, stats) ->
                    val yTreeBuilder = YTree.mapBuilder()
                        .key(GdiEntityStats.AVG_CLICK_COST.name()).value(stats[0] * 1_000_000L)
                        .key(GdiEntityStats.CPM_PRICE.name()).value(stats[1] * 1_000_000L)
                        .key(GdiEntityStats.CLICKS.name()).value(stats[2])
                        .key(GdiEntityStats.COST.name()).value(stats[3] * 1_000_000L)
                        .key(GdiEntityStats.SHOWS.name()).value(SHOWS)
                        .key(Tables.PHRASESTABLE_DIRECT.CID.name).value(groupInfo.campaignId)
                        .key(Tables.PHRASESTABLE_DIRECT.PID.name).value(groupInfo.adGroupId)
                        .key(Tables.PHRASESTABLE_DIRECT.ADGROUP_TYPE.name)
                        .value(groupInfo.adGroupType.name.lowercase(Locale.getDefault()))
                    yTreeBuilder.endMap().build()
                }.toList()))
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any(), ArgumentMatchers.anyBoolean())
    }

    private fun wrapInRowset(nodes: List<YTreeNode>): UnversionedRowset? {
        val rowset = Mockito.mock(UnversionedRowset::class.java)
        Mockito.doReturn(nodes).`when`(rowset).yTreeRows
        return rowset
    }
}
