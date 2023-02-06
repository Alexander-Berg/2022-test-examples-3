package ru.yandex.direct.core.entity.postviewofflinereport.repository

import com.nhaarman.mockitokotlin2.whenever
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import ru.yandex.direct.common.testing.softly
import ru.yandex.direct.ytcomponents.config.DirectYtDynamicConfig
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtCluster
import ru.yandex.direct.ytwrapper.model.YtDynamicOperator
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl
import ru.yandex.yt.ytclient.wire.UnversionedRowset
import java.time.LocalDate

class PostViewOfflineReportYtRepositoryTest {

    companion object {
        private val CLUSTER = YtCluster.HAHN

        private var ytConfig = Mockito.mock(DirectYtDynamicConfig::class.java)

        private lateinit var ytRepository: PostViewOfflineReportYtRepository

        private var ytProvider = Mockito.mock(YtProvider::class.java)

        @JvmStatic
        @BeforeClass
        internal fun setUp() {
            whenever(ytConfig.postViewOfflineReportYtClusters).thenReturn(listOf(CLUSTER))
            val tables = Mockito.mock(DirectYtDynamicConfig.Tables::class.java)
            whenever(ytConfig.tables()).thenReturn(tables)
            val directTables = Mockito.mock(DirectYtDynamicConfig.DirectTables::class.java)
            whenever(tables.direct()).thenReturn(directTables)
            whenever(directTables.postViewOfflineReportTasksTablePath()).thenReturn("//test")
            ytRepository = PostViewOfflineReportYtRepository(ytProvider, ytConfig)
        }
    }

    @Test
    fun getAnyTaskTest() {
        val operator = Mockito.mock(YtDynamicOperator::class.java)
        val rowSet = Mockito.mock(UnversionedRowset::class.java)
        whenever(rowSet.yTreeRows).thenReturn(
            listOf(
                YTreeMapNodeImpl(
                mapOf(
                    Pair(
                        PostViewOfflineReportYtRepository.REPORT_ID_COLUMN,
                        YTreeIntegerNodeImpl(true, 12345L, null)
                    ),
                    Pair(
                        PostViewOfflineReportYtRepository.CAMPAIGN_IDS_COLUMN,
                        YTreeStringNodeImpl("456, 56564, 263", null)
                    ),
                    Pair(
                        PostViewOfflineReportYtRepository.DATE_FROM_COLUMN,
                        YTreeStringNodeImpl("2021-10-22", null)
                    ),
                    Pair(
                        PostViewOfflineReportYtRepository.DATE_TO_COLUMN,
                        YTreeStringNodeImpl("2022-01-31", null)
                    ),
                ), null)
            )
        )
        whenever(operator.selectRows(ArgumentMatchers.anyString(), ArgumentMatchers.any())).thenReturn(rowSet)
        whenever(ytProvider.getDynamicOperator(ArgumentMatchers.any())).thenReturn(operator)

        val result = ytRepository.getAnyTask()
        result!!
        softly {
            assertThat(result.first == 12345L)
            val jobParams = result.second
            assertThat(jobParams.campaignIds == setOf(263L, 456L, 56564L))
            assertThat(jobParams.dateFrom == LocalDate.of(2021, 10, 22))
            assertThat(jobParams.dateTo == LocalDate.of(2022, 1, 31))
        }
    }
}
