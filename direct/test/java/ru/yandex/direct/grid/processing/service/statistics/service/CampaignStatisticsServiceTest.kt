package ru.yandex.direct.grid.processing.service.statistics.service

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import java.net.InetAddress
import java.time.LocalDate
import org.jooq.Select
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.common.net.NetAcl
import ru.yandex.direct.core.entity.metrika.service.MetrikaGoalsService
import ru.yandex.direct.core.entity.retargeting.model.Goal
import ru.yandex.direct.core.entity.user.model.User
import ru.yandex.direct.core.testing.data.TestCampaigns
import ru.yandex.direct.core.testing.info.CampaignInfo
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext
import ru.yandex.direct.grid.processing.model.client.GdClient
import ru.yandex.direct.grid.processing.model.client.GdClientInfo
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsContainer
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsFilter
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupBy
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupBy.REGION
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupByDate
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupByDate.DAY
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsGroupByDate.NONE
import ru.yandex.direct.grid.processing.model.statistics.GdCampaignStatisticsPeriod
import ru.yandex.direct.intapi.client.IntApiClient
import ru.yandex.direct.intapi.client.model.request.statistics.CampaignStatisticsRequest
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsItem
import ru.yandex.direct.intapi.client.model.response.statistics.CampaignStatisticsResponse
import ru.yandex.direct.regions.Region.CRIMEA_REGION_ID
import ru.yandex.direct.regions.Region.RUSSIA_REGION_ID
import ru.yandex.direct.regions.Region.SNG_REGION_ID
import ru.yandex.direct.regions.Region.TURKEY_REGION_ID
import ru.yandex.direct.regions.Region.UKRAINE_REGION_ID
import ru.yandex.direct.test.utils.checkContainsKey
import ru.yandex.direct.test.utils.checkEquals
import ru.yandex.direct.test.utils.checkNotContainsKey
import ru.yandex.direct.test.utils.checkNotNull
import ru.yandex.direct.ytcomponents.service.OfferStatDynContextProvider
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext
import ru.yandex.yt.ytclient.tables.TableSchema
import ru.yandex.yt.ytclient.wire.UnversionedRow
import ru.yandex.yt.ytclient.wire.UnversionedRowset

private const val COUNTER_ID = 123
private const val GOAL_ID = 1234L
private const val UNAVAILABLE_GOAL_ID = 1235L

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner::class)
class CampaignStatisticsServiceTest {

    @Autowired
    private lateinit var campaignStatisticsService: CampaignStatisticsService
    @Autowired
    private lateinit var intApiClient: IntApiClient
    @Autowired
    private lateinit var steps: Steps
    @Autowired
    private lateinit var netAcl: NetAcl
    @Autowired
    private lateinit var metrikaGoalsService: MetrikaGoalsService
    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport
    @Autowired
    private lateinit var gridYtSupport: YtDynamicSupport
    @Autowired
    private lateinit var offerStatDynContextProvider: OfferStatDynContextProvider

    private lateinit var campaignInfo: CampaignInfo
    private lateinit var clientInfo: ClientInfo

    private val requestCaptor = ArgumentCaptor.forClass(CampaignStatisticsRequest::class.java)

    @Before
    fun before() {
        doReturn(false).`when`(netAcl).isInternalIp(any(InetAddress::class.java))

        mockStatFromIntApi(
            listOf(
                CampaignStatisticsItem().apply {
                    region = CRIMEA_REGION_ID.toInt()
                },
                CampaignStatisticsItem().apply {
                    region = RUSSIA_REGION_ID.toInt()
                },
                CampaignStatisticsItem().apply {
                    region = UKRAINE_REGION_ID.toInt()
                },
            )
        )
        val textCampaign = TestCampaigns.activeTextCampaign(null, null).apply {
            metrikaCounters = listOf(COUNTER_ID.toLong())
        }
        campaignInfo = steps.campaignSteps().createCampaign(textCampaign)
        clientInfo = campaignInfo.clientInfo
    }

    @After
    fun after() {
        reset(netAcl, intApiClient, metrikaGoalsService)
    }

    @Test
    fun emptyStatisticsTest() {
        mockStatFromIntApi(emptyList())

        val payload = campaignStatisticsService.getCampaignStatistics(
            getRequest(), getClient(RUSSIA_REGION_ID), getContext()
        )

        payload.rowset.checkEquals(emptyList())
        payload.geoTreeStatistics.checkEquals(emptyMap())
        payload.totals.checkNotNull()
    }

    @Test
    fun russianClientCrimeaTest() {
        val payload = campaignStatisticsService.getCampaignStatistics(
            getRequest(), getClient(RUSSIA_REGION_ID), getContext()
        )

        payload.let {
            it.geoTreeStatistics.checkContainsKey(RUSSIA_REGION_ID)
            it.geoTreeStatistics[RUSSIA_REGION_ID]!!.childRegions.checkContainsKey(CRIMEA_REGION_ID)

            it.geoTreeStatistics.checkContainsKey(SNG_REGION_ID)
            it.geoTreeStatistics[SNG_REGION_ID]!!.childRegions.checkContainsKey(UKRAINE_REGION_ID)
            it.geoTreeStatistics[SNG_REGION_ID]!!.childRegions[UKRAINE_REGION_ID]!!.childRegions
                .checkNotContainsKey(CRIMEA_REGION_ID)
        }

        payload.rowset.checkEquals(emptyList())
        payload.totals.checkNotNull()
    }

    @Test
    fun ukrainianClientCrimeaTest() {
        val payload = campaignStatisticsService.getCampaignStatistics(
            getRequest(), getClient(UKRAINE_REGION_ID), getContext()
        )

        payload.let {
            it.geoTreeStatistics.checkContainsKey(RUSSIA_REGION_ID)
            it.geoTreeStatistics[RUSSIA_REGION_ID]!!.childRegions.checkNotContainsKey(CRIMEA_REGION_ID)

            it.geoTreeStatistics.checkContainsKey(SNG_REGION_ID)
            it.geoTreeStatistics[SNG_REGION_ID]!!.childRegions.checkContainsKey(UKRAINE_REGION_ID)
            it.geoTreeStatistics[SNG_REGION_ID]!!.childRegions[UKRAINE_REGION_ID]!!.childRegions
                .checkContainsKey(CRIMEA_REGION_ID)
        }

        payload.rowset.checkEquals(emptyList())
        payload.totals.checkNotNull()
    }

    @Test
    fun turkeyClientCrimeaTest() {
        val payload = campaignStatisticsService.getCampaignStatistics(
            getRequest(), getClient(TURKEY_REGION_ID), getContext()
        )

        payload.let {
            it.geoTreeStatistics.checkContainsKey(RUSSIA_REGION_ID)
            it.geoTreeStatistics[RUSSIA_REGION_ID]!!.childRegions.checkContainsKey(CRIMEA_REGION_ID)

            it.geoTreeStatistics.checkContainsKey(SNG_REGION_ID)
            it.geoTreeStatistics[SNG_REGION_ID]!!.childRegions.checkContainsKey(UKRAINE_REGION_ID)
            it.geoTreeStatistics[SNG_REGION_ID]!!.childRegions[UKRAINE_REGION_ID]!!.childRegions
                .checkNotContainsKey(CRIMEA_REGION_ID)
        }

        payload.rowset.checkEquals(emptyList())
        payload.totals.checkNotNull()
    }

    @Test
    fun regionAbsentInDirectGeoTreeTest() {
        val xojayliRegionId = 193331
        mockStatFromIntApi(
            listOf(
                CampaignStatisticsItem().apply {
                    region = xojayliRegionId
                })
        )

        val payload = campaignStatisticsService.getCampaignStatistics(
            getRequest(), getClient(RUSSIA_REGION_ID), getContext()
        )

        payload.rowset.checkEquals(emptyList())
        payload.geoTreeStatistics.checkEquals(emptyMap())
        payload.totals.checkNotNull()
    }

    @Test
    fun unavailableGoalsTest() {
        doReturn(setOf(Goal().apply { id = GOAL_ID })).`when`(metrikaGoalsService).getMetrikaGoalsByCounter(
            eq(clientInfo.uid), eq(clientInfo.clientId),
            anyCollection(), any(), any()
        )

        val payload = campaignStatisticsService.getCampaignStatistics(
                getRequest(listOf(GOAL_ID, UNAVAILABLE_GOAL_ID)), getClient(RUSSIA_REGION_ID), getContext()
        )

        verify(metrikaGoalsService, never()).getAvailableMetrikaGoalsForClient(anyLong(), any())

        payload.rowset.checkEquals(emptyList())
        payload.totals.checkNotNull()

        val goalIds = requestCaptor.value.reportOptions.goalIds
        goalIds.checkEquals(listOf(GOAL_ID))
    }

    @Test
    fun ytStatisticsFromCampaignGoalsTest() {
        ppcPropertiesSupport.set(PpcPropertyNames.USE_YT_STATISTICS_FOR_UC, "true")
        steps.featureSteps().addClientFeature(clientInfo.clientId,
            FeatureName.GRID_CAMPAIGN_GOALS_FILTRATION_FOR_STAT, true)
        val schema = TableSchema.Builder()
            .setStrict(true)
            .setUniqueKeys(false)
            .build()
        val rows = emptyList<UnversionedRow>()
        val rowset = UnversionedRowset(schema, rows)
        `when`(gridYtSupport.selectRows(any(Select::class.java)))
            .thenReturn(rowset)
        val ytDynamicContext = mock<YtDynamicContext> {
            on { executeSelect(any()) } doReturn rowset
        }
        doReturn(ytDynamicContext).`when`(offerStatDynContextProvider).context

        val payload = campaignStatisticsService.getCampaignStatistics(
            getRequest(groupBy = setOf(), groupByDate = DAY), getClient(RUSSIA_REGION_ID), getContext()
        )

        verify(intApiClient, never())
            .getCampaignStatistics(any())
        verify(metrikaGoalsService, never())
            .getMetrikaGoalsByCounter(anyLong(), any(), anyCollection(), any(), any())
        verify(metrikaGoalsService, times(1))
            .getAvailableMetrikaGoalsForClient(eq(clientInfo.uid), eq(clientInfo.clientId))

        payload.rowset.checkEquals(emptyList())
        payload.totals.checkNotNull()

        ppcPropertiesSupport.remove(PpcPropertyNames.USE_YT_STATISTICS_FOR_UC)
    }

    private fun mockStatFromIntApi(
        items: List<CampaignStatisticsItem>,
    ) {
        val intApiResponse = CampaignStatisticsResponse().apply {
            data = items
            totals = CampaignStatisticsItem()
        }

        doReturn(intApiResponse).`when`(intApiClient).getCampaignStatistics(requestCaptor.capture())
    }

    private fun getRequest(
        goalIds: List<Long>? = null,
        groupBy: Set<GdCampaignStatisticsGroupBy>? = setOf(REGION),
        groupByDate: GdCampaignStatisticsGroupByDate? = NONE
    ) = GdCampaignStatisticsContainer().apply {
        filter = GdCampaignStatisticsFilter().apply {
            campaignId = campaignInfo.campaignId
            period = GdCampaignStatisticsPeriod().apply {
                from = LocalDate.now()
                to = LocalDate.now()
            }
            this.goalIds = goalIds
        }
        expandToGeoTree = true
        this.groupBy = groupBy
        this.groupByDate = groupByDate
    }

    private fun getClient(regionId: Long) = GdClient().apply {
        info = GdClientInfo().apply {
            id = clientInfo.clientId!!.asLong()
            countryRegionId = regionId
        }
    }

    private fun getContext() = GridGraphQLContext(User().apply {
        uid = clientInfo.uid
    })
}
