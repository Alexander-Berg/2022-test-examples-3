package ru.yandex.direct.grid.core.entity.campaign.repository

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.mock
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.grid.core.entity.model.GdiEntityConversion
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.core.util.yt.YtStatisticSenecaSasSupport
import ru.yandex.direct.grid.schema.yt.Tables
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate
import ru.yandex.direct.ytcomponents.service.DirectGridStatDynContextProvider
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowBuilder
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import ru.yandex.direct.env.EnvironmentType
import ru.yandex.direct.env.EnvironmentTypeProvider
import ru.yandex.direct.grid.core.util.yt.YtTestTablesSupport

class GridCampaignYtRepositoryTest {
    private lateinit var environmentTypeProvider: EnvironmentTypeProvider
    private lateinit var ytDynamicSupport: YtDynamicSupport
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport
    private lateinit var ytStatisticSenecaSasSupport: YtStatisticSenecaSasSupport
    private lateinit var gridStatDynContextProvider: DirectGridStatDynContextProvider
    private lateinit var campaignService: CampaignService
    private lateinit var ytTestTablesSupport: YtTestTablesSupport

    private lateinit var ytRepository: GridCampaignYtRepository

    @Before
    fun setUp() {
        ytTestTablesSupport = mock(YtTestTablesSupport::class.java)
        environmentTypeProvider = mock(EnvironmentTypeProvider::class.java)
        ytDynamicSupport = mock(YtDynamicSupport::class.java)
        ppcPropertiesSupport = mock(PpcPropertiesSupport::class.java)
        ytStatisticSenecaSasSupport = mock(YtStatisticSenecaSasSupport::class.java)
        gridStatDynContextProvider = mock(DirectGridStatDynContextProvider::class.java)
        campaignService = mock(CampaignService::class.java)
        val property = mock(PpcProperty::class.java) as PpcProperty<Boolean>
        doCallRealMethod().`when`(property).getOrDefault(any())
        `when`(ppcPropertiesSupport.get(
            eq(PpcPropertyNames.USE_ONLY_SENECA_SAS_FOR_GRID_STATTISTIC),
            any()
        )).thenReturn(property)
        `when`(ppcPropertiesSupport.get(
            eq(PpcPropertyNames.USE_STAT_CLUSTER_CHOOSER_FOR_GRID_STATTISTIC),
            any()
        )).thenReturn(property)
        `when`(campaignService.getSubCampaignIdsWithMasterIds(any())).thenReturn(emptyMap())
        whenever(environmentTypeProvider.get()).thenReturn(EnvironmentType.TESTING)

        ytRepository = GridCampaignYtRepository(
            ytDynamicSupport,
            ytStatisticSenecaSasSupport,
            gridStatDynContextProvider,
            ytTestTablesSupport,
            campaignService,
            ppcPropertiesSupport,
            environmentTypeProvider
        )
    }

    @Test
    fun shouldReturnCampaignDailyConversions() {
        val campaignIds = listOf(1L, 2L)
        val startDate = LocalDate.now().minusWeeks(1)
        val endDate = LocalDate.now()
        `when`(ytDynamicSupport.selectRows(any()))
            .thenReturn(generateStatData(startDate, endDate))

        val result = ytRepository.getCampaignsConversions(campaignIds, startDate, endDate, null,
            ReportOptionGroupByDate.NONE, false, false, false)
        assertThat(result)
            .isNotNull
            .containsOnlyKeys(1L, 2L)
        assertThat(result[1L])
            .isNotNull
            .hasSize(7)
        for (i in result[1L]!!.values) {
            assertThat(i)
                .hasFieldOrPropertyWithValue("goals", 10L)
                .hasFieldOrPropertyWithValue("revenue", 10L)
        }
        assertThat(result[2L])
            .isNotNull
            .hasSize(1)
        assertThat(result[2L]!!.values)
            .containsExactly(GdiEntityConversion()
                .withDate(startDate)
                .withGoals(10L)
                .withRevenue(10L)
            )
    }

    @Test
    fun shouldReturnCampaignsConversionCount_usingGoalType() {
        val campaignIds = listOf(1L, 2L)
        val startDate = LocalDate.now().minusWeeks(1)
        val endDate = LocalDate.now()
        `when`(ytDynamicSupport.selectRows(any()))
            .thenReturn(generateStatData(startDate, endDate))

        val result = ytRepository.getCampaignsConversionCount(campaignIds, startDate, endDate)

        assertThat(result)
            .isNotNull
            .hasSize(2)
        assertThat(result[1L])
            .hasFieldOrPropertyWithValue("goals", 70L)
            .hasFieldOrPropertyWithValue("revenue", 70L)
        assertThat(result[2L])
            .hasFieldOrPropertyWithValue("goals", 10L)
            .hasFieldOrPropertyWithValue("revenue", 10L)
    }

    private fun generateStatData(startDay: LocalDate, endDay: LocalDate): UnversionedRowset {
        val rowsetBuilder = RowsetBuilder()

        //add data for campaign with id=1
        var i = startDay
        while (i.isBefore(endDay)) {
            rowsetBuilder.add(RowBuilder()
                .withColValue("effectiveCampaignId", 1L)
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.UPDATE_TIME.name, i.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC))
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM.name, 10)
                .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR.name, 10))
            i = i.plusDays(1)
        }

        //add data for campaign with id=2
        rowsetBuilder.add(RowBuilder()
            .withColValue("effectiveCampaignId", 2L)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.UPDATE_TIME.name, startDay.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC))
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM.name, 10)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR.name, 10)
        )
        rowsetBuilder.add(RowBuilder()
            .withColValue("effectiveCampaignId", 2L)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.UPDATE_TIME.name, endDay.toEpochSecond(LocalTime.MIDNIGHT, ZoneOffset.UTC))
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.GOALS_NUM.name, 0)
            .withColValue(Tables.DIRECTGRIDGOALSSTAT_BS.PRICE_CUR.name, 0)
        )

        return rowsetBuilder.build()
    }
}
