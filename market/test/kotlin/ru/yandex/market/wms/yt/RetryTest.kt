package ru.yandex.market.wms.yt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.retry.annotation.EnableRetry
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode
import ru.yandex.market.wms.shared.libs.env.conifg.Profiles
import ru.yandex.market.wms.yt.config.DbConfigForRetry
import ru.yandex.market.wms.yt.config.YtConfig
import ru.yandex.market.wms.yt.configuration.UtilModuleConfig
import ru.yandex.market.wms.yt.dao.SnapshotDao
import ru.yandex.market.wms.yt.entity.YtUploadTask
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

@EnableRetry
@Profile(ru.yandex.market.wms.shared.libs.env.conifg.Profiles.TEST)
@SpringBootTest(classes = [DbConfigForRetry::class, YtConfig::class, UtilModuleConfig::class])
class RetryTest {
    @Autowired
    @Qualifier("springSnapshotDao")
    private lateinit var snapshotDao: SnapshotDao

    @Autowired
    @Qualifier("mockJdbc")
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    private val ytTask = YtUploadTask(
        jobId = 1,
        runId = 2,
        dbTable = "ITRN",
        primaryKey = "ITRNKEY",
        ytPath = "//tmp/market-wms/itrn",
        status = "NEW",
        filterDate = LocalDateTime.now(),
        filterDateMin = null,
        addDate = LocalDateTime.now(),
        addWho = "TEST",
        editDate = LocalDateTime.now(),
        editWho = "TEST",
        allRows = null,
        uploadedRows = null,
        readRows = null,
        appendCursor = null,
        finishDate = null,
        startDate = null,
        flags = HashMap()
    )

    @Test
    fun `next page fail, spring dao`() {
        Mockito.`when`(namedParameterJdbcTemplate.query(any(), any<SqlParameterSource>(), any<RowMapper<*>>()))
            .then { throw SQLException() }
        Assertions.assertThrows(Exception::class.java) {
            snapshotDao.getNextPage(ytTask, ytTask.dbTable, 1, 0)
        }
    }

    @Test
    fun `next page success, spring dao`() {
        Mockito.reset(namedParameterJdbcTemplate)
        val cnt = AtomicInteger(3)
        Mockito.`when`(namedParameterJdbcTemplate.query(any(), any<SqlParameterSource>(), any<RowMapper<*>>()))
            .then { if (cnt.decrementAndGet() > 0) throw SQLException() else listOf<YTreeMapNode>() }
        snapshotDao.getNextPage(ytTask, ytTask.dbTable, 1, 0)
    }

    @Test
    fun `next diff page fail, spring dao`() {
        Mockito.`when`(namedParameterJdbcTemplate.query(any(), any<SqlParameterSource>(), any<RowMapper<*>>()))
            .then { throw SQLException() }
        Assertions.assertThrows(Exception::class.java) {
            snapshotDao.getNextPageDiff(ytTask, ytTask.dbTable, ytTask.dbTable, 1, 0)
        }
    }

    @Test
    fun `next diff page success, spring dao`() {
        Mockito.reset(namedParameterJdbcTemplate)
        val cnt = AtomicInteger(3)
        Mockito.`when`(namedParameterJdbcTemplate.query(any(), any<SqlParameterSource>(), any<RowMapper<*>>()))
            .then { if (cnt.decrementAndGet() > 0) throw SQLException() else listOf<YTreeMapNode>() }
        snapshotDao.getNextPageDiff(ytTask, ytTask.dbTable, ytTask.dbTable, 1, 0)
    }
}
