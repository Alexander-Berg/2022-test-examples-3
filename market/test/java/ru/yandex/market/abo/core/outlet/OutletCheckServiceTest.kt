package ru.yandex.market.abo.core.outlet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.outlet.model.OutletCheck
import ru.yandex.market.abo.core.outlet.model.ext.OutletForModeration
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicLong

/**
 * @author komarovns
 */
open class OutletCheckServiceTest @Autowired constructor(
    private val outletCheckService: OutletCheckService,
    private val jdbcTemplate: JdbcTemplate
) : EmptyTest() {
    private val counter = AtomicLong()

    @Test
    open fun `delete old checks`() {
        val newCheckId = createCheck(LocalDateTime.now())
        createCheck(LocalDateTime.now().minusYears(1))

        outletCheckService.deleteOldChecks()
        flushAndClear()

        assertEquals(listOf(newCheckId), "SELECT id FROM outlet_check".queryLongList())
        assertEquals(listOf(newCheckId), "SELECT check_id FROM outlet_check_history".queryLongList())
        assertEquals(listOf(newCheckId), "SELECT outlet_check_id FROM outlet_check_reason".queryLongList())
    }

    private fun createCheck(modificationTime: LocalDateTime): Long {
        val mbiId = counter.getAndIncrement()
        outletCheckService.save(listOf(OutletCheck.fromMbiCheck(OutletForModeration().apply {
            id = mbiId
            shopId = 0
            updateTime = Date()
            urgency = 0
        })))
        flushAndClear()

        val id = "SELECT id FROM outlet_check WHERE billing_id = $mbiId".queryLongList()[0]
        jdbcTemplate.update("""
            UPDATE outlet_check
            SET modification_time = ?,
                sent = TRUE
            WHERE id = ?
        """, modificationTime, id)
        jdbcTemplate.update("""
            INSERT INTO outlet_check_reason
            VALUES (nextval('s_outlet_check_reason'), $id, 1, '')""")
        return id
    }

    private fun String.queryLongList() = jdbcTemplate.queryForList(this, Long::class.java)
}
