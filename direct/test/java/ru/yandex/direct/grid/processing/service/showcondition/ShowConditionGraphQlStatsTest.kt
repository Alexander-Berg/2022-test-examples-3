package ru.yandex.direct.grid.processing.service.showcondition

import com.google.common.collect.ImmutableMap
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
import ru.yandex.direct.bsauction.BsTrafaretClient
import ru.yandex.direct.common.util.RepositoryUtils.booleanToLong
import ru.yandex.direct.core.entity.campaign.model.CampaignType
import ru.yandex.direct.core.entity.campaign.model.CampaignType.CPM_DEALS
import ru.yandex.direct.core.entity.campaign.model.CampaignType.INTERNAL_AUTOBUDGET
import ru.yandex.direct.core.entity.campaign.model.CampaignType.TEXT
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.info.KeywordInfo
import ru.yandex.direct.core.testing.mock.BsTrafaretClientMockUtils
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.entity.model.GdiEntityStats
import ru.yandex.direct.grid.core.entity.showcondition.model.GdiShowConditionType
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.model.GdEntityStatsFilter
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.showcondition.GdShowConditionFilter
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils
import ru.yandex.direct.grid.processing.util.ShowConditionTestDataUtils
import ru.yandex.direct.grid.processing.util.StatCalculationHelper.ratio
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.grid.schema.yt.Tables.BIDSTABLE_DIRECT
import ru.yandex.direct.integrations.configuration.IntegrationsConfiguration
import ru.yandex.direct.pokazometer.GroupResponse
import ru.yandex.direct.pokazometer.PhraseRequest
import ru.yandex.direct.pokazometer.PhraseResponse
import ru.yandex.direct.pokazometer.PokazometerClient
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.ytree.YTreeNode
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.math.BigDecimal
import java.util.IdentityHashMap

private val QUERY_TEMPLATE = """
    {
      client(searchBy: {login: "%s"}) {
        showConditions(input: %s) {
          rowset {
             stats {
                 avgClickCost
                 cpmPrice
             }
             ... on GdKeyword {
                 pokazometerData {
                   __typename
                 }
                 auctionData {
                   __typename
                 }
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
class ShowConditionGraphQlStatsTest {
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

    @Autowired
    @Qualifier(IntegrationsConfiguration.BS_TRAFARET_AUCTION_CLIENT)
    private lateinit var bsTrafaretClient: BsTrafaretClient

    @Autowired
    private lateinit var pokazometerClient: PokazometerClient

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

        BsTrafaretClientMockUtils.setCustomMockOnBsTrafaretClient(bsTrafaretClient,
            BigDecimal.valueOf(5_000_000),
            BigDecimal.valueOf(10_000_000))
    }

    @After
    fun after() {
        doReturn(Mockito.mock(UnversionedRowset::class.java))
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any(), ArgumentMatchers.anyBoolean())
        Mockito.reset(pokazometerClient)
    }

    fun parametrizedTestData() = listOf(
        listOf("Keyword in TEXT campaign with feature", true, TEXT, true, false, true),
        listOf("Keyword in CPM campaign with feature", true, CPM_DEALS, false, true, false),
        listOf("Keyword in INTERNAL campaign with feature", true, INTERNAL_AUTOBUDGET, true, true, true),
        listOf("Keyword in TEXT campaign without feature", false, TEXT, true, true, true),
        listOf("Keyword in CPM campaign without feature", false, CPM_DEALS, true, true, true),
        listOf("Keyword in INTERNAL campaign without feature", false, INTERNAL_AUTOBUDGET, true, true, true),
    )

    /**
     * Проверка получения корректных AvgClickCost/cpmPrice для разных фраз в разных типах кампаний
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
        expectTrafaretData: Boolean,
    ) {
        val campaignInfo = steps.campaignSteps().createActiveCampaignByCampaignType(campaignType, clientInfo)
        val adGroupInfo = steps.adGroupSteps().createDefaultAdGroup(campaignInfo)
        val keywordInfo = steps.keywordSteps().createKeyword(adGroupInfo)

        steps.bannerSteps().createBanner(
            activeTextBanner(keywordInfo.campaignId, keywordInfo.adGroupId), keywordInfo.adGroupInfo)

        mockDataFromYtAndStats(mapOf(keywordInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST)))

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, enableFeature)

        val data = sendRequestAndGetData(setOf(keywordInfo))
        val avgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val cpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val auctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/0/auctionData")
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

        if (expectTrafaretData) {
            soft.assertThat(auctionData)
                .`as`("auction data")
                .isNotNull
        } else {
            soft.assertThat(auctionData)
                .`as`("auction data")
                .isNull()
        }

        // totalStats/cpmPrice не отправляем на фронт
        soft.assertThat(totalCpmPrice)
            .`as`("Total cpm price")
            .isEqualTo(BigDecimal.ZERO)
        soft.assertAll()
    }


    /**
     * При отборе трех фраз от разных типов кампаний (CPC, CPM, не CPC/CPM) и включенной фиче
     * -> получаем корректные AvgClickCost/cpmPrice по каждой фразе и в общей статистике
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

        val textKeywordInfo = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo)
        )
        val cpmKeywordInfo = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo)
        )
        val internalKeywordInfo = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(internalCampaignInfo)
        )
        steps.bannerSteps().createBanner(
            activeTextBanner(textKeywordInfo.campaignId, textKeywordInfo.adGroupId), textKeywordInfo.adGroupInfo)
        steps.bannerSteps().createBanner(
            activeTextBanner(cpmKeywordInfo.campaignId, cpmKeywordInfo.adGroupId), cpmKeywordInfo.adGroupInfo)
        steps.bannerSteps().createBanner(
            activeTextBanner(internalKeywordInfo.campaignId, internalKeywordInfo.adGroupId), internalKeywordInfo.adGroupInfo)

        val keywordInfoToPrices = mapOf(
            textKeywordInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST),
            cpmKeywordInfo to arrayOf(avgClickCostForCpm, cpmPriceForCpm, clicksForCpm, costForCpm),
            internalKeywordInfo to arrayOf(avgClickCostForInternal, cpmPriceForInternal, clicksForInternal, costForInternal),
        )
        mockDataFromYtAndStats(keywordInfoToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, true)

        val data = sendRequestAndGetData(setOf(textKeywordInfo, cpmKeywordInfo, internalKeywordInfo))
        val textAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val textCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val textPokazometerData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/0/pokazometerData")
        val textAuctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/0/auctionData")
        val cpmAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val cpmCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val cpmPokazometerData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/1/pokazometerData")
        val cpmAuctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/1/auctionData")
        val internalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/avgClickCost")
        val internalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/cpmPrice")
        val internalPokazometerData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/2/pokazometerData")
        val internalAuctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/2/auctionData")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")
        val totalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/cpmPrice")

        val soft = SoftAssertions()
        soft.assertThat(textAvgClickCost.toDouble())
            .`as`("Avg click cost for keyword from text campaign")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(cpmAvgClickCost)
            .`as`("Avg click cost for keyword from cpm campaign")
            .isNull()
        soft.assertThat(internalAvgClickCost.toDouble())
            .`as`("Avg click cost for keyword from internal campaign")
            .isEqualTo(avgClickCostForInternal.toDouble())

        soft.assertThat(textCpmPrice)
            .`as`("Cpm price for keyword from text campaign")
            .isNull()
        soft.assertThat(cpmCpmPrice.toDouble())
            .`as`("Cpm price for keyword from cpm campaign")
            .isEqualTo(cpmPriceForCpm.toDouble())
        soft.assertThat(internalCpmPrice.toDouble())
            .`as`("Cpm price for keyword from internal campaign")
            .isEqualTo(cpmPriceForInternal.toDouble())

        soft.assertThat(textPokazometerData)
            .`as`("Pokazometer data from text campaign")
            .isNotNull
        soft.assertThat(cpmPokazometerData)
            .`as`("Pokazometer data from cpm campaign")
            .isNull()
        soft.assertThat(internalPokazometerData)
            .`as`("Pokazometer data from internal campaign")
            .isNotNull

        soft.assertThat(textAuctionData)
            .`as`("Auction data from text campaign")
            .isNotNull
        soft.assertThat(cpmAuctionData)
            .`as`("Auction data from cpm campaign")
            .isNull()
        soft.assertThat(internalAuctionData)
            .`as`("Auction data from internal campaign")
            .isNotNull

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
     * При отборе трех фраз от разных типов кампаний (CPC, CPM, не CPC/CPM) и выключенной фиче
     * -> получаем корректные AvgClickCost/cpmPrice по каждой фразе и в общей статистике
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

        val textKeywordInfo = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo)
        )
        val cpmKeywordInfo = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo)
        )
        val internalKeywordInfo = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(internalCampaignInfo)
        )
        steps.bannerSteps().createBanner(
            activeTextBanner(textKeywordInfo.campaignId, textKeywordInfo.adGroupId), textKeywordInfo.adGroupInfo)
        steps.bannerSteps().createBanner(
            activeTextBanner(cpmKeywordInfo.campaignId, cpmKeywordInfo.adGroupId), cpmKeywordInfo.adGroupInfo)
        steps.bannerSteps().createBanner(
            activeTextBanner(internalKeywordInfo.campaignId, internalKeywordInfo.adGroupId), internalKeywordInfo.adGroupInfo)

        val keywordInfoToPrices = mapOf(
            textKeywordInfo to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST, SHOWS),
            cpmKeywordInfo to arrayOf(avgClickCostForCpm, cpmPriceForCpm, clicksForCpm, costForCpm),
            internalKeywordInfo to arrayOf(avgClickCostForInternal, cpmPriceForInternal, clicksForInternal, costForInternal),
        )
        mockDataFromYtAndStats(keywordInfoToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, false)

        val data = sendRequestAndGetData(setOf(textKeywordInfo, cpmKeywordInfo, internalKeywordInfo))
        val textAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/avgClickCost")
        val textCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/0/stats/cpmPrice")
        val textPokazometerData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/0/pokazometerData")
        val textAuctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/0/auctionData")
        val cpmAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/avgClickCost")
        val cpmCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/1/stats/cpmPrice")
        val cpmPokazometerData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/1/pokazometerData")
        val cpmAuctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/1/auctionData")
        val internalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/avgClickCost")
        val internalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "rowset/2/stats/cpmPrice")
        val internalPokazometerData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/2/pokazometerData")
        val internalAuctionData = GraphQLUtils.getDataValue<Map<*, *>>(data, "rowset/2/auctionData")
        val totalAvgClickCost = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/avgClickCost")
        val totalCpmPrice = GraphQLUtils.getDataValue<BigDecimal>(data, "totalStats/cpmPrice")

        val soft = SoftAssertions()
        soft.assertThat(textAvgClickCost.toDouble())
            .`as`("Avg click cost for keyword from text campaign")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(cpmAvgClickCost.toDouble())
            .`as`("Avg click cost for keyword from cpm campaign")
            .isEqualTo(avgClickCostForCpm.toDouble())
        soft.assertThat(internalAvgClickCost.toDouble())
            .`as`("Avg click cost for keyword from internal campaign")
            .isEqualTo(avgClickCostForInternal.toDouble())

        soft.assertThat(textCpmPrice.toDouble())
            .`as`("Cpm price for keyword from text campaign")
            .isEqualTo(CPM_PRICE.toDouble())
        soft.assertThat(cpmCpmPrice.toDouble())
            .`as`("Cpm price for keyword from cpm campaign")
            .isEqualTo(cpmPriceForCpm.toDouble())
        soft.assertThat(internalCpmPrice.toDouble())
            .`as`("Cpm price for keyword from internal campaign")
            .isEqualTo(cpmPriceForInternal.toDouble())

        soft.assertThat(textPokazometerData)
            .`as`("Pokazometer data from text campaign")
            .isNotNull
        soft.assertThat(cpmPokazometerData)
            .`as`("Pokazometer data from cpm campaign")
            .isNotNull
        soft.assertThat(internalPokazometerData)
            .`as`("Pokazometer data from internal campaign")
            .isNotNull

        soft.assertThat(textAuctionData)
            .`as`("Auction data from text campaign")
            .isNotNull
        soft.assertThat(cpmAuctionData)
            .`as`("Auction data from cpm campaign")
            .isNotNull
        soft.assertThat(internalAuctionData)
            .`as`("Auction data from internal campaign")
            .isNotNull

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
     * При отборе фраз от двух CPC и двух CPM кампаний получаем корректные AvgClickCost/cpmPrice
     * по каждой фразе и в общей статистике
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

        val textKeywordInfo1 = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo1)
        )
        val textKeywordInfo2 = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(textCampaignInfo2)
        )
        val cpmKeywordInfo1 = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo1)
        )
        val cpmKeywordInfo2 = steps.keywordSteps().createKeyword(
            steps.adGroupSteps().createDefaultAdGroup(cpmCampaignInfo2)
        )

        val keywordInfoToPrices = mapOf(
            textKeywordInfo1 to arrayOf(AVG_CLICK_COST, CPM_PRICE, CLICKS, COST),
            textKeywordInfo2 to arrayOf(avgClickCostForText2, cpmPriceForText2, clicksForText2, costForText2),
            cpmKeywordInfo1 to arrayOf(avgClickCostForCpm1, cpmPriceForCpm1, clicksForCpm1, costForCpm1),
            cpmKeywordInfo2 to arrayOf(avgClickCostForCpm2, cpmPriceForCpm2, clicksForCpm2, costForCpm2),
        )
        mockDataFromYtAndStats(keywordInfoToPrices)

        steps.featureSteps().addClientFeature(clientId, FeatureName.CPC_AND_CPM_ON_ONE_GRID_ENABLED, true)

        val data = sendRequestAndGetData(setOf(textKeywordInfo1, textKeywordInfo2, cpmKeywordInfo1, cpmKeywordInfo2))
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
            .`as`("Avg click cost for first keyword from text campaign")
            .isEqualTo(AVG_CLICK_COST.toDouble())
        soft.assertThat(textAvgClickCost2.toDouble())
            .`as`("Avg click cost for second keyword from text campaign")
            .isEqualTo(avgClickCostForText2.toDouble())
        soft.assertThat(cpmAvgClickCost1)
            .`as`("Avg click cost for first keyword from cpm campaign")
            .isNull()
        soft.assertThat(cpmAvgClickCost2)
            .`as`("Avg click cost for second keyword from cpm campaign")
            .isNull()

        soft.assertThat(textCpmPrice1)
            .`as`("Cpm price for first keyword from text campaign")
            .isNull()
        soft.assertThat(textCpmPrice2)
            .`as`("Cpm price for second keyword from text campaign")
            .isNull()
        soft.assertThat(cpmCpmPrice1.toDouble())
            .`as`("Cpm price for first keyword from cpm campaign")
            .isEqualTo(cpmPriceForCpm1.toDouble())
        soft.assertThat(cpmCpmPrice2.toDouble())
            .`as`("Cpm price for second keyword from cpm campaign")
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

    private fun sendRequestAndGetData(keywordInfo: Set<KeywordInfo>): Map<String, Any> {
        val container = getKeywordsContainer(keywordInfo)

        val query = String.format(QUERY_TEMPLATE, clientInfo.login, GraphQlJsonUtils.graphQlSerialize(container))
        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)

        val clientData = (result.getData<Any>() as Map<*, *>)["client"] as Map<*, *>
        return clientData["showConditions"] as Map<String, Any>
    }

    private fun getKeywordsContainer(
        keywordsInfo: Set<KeywordInfo>
    ) = ShowConditionTestDataUtils.getDefaultGdShowConditionsContainer()
        .withFilter(GdShowConditionFilter()
            .withShowConditionIdIn(keywordsInfo.map { it.id }.toSet())
            .withCampaignIdIn(keywordsInfo.map { it.campaignId }.toSet())
            .withStats(GdEntityStatsFilter().withMinClicks(1L))
        )
        .withOrderBy(listOf(ShowConditionGraphQlServiceGetTotalStatsTest.ORDER_BY_ID))


    private fun mockDataFromYtAndStats(keywordToStats: Map<KeywordInfo, Array<Long>>) {
        val rowset = wrapInRowset(
            keywordToStats
                .map { (keywordInfo, stats) ->
                    val yTreeBuilder = YTree.mapBuilder()
                        .key(GdiEntityStats.AVG_CLICK_COST.name()).value(stats[0] * 1_000_000L)
                        .key(GdiEntityStats.CPM_PRICE.name()).value(stats[1] * 1_000_000L)
                        .key(GdiEntityStats.CLICKS.name()).value(stats[2])
                        .key(GdiEntityStats.COST.name()).value(stats[3] * 1_000_000L)
                        .key(GdiEntityStats.SHOWS.name()).value(SHOWS)
                        .key(BIDSTABLE_DIRECT.CID.name).value(keywordInfo.campaignId)
                        .key(BIDSTABLE_DIRECT.PID.name).value(keywordInfo.adGroupId)
                        .key(BIDSTABLE_DIRECT.ID.name).value(keywordInfo.id)
                        .key(BIDSTABLE_DIRECT.PHRASE_ID.name).value(keywordInfo.id)
                        .key(BIDSTABLE_DIRECT.PHRASE.name).value(keywordInfo.keyword.phrase)
                        .key(BIDSTABLE_DIRECT.IS_SUSPENDED.name).value(keywordInfo.keyword.isSuspended)
                        .key(BIDSTABLE_DIRECT.IS_DELETED.name).value(booleanToLong(false))
                        .key(BIDSTABLE_DIRECT.BID_TYPE.name).value(GdiShowConditionType.KEYWORD.name.lowercase())
                        .key(BIDSTABLE_DIRECT.IS_ARCHIVED.name).value(booleanToLong(false))
                        .key(BIDSTABLE_DIRECT.PRICE_CONTEXT.name).value(stats[1] * 1_000_000L)
                        .key(BIDSTABLE_DIRECT.PRICE.name).value(stats[1] * 1_000_000L)
                    yTreeBuilder.endMap().build()
                }.toList())
        doReturn(rowset)
            .`when`(gridYtSupport).selectRows(ArgumentMatchers.eq(shard), any(), ArgumentMatchers.anyBoolean())

        val phraseResponse: List<PhraseResponse> = keywordToStats.keys
            .map { PhraseResponse.on(PhraseRequest(it.keyword.phrase, 1000_000L, it.id)) }
            .onEach { it.contextCoverage = 1000_000L }
            .onEach { it.setPriceByCoverage(PhraseResponse.Coverage.LOW, 100_000L) }
            .onEach { it.setPriceByCoverage(PhraseResponse.Coverage.MEDIUM, 100_000L) }
            .onEach { it.setPriceByCoverage(PhraseResponse.Coverage.HIGH, 100_000L) }
            .onEach { it.clicksByCost = ImmutableMap.of(100_000L, 9, 400_000L, 21, 1_000_000L, 42) }

        doReturn(mapOf(null to GroupResponse.success(phraseResponse)).toMap(IdentityHashMap()))
            .`when`(pokazometerClient)
            .get(ArgumentMatchers.anyList())
    }

    private fun wrapInRowset(nodes: List<YTreeNode>): UnversionedRowset {
        val rowset = Mockito.mock(UnversionedRowset::class.java)
        doReturn(nodes).`when`(rowset).yTreeRows
        return rowset
    }
}
