package ru.yandex.direct.jobs.yt

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.direct.common.db.PpcProperty
import ru.yandex.direct.config.DirectConfig
import ru.yandex.direct.ytwrapper.client.YtClusterConfig
import ru.yandex.direct.ytwrapper.client.YtProvider
import ru.yandex.direct.ytwrapper.model.YtOperator
import ru.yandex.inside.yt.kosher.Yt
import ru.yandex.inside.yt.kosher.cypress.Cypress
import ru.yandex.inside.yt.kosher.cypress.YPath
import java.time.LocalDateTime

private const val YQL_PATH = "export/home-direct-db/include/common.yql"
private const val OUTPUT_FOLDER_CONFIG = "some_folder"
private const val OUTPUT_TABLE = "some_table"

@ExtendWith(MockitoExtension::class)
class ExecuteYQLAndExportDataJobTest {
    @Mock
    private lateinit var directConfig: DirectConfig
    @Mock
    private lateinit var ytProvider: YtProvider
    @Mock
    private lateinit var ytOperator: YtOperator
    @Mock
    private lateinit var clusterProperty: PpcProperty<String>
    @Mock
    private lateinit var lastProcessedLogTimeProperty: PpcProperty<LocalDateTime>

    @BeforeEach
    fun before() {
        whenever(directConfig.getBranch(any())).thenReturn(directConfig)
        whenever(directConfig.getStringList(eq("clusters"))).thenReturn(listOf("hahn"))
        whenever(directConfig.getString(eq(OUTPUT_FOLDER_CONFIG))).thenReturn(OUTPUT_TABLE)

        whenever(ytOperator.readTableRowCount(any())).thenReturn(1)
        whenever(ytProvider.getOperator(any(), any())).thenReturn(ytOperator)
        val ytClusterConfig = mock<YtClusterConfig>()
        whenever(ytProvider.getClusterConfig(any())).thenReturn(ytClusterConfig)
        whenever(ytClusterConfig.home).thenReturn(".")
        val yt = mock<Yt>()
        whenever(ytProvider.get(any())).thenReturn(yt)
        whenever(ytOperator.yt).thenReturn(yt)
        val cypress = mock<Cypress>()
        whenever(yt.cypress()).thenReturn(cypress)
        whenever(cypress.exists(any<YPath>())).thenReturn(true)

        whenever(clusterProperty.get()).thenReturn("hahn")
        whenever(lastProcessedLogTimeProperty.get()).thenReturn(LocalDateTime.now().minusDays(1))
    }

    @Test
    fun oneSourceTable() {
        val job = createJob(listOf("//source_table"))
        job.execute()

        val captor = argumentCaptor<String>()
        verify(ytOperator).yqlExecute(any(), captor.capture())
        val args = captor.allValues
        SoftAssertions.assertSoftly {
            it.assertThat(args).hasSize(2)
            it.assertThat(args[0]).isEqualTo("//source_table")
            it.assertThat(args[1]).contains(OUTPUT_TABLE)
        }
    }

    @Test
    fun twoSourceTable() {
        val job = createJob(listOf("//source_table1", "//source_table2"))
        job.execute()

        val captor = argumentCaptor<String>()
        verify(ytOperator).yqlExecute(any(), captor.capture())
        val args = captor.allValues
        SoftAssertions.assertSoftly {
            it.assertThat(args).hasSize(3)
            it.assertThat(args[0]).isEqualTo("//source_table1")
            it.assertThat(args[1]).isEqualTo("//source_table2")
            it.assertThat(args[2]).contains(OUTPUT_TABLE)
        }
    }

    private fun createJob(sourceTables: List<String>) =
        object : ExecuteYQLAndExportDataJob(directConfig, ytProvider,
            clusterProperty, lastProcessedLogTimeProperty, YQL_PATH) {
            override fun getSourceTablesPathTemplate() = sourceTables
            override fun getConfigBranch() = "some_branch"
            override fun getOutputTablesFolder() = "some_folder"
    }
}
