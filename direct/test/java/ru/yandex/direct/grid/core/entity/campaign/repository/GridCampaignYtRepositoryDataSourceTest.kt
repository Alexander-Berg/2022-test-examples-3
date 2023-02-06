package ru.yandex.direct.grid.core.entity.campaign.repository

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDate
import java.util.Collections.singletonList
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.jvm.isAccessible
import org.assertj.core.api.SoftAssertions
import org.jooq.Field
import org.jooq.Select
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.Mock
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.junit4.rules.SpringMethodRule
import ru.yandex.direct.common.db.PpcPropertiesSupport
import ru.yandex.direct.core.entity.campaign.service.CampaignService
import ru.yandex.direct.core.security.DirectAuthentication
import ru.yandex.direct.core.testing.info.ClientInfo
import ru.yandex.direct.core.testing.steps.FeatureSteps
import ru.yandex.direct.core.testing.steps.Steps
import ru.yandex.direct.env.EnvironmentType
import ru.yandex.direct.env.EnvironmentTypeProvider
import ru.yandex.direct.feature.FeatureName
import ru.yandex.direct.grid.core.configuration.GridCoreTest
import ru.yandex.direct.grid.core.util.stats.GridStatNew
import ru.yandex.direct.grid.core.util.stats.GridStatUtils
import ru.yandex.direct.grid.core.util.stats.completestat.DirectGridStatData
import ru.yandex.direct.grid.core.util.yt.YtDynamicSupport
import ru.yandex.direct.grid.core.util.yt.YtStatisticSenecaSasSupport
import ru.yandex.direct.grid.core.util.yt.YtTestTablesSupport
import ru.yandex.direct.rbac.RbacRepType
import ru.yandex.direct.ytcomponents.service.DirectGridStatDynContextProvider
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext
import ru.yandex.yt.ytclient.tables.ColumnValueType
import ru.yandex.yt.ytclient.tables.TableSchema
import ru.yandex.yt.ytclient.wire.UnversionedRow
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import ru.yandex.yt.ytclient.wire.UnversionedValue

@GridCoreTest
@RunWith(Parameterized::class)
class GridCampaignYtRepositoryDataSourceTest(
    private val testName: String,
    private val envType: EnvironmentType,
    private val useFeature: Boolean,
    private val expectedValue: Long,
) {
    @get:Rule
    var springMethodRule = SpringMethodRule()

    @Autowired
    private lateinit var campaignService: CampaignService

    @Autowired
    private lateinit var ppcPropertySupport: PpcPropertiesSupport

    @Autowired
    private lateinit var steps: Steps

    @Autowired
    private lateinit var featureSteps: FeatureSteps

    @Mock
    private lateinit var ytDynamicSupport: YtDynamicSupport

    @Mock
    private lateinit var directGridStatDynContextProvider: DirectGridStatDynContextProvider

    @Mock
    private lateinit var ytStatisticSenecaSasSupport: YtStatisticSenecaSasSupport

    @Mock
    private lateinit var ytTestTablesSupport: YtTestTablesSupport

    @Mock
    private lateinit var environmentTypeProvider: EnvironmentTypeProvider

    @Mock
    private lateinit var ytDynamicContext: YtDynamicContext

    private lateinit var gridCampaignYtRepository: GridCampaignYtRepository
    private lateinit var clientInfo: ClientInfo

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testParams() = arrayOf(
            arrayOf(
                "Production env, no feature",
                EnvironmentType.PRODUCTION,
                false,
                1
            ),
            arrayOf(
                "Production env, use feature",
                EnvironmentType.PRODUCTION,
                true,
                1
            ),
            arrayOf(
                "Testing env, no feature",
                EnvironmentType.TESTING,
                false,
                1
            ),
            arrayOf(
                "Testing env, use feature",
                EnvironmentType.TESTING,
                true,
                0
            )
        )

        private val gridStat = GridStatNew(DirectGridStatData.INSTANCE)
        private val RESULT_SCHEMA = TableSchema.Builder()
            .setUniqueKeys(false)
            .addKey("CheckValue", ColumnValueType.INT64)
            .build()
    }

    @Before
    fun init() {
        environmentTypeProvider = mock()
        ytDynamicSupport = mock()
        directGridStatDynContextProvider = mock()
        ytTestTablesSupport = mock()
        ytStatisticSenecaSasSupport = mock()
        ytDynamicContext = mock()

        clientInfo = steps.clientSteps().createDefaultClient()

        whenever(ytTestTablesSupport.selectStatistics(any())).thenReturn(getUnversionedRowset(0))
        whenever(ytDynamicSupport.selectRows(any())).thenReturn(getUnversionedRowset(1))
        whenever(ytStatisticSenecaSasSupport.selectStatistics(any())).thenReturn(getUnversionedRowset(2))
        whenever(directGridStatDynContextProvider.context).thenReturn(ytDynamicContext)
        whenever(ytDynamicContext.executeSelect(any())).thenReturn(getUnversionedRowset(3))
    }

    @Test
    fun checkDataSource() {
        whenever(environmentTypeProvider.get()).thenReturn(envType)

        val user = steps.userSteps().createUser(clientInfo, RbacRepType.CHIEF)
        featureSteps.setCurrentClient(clientInfo.clientId)

        val ctx = SecurityContextHolder.createEmptyContext()
        SecurityContextHolder.setContext(ctx)
        ctx.authentication = DirectAuthentication(user.user!!, user.user!!)

        if (useFeature) featureSteps.enableClientFeature(clientInfo.clientId, FeatureName.USE_TEST_YT_STAT)

        gridCampaignYtRepository = GridCampaignYtRepository(
            ytDynamicSupport,
            ytStatisticSenecaSasSupport,
            directGridStatDynContextProvider,
            ytTestTablesSupport,
            campaignService,
            ppcPropertySupport,
            environmentTypeProvider
        )

        val query: Select<*> = gridStat.constructCostSelect(
            listOf<Field<*>>(gridStat.tableData.updateTime().`as`("UpdateTime")),
            gridStat.tableData.campaignId().`in`(GridStatUtils.getAllCampaignIds(listOf(0L), mapOf(1L to 2L))),
            LocalDate.now().minusDays(5),
            LocalDate.now()
        )

        val gridCampaignYtRepositoryClass = gridCampaignYtRepository::class
        val selectRowsMethod = gridCampaignYtRepositoryClass.declaredFunctions.first { it.name == "selectRows" }
        selectRowsMethod.isAccessible = true

        val result = selectRowsMethod.call(gridCampaignYtRepository, query) as UnversionedRowset

        SoftAssertions().apply {
            assertThat(result)
                .isNotNull
            assertThat(result.rows[0].values[0].value)
                .isNotNull
                .isEqualTo(expectedValue)
        }.assertAll()
    }

    private fun getUnversionedRowset(value: Long): UnversionedRowset {
        val row = UnversionedRow(mutableListOf(UnversionedValue(0, ColumnValueType.INT64, false, value)))
        return UnversionedRowset(RESULT_SCHEMA, singletonList(row))
    }
}
