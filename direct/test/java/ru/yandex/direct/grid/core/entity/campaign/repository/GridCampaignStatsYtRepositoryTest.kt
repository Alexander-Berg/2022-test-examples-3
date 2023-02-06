package ru.yandex.direct.grid.core.entity.campaign.repository

import org.jooq.Select
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.any
import org.mockito.Mockito.doCallRealMethod
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.junit4.SpringRunner
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.common.db.PpcPropertyNames
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.grid.core.configuration.GridCoreTest
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.core.util.yt.YtStatisticSenecaSasSupport
import ru.yandex.direct.intapi.client.model.request.statistics.option.ReportOptionGroupByDate
import ru.yandex.direct.liveresource.LiveResourceFactory
import ru.yandex.direct.test.utils.QueryUtils
import ru.yandex.direct.ytcomponents.service.DirectGridStatDynContextProvider
import ru.yandex.direct.ytwrapper.dynamic.testutil.RowsetBuilder
import java.time.LocalDate
import ru.yandex.direct.env.EnvironmentTypeProvider
import ru.yandex.direct.grid.core.util.yt.YtTestTablesSupport

@GridCoreTest
@RunWith(SpringRunner::class)
class GridCampaignStatsYtRepositoryTest {
    @Mock
    private lateinit var ytDynamicSupport: YtDynamicSupport

    @Autowired
    private lateinit var ppcPropertiesSupport: PpcPropertiesSupport

    @Autowired
    private lateinit var ytStatisticSenecaSasSupport: YtStatisticSenecaSasSupport

    @Autowired
    private lateinit var gridStatDynContextProvider: DirectGridStatDynContextProvider

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var ytRepository: GridCampaignYtRepository

    @Autowired
    private lateinit var ytTestTablesSupport: YtTestTablesSupport

    @Autowired
    private lateinit var environmentTypeProvider: EnvironmentTypeProvider

    @Captor
    private lateinit var queryArgumentCaptor: ArgumentCaptor<Select<*>>

    private val startDate = LocalDate.of(2022, 3, 15).minusWeeks(1)
    private val endDate = LocalDate.of(2022, 3, 15)
    private val campaignIds = listOf(1L, 2L)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        ytDynamicSupport = mock(YtDynamicSupport::class.java)
        ppcPropertiesSupport = mock(PpcPropertiesSupport::class.java)
        ytStatisticSenecaSasSupport = mock(YtStatisticSenecaSasSupport::class.java)
        gridStatDynContextProvider = mock(DirectGridStatDynContextProvider::class.java)
        campaignService = mock(CampaignService::class.java)

        val property = mock(PpcProperty::class.java) as PpcProperty<Boolean>
        doCallRealMethod().`when`(property).getOrDefault(any())
        `when`(
            ppcPropertiesSupport.get(
                eq(PpcPropertyNames.USE_ONLY_SENECA_SAS_FOR_GRID_STATTISTIC),
                any()
            )
        ).thenReturn(property)
        `when`(
            ppcPropertiesSupport.get(
                eq(PpcPropertyNames.USE_STAT_CLUSTER_CHOOSER_FOR_GRID_STATTISTIC),
                any()
            )
        ).thenReturn(property)
        `when`(campaignService.getSubCampaignIdsWithMasterIds(any())).thenReturn(emptyMap())

        ytRepository = GridCampaignYtRepository(
            ytDynamicSupport,
            ytStatisticSenecaSasSupport,
            gridStatDynContextProvider,
            ytTestTablesSupport,
            campaignService,
            ppcPropertiesSupport,
            environmentTypeProvider,
        )
    }

    @Test
    fun getConversionsWithGoalId4() {
        checkQuery("conversionsWithGoalId4.query", true)
    }

    @Test
    fun getConversions() {
        checkQuery("conversions.query", false)
    }

    private fun checkQuery(
        queryPathFile: String,
        isUacWithOnlyInstallEnabled: Boolean
    ) {
        val expectedQuery = LiveResourceFactory.get("classpath:///conversions/$queryPathFile").content

        `when`(ytDynamicSupport.selectRows(any()))
            .thenReturn(RowsetBuilder().build())

        ytRepository.getCampaignsConversions(
            campaignIds, startDate, endDate, null,
            ReportOptionGroupByDate.NONE, false, false, isUacWithOnlyInstallEnabled
        )

        Mockito.verify(ytDynamicSupport).selectRows(queryArgumentCaptor.capture())
        val query = queryArgumentCaptor.value.toString()

        QueryUtils.compareQueries(expectedQuery, query)
    }
}
