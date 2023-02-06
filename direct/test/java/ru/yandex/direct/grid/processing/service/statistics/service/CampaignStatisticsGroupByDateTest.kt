package ru.yandex.direct.grid.processing.service.statistics.service

import junitparams.JUnitParamsRunner
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.dbutil.model.ClientId
import ru.yandex.direct.grid.processing.configuration.GridProcessingConfiguration
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsColumn.AVG_CPM
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsColumn.SHOWS
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsContainer
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsFilter
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupBy.AGE
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupBy.REGION
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupByDate
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod
import ru.yandex.direct.grid.processing.processor.GridGraphQLProcessor
import ru.yandex.direct.grid.processing.service.dataloader.GridContextProvider
import ru.yandex.direct.grid.processing.util.ContextHelper
import ru.yandex.direct.grid.processing.util.GraphQLUtils
import ru.yandex.direct.grid.processing.util.GraphQlJsonUtils.graphQlSerialize
import ru.yandex.direct.grid.processing.util.TestAuthHelper
import ru.yandex.direct.intapi.client.IntApiClient
import ru.yandex.direct.intapi.client.model.handle.CampaignStatisticsIntApiHandle
import ru.yandex.direct.intapi.client.model.request.statistics.CampaignStatisticsRequest
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse
import ru.yandex.direct.liveresource.LiveResourceFactory
import java.time.LocalDate

private val QUERY_TEMPLATE = """
        {
          client(searchBy: {userId: %s}) {
            campaignStatistics(input: %s) {
              period
              rowset {
                age
                date
              }
              totals {
                 avgCpm {
                   value
                 }
                 clicks {
                   value
                 }
                 cost {
                   value
                 }
              }
            }
          }
        }
    """.trimIndent()

@GridProcessingTest
@RunWith(JUnitParamsRunner::class)
class CampaignStatisticsGroupByDateTest {

    companion object {
        @ClassRule
        @JvmField
        val statisticsHandle = CampaignStatisticsIntApiHandle("stub")

        val springClassRule = SpringClassRule()

        private const val COUNTER_ID = 123

        const val AGES_0_17 = "_0_17"
        const val AGES_45_54 = "_45_54"
        const val AGES_UNKNOWN = "UNKNOWN"
        const val AGES_55_ = "_55_"
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
    private lateinit var intApiClient: IntApiClient

    private var shard = 0
    private lateinit var operator: User
    private lateinit var clientId: ClientId
    private lateinit var clientInfo: ClientInfo
    private lateinit var campaignInfo: CampaignInfo
    private lateinit var context: GridGraphQLContext
    private val requestCaptor = ArgumentCaptor.forClass(CampaignStatisticsRequest::class.java)

    @Before
    fun before() {
        val textCampaign = TestCampaigns.activeTextCampaign(null, null).apply {
            metrikaCounters = listOf(COUNTER_ID.toLong())
        }
        campaignInfo = steps.campaignSteps().createCampaign(textCampaign)
        clientInfo = campaignInfo.clientInfo
        clientId = clientInfo.clientId!!
        operator = clientInfo.chiefUserInfo!!.user!!
        shard = clientInfo.shard

        TestAuthHelper.setDirectAuthentication(operator)

        context = ContextHelper.buildContext(operator)
            .withFetchedFieldsReslover(null)
        gridContextProvider.gridContext = context
    }

    private fun mockStatFromIntApi(
        items: List<CampaignStatisticsItem>,
    ) {
        val intApiResponse = CampaignStatisticsResponse().apply {
            data = items
            totals = CampaignStatisticsItem()
        }

        Mockito.doReturn(intApiResponse).`when`(intApiClient).getCampaignStatistics(requestCaptor.capture())
    }

    /**
     * Проверка группировки по дням при получении общей статистики
     */
    @Test
    fun getCampaignStatisticsGroupByDayTest() {
        val campaignStatisticsItems =
            deserializeStatisticsResponseFrom("classpath:///group_by_date/campaign_statistics_by_day_example.json").data
        mockStatFromIntApi(campaignStatisticsItems)

        val expectedDate0 = "2020-02-28"
        val expectedDate1 = "2020-02-29"
        val expectedDate2 = "2020-02-29"
        val expectedDate3 = "2020-02-29"
        val expectedPeriod = GdCampaignStatisticsGroupByDate.DAY.name

        val data = sendRequestAndGetData(GdCampaignStatisticsGroupByDate.DAY)

        val age0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/age")
        val age1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/age")
        val age2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/age")
        val age3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/age")

        val period = GraphQLUtils.getDataValue<String>(data, "period")

        val date0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/date")
        val date1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/date")
        val date2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/date")
        val date3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/date")

        val soft = SoftAssertions()
        soft.assertThat(age0)
            .isEqualTo(AGES_55_)
        soft.assertThat(age1)
            .isEqualTo(AGES_UNKNOWN)
        soft.assertThat(age2)
            .isEqualTo(AGES_0_17)
        soft.assertThat(age3)
            .isEqualTo(AGES_45_54)

        soft.assertThat(period)
            .isEqualTo(expectedPeriod)

        soft.assertThat(date0)
            .isEqualTo(expectedDate0)
        soft.assertThat(date1)
            .isEqualTo(expectedDate1)
        soft.assertThat(date2)
            .isEqualTo(expectedDate2)
        soft.assertThat(date3)
            .isEqualTo(expectedDate3)

        soft.assertAll()
    }

    /**
     * Проверка группировки по неделям при получении общей статистики
     */
    @Test
    fun getCampaignStatisticsGroupByWeekTest() {
        val campaignStatisticsItems =
            deserializeStatisticsResponseFrom("classpath:///group_by_date/campaign_statistics_by_week_example.json").data
        mockStatFromIntApi(campaignStatisticsItems)

        val expectedDate0 = "2020-02-24"
        val expectedDate1 = "2020-02-24"
        val expectedDate2 = "2020-02-24"
        val expectedDate3 = "2020-03-02"
        val expectedPeriod = GdCampaignStatisticsGroupByDate.WEEK.name

        val data = sendRequestAndGetData(GdCampaignStatisticsGroupByDate.WEEK)

        val age0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/age")
        val age1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/age")
        val age2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/age")
        val age3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/age")

        val period = GraphQLUtils.getDataValue<String>(data, "period")

        val date0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/date")
        val date1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/date")
        val date2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/date")
        val date3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/date")

        val soft = SoftAssertions()
        soft.assertThat(age0)
            .isEqualTo(AGES_UNKNOWN)
        soft.assertThat(age1)
            .isEqualTo(AGES_0_17)
        soft.assertThat(age2)
            .isEqualTo(AGES_55_)
        soft.assertThat(age3)
            .isEqualTo(AGES_0_17)

        soft.assertThat(period)
            .isEqualTo(expectedPeriod)

        soft.assertThat(date0)
            .isEqualTo(expectedDate0)
        soft.assertThat(date1)
            .isEqualTo(expectedDate1)
        soft.assertThat(date2)
            .isEqualTo(expectedDate2)
        soft.assertThat(date3)
            .isEqualTo(expectedDate3)

        soft.assertAll()
    }

    /**
     * Проверка группировки по месяцам при получении общей статистики
     */
    @Test
    fun getCampaignStatisticsGroupByMonthTest() {
        val campaignStatisticsItems =
            deserializeStatisticsResponseFrom("classpath:///group_by_date/campaign_statistics_by_month_example.json").data
        mockStatFromIntApi(campaignStatisticsItems)

        val expectedDate0 = "2020-02-01"
        val expectedDate1 = "2020-02-01"
        val expectedDate2 = "2020-02-01"
        val expectedDate3 = "2020-02-01"
        val expectedPeriod = GdCampaignStatisticsGroupByDate.MONTH.name

        val data = sendRequestAndGetData(GdCampaignStatisticsGroupByDate.MONTH)

        val age0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/age")
        val age1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/age")
        val age2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/age")
        val age3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/age")

        val period = GraphQLUtils.getDataValue<String>(data, "period")

        val date0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/date")
        val date1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/date")
        val date2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/date")
        val date3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/date")

        val soft = SoftAssertions()
        soft.assertThat(age0)
            .isEqualTo(AGES_UNKNOWN)
        soft.assertThat(age1)
            .isEqualTo(AGES_0_17)
        soft.assertThat(age2)
            .isEqualTo(AGES_45_54)
        soft.assertThat(age3)
            .isEqualTo(AGES_55_)

        soft.assertThat(period)
            .isEqualTo(expectedPeriod)

        soft.assertThat(date0)
            .isEqualTo(expectedDate0)
        soft.assertThat(date1)
            .isEqualTo(expectedDate1)
        soft.assertThat(date2)
            .isEqualTo(expectedDate2)
        soft.assertThat(date3)
            .isEqualTo(expectedDate3)

        soft.assertAll()
    }

    /**
     * Проверка группировки по дате, когда передается поле "NONE" при получении общей статистики
     */
    @Test
    fun getCampaignStatisticsGroupByNoneTest() {
        val campaignStatisticsItems =
            deserializeStatisticsResponseFrom("classpath:///group_by_date/campaign_statistics_by_none_example.json").data
        mockStatFromIntApi(campaignStatisticsItems)

        val expectedPeriod = GdCampaignStatisticsGroupByDate.NONE.name

        val data = sendRequestAndGetData(GdCampaignStatisticsGroupByDate.NONE)

        val age0 = GraphQLUtils.getDataValue<String>(data, "rowset/0/age")
        val age1 = GraphQLUtils.getDataValue<String>(data, "rowset/1/age")
        val age2 = GraphQLUtils.getDataValue<String>(data, "rowset/2/age")
        val age3 = GraphQLUtils.getDataValue<String>(data, "rowset/3/age")

        val period = GraphQLUtils.getDataValue<String>(data, "period")

        val soft = SoftAssertions()
        soft.assertThat(age0)
            .isEqualTo(AGES_0_17)
        soft.assertThat(age1)
            .isEqualTo(AGES_UNKNOWN)
        soft.assertThat(age2)
            .isEqualTo(AGES_45_54)
        soft.assertThat(age3)
            .isEqualTo(AGES_55_)

        soft.assertThat(period)
            .isEqualTo(expectedPeriod)

        soft.assertAll()
    }

    private fun getRequest(
        groupByDate: GdCampaignStatisticsGroupByDate
    ) = GdCampaignStatisticsContainer().apply {
        filter = GdCampaignStatisticsFilter().apply {
            campaignId = campaignInfo.campaignId
            period = GdCampaignStatisticsPeriod().apply {
                from = LocalDate.now()
                to = LocalDate.now()
            }
        }
        expandToGeoTree = false
        columns = setOf(AVG_CPM, SHOWS)
        this.groupBy = setOf(REGION, AGE)
        this.groupByDate = groupByDate
    }

    private fun sendRequestAndGetData(groupByDate: GdCampaignStatisticsGroupByDate): Map<*, *> {
        val campaignsContainer = getRequest(groupByDate = groupByDate)

        val query = String.format(QUERY_TEMPLATE, clientInfo.uid, graphQlSerialize(campaignsContainer))

        val result = processor.processQuery(null, query, null, context)
        GraphQLUtils.checkErrors(result.errors)

        val clientData = (result.getData<Any>() as Map<*, *>)["client"] as Map<*, *>
        return clientData["campaignStatistics"] as Map<*, *>
    }

    private fun deserializeStatisticsResponseFrom(filePath: String): CampaignStatisticsResponse {
        val campaignStatisticsRaw = readFileToString(filePath)
        return statisticsHandle.deserializeResponse(campaignStatisticsRaw)
    }

    private fun readFileToString(filePath: String): String = LiveResourceFactory.get(filePath).content
}
