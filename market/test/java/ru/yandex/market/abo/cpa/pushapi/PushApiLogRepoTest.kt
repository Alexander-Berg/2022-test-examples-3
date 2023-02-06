package ru.yandex.market.abo.cpa.pushapi

import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest

class PushApiLogRepoTest @Autowired constructor(
    private val pushApiLogRepo: PushApiLogRepo
) : EmptyTest() {

    @Test
    fun deleteAllByEventTimeBeforeTest() {
        val now = LocalDateTime.now()
        val pushApiLogs = listOf(
            PushApiLog().apply {
                requestId = "1"
                eventTime = now
            },
            PushApiLog().apply {
                requestId = "2"
                eventTime = now.minusDays(2)
            }
        )
        pushApiLogRepo.saveAll(pushApiLogs)
        pushApiLogRepo.deleteAllByEventTimeBefore(now.minusDays(1))
        val dbApiLogs = pushApiLogRepo.findAll()
        assertEquals(1, dbApiLogs.size)
        assertEquals("1", dbApiLogs[0].requestId)
    }

    @Test
    fun deleteAllByEventTimeBeforeAndShopIdTest() {
        val now = LocalDateTime.now()
        val pushApiLogs = listOf(
            PushApiLog().apply {
                requestId = "1"
                eventTime = now
                shopId = 1
            },
            PushApiLog().apply {
                requestId = "2"
                eventTime = now.minusDays(2)
                shopId = 1
            },
            PushApiLog().apply {
                requestId = "3"
                eventTime = now
                shopId = 2
            },
            PushApiLog().apply {
                requestId = "4"
                eventTime = now.minusDays(2)
                shopId = 2
            },
        )
        pushApiLogRepo.saveAll(pushApiLogs)
        pushApiLogRepo.deleteAllByEventTimeBeforeAndShopId(now.minusDays(1), 2)
        val dbApiLogs = pushApiLogRepo.findAll()
        assertEquals(dbApiLogs.map { it.requestId }, listOf("1", "2", "3"))
    }
}
