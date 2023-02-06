package ru.yandex.market.abo.core.spark.status

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.core.startrek.StartrekTicketManager

class SparkStatusManagerTest @Autowired constructor(
    val sparkStatusManager: SparkStatusManager,
) : EmptyTest() {
    @Test
    fun process() {
        val ticketManager: StartrekTicketManager = mock()
        sparkStatusManager.checkStatus()
    }
}
