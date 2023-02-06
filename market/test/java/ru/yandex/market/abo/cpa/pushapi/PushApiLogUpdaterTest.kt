package ru.yandex.market.abo.cpa.pushapi

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.checkout.checkouter.order.ApiSettings
import ru.yandex.market.checkout.checkouter.order.Context
import ru.yandex.market.checkout.checkouter.order.UserGroup

open class PushApiLogUpdaterTest @Autowired constructor(
    private val pushApiLogUpdater: PgBatchUpdater<PushApiLog>,
    private val pushApiLogRepo: PushApiLogRepo
) : EmptyTest() {

    @Test
    open fun updater() {
        val row = PushApiLog().apply {
            requestId = "1"
            eventTime = LocalDateTime.now()
            shopId = 2
            orderId = 3
            userId = 4
            isSuccess = true
            requestMethod = "5"
            responseTime = 6
            context = Context.CHECK_ORDER
            apiSettings = ApiSettings.SANDBOX
            userGroup = UserGroup.ABO
        }
        pushApiLogUpdater.insertOrUpdate(listOf(row))

        assertThat(pushApiLogRepo.findByIdOrNull("1"))
            .usingRecursiveComparison()
            .isEqualTo(row)
    }
}
