package ru.yandex.market.abo.core.pinger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.pinger.model.MpGeneratorType
import ru.yandex.market.abo.core.pinger.model.PingerContentTask
import ru.yandex.market.abo.core.pinger.service.PingerContentTaskService
import java.time.LocalDateTime

internal class GoodsPingMpStatProviderTest @Autowired constructor(
    val goodsPingMpStatProvider: GoodsPingMpStatProvider,
    val pingerContentTaskService: PingerContentTaskService,
    val jdbcTemplate: JdbcTemplate
) : EmptyTest() {

    @Test
    fun `3 tasks`() {
        val task1 = createTask(200, 500, 0)
        val task2 = createTask(200, 499, 1)
        val task3 = createTask(500, 500, 2)
        pingerContentTaskService.saveAll(listOf(task1, task2, task3))
        flushAndClear()
        val stat = goodsPingMpStatProvider.getStatByPartner()
        val expectedStat = mapOf(PARTNER_ID to MpStat(3, 1, true))
        assertEquals(expectedStat, stat)
    }


    @Test
    fun `5 tasks`() {
        val task1 = createTask(200, 499, 0)
        val task2 = createTask(404, 499, 1)
        val task3 = createTask(2000, 499, 2)
        val task4 = createTask(1000, 500, 3)
        val task5 = createTask(500, 500, 4)
        pingerContentTaskService.saveAll(listOf(task1, task2, task3, task4, task5))
        flushAndClear()
        val stat = goodsPingMpStatProvider.getStatByPartner()
        val expectedStat = mapOf(PARTNER_ID to MpStat(4, 2, false))
        assertEquals(expectedStat, stat)
    }


    private fun createTask(httpStatus: Int, contentSize: Long, createdMinutesAgo: Long) = PingerContentTask().apply {
        this.contentSize = contentSize
        this.httpStatus = httpStatus
        this.creationTime = LocalDateTime.now().minusMinutes(createdMinutesAgo)
        this.genId = GEN.id
        this.shopId = 1L
        this.result = true
    }

    companion object {
        private const val PARTNER_ID = 1L
        private val GEN = MpGeneratorType.GOODS_PING
    }
}

