package ru.yandex.market.abo.cpa.ow.arbitrage

import java.time.LocalDateTime
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater

open class OwArbitrageUpdaterTest @Autowired constructor(
    private val owArbitrageUpdater: PgBatchUpdater<OwArbitrage>,
    private val owArbitrageRepo: OwArbitrageRepo
) : EmptyTest() {

    @Test
    open fun `test save`() {
        val arbitrage = OwArbitrage(
            orderId = 10,
            shopId = 20,
            verdictDate = LocalDateTime.now()
        )
        owArbitrageUpdater.insertWithoutUpdate(listOf(arbitrage))
        val dbArbitrage = owArbitrageRepo.findByIdOrNull(arbitrage.orderId)
        assertThat(arbitrage)
            .usingRecursiveComparison().isEqualTo(dbArbitrage)
    }
}
