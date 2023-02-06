package ru.yandex.market.abo.core.lom

import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.EmptyTest
import ru.yandex.market.abo.util.db.batch.PgBatchUpdater
import ru.yandex.market.abo.util.kotlin.atEndOfDay

/**
 * @author Aleksei Neliubin (neliubin@yandex-team.ru)
 * @date 04.10.2021
 */
open class LomOrderUpdaterTest @Autowired constructor(
    private val lomOrderBatchUpdater: PgBatchUpdater<LomOrder>,
    private val lomOrderRepo: LomOrderRepo
) : EmptyTest() {
    @Test
    fun `save test`() {
        val order = LomOrder(
            id = 1,
            checkouterOrderId = 1111,
            creationTime = LocalDateTime.now(),
            logisticPointId = 123,
            isDropoff = true,
            isIntake = true
        )
        lomOrderBatchUpdater.insertWithoutUpdate(listOf(order))
        val dbOrder = lomOrderRepo.findByIdOrNull(order.id)
        assertThat(order)
            .usingRecursiveComparison().isEqualTo(dbOrder)
    }

    @Test
    fun `update estimated intake time test`() {
        val order = LomOrder(
            id = 1,
            checkouterOrderId = 1111,
            creationTime = LocalDateTime.now(),
            logisticPointId = 123,
            isDropoff = true,
            isIntake = true
        )
        lomOrderBatchUpdater.insertWithoutUpdate(listOf(order))
        flushAndClear()
        val estimatedIntakeTime = LocalDate.now().atEndOfDay()
        lomOrderRepo.updateEstimatedIntakeTime(order.id, estimatedIntakeTime)
        val dbOrder = lomOrderRepo.findByIdOrNull(order.id)
        assertThat(order)
            .usingRecursiveComparison(
                RecursiveComparisonConfiguration.builder().withIgnoredFields("estimatedIntakeTime").build()
            ).isEqualTo(dbOrder)
        assertEquals(estimatedIntakeTime, dbOrder?.estimatedIntakeTime)
    }
}
