package ru.yandex.direct.grid.processing.service.showcondition.retargeting

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
import org.mockito.Mockito.doReturn
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_DEALS
import ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_AUTOBUDGET
import ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.RetargetingInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.model.GdEntityStatsFilter
import ru.yandex.direct.grid.model.GdStatRequirements
import ru.yandex.direct.grid.model.Order
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.GdLimitOffset
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingFilter
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderBy
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingOrderByField
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingsContainer
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.StatCalculationHelper.ratio
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.schema.yt.Tables.BIDS_RETARGETINGTABLE_DIRECT
import ru.yandex.direct.grid.schema.yt.Tables.RETARGETING_CONDITIONSTABLE_DIRECT
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.math.BigDecimal
import java.time.LocalDate

private val QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        retargetings(input: %s) {
          rowset {
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
class GetRetargetingGraphQlStatsTest {
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
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any(), ArgumentMatchers.anyBoolean())
        doReturn(Mockito.mock(UnversionedRowset::class.java))
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any())
    }

    fun parametrizedTestData() = listOf(
        listOf("Retargeting in TEXT campaign with feature", true, TEXT, true, false),
        listOf("Retargeting in CPM campaign with feature", true, CPM_DEALS, false, true),
        listOf("Retargeting in INTERNAL campaign with feature", true, INTERNAL_AUTOBUDGET, true, true),
        listOf("Retargeting in TEXT campaign without feature", false, TEXT, true, true),
        listOf("Retargeting in CPM campaign without feature", false, CPM_DEALS, true, true),
        listOf("Retargeting in INTERNAL campaign without feature", false, INTERNAL_AUTOBUDGET, true, true),
    )

    /**
     * Проверка получения корректных AvgClickCost/cpmPrice для разных ретаргетингов в разных типах кампаний
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
        val retargetingInfo = steps.retargetingSteps().createDefaultRetargeting(adGroupInfo)

        doReturnFromYt(mapOf(retargetingInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST)))

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(retargetingInfo))
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
     * При отборе трех ретаргетингов от разных типов кампаний (CPC, CPM, не CPC/CPM) и включенной фиче
     * -> получаем корректные AvgClickCost/cpmPrice по каждому ретаргетингу и в общей статистике
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

        val textCampaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        val cpmCampaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        val internalCampaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(INTERNAL_AUTOBUDGET, clientInfo)

        val textRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo)
        )
        val cpmRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo)
        )
        val internalRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(internalCampaignInfo)
        )

        val retargetingInfoToPrices = mapOf(
            textRetargetingInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST),
            cpmRetargetingInfo to arrayOf(avgClickCostForCpm, cpmPriceForCpm, clicksForCpm, costForCpm),
            internalRetargetingInfo to arrayOf(avgClickCostForInternal, cpmPriceForInternal, clicksForInternal, costForInternal),
        )
        doReturnFromYt(retargetingInfoToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, true)

        val data = sendRequestAndGetData(setOf(textRetargetingInfo, cpmRetargetingInfo, internalRetargetingInfo))
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
            .`as`("Avg click cost for retargeting from text campaign")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(cpmAvgClickCost)
            .`as`("Avg click cost for retargeting from cpm campaign")
            .isNull()
        soft.assertThat(internalAvgClickCost.toDouble())
            .`as`("Avg click cost for retargeting from internal campaign")
            .isEqualTo(avgClickCostForInternal.toDouble())

        soft.assertThat(textCpmPrice)
            .`as`("Cpm price for retargeting from text campaign")
            .isNull()
        soft.assertThat(cpmCpmPrice.toDouble())
            .`as`("Cpm price for retargeting from cpm campaign")
            .isEqualTo(cpmPriceForCpm.toDouble())
        soft.assertThat(internalCpmPrice.toDouble())
            .`as`("Cpm price for retargeting from internal campaign")
            .isEqualTo(cpmPriceForInternal.toDouble())

        val totalCost = COST + costForInternal
        val totalClicks = CLICKS + clicksForInternal

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
     * При отборе трех ретаргетингов от разных типов кампаний (CPC, CPM, не CPC/CPM) и выключенной фиче
     * -> получаем корректные AvgClickCost/cpmPrice по каждому ретаргетингу и в общей статистике
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

        val textCampaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        val cpmCampaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        val internalCampaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(INTERNAL_AUTOBUDGET, clientInfo)

        val textRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo)
        )
        val cpmRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo)
        )
        val internalRetargetingInfo = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(internalCampaignInfo)
        )

        val retargetingInfoToPrices = mapOf(
            textRetargetingInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST, SHOWS),
            cpmRetargetingInfo to arrayOf(avgClickCostForCpm, cpmPriceForCpm, clicksForCpm, costForCpm),
            internalRetargetingInfo to arrayOf(avgClickCostForInternal, cpmPriceForInternal, clicksForInternal, costForInternal),
        )
        doReturnFromYt(retargetingInfoToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, false)

        val data = sendRequestAndGetData(setOf(textRetargetingInfo, cpmRetargetingInfo, internalRetargetingInfo))
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
            .`as`("Avg click cost for retargeting from text campaign")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(cpmAvgClickCost.toDouble())
            .`as`("Avg click cost for retargeting from cpm campaign")
            .isEqualTo(avgClickCostForCpm.toDouble())
        soft.assertThat(internalAvgClickCost.toDouble())
            .`as`("Avg click cost for retargeting from internal campaign")
            .isEqualTo(avgClickCostForInternal.toDouble())

        soft.assertThat(textCpmPrice.toDouble())
            .`as`("Cpm price for retargeting from text campaign")
            .isEqualTo(CPM_PRICE.toDouble())
        soft.assertThat(cpmCpmPrice.toDouble())
            .`as`("Cpm price for retargeting from cpm campaign")
            .isEqualTo(cpmPriceForCpm.toDouble())
        soft.assertThat(internalCpmPrice.toDouble())
            .`as`("Cpm price for retargeting from internal campaign")
            .isEqualTo(cpmPriceForInternal.toDouble())

        val totalCost = COST + costForCpm + costForInternal
        val totalClicks = CLICKS + clicksForCpm + clicksForInternal

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
     * При отборе ретаргетингов от двух CPC и двух CPM кампаний получаем корректные AvgClickCost/cpmPrice
     * по каждому ретаргетингу и в общей статистике
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

        val textCampaignInfo1 = steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        val textCampaignInfo2 = steps.campaignSteps().createActiveCampaignByCampaignType(TEXT, clientInfo)
        val cpmCampaignInfo1 = steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)
        val cpmCampaignInfo2 = steps.campaignSteps().createActiveCampaignByCampaignType(CPM_DEALS, clientInfo)

        val textRetargetingInfo1 = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo1)
        )
        val textRetargetingInfo2 = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo2)
        )
        val cpmRetargetingInfo1 = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo1)
        )
        val cpmRetargetingInfo2 = steps.retargetingSteps().createDefaultRetargeting(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo2)
        )

        val retargetingInfoToPrices = mapOf(
            textRetargetingInfo1 to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST),
            textRetargetingInfo2 to arrayOf(avgClickCostForText2, cpmPriceForText2, clicksForText2, costForText2),
            cpmRetargetingInfo1 to arrayOf(avgClickCostForCpm1, cpmPriceForCpm1, clicksForCpm1, costForCpm1),
            cpmRetargetingInfo2 to arrayOf(avgClickCostForCpm2, cpmPriceForCpm2, clicksForCpm2, costForCpm2),
        )
        doReturnFromYt(retargetingInfoToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, true)

        val data = sendRequestAndGetData(setOf(textRetargetingInfo1, textRetargetingInfo2, cpmRetargetingInfo1, cpmRetargetingInfo2))
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
            .`as`("Avg click cost for first retargeting from text campaign")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(textAvgClickCost2.toDouble())
            .`as`("Avg click cost for second retargeting from text campaign")
            .isEqualTo(avgClickCostForText2.toDouble())
        soft.assertThat(cpmAvgClickCost1)
            .`as`("Avg click cost for first retargeting from cpm campaign")
            .isNull()
        soft.assertThat(cpmAvgClickCost2)
            .`as`("Avg click cost for second retargeting from cpm campaign")
            .isNull()

        soft.assertThat(textCpmPrice1)
            .`as`("Cpm price for first retargeting from text campaign")
            .isNull()
        soft.assertThat(textCpmPrice2)
            .`as`("Cpm price for second retargeting from text campaign")
            .isNull()
        soft.assertThat(cpmCpmPrice1.toDouble())
            .`as`("Cpm price for first retargeting from cpm campaign")
            .isEqualTo(cpmPriceForCpm1.toDouble())
        soft.assertThat(cpmCpmPrice2.toDouble())
            .`as`("Cpm price for second retargeting from cpm campaign")
            .isEqualTo(cpmPriceForCpm2.toDouble())

        val totalCost = COST + costForText2
        val totalClicks = CLICKS + clicksForText2

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

    private fun sendRequestAndGetData(retargetingInfo: Set<RetargetingInfo>): Map<String, Any> {
        val container = getRetargetingsContainer(retargetingInfo)

        val query = String.format(QUERY_TEMPLATE, clientInfo.login,
            GraphQlJsonUtils.graphQlSerialize(container))
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)

        val clientData = (result.getData<Any>() as Map<*, *>)["client"] as Map<*, *>
        return clientData["retargetings"] as Map<String, Any>
    }

    private fun getRetargetingsContainer(retargetingInfo: Set<RetargetingInfo>): GdRetargetingsContainer {
        val filter = GdRetargetingFilter()
            .withCampaignIdIn(retargetingInfo.map { it.campaignId }.toSet())
            .withAdGroupIdIn(retargetingInfo.map { it.adGroupId }.toSet())
            .withStats(GdEntityStatsFilter()
                .withMinClicks(1L))

        val now = LocalDate.now()
        return GdRetargetingsContainer()
            .withOrderBy(listOf(GdRetargetingOrderBy()
                .withField(GdRetargetingOrderByField.CAMPAIGN_ID)
                .withOrder(Order.ASC)))
            .withStatRequirements(GdStatRequirements()
                .withFrom(now.minusDays(1))
                .withTo(now))
            .withLimitOffset(GdLimitOffset().withOffset(0).withLimit(100))
            .withFilter(filter)
    }

    private fun doReturnFromYt(retargetingToStats: Map<RetargetingInfo, Array<Long>>) {
        val rowset = wrapInRowset(
            retargetingToStats
                .map { (retargetingInfo, stats) ->
                    val yTreeBuilder = YTree.mapBuilder()
                        .key(GdiEntityStats.AVG_CLICK_COST.name()).value(stats[0] * 1_000_000L)
                        .key(GdiEntityStats.CPM_PRICE.name()).value(stats[1] * 1_000_000L)
                        .key(GdiEntityStats.CLICKS.name()).value(stats[2])
                        .key(GdiEntityStats.COST.name()).value(stats[3] * 1_000_000L)
                        .key(GdiEntityStats.SHOWS.name()).value(SHOWS)
                        .key(BIDS_RETARGETINGTABLE_DIRECT.RET_ID.name).value(retargetingInfo.retargetingId)
                        .key(BIDS_RETARGETINGTABLE_DIRECT.CID.name).value(retargetingInfo.campaignId)
                        .key(BIDS_RETARGETINGTABLE_DIRECT.PID.name).value(retargetingInfo.adGroupId)
                        .key(RETARGETING_CONDITIONSTABLE_DIRECT.CLIENT_ID.name).value(retargetingInfo.clientId.asLong())
                        .key(RETARGETING_CONDITIONSTABLE_DIRECT.RET_COND_ID.name).value(retargetingInfo.retConditionId)
                    yTreeBuilder.endMap().build()
                }.toList())

        doReturn(rowset)
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any(), ArgumentMatchers.anyBoolean())
        doReturn(rowset)
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any())
    }

    private fun wrapInRowset(nodes: List<YTreeNode>): UnversionedRowset {
        val rowset = Mockito.mock(UnversionedRowset::class.java)
        doReturn(nodes).`when`(rowset).yTreeRows
        return rowset
    }
}
